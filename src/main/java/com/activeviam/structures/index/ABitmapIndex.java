/*
 * (C) ActiveViam 2010-2019
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.structures.index;

import com.activeviam.structures.bitmap.IBitmap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Bitmap index working on any implementation of {@link IBitmap}.
 *
 * @author ActiveViam
 */
public abstract class ABitmapIndex<B extends IBitmap> implements IWritableBitmapIndex {

	/**
	 * Index structure: bitmaps stored by level and by value within the level.
	 */
	protected LevelBitmapIndex[] index;

	/** The size of the index (number of indexed tuples) */
	protected int size;

	/**
	 * Constructor
	 *
	 * @param levels The number of indexed levels
	 */
	@SuppressWarnings("unchecked")
	public ABitmapIndex(final int levels) {
		this.index = new ABitmapIndex.LevelBitmapIndex[levels];
		this.size = 0;
		for (int l = 0; l < levels; ++l) {
			this.index[l] = createLevelIndex(l);
		}
	}

	/**
	 * Creates the {@link LevelBitmapIndex} for the given level.
	 *
	 * @param level A level
	 * @return Its associated {@link LevelBitmapIndex}
	 */
	protected LevelBitmapIndex createLevelIndex(final int level) {
		return new LevelBitmapIndex();
	}

	/**
	 * Creates an (empty) array of bitmaps.
	 *
	 * @param length The array length
	 * @return The created array
	 */
	protected abstract B[] createBitmapArray(final int length);

	@Override
	public IBitmap matchBitmap(final int[] pattern) {
		checkTuple(pattern);

		int predicateCount = countPredicates(pattern);
		if (predicateCount < 0) {
			// One of the predicates is not valid (i.e. does not
			// exist in this bitmap). Nothing is valid
			return createEmptyBitmap();

		} else if (predicateCount == 0) {
			// No predicate: all rows are valid
			return createOnesBitmap(this.size);

		} else {
			// Get all the predicated bitmaps to perform the AND operation on
			final B[] predicateBitmaps = createBitmapArray(predicateCount);
			predicateCount = 0;
			for (int lvl = 0, numLevels = this.index.length; lvl < numLevels; ++lvl) {
				final int predicate = pattern[lvl];
				if (predicate != ANY) {
					predicateBitmaps[predicateCount++] = getBitmap(lvl, predicate);
				}
			}

			return and(predicateBitmaps);
		}
	}

	@Override
	public IBitmap matchBitmap(final int[][] compositePattern) {
		boolean returnAll = true;
		IBitmap result = null;
		IBitmap cache = createBitmap();

		checkTuple(compositePattern);

		for (int lvl = 0; lvl < index.length; ++lvl) {

			// Use bitmap OR operations to determine
			// the rows that match any of the level predicates.
			int[] predicates = compositePattern[lvl];

			IBitmap levelResult = null;
			boolean any = false;
			for (int i = 0; i < predicates.length; ++i) {
				final int predicate = predicates[i];
				if (predicate != ANY) {

					// Load the right bitmap from the index
					IBitmap bitmap = getBitmap(lvl, predicate);
					if (bitmap == null) {
						continue;
					}

					if (levelResult == null) {
						levelResult = bitmap.clone();
					} else {
						cache.clear();
						levelResult.or(bitmap, cache);
						IBitmap temp = levelResult;
						levelResult = cache;
						cache = temp;
					}
				} else {
					// If one of the predicates is 'ANY' all the rows
					// match the composite predicate.
					any = true;
					levelResult = null;
					break;
				}
			}

			if (!any) {
				if (levelResult == null) {
					// No position for the current level
					return createEmptyBitmap();
				}

				returnAll = false;
				if (result == null) {
					result = levelResult;
				} else {
					cache.clear();
					result.and(levelResult, cache);
					IBitmap temp = result;
					result = cache;
					cache = temp;
				}
			}
		}

		if (returnAll) {
			return createOnesBitmap(this.size);
		} else {
			return result;
		}
	}

	@Override
	public final B getBitmap(final int level, final int position) {
		return getBitmap(level, position, false);
	}

	@Override
	public long sizeInBytes() {
		// 16: Object header
		// 8:  Reference to the levels array
		// 8:  Size attribute
		long sizeInBytes = 16 + 8 + 8;

		// 16: Header of the levels array object
		// 8 bytes for the size of the array
		// 8 bytes per reference stored in the level array
		sizeInBytes += 16;
		sizeInBytes += 8;
		sizeInBytes += 8 * this.index.length;

		// Add the size of each level index
		for (int l = 0, len = this.index.length; l < len; ++l) {
			sizeInBytes += this.index[l].sizeInBytes();
		}
		return sizeInBytes;
	}

	@Override
	public int append(final int[] tuple) {
		checkTuple(tuple);

		final int levels = index.length;
		for (int lvl = 0; lvl < levels; ++lvl) {
			final int value = tuple[lvl];
			IBitmap bitmap = getOrCreateBitmap(lvl, value);

			// Set the bit
			bitmap.set(size);
		}
		return ++size;
	}

	/**
	 * Retrieves and creates if necessary the bitmap stored at that position of that level.
	 *
	 * @param level The level
	 * @param position The position in that level
	 * @return the stored bitmap
	 * @see #getBitmap(int, int)
	 */
	protected final B getOrCreateBitmap(final int level, final int position) {
		return getBitmap(level, position, true);
	}

	/**
	 * Retrieves and creates if necessary the bitmap stored at that position of that level.
	 *
	 * @param level The level
	 * @param position The position in that level
	 * @param create {@code true} to create the bitmap if it does not exists
	 * @return the bitmap stored or {@code null} if it does not exist and {@code create} is
	 *         {@code false}
	 * @see #getBitmap(int, int)
	 * @see #getOrCreateBitmap(int, int)
	 */
	protected final B getBitmap(final int level, final int position, final boolean create) {
		return this.index[level].getBitmap(position, create);
	}

	/**
	 * Checks that the input predicate can be used by this index
	 *
	 * @param predicates A predicates tuple
	 */
	protected void checkTuple(final int[] predicates) {
		if (predicates == null) {
			throw new NullPointerException("The input should not be null");
		} else if (predicates.length != index.length) {
			throw new IllegalArgumentException("The input does not have the right size: " + predicates.length + " should be " + index.length);
		}
	}

	/**
	 * Checks that the input predicate can be used by this index
	 *
	 * @param compositePredicates A composite predicates tuple
	 */
	protected void checkTuple(final int[][] compositePredicates) {
		if (compositePredicates == null) {
			throw new NullPointerException("The input should not be null");
		} else if (compositePredicates.length != index.length) {
			throw new IllegalArgumentException("The input does not have the right size: " + compositePredicates.length + " should be " + index.length);
		}
	}

	/**
	 * Creates an empty bitmap to be used with this index.
	 * @return The created bitmap
	 */
	protected abstract B createBitmap();

	/**
	 * Creates an empty bitmap to be used with this index.
	 *
	 * <p>
	 * This should be the smallest possible empty bitmap.
	 *
	 * @return The created bitmap
	 */
	protected B createEmptyBitmap() {
		return createBitmap();
	}

	/**
	 * Creates a bitmap with all the bits set.
	 *
	 * @param size The size of the bitmap (in bits)
	 * @return The created bitmap
	 */
	protected abstract B createOnesBitmap(final int size);

	/**
	 * Counts the number of predicates different from ANY
	 * and for which there exist at least one indexed point.
	 *
	 * @param pattern A pattern indicating which value is set for each level
	 * @return the number of predicates (different from ANY) or -1 if a predicate is invalid
	 */
	protected int countPredicates(final int[] pattern) {
		// Count the predicates in the pattern
		int predicateCount = 0;

		for (int lvl = 0; lvl < index.length; ++lvl) {
			int predicate = pattern[lvl];
			if (predicate != ANY) {
				++predicateCount;
				if (!exists(lvl, predicate)) {
					return -1;
				}
			}
		}

		return predicateCount;
	}

	/**
	 * Tests if the position on that level is indexed at least once.
	 *
	 * @param level index of a level to test
	 * @param position position to check
	 * @return true if this position is indexed at least once.
	 */
	protected boolean exists(final int level, final int position) {
		return this.index[level].exists(position);
	}

	/**
	 * Performs a logical AND between the bitmaps and return the result.
	 *
	 * @param bitmaps The bitmaps to AND
	 * @return The resulting bitmap
	 */
	protected IBitmap and(final IBitmap... bitmaps) {
		if (bitmaps.length == 1) {
			return bitmaps[0].clone();
		} else {
			IBitmap result = bitmaps[0].and(bitmaps[1]);
			IBitmap cache = createBitmap();
			for (int i = 2; i < bitmaps.length; ++i) {
				cache.clear();
				result.and(bitmaps[i], cache);
				IBitmap tmp = cache;
				cache = result;
				result = tmp;
			}
			return result;
		}
	}

	@Override
	public int size() {
		return this.size;
	}

	/**
	 * The index for a specific level.
	 *
	 * @author ActiveViam
	 */
	protected class LevelBitmapIndex extends AtomicInteger {

		/** Order of the first bucket*/
		static final int FIRST_BUCKET_ORDER = 4;

		/** Length of the first bucket */
		static final int FIRST_BUCKET_LENGTH = 1 << FIRST_BUCKET_ORDER;

		/** For serialization (from superclass) */
		private static final long serialVersionUID = 1L;

		/** Interruption mask to check interruption in bulk methods */
		protected static final int INTERRUPTION_MASK = (1 << 7) - 1;

		/** The {@link IBitmap bitmaps} stored in this index for this level, bucketed */
		@SuppressWarnings("unchecked")
		protected final B[][] buckets = (B[][]) new IBitmap[32 - FIRST_BUCKET_ORDER][];

		/**
		 * Clears all bitmaps, truncating their size to {@code 0}.
		 */
		public void clear() {
			for (int bucketIdx = 0, numBuckets = this.buckets.length; bucketIdx < numBuckets; ++bucketIdx) {
				final B[] bucket = this.buckets[bucketIdx];
				if (bucket != null) {
					for (int b = 0, len = bucket.length; b < len; ++b) {
						final B bitmap = bucket[b];
						if(bitmap != null) {
							bitmap.clear();
						}
					}
				}
			}
		}

		/**
		 * Truncates all bitmaps.
		 */
		public void truncate(final int newSize) {
			for (int bucketIdx = 0, numBuckets = this.buckets.length; bucketIdx < numBuckets; ++bucketIdx) {
				final B[] bucket = this.buckets[bucketIdx];
				if (bucket != null) {
					for (int b = 0, len = bucket.length; b < len; ++b) {
						final B bitmap = bucket[b];
						if(bitmap != null) {
							bitmap.truncate(newSize);
						}
					}
				}
			}
		}

		/**
		 * Tests if the position is indexed at least once.
		 *
		 * @param position The position to check
		 * @return true if this position is indexed at least once.
		 */
		public boolean exists(final int position) {
			final int i = position + FIRST_BUCKET_LENGTH;
			final int high = 31 - Integer.numberOfLeadingZeros(i);
			final int bucket = high - FIRST_BUCKET_ORDER;
			final int idx = i ^ (1 << high);
			return (this.buckets[bucket] != null) && (this.buckets[bucket][idx] != null);
		}

		/**
		 * Retrieves and creates if necessary the bitmap stored at
		 * the given position.
		 *
		 * @param position The bitmap's position
		 * @param create <code>true</code> to create the bitmap if it does not exists
		 * @return the bitmap stored or <code>null</code> if it does not exist and <code>create</code>
		 *         is <code>false</code>
		 * @see #getBitmap(int, int)
		 * @see #getOrCreateBitmap(int, int)
		 */
		public B getBitmap(final int position, final boolean create) {
			final int i = position + FIRST_BUCKET_LENGTH;
			final int high = 31 - Integer.numberOfLeadingZeros(i);
			final int bucketIdx = high - FIRST_BUCKET_ORDER;

			// Get or create the bucket
			B[] bucket = this.buckets[bucketIdx];
			if (null == bucket) {
				if (create) {
					bucket = createBucket(bucketIdx);
				} else {
					return null;
				}
			}

			// Get or create the bitmap in the bucket
			final int idx = i ^ (1 << high);
			B bitmap = bucket[idx];
			if (null == bitmap) {
				if (create) {
					bitmap = createBitmap(bucket, idx);
				} else {
					return null;
				}
			}

			return bitmap;
		}

		/**
		 * Creates a new bucket
		 *
		 * @param bucket The bucket index
		 * @return The created bucket, or the existing value if it
		 *         alerady existed
		 */
		@SuppressWarnings("unchecked")
		protected B[] createBucket(final int bucket) {
			final B[][] levelArray = this.buckets;
			B[] bucketArray;
			if ((bucketArray = levelArray[bucket]) == null) {
				synchronized (levelArray) {
					if ((bucketArray = levelArray[bucket]) == null) {
						levelArray[bucket] = bucketArray = (B[]) new IBitmap[FIRST_BUCKET_LENGTH << bucket];
					}
				}
			}
			return bucketArray;
		}

		/**
		 * Creates a bitmap at the given index in the bucket
		 *
		 * @param bucket A bucket
		 * @param idx An index in the bucket
		 * @return The created bitmaps, or the existing one if a
		 *         bitmap already existed at that index.
		 */
		protected B createBitmap(final B[] bucket, final int idx) {
			B bitmap;
			if ((bitmap = bucket[idx]) == null) {
				synchronized (bucket) {
					if ((bitmap = bucket[idx]) == null) {
						// Create the bitmap
						bucket[idx] = bitmap = ABitmapIndex.this.createBitmap();
						// We've added a new bitmap.
						// Increment the count.
						getAndIncrement();
					}
				}

			}
			return bitmap;
		}

		/**
		 * Estimates the size in bytes of the index.
		 *
		 * @return The size in bytes of the index.
		 */
		public long sizeInBytes() {
			// 16: Object header
			// 8:  Reference to the bitmap array
			// 8:  Size attribute (AtomicInteger value field) + padding
			long sizeInBytes = 16 + 8 + 8;

			// 16: Header of the bitmap array object
			// 8 bytes for the size of the array
			// 8 bytes per reference stored in the bitmap array
			sizeInBytes += 16;
			sizeInBytes += 8;
			sizeInBytes += 8 * this.buckets.length;

			// Compute the size of each materialized bitmap
			for (int d = 0; d < buckets.length; ++d) {
				final IBitmap[] bucket = this.buckets[d];
				if (bucket != null) {

					// 16 bytes for each bitmap array header
					// 8 bytes for the size of each bitmap array
					// 8 bytes per reference stored in each bitmap sub array
					sizeInBytes += 16;
					sizeInBytes += 8;
					sizeInBytes += 8 * bucket.length;

					for (int c = 0; c < bucket.length; ++c) {
						if (bucket[c] != null) {
							sizeInBytes += bucket[c].sizeInBytes();
						}
					}
				}
			}
			return sizeInBytes;
		}

	}

}

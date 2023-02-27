package com.activeviam.structures.index;

import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.store.ITable;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A column imprint index.
 */
public abstract class AColumnImprintsIndex implements IWritableColumnImprintsIndex {

	/** The base of the table */
	protected final ITable base;

	protected final ReadWriteLock lock = new ReentrantReadWriteLock();

	/** Index structure */
	protected LevelColumnImprintsIndex[] index;

	/** The size of the index (number of indexed records) */
	protected int size;

	/**
	 * Constructor
	 *
	 * @param indexedFields The indexes of the fields to index
	 * @param base The base of the table to index
	 */
	public AColumnImprintsIndex(int[] indexedFields, ITable base) {
		this.base = base;
		this.index = new LevelColumnImprintsIndex[indexedFields.length];
		this.size = 0;
		for (int lvl = 0; lvl < indexedFields.length; ++lvl) {
			this.index[lvl] = createLevelIndex(indexedFields[lvl]);
		}
	}

	/**
	 * Creates the {@link LevelColumnImprintsIndex} for the given level.
	 *
	 * @param field The index of the field to index
	 * @return its associated {@link LevelColumnImprintsIndex}
	 */
	protected LevelColumnImprintsIndex createLevelIndex(final int field) {
		return new LevelColumnImprintsIndex(field);
	}

	@Override
	public IBitmap matchBitmap(final int[] pattern, final int size) {
		checkTuple(pattern);

		int predicateCount = countPredicates(pattern);
		if (predicateCount < 0) {
			// One of the predicates is not valid (i.e. does not
			// exist in this bitmap). Nothing is valid
			return createEmptyBitmap();
		}
		else if (predicateCount == 0) {
			// No predicate: all rows are valid
			return createOnesBitmap(this.size);
		}
		else {
			// Get all the predicated bitmaps to perform the AND operation on
			final IBitmap[] predicateBitmaps = createBitmapArray(predicateCount);
			predicateCount = 0;
			for (int lvl = 0; lvl < levels(); ++lvl) {
				final int predicate = pattern[lvl];
				if (predicate != ANY) {
					predicateBitmaps[predicateCount++] = getBitmap(lvl, predicate, size);
				}
			}

			return and(predicateBitmaps);
		}
	}

	@Override
	public IBitmap matchBitmap(final int[][] compositePattern, final int size) {
		checkTuple(compositePattern);

		final boolean[] anys = new boolean[levels()];
		int predicateCount = countPredicates(compositePattern, anys);
		if (predicateCount < 0) {
			// One of the predicates is not valid (i.e. does not
			// exist in this bitmap). Nothing is valid
			return createEmptyBitmap();
		}
		else if (predicateCount == 0) {
			// No predicate: all rows are valid
			return createOnesBitmap(this.size);
		}
		else {
			// Get all the predicated bitmaps to perform the AND operation on
			final IBitmap[] predicateBitmaps = createBitmapArray(predicateCount);
			predicateCount = 0;
			for (int lvl = 0; lvl < levels(); ++lvl) {
				final int[] predicates = compositePattern[lvl];
				if (!anys[lvl]) {
					predicateBitmaps[predicateCount++] = getBitmap(lvl, predicates, size);
				}
			}

			return and(predicateBitmaps);
		}
	}

	//@Override
	public IBitmap matchBitmapDeprecated(final int[][] compositePattern, final int size) {
		boolean returnAll = true;
		IBitmap result = null;
		IBitmap cache = createEmptyBitmap();

		checkTuple(compositePattern);

		for (int lvl = 0; lvl < index.length; ++lvl) {
			// Use bitmap OR operations to determine
			// the rows that match any of the level predicates.
			final int[] predicates = compositePattern[lvl];

			IBitmap levelResult = null;
			boolean any = false;
			for (int i = 0; i < predicates.length; ++i) {
				final int predicate = predicates[i];
				if (predicate != ANY) {
					// Create the right bitmap from the index
					final IBitmap bitmap = getBitmap(lvl, predicate, size);
					if (bitmap == null) {
						continue;
					}

					if (levelResult == null) {
						levelResult = bitmap.clone();
					}
					else {
						cache.clear();
						levelResult.or(bitmap, cache);
						IBitmap temp = levelResult;
						levelResult = cache;
						cache = temp;
					}
				}
				else {
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
				}
				else {
					cache.clear();
					result.and(levelResult, cache);
					IBitmap temp = result;
					result = cache;
					cache = temp;
				}
			}
		}

		if (returnAll) {
			return createOnesBitmap(size);
		}
		else {
			return result;
		}
	}

	@Override
	public long sizeInBytes() {
		// 16: Object header
		// 8:  Reference to the levels array
		// 8:  Size attribute
		// 8:  Reference to the base
		long sizeInBytes = 16 + 8 + 8 + 8;

		// 16: Header of the levels array object
		// 8 bytes for the size of the array
		// 8 bytes per reference stored in the level array
		sizeInBytes += 16;
		sizeInBytes += 8;
		sizeInBytes += 8 * levels();

		// Add the size of each level index
		for (int lvl = 0; lvl < levels(); ++lvl) {
			sizeInBytes += this.index[lvl].sizeInBytes();
		}
		return sizeInBytes;
	}

	@Override
	public int append(final int[] tuple) {
		checkTuple(tuple);

		for (int lvl = 0; lvl < levels(); ++lvl) {
			index[lvl].append(tuple[lvl]);
		}

		return ++size;
	}

	@Override
	public void rebuild() {
		// Rebuild some index levels if necessary
		for (int lvl = 0; lvl < levels(); ++lvl) {
			index[lvl].rebuild();
		}
	}

	@Override
	public void truncate(final int newSize,
			final int[] imprintVectorsSizes,
			final long[] lastImprintVectors,
			final int[] cachelineDictionariesSizes,
			final int[] lastCachelineDictionaries) {
		// Check if the new size is valid
		assert newSize <= size : "The new size of the index cannot be greater than its current size.";

		// Check if the size has changed
		if (newSize == size) {
			return;
		}

		// Reset size
		size = newSize;

		// Check array parameters sizes
		final int numLevels = levels();
		if (imprintVectorsSizes.length != numLevels ||
				lastImprintVectors.length != numLevels ||
				cachelineDictionariesSizes.length != numLevels ||
				lastCachelineDictionaries.length != numLevels) {
			throw new IllegalArgumentException("The array sizes should be equal to " + numLevels);
		}

		// Trucate each level
		for (int lvl = 0; lvl < numLevels; ++lvl) {
			index[lvl].truncate(newSize,
					imprintVectorsSizes[lvl],
					lastImprintVectors[lvl],
					cachelineDictionariesSizes[lvl],
					lastCachelineDictionaries[lvl]);
		}
	}

	@Override
	public int size() {
		return this.size;
	}

	/**
	 * Gets the number of indexed fields.
	 *
	 * @return the number of indexed fields
	 */
	public int levels() {
		return index.length;
	}

	/**
	 * Gets the size of imprint vectors array of that level.
	 *
	 * @param level A level
	 * @return the number of imprint vectors
	 */
	public int imprintVectorsSize(final int level) {
		return index[level].imprintVectorsSize();
	}

	/**
	 * Gets the last imprint vector of that level.
	 *
	 * @param level A level
	 * @return the last imprint vector
	 */
	public long lastImprintVector(final int level) {
		return index[level].lastImprintVector();
	}

	/**
	 * Gets the size of cacheline dictionaries array of that level.
	 *
	 * @param level A level
	 * @return the number of cacheline dictionaries
	 */
	public int cachelineDictionariesSize(final int level) {
		return index[level].cachelineDictionariesSize();
	}

	/**
	 * Gets the last cacheline dictionary of that level.
	 *
	 * @param level A level
	 * @return the last cacheline dictionary
	 */
	public int lastCachelineDictionary(final int level) {
		return index[level].lastCachelineDictionary();
	}

	/**
	 * Checks that the input predicate can be used by this index.
	 *
	 * @param predicates A predicates tuple
	 */
	protected void checkTuple(final int[] predicates) {
		if (predicates == null) {
			throw new NullPointerException("The input should not be null");
		} else if (predicates.length != levels()) {
			throw new IllegalArgumentException("The input does not have the right size: " + predicates.length + " should be " + levels());
		}
	}

	/**
	 * Checks that the input predicate can be used by this index.
	 *
	 * @param compositePredicates A composite predicates tuple
	 */
	protected void checkTuple(final int[][] compositePredicates) {
		if (compositePredicates == null) {
			throw new NullPointerException("The input should not be null");
		} else if (compositePredicates.length != levels()) {
			throw new IllegalArgumentException("The input does not have the right size: " + compositePredicates.length + " should be " + levels());
		}
	}

	/**
	 * Creates a bitmap with an initial size to be used with this index.
	 *
	 * @param size The size of the bitmap (in bits)
	 * @return the created bitmap
	 */
	protected abstract IBitmap createBitmap(final int size);

	/**
	 * Creates an empty bitmap to be used with this index.
	 *
	 * <p>
	 *   This should be the smallest possible empty bitmap.
	 * </p>
	 *
	 * @return the created bitmap
	 */
	protected abstract IBitmap createEmptyBitmap();

	/**
	 * Creates a bitmap with all the bits set.
	 *
	 * @param size The size of the bitmap (in bits)
	 * @return the created bitmap
	 */
	protected abstract IBitmap createOnesBitmap(final int size);

	/**
	 * Creates an (empty) array of bitmaps.
	 *
	 * @param length The array length
	 * @return the created array
	 */
	protected abstract IBitmap[] createBitmapArray(final int length);

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

		for (int lvl = 0; lvl < levels(); ++lvl) {
			final int predicate = pattern[lvl];
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
	 * Counts the number of predicates different from ANY
	 * and for which there exist at least one indexed point.
	 *
	 * @param compositePattern A composite pattern indicating which values are set for each level
	 * @param anys The result array indicating if a predicates array holds the ANY value
	 * @return the number of predicates (different from ANY) or -1 if a predicate is invalid
	 */
	protected int countPredicates(final int[][] compositePattern, final boolean[] anys) {
		// Count the predicates in the pattern
		int predicateCount = 0;

		for (int lvl = 0; lvl < levels(); ++lvl) {
			final int[] predicates = compositePattern[lvl];
			boolean exist = false;
			for (int i = 0; i < predicates.length; ++i) {
				final int predicate = predicates[i];
				if (predicate == ANY) {
					anys[lvl] = true;
					break;
				}
				if (!exist && exists(lvl, predicate)) {
					exist = true;
				}
			}
			if (!anys[lvl]) {
				if (!exist) {
					return -1;
				}
				++predicateCount;
			}
		}

		return predicateCount;
	}

	/**
	 * Tests if the value on that level is potentially indexed.
	 *
	 * @param level Index of a level to test
	 * @param value The value to check
	 * @return true if this value is potentially indexed
	 */
	protected boolean exists(final int level, final int value) {
		if (value < 0) {
			return false;
		}
		return this.index[level].exists(value);
	}

	/**
	 * Performs a logical AND between the bitmaps and return the result.
	 *
	 * @param bitmaps The bitmaps to AND
	 * @return the resulting bitmap
	 */
	protected IBitmap and(final IBitmap... bitmaps) {
		if (bitmaps.length == 1) {
			return bitmaps[0].clone();
		} else {
			IBitmap result = bitmaps[0].and(bitmaps[1]);
			IBitmap cache = createEmptyBitmap();
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

	/**
	 * Gets a bitmap indicating the rows of the records of that level matching the value.
	 *
	 * @param level A level
	 * @param value The value to match in the level
	 * @param size The size of the table
	 * @return the bitmap
	 */
	protected final IBitmap getBitmap(final int level, final int value, int size) {
		return this.index[level].getBitmap(value, size);
	}

	/**
	 * Gets a bitmap indicating the rows of the records of that level matching one of the values.
	 *
	 * @param level A level
	 * @param values The values to match in the level
	 * @param size The size of the table
	 * @return the bitmap
	 */
	protected final IBitmap getBitmap(final int level, final int[] values, int size) {
		return this.index[level].getBitmap(values, size);
	}

	/**
	 * Reads the attribute of a record.
	 *
	 * @param row the row of the record to read
	 * @param column the index of the attribute to read
	 * @return the value of the attribute
	 */
	protected int readInt(final int row, final int column) {
		return base.readInt(row,  column);
	}

	/**
	 * Reads attribute of records in the base.
	 *
	 * @param row The first row of the records to read
	 * @param column The index of the attribute to read
	 * @param number The number of records to read from first row
	 * @param result The array holding the values of the attribute to read
	 */
	protected void readInts(final int row, final int column, final int number, final int[] result) {
		base.readInts(row, column, number, result);
	}

	/**
	 * The index for a specific level.
	 */
	protected class LevelColumnImprintsIndex extends AtomicInteger {

		/** Cacheline order, at the power of 2 it is the cacheline size */
		protected static final int CACHELINE_ORDER = 6;

		/** Number of records covered by an imprint vector */
		protected static final int CACHELINE_LENGTH = 1 << (CACHELINE_ORDER - 2);

		/** Size of an imprint vector in bits (long = 64 bits) */
		protected static final int IMPRINT_VECTOR_SIZE = 1 << 6;

		/** Initial order, at the power of 2 it is the initial size of cacheline dictionaries and imprint vectors arrays */
		protected static final int INITIAL_ORDER = 6;

		/** The maximal percentage of values which can fall into the last bin before rebuilding the index */
		protected static final double BINNING_TOLERANCE = 0.05D;

		/** The number of indexed records from which using the sampled binning method */
		protected static final int SAMPLE_TOLERANCE = 20_480;

		/** The percentage of values in the table to sample when rebuilding the index */
		protected static final double SAMPLE_PERCENTAGE = 0.1D;

		/** The value to add to increment a cacheline dictionary counter of 1 */
		protected static final int COUNT_INCREMENT = 0b10;

		/** Repeat mask to manipulate cacheline dictionaries */
		protected static final int REPEAT_MASK = 0b1;

		/** For serialization (from superclass) */
		protected static final long serialVersionUID = 1L;

		/** The size of the index level (number of indexed records) */
		protected int size;

		/** The index of the indexed field */
		protected final int field;

		/** Array of imprint vectors */
		protected long[] imprintVectors;

		/** The number of imprit vectors in {@link #imprintVectors} */
		protected int imprintVectorsSize;

		/** Array of cacheline dictionaries */
		protected int[] cachelineDictionaries;

		/** The number of elements in {@link #cachelineDictionaries} */
		protected int cachelineDictionariesSize;

		/** Array holding the bounds of the bins */
		protected int[] bins = new int[IMPRINT_VECTOR_SIZE - 1];

		/** Maximum value of the level */
		protected int maxValue;

		/** Charge of the last bin */
		protected int lastBinCharge;

		/**
		 * Constructor
		 *
		 * @param field The index of the indexed field
		 */
		public LevelColumnImprintsIndex(final int field) {
			this.field = field;
			size = 0;
			cachelineDictionaries = new int[1 << INITIAL_ORDER];
			imprintVectors = new long[1 << INITIAL_ORDER];
			cachelineDictionariesSize = 0;
			imprintVectorsSize = 0;
			maxValue = -1;
			lastBinCharge = 0;

			// Initialize bins
			for (int i = 0; i < IMPRINT_VECTOR_SIZE - 1; ++i) {
				bins[i] = i + 1;
			}
		}

		/**
		 * Appends the value of a record to the index.
		 *
		 * @param value The value to append
		 */
		public void append(final int value) {
			// Check if the value to append is valid
			assert value >= 0 : "Negative values are not supported by this implementation";

			// We could lock on a smaller section
			lock.writeLock().lock();
			try {
				// Update size
				++size;

				// Update max value
				if (value > maxValue) {
					maxValue = value;
				}

				// Get bin of value
				final int bin = getBin(bins, value);
				if (bin == IMPRINT_VECTOR_SIZE - 1) {
					++lastBinCharge;
				}

				// Create new imprint vector and cacheline dictionary
				if (size % CACHELINE_LENGTH == 1) {
					// Double size of imprint vectors array if it's already filled
					if (imprintVectorsSize == imprintVectors.length) {
						imprintVectors = Arrays.copyOf(imprintVectors, imprintVectorsSize << 1);
					}

					// Append new imprint vector and update size of imprint vectors array
					imprintVectors[imprintVectorsSize] = 1L << bin;

					// Update size of imprint vectors array
					++imprintVectorsSize;

					// Double size of cacheline dictionaries array if it's already filled
					if (cachelineDictionariesSize == cachelineDictionaries.length) {
						cachelineDictionaries = Arrays.copyOf(cachelineDictionaries, cachelineDictionariesSize << 1);
					}

					// Append new cacheline dictionary and update size of cacheline dictionaries array
					cachelineDictionaries[cachelineDictionariesSize] = COUNT_INCREMENT;

					// Update size of cacheline dictionaries array
					++cachelineDictionariesSize;
				}

				// Update last imprint vector and cacheline dictionary
				else {
					// Update last imprint vector
					imprintVectors[imprintVectorsSize - 1] = updateImprintVector(imprintVectors[imprintVectorsSize - 1], bin);

					// Update cacheline dictionaries array
					if (size % CACHELINE_LENGTH == 0 && imprintVectorsSize >= 2) {
						// Get previous cacheline dictionary
						final int cachelineDictionary = cachelineDictionaries[cachelineDictionariesSize - 2];

						// Update last cacheline dictionaries
						if (imprintVectors[imprintVectorsSize - 1] == imprintVectors[imprintVectorsSize - 2]) {
							if (isRepeat(cachelineDictionary)) {
								cachelineDictionaries[cachelineDictionariesSize - 2] = incrementCounter(cachelineDictionary);

								// Update size of cacheline dictionaries array
								--cachelineDictionariesSize;
							}
							else if (getCounter(cachelineDictionary) == 1) {
								cachelineDictionaries[cachelineDictionariesSize - 2] = setRepeat(incrementCounter(cachelineDictionary));

								// Update size of cacheline dictionaries array
								--cachelineDictionariesSize;
							}
							else {
								cachelineDictionaries[cachelineDictionariesSize - 2] = decrementCounter(cachelineDictionary);
								cachelineDictionaries[cachelineDictionariesSize - 1] = setRepeat(2 * COUNT_INCREMENT);
							}

							// Update size of imprint vectors array
							--imprintVectorsSize;
						}
						else if (!isRepeat(cachelineDictionary)) {
							cachelineDictionaries[cachelineDictionariesSize - 2] = incrementCounter(cachelineDictionary);

							// Update size of cacheline dictionaries array
							--cachelineDictionariesSize;
						}
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}

		/**
		 * Rebuilds the index level if necessary.
		 */
		public void rebuild() {
			// Check if the level needs to be rebuilt
			if (maxValue > bins[IMPRINT_VECTOR_SIZE - 2] && lastBinCharge > (int) (size * BINNING_TOLERANCE)) {
				// Check which binning method to use
				if (size < SAMPLE_TOLERANCE) {
					binning();
				}
				else {
					sampledBinning();
				}
			}
		}

		/**
		 * Truncates the column imprints level index.
		 *
		 * @param newSize The new size of the index
		 * @param imprintVectorsSize The new size of imprint vectors array
		 * @param lastImprintVector The last imprint vector
		 * @param cachelineDictionariesSize The new size of cacheline dictionaries array
		 * @param lastCachelineDictionary The last cacheline dictionary
		 */
		public void truncate(final int newSize,
				final int imprintVectorsSize,
				final long lastImprintVector,
				final int cachelineDictionariesSize,
				final int lastCachelineDictionary) {
			lock.writeLock().lock();
			try {
				// Update sizes
				size = newSize;
				this.imprintVectorsSize = imprintVectorsSize;
				this.cachelineDictionariesSize = cachelineDictionariesSize;

				// Update last imprint vector and cacheline dictionary
				if (cachelineDictionariesSize > 0) {
					if (newSize % CACHELINE_LENGTH == 0) {
						cachelineDictionaries[cachelineDictionariesSize - 1] = lastCachelineDictionary;
					}
					else {
						imprintVectors[imprintVectorsSize - 1] = lastImprintVector;
						if (cachelineDictionariesSize > 1) {
							cachelineDictionaries[cachelineDictionariesSize - 2] = lastCachelineDictionary;
						}
						cachelineDictionaries[cachelineDictionariesSize - 1] = COUNT_INCREMENT;
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}

		/**
		 * Tests if the value is potentially indexed.
		 *
		 * @param value The value to check
		 * @return true if this value is potentially indexed
		 */
		protected boolean exists(final int value) {
			return value <= maxValue;
		}

		/**
		 * Creates a bitmap indicating the records matching the value.
		 *
		 * @param value The value to match
		 * @param size The size of the table
		 * @return the bitmap
		 */
		public IBitmap getBitmap(final int value, int size) {
			// Take a snapshot of the index
			final int actualSize;
			final int[] bins;
			final int[] cachelineDictionaries;
			final long[] imprintVectors;
			final int lastCachelineDictionary;
			final long lastImprintVector;
			final int cachelineDictionariesSize;
			final int imprintVectorsSize;
			lock.readLock().lock();
			try {
				actualSize = this.size;
				bins = this.bins;
				cachelineDictionaries = this.cachelineDictionaries;
				imprintVectors = this.imprintVectors;
				lastCachelineDictionary = lastCachelineDictionary();
				lastImprintVector = lastImprintVector();
				cachelineDictionariesSize = this.cachelineDictionariesSize;
				imprintVectorsSize = this.imprintVectorsSize;
			} finally {
				lock.readLock().unlock();
			}

			// Set size accordingly to cacheline length
			if (size % CACHELINE_LENGTH > 0) {
				size += CACHELINE_LENGTH - (size % CACHELINE_LENGTH);
			}

			// Create bitmap
			IBitmap bitmap = AColumnImprintsIndex.this.createBitmap(size);

			// Creates the query
			final int bin = getBin(bins, value);
			final long query = 1L << bin;

			// Initialize variables
			int imprintIndex = 0;
			long imprintVector;
			int row = 0;
			final int[] cache = new int[CACHELINE_LENGTH];

			for (int cd = 0; cd < cachelineDictionariesSize; ++cd) {
				// Get cacheline dictionary
				int cacheDict = cachelineDictionaries[cd];
				if (cd >= cachelineDictionariesSize - 2) {
					if (actualSize % CACHELINE_LENGTH == 0) {
						if (cd == cachelineDictionariesSize - 1) {
							cacheDict = lastCachelineDictionary;
						}
					}
					else {
						if (cd == cachelineDictionariesSize - 2) {
							cacheDict = lastCachelineDictionary;
						}
						else {
							cacheDict = COUNT_INCREMENT;
						}
					}
				}
				final int counter = getCounter(cacheDict);

				// If repeat value of cacheline dictionary is set to true
				if (isRepeat(cacheDict)) {
					// Get imprint vector
					imprintVector = imprintIndex < imprintVectorsSize - 1 ? imprintVectors[imprintIndex] : lastImprintVector;
					++imprintIndex;

					// Check the imprint vector
					if ((query & imprintVector) != 0) {
						for (int i = 0; i < counter; ++i) {
							AColumnImprintsIndex.this.readInts(row, field, CACHELINE_LENGTH, cache);
							for (int j = 0; j < CACHELINE_LENGTH; ++j) {
								if (cache[j] == value) {
									bitmap.set(row + j);
								}
							}

							// Update row count and stop the search if the count exceed the size
							row += CACHELINE_LENGTH;
							if (row >= size) {
								break;
							}
						}
					}
					else {
						// Update row count
						row += CACHELINE_LENGTH * counter;
					}
				}

				else {
					for (int i = 0; i < counter; ++i) {
						// Get imprint vector
						imprintVector = imprintIndex < imprintVectorsSize - 1 ? imprintVectors[imprintIndex] : lastImprintVector;
						++imprintIndex;

						// Check the imprint vector
						if ((query & imprintVector) != 0) {
							AColumnImprintsIndex.this.readInts(row, field, CACHELINE_LENGTH, cache);
							for (int j = 0; j < CACHELINE_LENGTH; ++j) {
								if (cache[j] == value) {
									bitmap.set(row + j);
								}
							}
						}

						// Update row count and stop the search if the count exceed the size
						row += CACHELINE_LENGTH;
						if (row >= size) {
							break;
						}
					}
				}

				// Stop the search if the row count exceed the size
				if (row >= size) {
					break;
				}
			}

			return bitmap;
		}

		/**
		 * Creates a bitmap indicating the records matching one of the values.
		 *
		 * @param values The values to match
		 * @param size The size of the table
		 * @return the bitmap
		 */
		public IBitmap getBitmap(final int[] values, int size) {
			// Take a snapshot of the index
			final int actualSize;
			final int[] bins;
			final int[] cachelineDictionaries;
			final long[] imprintVectors;
			final int lastCachelineDictionary;
			final long lastImprintVector;
			final int cachelineDictionariesSize;
			final int imprintVectorsSize;
			lock.readLock().lock();
			try {
				actualSize = this.size;
				bins = this.bins;
				cachelineDictionaries = this.cachelineDictionaries;
				imprintVectors = this.imprintVectors;
				lastCachelineDictionary = lastCachelineDictionary();
				lastImprintVector = lastImprintVector();
				cachelineDictionariesSize = this.cachelineDictionariesSize;
				imprintVectorsSize = this.imprintVectorsSize;
			} finally {
				lock.readLock().unlock();
			}

			// Set size accordingly to cacheline length
			if (size % CACHELINE_LENGTH > 0) {
				size += CACHELINE_LENGTH - (size % CACHELINE_LENGTH);
			}

			// Create bitmap
			IBitmap bitmap = AColumnImprintsIndex.this.createBitmap(size);

			// Creates the query
			long query = 0L;
			for (int value : values) {
				final int bin = getBin(bins, value);
				query = query | (1L << bin);
			}

			// Initialize variables
			int imprintIndex = 0;
			long imprintVector;
			int row = 0;
			final int[] cache = new int[CACHELINE_LENGTH];

			for (int cd = 0; cd < cachelineDictionariesSize; ++cd) {
				// Get cacheline dictionary
				int cacheDict = cachelineDictionaries[cd];
				if (cd >= cachelineDictionariesSize - 2) {
					if (actualSize % CACHELINE_LENGTH == 0) {
						if (cd == cachelineDictionariesSize - 1) {
							cacheDict = lastCachelineDictionary;
						}
					}
					else {
						if (cd == cachelineDictionariesSize - 2) {
							cacheDict = lastCachelineDictionary;
						}
						else {
							cacheDict = COUNT_INCREMENT;
						}
					}
				}
				final int counter = getCounter(cacheDict);

				// If repeat value of cacheline dictionary is set to true
				if (isRepeat(cacheDict)) {
					// Get imprint vector
					imprintVector = imprintIndex < imprintVectorsSize - 1 ? imprintVectors[imprintIndex] : lastImprintVector;
					++imprintIndex;

					// Check the imprint vector
					if ((query & imprintVector) != 0) {
						for (int i = 0; i < counter; ++i) {
							AColumnImprintsIndex.this.readInts(row, field, CACHELINE_LENGTH, cache);
							for (int j = 0; j < CACHELINE_LENGTH; ++j) {
								final int value = cache[j];
								for (int targetValue : values) {
									if (value == targetValue) {
										bitmap.set(row + j);
										break;
									}
								}
							}

							// Update row count and stop the search if the count exceed the size
							row += CACHELINE_LENGTH;
							if (row >= size) {
								break;
							}
						}
					}
					else {
						// Update row count
						row += CACHELINE_LENGTH * counter;
					}
				}

				else {
					for (int i = 0; i < counter; ++i) {
						// Get imprint vector
						imprintVector = imprintIndex < imprintVectorsSize - 1 ? imprintVectors[imprintIndex] : lastImprintVector;
						++imprintIndex;

						// Check the imprint vector
						if ((query & imprintVector) != 0) {
							AColumnImprintsIndex.this.readInts(row, field, CACHELINE_LENGTH, cache);
							for (int j = 0; j < CACHELINE_LENGTH; ++j) {
								final int value = cache[j];
								for (int targetValue : values) {
									if (value == targetValue) {
										bitmap.set(row + j);
										break;
									}
								}
							}
						}

						// Update row count and stop the search if the count exceed the size
						row += CACHELINE_LENGTH;
						if (row >= size) {
							break;
						}
					}
				}

				// Stop the search if the row count exceed the size
				if (row >= size) {
					break;
				}
			}

			return bitmap;
		}

		/**
		 * Gets the size of imprint vectors array.
		 *
		 * @return the number of imprint vectors
		 */
		public int imprintVectorsSize() {
			return imprintVectorsSize;
		}

		/**
		 * Gets the last imprint vector.
		 *
		 * @return the last imprint vector
		 */
		public long lastImprintVector() {
			return imprintVectorsSize == 0 ? 0L : imprintVectors[imprintVectorsSize - 1];
		}

		/**
		 * Gets the size of cacheline dictionaries array.
		 *
		 * @return the number of cacheline dictionaries
		 */
		public int cachelineDictionariesSize() {
			return cachelineDictionariesSize;
		}

		/**
		 * Gets the last cacheline dictionary.
		 *
		 * @return the last cacheline dictionary
		 */
		public int lastCachelineDictionary() {
			if (cachelineDictionariesSize > 0) {
				if (size % CACHELINE_LENGTH == 0) {
					return cachelineDictionaries[cachelineDictionariesSize - 1];
				}
				else if (cachelineDictionariesSize > 1) {
					return cachelineDictionaries[cachelineDictionariesSize - 2];
				}
			}
			return 0;
		}

		/**
		 * Determines the bin the value falls into.
		 *
		 * @param bins The bin distribution
		 * @param value The value
		 * @return the index of the bin array
		 */
		protected int getBinDeprecated(final int[] bins, final int value) {
			// Check if the binning is trivial
			if (maxValue < IMPRINT_VECTOR_SIZE) {
				return value;
			}

			// Check if the value falls into the "infinite" bin
			if (value >= bins[IMPRINT_VECTOR_SIZE - 2]) {
				return IMPRINT_VECTOR_SIZE - 1;
			}

			// Dichotomic search
			int left = 0, right = IMPRINT_VECTOR_SIZE - 2;
			int middle = (left + right + 1) / 2;
			while (left < right) {
				if (value < bins[middle - 1]) {
					right = middle - 1;
				}
				else if (value >= bins[middle]) {
					left = middle + 1;
				}
				else {
					return middle;
				}
				middle = (left + right + 1) / 2;
			}

			return middle;
		}

		/**
		 * Determines the bin the value falls into.
		 * Inline version of the dichotomic search
		 * for IMPRINT_VECTOR_SIZE = 64.
		 *
		 * @param bins The bin distribution
		 * @param value The value
		 * @return the index of the bin array
		 */
		protected int getBin(final int[] bins, final int value) {
			// Check if the binning is trivial
			if (maxValue < 64) {
				return value;
			}

			// Check if the value falls into the "infinite" bin
			if (value >= bins[62]) {
				return 63;
			}

			// Inline dichotomic search
			// left = 0, right = 62, middle = 31
			if (value < bins[30]) {
				// left = 0, right = 30, middle = 15
				if (value < bins[14]) {
					// left = 0, right = 14, middle = 7
					if (value < bins[6]) {
						// left = 0, right = 6, middle = 3
						if (value < bins[2]) {
							// left = 0, right = 2, middle = 1
							if (value < bins[0]) {
								return 0;
							} else if (value >= bins[1]) {
								return 2;
							} else {
								return 1;
							}
						} else if (value >= bins[3]) {
							// left = 4, right = 6, middle = 5
							if (value < bins[4]) {
								return 4;
							} else if (value >= bins[5]) {
								return 6;
							} else {
								return 5;
							}
						} else {
							return 3;
						}
					} else if (value >= bins[7]) {
						// left = 8, right = 14, middle = 11
						if (value < bins[10]) {
							// left = 8, right = 10, middle = 9
							if (value < bins[8]) {
								return 8;
							} else if (value >= bins[9]) {
								return 10;
							} else {
								return 9;
							}
						} else if (value >= bins[11]) {
							// left = 12, right = 14, middle = 13
							if (value < bins[12]) {
								return 12;
							} else if (value >= bins[13]) {
								return 14;
							} else {
								return 13;
							}
						} else {
							return 11;
						}
					} else {
						return 7;
					}
				} else if (value >= bins[15]) {
					// left = 16, right = 30, middle = 23
					if (value < bins[22]) {
						// left = 16, right = 22, middle = 19
						if (value < bins[18]) {
							// left = 16, right = 18, middle = 17
							if (value < bins[16]) {
								return 16;
							} else if (value >= bins[17]) {
								return 18;
							} else {
								return 17;
							}
						} else if (value >= bins[19]) {
							// left = 20, right = 22, middle = 21
							if (value < bins[20]) {
								return 20;
							} else if (value >= bins[21]) {
								return 22;
							} else {
								return 21;
							}
						} else {
							return 19;
						}
					} else if (value >= bins[23]) {
						// left = 24, right = 30, middle = 27
						if (value < bins[26]) {
							// left = 24, right = 26, middle = 25
							if (value < bins[24]) {
								return 24;
							} else if (value >= bins[25]) {
								return 26;
							} else {
								return 25;
							}
						} else if (value >= bins[27]) {
							// left = 28, right = 30, middle = 29
							if (value < bins[28]) {
								return 28;
							} else if (value >= bins[29]) {
								return 30;
							} else {
								return 29;
							}
						} else {
							return 27;
						}
					} else {
						return 23;
					}
				} else {
					return 15;
				}
			} else if (value >= bins[31]) {
				// left = 32, right = 62, middle = 47
				if (value < bins[46]) {
					// left = 32, right = 46, middle = 39
					if (value < bins[38]) {
						// left = 32, right = 38, middle = 35
						if (value < bins[34]) {
							// left = 32, right = 34, middle = 33
							if (value < bins[32]) {
								return 32;
							} else if (value >= bins[33]) {
								return 34;
							} else {
								return 33;
							}
						} else if (value >= bins[35]) {
							// left = 36, right = 38, middle = 37
							if (value < bins[36]) {
								return 36;
							} else if (value >= bins[37]) {
								return 38;
							} else {
								return 37;
							}
						} else {
							return 35;
						}
					} else if (value >= bins[39]) {
						// left = 40, right = 46, middle = 43
						if (value < bins[42]) {
							// left = 40, right = 42, middle = 41
							if (value < bins[40]) {
								return 40;
							} else if (value >= bins[41]) {
								return 42;
							} else {
								return 41;
							}
						} else if (value >= bins[43]) {
							// left = 44, right = 46, middle = 45
							if (value < bins[44]) {
								return 44;
							} else if (value >= bins[45]) {
								return 46;
							} else {
								return 45;
							}
						} else {
							return 43;
						}
					} else {
						return 39;
					}
				} else if (value >= bins[47]) {
					// left = 48, right = 62, middle = 55
					if (value < bins[54]) {
						// left = 48, right = 54, middle = 51
						if (value < bins[50]) {
							// left = 48, right = 50, middle = 49
							if (value < bins[48]) {
								return 48;
							} else if (value >= bins[49]) {
								return 50;
							} else {
								return 49;
							}
						} else if (value >= bins[51]) {
							// left = 52, right = 54, middle = 53
							if (value < bins[52]) {
								return 52;
							} else if (value >= bins[53]) {
								return 54;
							} else {
								return 53;
							}
						} else {
							return 51;
						}
					} else if (value >= bins[55]) {
						// left = 56, right = 62, middle = 59
						if (value < bins[58]) {
							// left = 56, right = 58, middle = 57
							if (value < bins[56]) {
								return 56;
							} else if (value >= bins[57]) {
								return 58;
							} else {
								return 57;
							}
						} else if (value >= bins[59]) {
							// left = 60, right = 62, middle = 61
							if (value < bins[60]) {
								return 60;
							} else if (value >= bins[61]) {
								return 62;
							} else {
								return 61;
							}
						} else {
							return 59;
						}
					} else {
						return 55;
					}
				} else {
					return 47;
				}
			} else {
				return 31;
			}
		}

		/**
		 * Defines the ranges of the bins.
		 */
		protected void binning() {
			// Create histogram of values of the indexed field
			final int[] histogram = new int[maxValue + 1];
			int number = CACHELINE_LENGTH;
			final int[] cache = new int[CACHELINE_LENGTH];
			for (int row = 0; row < size; row += CACHELINE_LENGTH) {
				if (row + CACHELINE_LENGTH > size) {
					number = size % CACHELINE_LENGTH;
				}
				AColumnImprintsIndex.this.readInts(row, field, number, cache);
				for (int i = 0; i < number; ++i) {
					++histogram[cache[i]];
				}
			}

			// Calculate new bins according to the histogram
			final int[] bins = new int[IMPRINT_VECTOR_SIZE - 1];
			int bin = 0;
			int counter = 0;
			final double dstep = ((double) size) / (IMPRINT_VECTOR_SIZE - 1);
			double step = dstep;
			for (int idx = 0; idx <= maxValue; ++idx) {
				counter += histogram[idx];
				if (counter > (int) step) {
					bins[bin] = idx + 1;
					++bin;
					step += dstep;
				}
			}

			// Fill in last bins
			final int dbin = bin < IMPRINT_VECTOR_SIZE - 1 ? (maxValue + 1 - bins[bin - 1]) / (IMPRINT_VECTOR_SIZE - bin - 1) : 1;
			for (int i = bin; i < IMPRINT_VECTOR_SIZE - 1; ++i) {
				bins[i] = bins[i - 1] + dbin;
			}

			// Rebuild the index
			imprints(bins);
		}

		/**
		 * Defines the ranges of the bins with a sample of values.
		 */
		protected void sampledBinning() {
			// Get a sorted array of samples randomly got in the table
			final int samplesSize = (int) (size * SAMPLE_PERCENTAGE);
			final int[] samples = new int[samplesSize];
			final Random rand = new Random();
			for (int i = 0; i < samplesSize; ++i) {
				final int row = rand.nextInt(size);
				final int sample = AColumnImprintsIndex.this.readInt(row, field);
				samples[i] = sample;
			}
			Arrays.sort(samples);

			// Calculate new bins according to the samples
			final int[] bins = new int[IMPRINT_VECTOR_SIZE - 1];
			for (int idx = 0; idx < IMPRINT_VECTOR_SIZE - 1; ++idx) {
				final int step = ((idx + 1) * (samplesSize - 1)) / (IMPRINT_VECTOR_SIZE - 1);
				int bin = samples[step];
				if (idx > 0 && bin == bins[idx - 1]) {
					++bin;
				}
				bins[idx] = bin;
			}

			// Rebuild the index
			imprints(bins);
		}

		/**
		 * Rebuilds the column imprints index of this level
		 * according to a new bin distribution.
		 *
		 * @param bins The new bins of the index
		 */
		protected void imprints(final int[] bins) {
			// Create new index attributes
			int newSize = 0;
			int[] cachelineDictionaries = new int[1 << INITIAL_ORDER];
			long[] imprintVectors = new long[1 << INITIAL_ORDER];
			int cachelineDictionariesSize = 0;
			int imprintVectorsSize = 0;
			lastBinCharge = 0;

			// Append every value in the new index
			int number = CACHELINE_LENGTH;
			final int[] cache = new int[CACHELINE_LENGTH];
			for (int row = 0; row < size; row += CACHELINE_LENGTH) {
				if (row + CACHELINE_LENGTH > size) {
					number = size % CACHELINE_LENGTH;
				}
				AColumnImprintsIndex.this.readInts(row, field, number, cache);
				for (int i = 0; i < number; ++i) {
					// Get value
					final int value = cache[i];

					// Update size
					++newSize;

					// Get bin of value
					final int bin = getBin(bins, value);
					if (bin == IMPRINT_VECTOR_SIZE - 1) {
						++lastBinCharge;
					}

					// Create new imprint vector and cacheline dictionary
					if (newSize % CACHELINE_LENGTH == 1) {
						// Double size of imprint vectors array if it's already filled
						if (imprintVectorsSize == imprintVectors.length) {
							imprintVectors = Arrays.copyOf(imprintVectors, imprintVectorsSize << 1);
						}

						// Append new imprint vector and update size of imprint vectors array
						imprintVectors[imprintVectorsSize] = 1L << bin;

						// Update size of imprint vectors array
						++imprintVectorsSize;

						// Double size of cacheline dictionaries array if it's already filled
						if (cachelineDictionariesSize == cachelineDictionaries.length) {
							cachelineDictionaries = Arrays.copyOf(cachelineDictionaries, cachelineDictionariesSize << 1);
						}

						// Append new cacheline dictionary and update size of cacheline dictionaries array
						cachelineDictionaries[cachelineDictionariesSize] = COUNT_INCREMENT;

						// Update size of cacheline dictionaries array
						++cachelineDictionariesSize;
					}

					// Update last imprint vector and cacheline dictionary
					else {
						// Update last imprint vector
						imprintVectors[imprintVectorsSize - 1] = updateImprintVector(imprintVectors[imprintVectorsSize - 1], bin);

						// Update cacheline dictionaries array
						if (newSize % CACHELINE_LENGTH == 0 && imprintVectorsSize >= 2) {
							// Get previous cacheline dictionary
							final int cachelineDictionary = cachelineDictionaries[cachelineDictionariesSize - 2];

							// Update last cacheline dictionaries
							if (imprintVectors[imprintVectorsSize - 1] == imprintVectors[imprintVectorsSize - 2]) {
								if (isRepeat(cachelineDictionary)) {
									cachelineDictionaries[cachelineDictionariesSize - 2] = incrementCounter(cachelineDictionary);

									// Update size of cacheline dictionaries array
									--cachelineDictionariesSize;
								}
								else if (getCounter(cachelineDictionary) == 1) {
									cachelineDictionaries[cachelineDictionariesSize - 2] = setRepeat(incrementCounter(cachelineDictionary));

									// Update size of cacheline dictionaries array
									--cachelineDictionariesSize;
								}
								else {
									cachelineDictionaries[cachelineDictionariesSize - 2] = decrementCounter(cachelineDictionary);
									cachelineDictionaries[cachelineDictionariesSize - 1] = setRepeat(2 * COUNT_INCREMENT);
								}

								// Update size of imprint vectors array
								--imprintVectorsSize;
							}
							else if (!isRepeat(cachelineDictionary)) {
								cachelineDictionaries[cachelineDictionariesSize - 2] = incrementCounter(cachelineDictionary);

								// Update size of cacheline dictionaries array
								--cachelineDictionariesSize;
							}
						}
					}
				}
			}

			// Set new index attributes
			lock.writeLock().lock();
			try {
				this.bins = bins;
				this.cachelineDictionaries = cachelineDictionaries;
				this.imprintVectors = imprintVectors;
				this.cachelineDictionariesSize = cachelineDictionariesSize;
				this.imprintVectorsSize = imprintVectorsSize;
			} finally {
				lock.writeLock().unlock();
			}
		}

		/**
		 * Updates an imprint vector to index a new value.
		 *
		 * @param imprintVector The imprint vector to update
		 * @param bin The bin where the value falls into
		 * @return the updated imprint vector
		 */
		protected long updateImprintVector(final long imprintVector, final int bin) {
			return imprintVector | (1L << bin);
		}

		/**
		 * Tests if a cacheline dictionary is set as repeat.
		 *
		 * @param cachelineDictionary The cacheline dictionary to test
		 * @return true if the cacheline dictionary is set as repeat
		 */
		protected boolean isRepeat(final int cachelineDictionary) {
			return (cachelineDictionary & REPEAT_MASK) == REPEAT_MASK;
		}

		/**
		 * Sets the repeat value of a cacheline dictionary to true.
		 *
		 * @param cachelineDictionary The cacheline dictionary to set
		 * @return the updated cacheline dictionary
		 */
		protected int setRepeat(final int cachelineDictionary) {
			return cachelineDictionary | REPEAT_MASK;
		}

		/**
		 * Gets the counter value of a cacheline dictionary.
		 *
		 * @param cachelineDictionary The cacheline dictionary
		 * @return the counter value
		 */
		protected int getCounter(final int cachelineDictionary) {
			return cachelineDictionary >>> 1;
		}

		/**
		 * Increment the counter value of a cacheline dictionary.
		 *
		 * @param cachelineDictionary The cacheline dictionary to set
		 * @return the updated cacheline dictionary
		 */
		protected int incrementCounter(final int cachelineDictionary) {
			return cachelineDictionary + COUNT_INCREMENT;
		}

		/**
		 * Decrement the counter value of a cacheline dictionary.
		 *
		 * @param cachelineDictionary The cacheline dictionary to set
		 * @return the updated cacheline dictionary
		 */
		protected int decrementCounter(final int cachelineDictionary) {
			return cachelineDictionary - COUNT_INCREMENT;
		}

		/**
		 * Estimates the size in bytes of the index.
		 *
		 * <p>
		 *   This includes data as well as abject internal attributes, class pointers, ...
		 * </p>
		 *
		 * @return the size in bytes of the index.
		 */
		public long sizeInBytes() {
			// 16: Object header
			// 4:  size attribute
			// 4:  field attribute
			long sizeInBytes = 16 + 4 + 4;

			// 4:  Reference to imprintVectors
			// 12: Header of imprintVectors
			// 4:  Size of imprintVectors
			sizeInBytes += 4 + 12 + 4 + imprintVectors.length * 8;

			// 4:  Reference to cachelineDictionaries
			// 12: Header of cachelineDictionaries
			// 4:  Size of cachelineDictionaries
			sizeInBytes += 4 + 12 + 4 + cachelineDictionaries.length * 4;

			// 4:  Reference to bins
			// 12: Header of bins
			sizeInBytes += 4 + 12 + bins.length * 4;

			// 4:  maxValue attribute
			// 4:  lastBinCharge attribute
			sizeInBytes += 4 + 4;

			return sizeInBytes;
		}

	}

}

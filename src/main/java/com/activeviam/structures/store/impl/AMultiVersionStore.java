package com.activeviam.structures.store.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.impl.AMultiVersion;
import com.activeviam.structures.index.IMultiVersionSecondaryIndex;
import com.activeviam.structures.store.IMultiVersionStore;
import com.activeviam.structures.store.IStoreBase;
import com.activeviam.structures.store.IStoreTransaction;
import com.activeviam.structures.store.IStoreVersion;
import com.activeviam.structures.store.impl.ColumnarTable.TableFormat;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Framework to implement {@link IMultiVersionStore}.
 *
 * @author ActiveViam
 */
public abstract class AMultiVersionStore extends AMultiVersion<IStoreVersion, IStoreTransaction, IStoreBase>
		implements IMultiVersionStore {

	/**
	 * Constructor which does not impose the implementation of the
	 * {@link IMultiVersionSecondaryIndex}.
	 *
	 * @param base the base
	 */
	public AMultiVersionStore(AStoreBase base) {
		super(base);
	}

	@Override
	protected IStoreTransaction createNewTransaction() {
		return new StoreTransaction(base);
	}

	@Override
	public IStoreVersion commit(IEpoch epoch) {
		base.commit(epoch);
		return super.commit(epoch);
	}
	
	@Override
	protected IStoreVersion createVersion(IEpoch epoch, IStoreTransaction transaction) {
		return new StoreVersion(epoch, base.getTable().getMostRecentVersion(),
				base.getPrimaryIndex().getMostRecentVersion(), base.getSecondaryIndex().getMostRecentVersion());
	}

	@Override
	public long sizeInBytes() {
		// 16: Object header
		// 4: reference to the base
		// 4:                  Reference to the latest version
		return 16 + 4 + base.sizeInBytes() + 4;
	}

	@Override
	public void discardBefore(long epoch) {
		base.discardBefore(epoch);
	}

	public static class StoreFormat extends TableFormat {

		/**
		 * The fields used in the primary keys
		 */
		protected final int[] keyFields;

		/**
		 * The fields indexed in the secondary index of the {@link AMultiVersionStore}.
		 */
		protected final int[] indexedFields;

		public StoreFormat(int attributeCount, int valueCount, int[] keyFields, int chunkSize) {
			// Do not include key fields in the secondary index
			this(attributeCount, valueCount, keyFields,
					IntStream.range(0, attributeCount).filter(f -> Arrays.binarySearch(keyFields, f) < 0).toArray(), chunkSize);
		}

		public StoreFormat(int attributeCount, int valueCount, int[] keyFields, int[] indexedFields,
				int chunkSize) {
			super(attributeCount, valueCount, chunkSize);
			this.keyFields = keyFields;
			this.indexedFields = indexedFields;
		}
		
		/**
		 * Gets the total size of the table.
		 * 
		 * <p>
		 *   This includes data as well as abject internal attributes, class pointers, ...
		 * </p>
		 * 
		 * @return estimated size (in bytes) of the table
		 */
		public long sizeInBytes() {
			// 16: Object header
			// 4:  Reference to keyFields
			// 12: Header of keyFields
			long sizeInBytes = 4 + 12 + keyFields.length * 4;
			
			// 4:  Reference to indexedFields
			// 12: Header of indexedFields
			sizeInBytes += 4 + 12 + 4 + indexedFields.length * 4;
			
			return sizeInBytes;
		}

		public int[] getIndexedFields() {
			return indexedFields;
		}
	}
}

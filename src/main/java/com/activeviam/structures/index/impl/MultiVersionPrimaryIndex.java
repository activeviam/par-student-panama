package com.activeviam.structures.index.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.impl.AMultiVersion;
import com.activeviam.structures.array.IIntegerArray;
import com.activeviam.structures.array.IWritableIntegerArray;
import com.activeviam.structures.array.impl.MultiVersionIntegerArray;
import com.activeviam.structures.index.IMultiVersionPrimaryIndex;
import com.activeviam.structures.index.IPrimaryIndexTransaction;
import com.activeviam.structures.index.IPrimaryIndexVersion;
import com.activeviam.structures.index.impl.MultiVersionPrimaryIndex.SingleFieldPrimaryIndexBase;
import com.activeviam.structures.store.IRecord;

/**
 * Basic implementation of {@link IMultiVersionPrimaryIndex} based on a
 * {@link MultiVersionIntegerArray}.
 *
 * @author ActiveViam
 */
public class MultiVersionPrimaryIndex extends
		AMultiVersion<IPrimaryIndexVersion, IPrimaryIndexTransaction, SingleFieldPrimaryIndexBase>
		implements IMultiVersionPrimaryIndex {

	/**
	 * Constructor
	 *
	 * @param keyFields The indexes of the key fields
	 */
	public MultiVersionPrimaryIndex(int[] keyFields) {
		super(new SingleFieldPrimaryIndexBase(keyFields[0])); //why is it a table?
		if (keyFields.length > 1) {
			throw new UnsupportedOperationException("We do not need to support this case");
		}
	}

	@Override
	public IPrimaryIndexVersion commit(IEpoch epoch) {
		base.mvMapping.commit(epoch);
		return super.commit(epoch);
	}

	@Override
	public void rollback() {
		base.mvMapping.rollback();
		super.rollback();
	}

	@Override
	public long sizeInBytes() {
		// 16:                 Object header
		// 4:                  Reference to the Base
		// base.sizeInBytes(): Base
		// 4:                  Reference to the latest version
		return 16 + 4 + base.sizeInBytes() + 4;
	}

	public static class SingleFieldPrimaryIndexBase {

		/**
		 * The index of the key field.
		 */
		protected final int keyField; // Colonne des primary keys, ici 0

		/**
		 * Mapping between value of the key field and the position of the associated record.
		 */
		protected final MultiVersionIntegerArray mvMapping;

		/**
		 * Constructor
		 *
		 * @param keyField the index of the key field
		 */
		public SingleFieldPrimaryIndexBase(int keyField) {
			this.keyField = keyField;
			this.mvMapping = new MultiVersionIntegerArray(-1);
		}

		/**
		 * Gets the total size of the index.
		 * <p>
		 *   This includes data as well as abject internal attributes, class pointers, ...
		 * </p>
		 * @return estimated size (in bytes) of the index
		 */
		public long sizeInBytes() {
			// 16: Object header
			// 4: keyField attribute
			// 4: reference to mvMapping
			return 16 + 4 + 4 + 12 + mvMapping.getSizeInBytes();
		}

		protected int getRow(IRecord record, IIntegerArray mapping) {
			final int v = record.readInt(keyField);
			return mapping.getValue(v);
		}

		protected int getRow(int[] point, IIntegerArray mapping) {
			assert point.length == 1;
			return mapping.getValue(point[0]);
		}

		protected void index(int[] point, int row, IWritableIntegerArray mapping) {
			assert point.length == 1;
			mapping.set(point[0], row);
		}

	}

	@Override
	protected IPrimaryIndexTransaction createNewTransaction() {
		return new SingleFieldPrimaryIndexTransaction(base);
	}

	@Override
	protected IPrimaryIndexVersion createVersion(IEpoch epoch,
			IPrimaryIndexTransaction transaction) {
		return new SingleFieldPrimaryIndexVersion(epoch, base.mvMapping.getMostRecentVersion(),
				base);
	}

}

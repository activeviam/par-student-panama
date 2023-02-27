package com.activeviam.structures.index.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.impl.AMultiVersion;
import com.activeviam.structures.index.IMultiVersionSecondaryIndex;
import com.activeviam.structures.index.ISecondaryIndexTransaction;
import com.activeviam.structures.index.ISecondaryIndexVersion;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.ITable;

/**
 * MVCC secondary index based on a column imprints index.
 * 
 * <p>
 *   First prototype.
 * </p>
 */
public class MultiVersionSecondaryIndexV3  extends
		AMultiVersion<ISecondaryIndexVersion, ISecondaryIndexTransaction, ColumnImprintsAppendOnlySecondaryIndex>
		implements IMultiVersionSecondaryIndex {
	
	/**
	* Constructor
	*
	* @param indexedFields The indexes of the fields (in {@link IRecord#getAttributes()}) to index
	*/
	public MultiVersionSecondaryIndexV3(int[] indexedFields, ITable base) {
		this(new ColumnImprintsAppendOnlySecondaryIndex(indexedFields, base));
	}
	
	/**
	* Constructor.
	*
	* @param base The base of the multiversion
	*/
	protected MultiVersionSecondaryIndexV3(ColumnImprintsAppendOnlySecondaryIndex base) {
		super(base);
	}
	
	@Override
	protected ISecondaryIndexTransaction createNewTransaction() {
		return new SecondaryIndexTransactionV3(base);
	}
	
	@Override
	protected ISecondaryIndexVersion createVersion(IEpoch epoch,
			ISecondaryIndexTransaction transaction) {
		return new SecondaryIndexVersionV3(epoch, base);
	}
	
	@Override
	public long sizeInBytes() {
		// 16:                 Object header
		// 8:                  Reference to the Base
		// base.sizeInBytes(): Base
		// 8:                  Reference to the latest version
		// 8:                  Reference to the current transaction
		// 8:                  Reference to the lock
		// 16:                 Lock (only Object header)
		return 16 + 8 + base.sizeInBytes() + 8 + 8 + 8 + 16;
	}

	@Override
	public void discardBefore(long epoch, long[][] versions, int[] deletedRows) {
		System.out.println("discardBefore is not supported on " + getClass().getSimpleName());
	}

}

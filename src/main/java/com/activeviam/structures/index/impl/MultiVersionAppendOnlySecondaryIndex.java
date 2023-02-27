package com.activeviam.structures.index.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.impl.AMultiVersion;
import com.activeviam.structures.index.IMultiVersionSecondaryIndex;
import com.activeviam.structures.index.ISecondaryIndexBase;
import com.activeviam.structures.index.ISecondaryIndexTransaction;
import com.activeviam.structures.index.ISecondaryIndexVersion;

/**
 * MVCC secondary index based on a bitmap index.
 *
 * @author ActiveViam
 */
public class MultiVersionAppendOnlySecondaryIndex<B extends ISecondaryIndexBase> extends
		AMultiVersion<ISecondaryIndexVersion, ISecondaryIndexTransaction, B>
		implements IMultiVersionSecondaryIndex {

	/**
	 * Constructor.
	 *
	 * @param base the base of the multiversion
	 */
	public MultiVersionAppendOnlySecondaryIndex(B base) {
		super(base);
	}

	@Override
	protected ISecondaryIndexTransaction createNewTransaction() {
		return new SecondaryIndexTransaction(base);
	}

	@Override
	protected ISecondaryIndexVersion createVersion(IEpoch epoch,
			ISecondaryIndexTransaction transaction) {
		return new SecondaryIndexVersion(epoch, base);
	}

	@Override
	public long sizeInBytes() {
		// 16:                 Object header
		// 4:                  Reference to the Base
		// base.sizeInBytes(): Base
		// 4:                  Reference to the latest version
		return 16 + 4 + base.sizeInBytes() + 4;
	}

	@Override
	public void discardBefore(long epoch, long[][] versions, int[] deletedRows) {
		base.discardBefore(epoch, versions, deletedRows);
	}

}
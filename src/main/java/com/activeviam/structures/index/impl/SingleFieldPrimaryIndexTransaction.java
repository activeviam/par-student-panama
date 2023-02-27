package com.activeviam.structures.index.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.structures.array.impl.IntegerArrayTransaction;
import com.activeviam.structures.index.IMultiVersionPrimaryIndex;
import com.activeviam.structures.index.IPrimaryIndexTransaction;
import com.activeviam.structures.index.impl.MultiVersionPrimaryIndex.SingleFieldPrimaryIndexBase;

/**
 * Transaction for {@link IMultiVersionPrimaryIndex} that delegates everything to the base.
 *
 * @author ActiveViam
 */
public class SingleFieldPrimaryIndexTransaction implements IPrimaryIndexTransaction {

	protected final SingleFieldPrimaryIndexBase base;

	/**
	 * Mapping between value of the key field and the position of the associated record.
	 */
	protected final IntegerArrayTransaction mapping;

	/**
	 * @param base
	 */
	public SingleFieldPrimaryIndexTransaction(SingleFieldPrimaryIndexBase base) {
		this.base = base;
		this.mapping = base.mvMapping.getOrCreateTransaction();
	}

	@Override
	public void commit(IEpoch epoch) {
	}

	@Override
	public void rollback() {}

	@Override
	public int getRow(int[] point) {
		return base.getRow(point, mapping);
	}

	@Override
	public void index(int[] point, int row) {
		base.index(point, row, mapping);
	}

}

package com.activeviam.structures.index.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.ITransaction;
import com.activeviam.mvcc.IVersion;
import com.activeviam.mvcc.impl.ABasicVersion;
import com.activeviam.structures.array.impl.IntegerArrayVersion;
import com.activeviam.structures.index.IPrimaryIndexVersion;
import com.activeviam.structures.index.impl.MultiVersionPrimaryIndex.SingleFieldPrimaryIndexBase;
import com.activeviam.structures.store.IRecord;

/**
 * @author ActiveViam
 */
public class SingleFieldPrimaryIndexVersion extends ABasicVersion<SingleFieldPrimaryIndexBase>
		implements IPrimaryIndexVersion {

	protected final IntegerArrayVersion mapping;

	/**
	 * @param mapping
	 * @param base
	 */
	public SingleFieldPrimaryIndexVersion(IEpoch epoch, IntegerArrayVersion mapping,
			SingleFieldPrimaryIndexBase base) {
		super(epoch, base);
		this.mapping = mapping;
	}

	@Override
	public void markObsolete(IVersion newLatest, ITransaction transaction) {
		// Nothing to do
	}

	@Override
	public int getRow(final IRecord record) {
		return base.getRow(record, mapping);
	}

}

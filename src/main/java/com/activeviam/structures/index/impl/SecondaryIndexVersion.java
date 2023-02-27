package com.activeviam.structures.index.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.ITransaction;
import com.activeviam.mvcc.IVersion;
import com.activeviam.mvcc.impl.ABasicVersion;
import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.index.ISecondaryIndexBase;
import com.activeviam.structures.index.ISecondaryIndexVersion;
import com.activeviam.structures.store.IRecord;

public class SecondaryIndexVersion extends ABasicVersion<ISecondaryIndexBase> implements ISecondaryIndexVersion {

	/**
	 * The number of indexed records at this version.
	 * The rows of the indexed records are <code>< size</code>.
	 */
	protected final int size;

    public SecondaryIndexVersion(IEpoch epoch, ISecondaryIndexBase base) {
        super(epoch, base);
        this.size = base.getSize();
    }

    @Override
    public void markObsolete(IVersion newLatest, ITransaction transaction) {
        // Nothing to do
    }

    @Override
    public IBitmap getRows(final int[] pattern) {
        return base.getRows(pattern);
    }

    @Override
    public IBitmap getRows(final int[][] compositePattern) {
        return base.getRows(compositePattern);
    }

    @Override
    public int[] extractPoint(IRecord record) {
        return base.extractPoint(record);
    }

}
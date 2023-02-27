package com.activeviam.structures.index.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.index.ISecondaryIndexBase;
import com.activeviam.structures.index.ISecondaryIndexTransaction;
import com.activeviam.structures.store.IRecord;

public class SecondaryIndexTransaction implements ISecondaryIndexTransaction {

    protected final ISecondaryIndexBase base;
    protected final int initialSize;

    public SecondaryIndexTransaction(ISecondaryIndexBase base) {
        this.base = base;
        this.initialSize = base.getSize();
    }

    @Override
    public void commit(IEpoch epoch) {
        // Nothing to do
    }

    @Override
    public void rollback() {
        base.truncate(initialSize);
    }

    @Override
    public void index(IRecord record, int row) {
        base.index(record, row);
    }

    @Override
    public void remove(IRecord record, int row) {
        // Nothing to do
    }

    @Override
    public IBitmap getRows(final int[] pattern) {
		throw new UnsupportedOperationException();
    }

    @Override
    public IBitmap getRows(final int[][] compositePattern) {
		throw new UnsupportedOperationException();
    }

    @Override
    public int[] extractPoint(IRecord record) {
        return base.extractPoint(record);
    }

}
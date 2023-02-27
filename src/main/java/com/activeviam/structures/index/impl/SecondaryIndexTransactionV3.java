package com.activeviam.structures.index.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.index.ISecondaryIndexTransaction;
import com.activeviam.structures.store.IRecord;

public class SecondaryIndexTransactionV3 implements ISecondaryIndexTransaction {

    protected final ColumnImprintsAppendOnlySecondaryIndex base;
    protected final int initialSize;

    protected final int[] imprintVectorsSizes;
    protected final long[] lastImprintVectors;
    protected final int[] cachelineDictionariesSizes;
    protected final int[] lastCachelineDictionaries;

    public SecondaryIndexTransactionV3(ColumnImprintsAppendOnlySecondaryIndex base) {
        this.base = base;
        this.initialSize = base.getSize();

        final int numLevels = base.getIndexedFieldsSize();
        imprintVectorsSizes = new int[numLevels];
        lastImprintVectors = new long[numLevels];
        cachelineDictionariesSizes = new int[numLevels];
        lastCachelineDictionaries = new int[numLevels];

        for (int lvl = 0; lvl < numLevels; ++lvl) {
        	imprintVectorsSizes[lvl] = base.imprintVectorsSize(lvl);
        	lastImprintVectors[lvl] = base.lastImprintVector(lvl);
        	cachelineDictionariesSizes[lvl] = base.cachelineDictionariesSize(lvl);
        	lastCachelineDictionaries[lvl] = base.lastCachelineDictionary(lvl);
        }
    }

    @Override
    public void commit(IEpoch epoch) {
    	// If the rebuild returned a new base we would not need to take a lock to do it
    	base.rebuild();
    }

    @Override
    public void rollback() {
        base.truncate(initialSize,
        		imprintVectorsSizes,
        		lastImprintVectors,
        		cachelineDictionariesSizes,
        		lastCachelineDictionaries);
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

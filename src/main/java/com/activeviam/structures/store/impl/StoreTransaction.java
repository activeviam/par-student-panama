package com.activeviam.structures.store.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.structures.index.IPrimaryIndexTransaction;
import com.activeviam.structures.index.ISecondaryIndexTransaction;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.IStoreBase;
import com.activeviam.structures.store.IStoreTransaction;
import com.activeviam.structures.store.ITableTransaction;
import java.util.Collection;

/**
 * Transaction for {@link AMultiVersionStore}.
 *
 * @author ActiveViam
 */
public class StoreTransaction implements IStoreTransaction {

	protected final IStoreBase base;
	protected final int[] keyFields;
	protected final ITableTransaction tableTransaction;
	protected final IPrimaryIndexTransaction primaryIndexTransaction;
	protected final ISecondaryIndexTransaction secondaryIndexTransaction;

	/** Recycled array to extract the keys of the records */
	protected final int[] keyBuffer;

	public StoreTransaction(IStoreBase base) {
		this.base = base;
		this.keyFields = base.getFormat().keyFields;
		this.tableTransaction = base.getTable().getOrCreateTransaction();
		this.primaryIndexTransaction = base.getPrimaryIndex().getOrCreateTransaction();
		this.secondaryIndexTransaction = base.getSecondaryIndex().getOrCreateTransaction();
		this.keyBuffer = new int[this.keyFields.length];
	}

	@Override
	public void commit(IEpoch epoch) {
		// Nothing to do
	}

	@Override
	public void rollback() {
		base.getTable().rollback();
		base.getPrimaryIndex().rollback();
		base.getSecondaryIndex().rollback();
	}

	@Override
	public void submitRecords(Collection<IRecord> records) {
		tableTransaction.ensureCanAdd(records.size());

		for (IRecord record : records) {
			int row = tableTransaction.append(record);

			transferKeyInBuffer(record);

			int previousRow = primaryIndexTransaction.getRow(keyBuffer);
			if (previousRow != -1) {
				tableTransaction.delete(previousRow);
				secondaryIndexTransaction.remove(record, previousRow);
			}

			primaryIndexTransaction.index(keyBuffer, row);
			secondaryIndexTransaction.index(record, row);
		}
	}

	/**
	 * Puts the key of the record in {@link #keyBuffer}.
	 *
	 * @param record
	 */
	protected void transferKeyInBuffer(IRecord record) {
		for (int i = 0; i < keyFields.length; ++i) {
			keyBuffer[i] = record.readInt(keyFields[i]);
		}
	}

	@Override
	public void deleteRecord(IRecord key) {
		System.arraycopy(key.getAttributes(), 0, keyBuffer, 0, keyBuffer.length);
		final int previousRow = primaryIndexTransaction.getRow(keyBuffer);
		if (previousRow == -1) {
			throw new IllegalStateException(key.toString());
		}

		IRecord previousRecord = tableTransaction.read(previousRow);
		tableTransaction.delete(previousRow);
		primaryIndexTransaction.index(keyBuffer, -1);
		secondaryIndexTransaction.remove(previousRecord, previousRow);
	}

}

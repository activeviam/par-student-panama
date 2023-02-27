package com.activeviam.structures.store;

import com.activeviam.mvcc.ITransaction;
import java.util.Collection;
import java.util.Collections;

/**
 * Transaction for {@link IMultiVersionStore}.
 *
 * @author ActiveViam
 */
public interface IStoreTransaction extends ITransaction {

	void submitRecords(Collection<IRecord> records);

	default void submitRecord(IRecord record) {
		submitRecords(Collections.singleton(record));
	}

	void deleteRecord(IRecord key);

}

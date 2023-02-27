package com.activeviam.structures.store;

import com.activeviam.mvcc.ITransaction;

/**
 * Transaction for {@link IMultiVersionTable}.
 *
 * @author ActiveViam
 */
public interface ITableTransaction extends ITransaction {

	/**
	 * Adds a record to the table.
	 *
	 * @param record the record to add
	 * @return the row at which this record was added
	 */
	int append(IRecord record);

	/**
	 * Deletes the record at the given row
	 *
	 * @param row the row of the record to remove
	 */
	void delete(int row);

	/**
	 * Gets the record stored at the given row.
	 *
	 * @param row the row of the record
	 * @return the record at the give row
	 */
	IRecord read(int row);

	boolean exists(int row);

	/**
	 * Reserves enough memory to add the given number of records.
	 *
	 * @param count the number of record that will be added
	 */
	void ensureCanAdd(int count);

}

package com.activeviam.structures.store;

import java.util.BitSet;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

/**
 * A table contains {@link IRecord}.
 *
 * @author ActiveViam
 */
public interface ITable {

	/**
	 * Fetches the record stored at the given row.
	 * @param row the row
	 * @return the record
	 */
	IRecord getRecord(int row);

	/**
	 * Reads the attribute of a record without materializing it like {@link #getRecord(int)}.
	 * @param row the row of the record to read
	 * @param column the index of the attribute to read
	 * @return the value of the attribute
	 */
	int readInt(int row, int column);
	
	/**
	 * Reads attribute of records without materializing them like {@link #getRecord(int)}.
	 * 
	 * @param row The first row of the records to read
	 * @param column The index of the attribute to read
	 * @param number The number of records to read from first row
	 * @param result The array holding the values of the attribute to read
	 */
	void readInts(final int row, final int column, final int number, final int[] result);

	/**
	 * Reads the value of a record without materializing it like {@link #getRecord(int)}.
	 * @param row the row of the record to read
	 * @param column the index of the value to read
	 * @return the value of the {@link IRecord#getValues() value}
	 */
	double readDouble(int row, int column);

	/**
	 * @return the number of records in the table
	 */
	int size();

	/**
	 * Finds the rows whose attributes match the given predicate.
	 *
	 * <p>The size of the predicate must be equals to the number of attributes.
	 *
	 * <p>A negative value means no condition on the attribute.
	 *
	 * @param predicate
	 * @return the rows matching the given predicate
	 */
	default BitSet findRowsAsBitSet(int[] predicate) {
		throw new RuntimeException("TODO");
	};

}

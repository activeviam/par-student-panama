package com.activeviam.structures.store;

/**
 * Interface to edit an {@link ITable}.
 *
 * @author ActiveViam
 */
public interface IWritableTable extends ITable {

	/**
	 * Adds a record to the table.
	 *
	 * @param record the record to add
	 * @return the row at which this record was added
	 */
	int append(IRecord record);

	/**
	 * This method can be called before {@link #append(IRecord) adding} several records to allocate
	 * in one operation the memory to store the records.
	 *
	 * @param capacity the target capacity
	 * @return the new capacity
	 */
	int ensureCapacity(int capacity);

	/**
	 * Reduces the size of the table. Rows above this size will be removed.
	 *
	 * @param newSize the target size
	 */
	void truncate(int newSize);

	interface ITableWriter {

		/**
		 * Sets the value of an attribute column.
		 *
		 * @param column The attribute column
		 * @param value The value
		 */
		void writeInt(int column, int value);

		/**
		 * Sets the value of a value column.
		 *
		 * @param column The value column
		 * @param value The value
		 */
		void writeDouble(int column, double value);

		/**
		 * Sets the row on which to write.
		 *
		 * @param row the row on which to write
		 */
		void setRow(int row);
		
		/**
		 * Gets the total size of the table.
		 * 
		 * <p>
		 *   This includes data as well as abject internal attributes, class pointers, ...
		 * </p>
		 * 
		 * @return estimated size (in bytes) of the table
		 */
		long sizeInBytes();
	}
}

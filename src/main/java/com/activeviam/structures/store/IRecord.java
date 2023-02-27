package com.activeviam.structures.store;

/**
 * A record is a row of a table.
 * <p>
 * Its fields can have numerics values ({@link #getValues()} stored as doubles. The other types of
 * fields will be stored as ints ({@link #getAttributes()}) (we will suppose that the encoding of
 * the values is done before storing the records).
 *
 * @author ActiveViam
 * @see ITable
 */
public interface IRecord {

	int[] getAttributes();

	double[] getValues();

	int readInt(int attributeIndex);

	double readDouble(int valueIndex);

	IRecord clone();

}

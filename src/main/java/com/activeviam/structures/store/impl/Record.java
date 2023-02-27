package com.activeviam.structures.store.impl;

import com.activeviam.structures.store.IRecord;
import java.util.Arrays;

/**
 * Basic implementation of {@link IRecord}.
 *
 * @author ActiveViam
 */
public class Record implements IRecord {

	protected final int[] attributes;
	protected final double[] values;

	/**
	 * @param attributes
	 * @param values
	 */
	public Record(int[] attributes, double[] values) {
		this.attributes = attributes;
		this.values = values;
	}

	@Override
	public int[] getAttributes() {
		return attributes;
	}

	@Override
	public double[] getValues() {
		return values;
	}

	@Override
	public int readInt(int attributeIndex) {
		return attributes[attributeIndex];
	}

	@Override
	public double readDouble(int valueIndex) {
		return values[valueIndex];
	}

	@Override
	public IRecord clone() {
		return new Record(Arrays.copyOf(attributes, attributes.length),
				Arrays.copyOf(values, values.length));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(attributes);
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Record other = (Record) obj;
		if (!Arrays.equals(attributes, other.attributes))
			return false;
		if (!Arrays.equals(values, other.values))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Record [attributes=" + Arrays.toString(attributes) + ", values="
				+ Arrays.toString(values) + "]";
	}

}

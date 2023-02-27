package com.activeviam.structures.array.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.ITransaction;
import com.activeviam.structures.array.IIntegerArray;
import com.activeviam.structures.array.IWritableIntegerArray;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * A transaction on an {@link IIntegerArray} that contains the transient, writable
 * state of the array.
 *
 * @author ActiveViam
 */
public class IntegerArrayTransaction implements ITransaction, IWritableIntegerArray {

	/**
	 * The base of the multiversion
	 */
	protected final IWritableIntegerArray base;

	/**
	 * The size of {@link #base} when the transaction was created
	 */
	protected final int baseSize;

	/**
	 * The new array's size if the transaction is committed
	 */
	protected int newSize;

	/**
	 * The new values of the transaction
	 */
	protected TIntIntMap positiveDelta;

	/**
	 * The changed values of the transaction
	 */
	protected TIntIntMap negativeDelta;

	/**
	 * Constructor
	 * @param base the base of the multiversion
	 */
	public IntegerArrayTransaction(IWritableIntegerArray base) {
		this.base = base;
		this.baseSize = base.size();
		this.newSize = this.baseSize;
		positiveDelta = new TIntIntHashMap();
		negativeDelta = new TIntIntHashMap();
	}

	/**
	 * @return the delta containing the changed values of the transaction
	 */
	public TIntIntMap getDelta() {
		return negativeDelta;
	}

	@Override
	public int getEmptyValue() {
		return base.getEmptyValue();
	}

	@Override
	public int getValue(int index) {
		if (positiveDelta.containsKey(index)) {
			return positiveDelta.get(index);
		} else if (index < baseSize) {
			return base.getValue(index);
		} else {
			return getEmptyValue();
		}
	}

	@Override
	public int size() {
		return newSize;
	}

	@Override
	public void set(int index, int value) {
		// Update the new size
		newSize = Math.max(index + 1, newSize);

		// Add the new key/value in the positive delta
		positiveDelta.put(index, value);

		// Save the old key/value in the negative delta
		if (index < baseSize) {
			negativeDelta.put(index, base.getValue(index));
		}
	}

	@Override
	public void commit(IEpoch epoch) {
		positiveDelta.forEachEntry((key, value) -> {
			base.set(key, value);
			return true;
		});
	}

	@Override
	public void rollback() {
	}

}

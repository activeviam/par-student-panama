package com.activeviam.structures.array.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.impl.AMultiVersion;
import com.activeviam.structures.array.IIntegerArray;
import com.activeviam.structures.array.IWritableIntegerArray;

/**
 * MVCC implementation of the {@link IWritableIntegerArray}.
 *
 * @author ActiveViam
 */
public class MultiVersionIntegerArray
		extends AMultiVersion<IntegerArrayVersion, IntegerArrayTransaction, IWritableIntegerArray> {

	/**
	 * Constructor
	 *
	 * @param emptyValue See {@link IIntegerArray#getEmptyValue()}
	 */
	public MultiVersionIntegerArray(int emptyValue) {
		super(new IntegerArray(emptyValue));
	}

	@Override
	protected IntegerArrayTransaction createNewTransaction() {
		return new IntegerArrayTransaction(base);
	}

	@Override
	protected IntegerArrayVersion createVersion(IEpoch epoch, IntegerArrayTransaction transaction) {
		if (transaction != null) {
			return new IntegerArrayVersion(epoch, base, transaction.size());
		}
		return new IntegerArrayVersion(epoch, base, base.size());
	}

	/**
	 * Gets the total memory used by the array.
	 * <p>
	 *   This includes data as well as abject internal attributes, class pointers, ...
	 * </p>
	 * @return estimated size (in bytes) of the array
	 */
	public long getSizeInBytes() {
		// 16:                 Object header
		// 4:                  Reference to the Base
		// base.sizeInBytes(): Base
		// 4:                  Reference to the latest version
		return 16 + 4 + ((IntegerArray) base).getSizeInBytes() + 4;
	}

}

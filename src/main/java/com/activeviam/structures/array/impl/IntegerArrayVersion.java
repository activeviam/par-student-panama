package com.activeviam.structures.array.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.impl.AVersionWithDelta;
import com.activeviam.structures.array.IIntegerArray;
import gnu.trove.map.TIntIntMap;

/**
 * An {@link IIntegerArray integer array} version defined by a
 * base reference array and some delta mappings that override the
 * mappings in the base array.
 *
 * @author ActiveViam
 */
public class IntegerArrayVersion
		extends AVersionWithDelta<IIntegerArray, IntegerArrayTransaction, IntegerArrayVersion>
		implements IIntegerArray {

	/**
	 * The array's size of the version
	 */
	protected int size;

	public IntegerArrayVersion(IEpoch epoch, IIntegerArray base, int size) {
		super(epoch, base);
		this.size = size;
	}

	@Override
	public int getEmptyValue() {
		return base.getEmptyValue();
	}

	@Override
	public int getValue(int index) {
		if (index >= size) {
			return base.getEmptyValue();
		}

		// Get the up-to-date value in the base
		final int value = base.getValue(index);

		// Get the version's transition
		IntegerArrayTransition transition = getTransition();

		// Loop through the next versions' transitions until a delta containing the index is found
		while (transition != null) {
			if (transition.getDelta().containsKey(index)) {
				return transition.getDelta().get(index);
			}
			transition = transition.getVersion().getTransition();
		}

		// If the version is the latest or if no such delta is found,
		// returns the current value in the base
		return value;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	protected IntegerArrayTransition createTransition(IntegerArrayVersion newVersion,
			IntegerArrayTransaction transaction) {
		return new IntegerArrayTransition(newVersion, transaction.getDelta());
	}

	@Override
	protected IntegerArrayTransition getTransition() {
		return (IntegerArrayTransition) super.getTransition();
	}

	/**
	 * Class containing the changes between the version holding it and the
	 * {@link IntegerArrayTransition#getVersion() next version}.
	 *
	 * @author ActiveViam
	 */
	public static class IntegerArrayTransition extends Transition<IntegerArrayVersion> {

		/**
		 * The delta containing the changed values between the versions
		 */
		protected final TIntIntMap delta;

		/**
		 * Constructor.
		 *
		 * @param next The next version to which this transition points.
		 * @param delta
		 */
		public IntegerArrayTransition(IntegerArrayVersion next, TIntIntMap delta) {
			super(next);
			this.delta = delta;
		}

		/**
		 * @return the delta containing the changed values between the versions
		 */
		protected TIntIntMap getDelta() {
			return delta;
		}

	}

}

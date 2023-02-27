package com.activeviam.mvcc.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.IVersion;

/**
 * The abstract implementation of a {@link IVersion} using a mask for its
 * content with regard to the base.
 *
 * @author ActiveViam
 * @param <B> the type of the base
 */
public abstract class ABasicVersion<B> implements IVersion {

	/** The epoch at which this version was created. */
	protected final IEpoch epoch;

	/**
	 * Base of the current versioned object.
	 */
	protected final B base;

	/**
	 * Constructor
	 *
	 * @param epoch The epoch at which this version was created.
	 * @param base The base.
	 */
	public ABasicVersion(IEpoch epoch, B base) {
		this.epoch = epoch;
		this.base = base;
	}

	@Override
	public IEpoch getEpoch() {
		return epoch;
	}

	/**
	 * @return The base of this versioned Object
	 */
	public B getBase() {
		return base;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + epoch.getId();
	}

}

package com.activeviam.mvcc.impl;

import com.activeviam.mvcc.IEpoch;

/**
 * Implementation of {@link IEpoch}.
 *
 * @author ActiveViam
 */
public class Epoch implements IEpoch {

	/**
	 * The instance of the first epoch
	 */
	public static final Epoch INITIAL_EPOCH = new Epoch(INITIAL_EPOCH_ID, MASTER_BRANCH_NAME);

	/** Epoch sequence id */
	protected final long epoch;

	protected final String branch;

	/**
	 * Constructor for an epoch of the master branch at a given time
	 *
	 * @param epoch epoch id
	 */
	public Epoch(long epoch) {
		this(epoch, MASTER_BRANCH_NAME);
	}

	/**
	 * Constructor for an epoch of a branch at a given time
	 *
	 * @param epoch epoch id
	 * @param branch name of the branch
	 */
	public Epoch(long epoch, String branch) {
		this.epoch = epoch;
		this.branch = branch;
	}

	@Override
	public long getId() {
		return epoch;
	}

	@Override
	public String getBranch() {
		return branch;
	}

}

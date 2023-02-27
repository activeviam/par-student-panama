package com.activeviam.mvcc;

/**
 * Epochs represent instant of the system timeline
 * at which the state of the application changed.
 *
 * @author ActiveViam
 */
public interface IEpoch {

	/** The id of the initial epoch (zero) */
	static final long INITIAL_EPOCH_ID = 0L;

	/**
	 * The name of the default branch.
	 */
	static final String MASTER_BRANCH_NAME = "master";

	/** @return the id of the epoch */
	long getId();

	/**
	 * Get the branch of the epoch.
	 * <p>
	 * A branch is a succession of epochs. Branches allows the system to have several timelines.
	 *
	 * @return the branch
	 */
	String getBranch();

}

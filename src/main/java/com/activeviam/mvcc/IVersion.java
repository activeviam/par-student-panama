package com.activeviam.mvcc;

/**
 * A consistent version of a base Object.
 *
 * @author ActiveViam
 */
public interface IVersion {

	/**
	 * Give the epoch at which this version was created.
	 *
	 * @return The epoch of this version.
	 */
	IEpoch getEpoch();

	/**
	 * Marks this version as obsolete, meaning that this version is no longer the most recent one.
	 *
	 * @param newLatest The new valid version that replaces this one.
	 * @param transaction The transaction that made this version obsolete.
	 */
	void markObsolete(IVersion newLatest, ITransaction transaction);

}

package com.activeviam.mvcc;

/**
 *
 * @author ActiveViam
 * @param <V> the type of the versions
 * @param <T> the type of the transaction
 */
public interface IMultiVersion<V extends IVersion, T extends ITransaction> {

	/**
	 * Retrieves the latest (i.e. current) version of the
	 * object.
	 *
	 * @return The latest version of the object.
	 */
	V getMostRecentVersion();

	/**
	 * Returns the current transaction.
	 *
	 * @return The current transaction or <code>null</code> when there is no
	 *         transaction
	 *
	 * @see #getOrCreateTransaction()
	 */
	T getTransaction();

	/**
	 * Returns the current transaction. <br/>
	 * The result is never <code>null</code> contrary to
	 * {@link #getTransaction()}.
	 *
	 * <p>
	 * THIS METHOD SHOULD BE CALLED ONLY IF {@link #commit(IEpoch)} (or
	 * {@link #rollback()}) IS GUARANTEED TO BE CALLED otherwise this could
	 * cause a memory leak if the transaction holds a version (because the
	 * multiversion will keep the transaction)
	 *
	 * @return the current transaction
	 * @see #getTransaction()
	 */
	T getOrCreateTransaction();

	/**
	 * Commits the pending transient state at the specified epoch.
	 *
	 * @param epoch The epoch associated with this commit.
	 * @return The version that prevails after this commit.
	 */
	V commit(final IEpoch epoch);

	/**
	 * Rolls back the changes that have been written in
	 * the transient state.
	 */
	void rollback();

}

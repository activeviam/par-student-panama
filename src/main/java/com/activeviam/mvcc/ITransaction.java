package com.activeviam.mvcc;

/**
 * A transaction on a versioned Object.
 *
 * @author ActiveViam
 */
public interface ITransaction {

	/**
	 * Commits this transaction into the base version.
	 *
	 * @param epoch The commit's epoch
	 */
	void commit(IEpoch epoch);

	/**
	 * Rolls the transaction back.
	 *
	 * <p>
	 * This transaction object cannot be re-used after being rolled back.
	 */
	void rollback();

}

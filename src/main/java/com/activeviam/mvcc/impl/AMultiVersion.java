package com.activeviam.mvcc.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.IMultiVersion;
import com.activeviam.mvcc.ITransaction;
import com.activeviam.mvcc.IVersion;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract class to help implementing {@link IMultiVersion}.
 *
 * @author ActiveViam
 */
public abstract class AMultiVersion<V extends IVersion, T extends ITransaction, B>
		implements IMultiVersion<V, T> {

	/** The base version that is being updated and is kept up to date */
	protected volatile B base;

	/**
	 * The head of the version chain. This is the most recent version of the versioned Object.
	 */
	protected volatile V latest;

	/** The transaction that holds pending changes until they are merged into the base */
	protected volatile T transaction;

	/** Lock to prevent multiple threads from creating a new transaction */
	protected final Lock transactionCreationLock = new ReentrantLock();

	public AMultiVersion(B base) {
		this.base = Objects.requireNonNull(base);
		this.latest = createVersion(Epoch.INITIAL_EPOCH, null);
	}

	@Override
	public V getMostRecentVersion() {
		return this.latest;
	}

	@Override
	public V commit(final IEpoch epoch) {
		final T transaction = getTransaction();
		if (null == transaction) {
			throw new IllegalStateException("No transaction started");
		}
		final V currentLatest = this.latest;

		// Epoch are only allowed to increase
		if (epoch.getId() <= currentLatest.getEpoch().getId()) {
			throw new IllegalStateException(this + ": cannot commit epoch " + epoch.getId()
					+ " when the current one is already " + currentLatest.getEpoch().getId());
		}

		// Create a new version to replace the current latest
		final V newLatest = createVersion(epoch, transaction);

		// Mark the current version as obsolete and link it to the new
		// latest. This protects the concurrent readers from seeing the
		// changes that are about to be done to the base with this
		// transaction.
		currentLatest.markObsolete(newLatest, transaction);

		beforeTransactionCommit(newLatest);

		// Now that concurrent readers are protected we can propagate
		// the new state to the base Object
		transaction.commit(epoch);

		afterTransactionCommit(newLatest);

		// Return the new current version
		return newLatest;
	}

	@Override
	public void rollback() {
		final T transaction = this.transaction;
		if (transaction != null) {
			// Roll back the transaction
			transaction.rollback();

			// Dereference the current transaction
			this.transaction = null;
		}
	}

	/**
	 * Creates a new {@link ITransaction transaction}.
	 *
	 * @return A newly created transaction
	 */
	protected abstract T createNewTransaction();

	@Override
	public T getOrCreateTransaction() {
		T transaction = this.transaction;
		if (transaction == null) {
			// Create the transaction:

			// Acquire the lock to prevent multiple threads from creating it
			this.transactionCreationLock.lock();

			try {
				transaction = this.transaction;
				if (transaction == null) {
					transaction = createNewTransaction();
					this.transaction = transaction;
				}
			} finally {
				// Release the transaction creation lock
				this.transactionCreationLock.unlock();
			}
		}
		return transaction;
	}

	@Override
	public T getTransaction() {
		return transaction;
	}

	/**
	 * Creates a new {@link IVersion version}.
	 *
	 * @param epoch The new latest epoch.
	 * @param transaction The current transaction. Its {@link ITransaction#commit(IEpoch) commit}
	 *        method must not be called within this method.
	 * @return A new version
	 */
	protected abstract V createVersion(IEpoch epoch, T transaction);

	/**
	 * During the commit operation, this method is called just before the transaction commit, and
	 * just after the new version was created (the version that will become the latest).
	 *
	 * @param newLatest the version about to become the latest
	 */
	protected void beforeTransactionCommit(V newLatest) {}

	/**
	 * During the commit operation, this method is called just after the commit of the transaction.
	 * This method must update {@link #latest} and clears the current transaction.
	 *
	 * @param newLatest the new latest
	 */
	protected void afterTransactionCommit(V newLatest) {
		// Publish the new latest.
		// This makes this commit visible.
		this.latest = newLatest;

		// Dereference the current transaction
		this.transaction = null;
	}

}

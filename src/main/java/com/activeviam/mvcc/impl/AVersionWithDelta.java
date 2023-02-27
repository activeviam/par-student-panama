package com.activeviam.mvcc.impl;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.ITransaction;
import com.activeviam.mvcc.ITransition;
import com.activeviam.mvcc.IVersion;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * The abstract implementation of a {@link IVersion} using a mask for its content with regard to the
 * base.
 *
 * @author ActiveViam
 * @param <B> the type of the base
 * @param <T> the type of the transaction
 * @param <V> the type of the version extending this class
 */
public abstract class AVersionWithDelta<B, T extends ITransaction, V extends AVersionWithDelta<B, T, V>>
		extends ABasicVersion<B> {

	/**
	 * {@link VarHandle} on {@link #transition}.
	 */
	protected static final VarHandle TRANSITION_HANDLE;
	static {
		try {
			TRANSITION_HANDLE = MethodHandles.lookup().findVarHandle(AVersionWithDelta.class, "transition",
					Transition.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The transition to the version created after this one. <code>null</code> while this version is
	 * the latest.
	 */
	protected volatile Transition<V> transition;

	/**
	 * Constructor
	 *
	 * @param epoch The epoch at which this version was created.
	 * @param base The base.
	 */
	public AVersionWithDelta(IEpoch epoch, B base) {
		super(epoch, base);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void markObsolete(final IVersion newVersion, final ITransaction transaction) {
		V typedVersion = (V) newVersion;
		// The next field are not updated when the base is changed.
		if (this.base != typedVersion.getBase()) {
			return;
		}

		Transition<V> transition = createTransition(typedVersion, (T) transaction);
		setTransition(transition);
	}

	/**
	 * Creates a {@link Transition} object for the transaction.
	 *
	 * @param newVersion new created version.
	 * @param transaction the transaction that created this newVersion.
	 * @return the transition object for this transaction.
	 */
	protected abstract Transition<V> createTransition(V newVersion, T transaction);

	/**
	 * Sets the value of {@link #transition}.
	 *
	 * @param transition The {@link Transition}
	 */
	protected final void setTransition(Transition<V> transition) {
		// setTransition needs to act as a R/W barrier
		if (!TRANSITION_HANDLE.compareAndSet(this, null, transition)) {
			throw new IllegalStateException("The 'transition' field should not be set concurrently");
		}
	}

	/**
	 * Retrieves the current value of the {@link #transition}
	 * field.
	 * <p> In addition to volatile semantics, this method
	 * provides a double-sided re-ordering prohibition for
	 * reads. This allows the user to use this method as
	 * a validator with the following pattern:
	 * <pre>
	 *   if (getTransition() == null) {
	 *     // We are the latest version. Read something
	 *     // directly from the base and validate that
	 *     // no new versions have been committed in the
	 *     // meantime.
	 *     final Object something = base.readSomething();
	 *     if (getTransition() == null)
	 *       return something;
	 *   }
	 *
	 *   // If we arrive here, we are not the latest version.
	 * </pre>
	 * @return the current value of the {@link #transition} field.
	 */
	protected Transition<V> getTransition() {
		return (Transition<V>) TRANSITION_HANDLE.getVolatile(this);
	}

	/**
	 * A immutable object that contains a version.
	 *
	 * @author ActiveViam
	 *
	 * @param <V> Version Type
	 */
	public static class Transition<V extends IVersion> implements ITransition<V> {

		/** The version pointed to by this transition */
		protected final V version;

		/**
		 * Constructor
		 *
		 * @param version The version pointed to by this transition
		 */
		public Transition(V version) {
			assert version != null;
			this.version = version;
		}

		@Override
		public V getVersion() {
			return version;
		}

		@Override
		public String toString() {
			return "Transition [version=" + version + "]";
		}

	}

}

package com.activeviam.mvcc;

import com.activeviam.mvcc.impl.AVersionWithDelta;

/**
 * The transition of a {@link AVersionWithDelta}. It should contains the
 * next version of the version that owns it.
 *
 * @author ActiveViam
 *
 * @param <V>
 */
public interface ITransition<V extends IVersion> {

	/**
	 *
	 * @return the version contained by this transition.
	 */
	public V getVersion();
}

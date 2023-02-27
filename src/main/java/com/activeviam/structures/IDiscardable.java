/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.structures;

/**
 * @author ActiveViam
 */
public interface IDiscardable {

	/**
	 * Physically removes the content that no more exist since the given epoch.
	 *
	 * <p>
	 *   This means that that will be no more query on epochs lower than the given one.
	 * </p>
	 *
	 * @param epoch the oldest epoch on which queries can still be made
	 */
	void discardBefore(long epoch);

}

/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam;

/**
 * @author ActiveViam
 */
public enum Types {

	DOUBLE,
	INTEGER;
	
	public static int getSize(Types type) {
		return switch(type) {
			case DOUBLE -> 8;
			case INTEGER -> 4;
		};
	}
}

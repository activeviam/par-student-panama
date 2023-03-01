package com.activeviam.structures.index.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author ActiveViam
 */
public class TestSimpleDictionary {

	@Test
	public void test() {
		final SimpleDictionary<String> dic = new SimpleDictionary<>();

		assertEquals(0, dic.size());
		assertEquals(-1, dic.getPosition("test"));

		assertEquals(0, dic.map("test"));
		assertEquals(0, dic.getPosition("test"));
		assertEquals(1, dic.size());
		assertEquals("test", dic.read(0));
		assertEquals(0, dic.map("test"));

		assertEquals(-1, dic.getPosition("test2"));
		assertEquals(1, dic.map("test2"));
		assertEquals(2, dic.size());
		assertEquals("test", dic.read(0));
		assertEquals("test2", dic.read(1));
	}

	@Test
	public void testResize() {
		final SimpleDictionary<String> dic = new SimpleDictionary<>();
		final int size = 132;
		for (int i = 0; i < size; i++) {
			dic.map(String.valueOf(i));
		}

		assertEquals(size, dic.size());
		for (int i = 0; i < size; i++) {
			assertEquals(String.valueOf(i), dic.read(i));
		}
	}

}

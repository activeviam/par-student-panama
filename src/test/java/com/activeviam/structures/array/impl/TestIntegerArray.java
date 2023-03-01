package com.activeviam.structures.array.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IntegerArray}.
 *
 * @author ActiveViam
 */
public class TestIntegerArray {

	@Test
	public void testBasic() {
		final IntegerArray array = new IntegerArray(-1);
		for (int i = 0; i < 100; i++) {
			array.set(i, i);
			for (int j = 0; j <= i; j++) {
				assertEquals(j, array.getValue(j));
			}
		}
	}

	@Test
	public void testSize() {
		final IntegerArray array = new IntegerArray(-1);
		assertEquals(0, array.size());

		array.set(1, 5);
		assertEquals(2, array.size());

		array.set(2, 6);
		assertEquals(3, array.size());

		array.set(2, array.getEmptyValue());
		assertEquals(3, array.size());

		array.set(3, array.getEmptyValue());
		assertEquals(4, array.size());
	}

	@Test
	public void testIncreaseCapacity() {
		final IntegerArray array = new IntegerArray(-2);
		final int initialCapacity = array.data.length;
		final int index = 3 * initialCapacity + 1;
		array.set(index, 42);
		assertEquals(42, array.getValue(index));
		assertEquals(-2, array.getValue(index - 1));
		assertEquals(-2, array.data[index + 1]);
	}

}

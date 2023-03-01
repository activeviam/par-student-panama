package com.activeviam.structures.bitmap.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.activeviam.structures.bitmap.IBitmap;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 *
 * @author ActiveViam
 * @param <B> the type of the bitmap
 */
public abstract class ATestBitmap<B extends IBitmap> {

	protected abstract B createBitmap();

	@Test
	public void and() {
		// Simple manual test
		IBitmap bitmap1 = createBitmap();
		bitmap1.set(0);
		bitmap1.set(111);
		bitmap1.set(100_000);
		bitmap1.set(100_001);

		IBitmap bitmap2 = createBitmap();
		bitmap2.set(0);
		bitmap2.set(100);
		bitmap2.set(112);
		bitmap2.set(100_001);

		Set<Integer> rowSet = new HashSet<>();
		rowSet.add(0);
		rowSet.add(100_001);

		IBitmap and = bitmap1.and(bitmap2);
		match(and, rowSet);

		// AND a bitmap with itself
		and = and.and(and);
		match(and, rowSet);

		// Test with one bitmap full of dirty words
		bitmap1 = createBitmap();
		for(int row = 3; row < 1_000_000; row += 3) {
			bitmap1.set(row);
		}

		bitmap2 = createBitmap();
		for(int row = 300; row < 1_000_000; row += 300) {
			bitmap2.set(row);
		}

		and = bitmap1.and(bitmap2);
		long matches = and.stream().peek(row -> {
			assertEquals(0, row % 300, "Unexpected row: " + row);
		}).count();
		assertEquals(3333, matches, "Matches");

		bitmap1 = createBitmap();
		bitmap1.set(1);
		bitmap1.set(777_777_777);

		bitmap2 = createBitmap();
		bitmap2.set(1);
		bitmap2.set(777_777_777);

		rowSet = new HashSet<>();
		rowSet.add(1);
		rowSet.add(777_777_777);

		and = bitmap1.and(bitmap2);
		match(and, rowSet);
	}

	/**
	 * Test AND operations on bitmaps with unaligned sizes.
	 */
	@Test
	public void unalignedAnd() {
		IBitmap bitmap1 = createBitmap();
		bitmap1.set(0);
		bitmap1.set(127);
		bitmap1.set(128);
		bitmap1.set(10_000_000);

		IBitmap bitmap2 = createBitmap();
		bitmap2.set(0);
		bitmap2.set(128);

		Set<Integer> rowSet = new HashSet<>();
		rowSet.add(0);
		rowSet.add(128);

		IBitmap and = bitmap1.and(bitmap2);
		match(and, rowSet);

		and = bitmap2.and(bitmap1);
		match(and, rowSet);
	}

	@Test
	public void or() {

		// Simple manual test
		IBitmap bitmap1 = createBitmap();
		bitmap1.set(0);
		bitmap1.set(111);
		bitmap1.set(100000);
		bitmap1.set(100001);

		IBitmap bitmap2 = createBitmap();
		bitmap2.set(0);
		bitmap2.set(112);
		bitmap2.set(100001);

		Set<Integer> rowSet = new HashSet<>();
		rowSet.add(0);
		rowSet.add(111);
		rowSet.add(112);
		rowSet.add(100_000);
		rowSet.add(100_001);

		IBitmap or = bitmap1.or(bitmap2);
		match(or, rowSet);

		or = createBitmap();
		bitmap1.or(bitmap2, or);
		match(or, rowSet);

		// Periodic bitmaps
		bitmap1 = createBitmap();
		for(int row = 3; row < 1_000_000; row += 3) {
			bitmap1.set(row);
		}

		bitmap2 = createBitmap();
		for(int row = 300; row < 1_000_000; row += 300) {
			bitmap2.set(row);
		}

		or = bitmap1.or(bitmap2);
		long matches = or.stream().peek(row -> {
			assertEquals(0, row % 3, "Unexpected row: " + row);
		}).count();
		assertEquals(333_333, matches, "Matches");

		bitmap1 = createBitmap();
		bitmap2 = createBitmap();
		rowSet = new HashSet<>();

		bitmap1.set(64*2);

		for (int row = 64; row < 4 * 64; ++row) {
			bitmap2.set(row);
			rowSet.add(row);
		}

		or = bitmap1.or(bitmap2);
		match(or, rowSet);
	}

	/**
	 * Test OR operations on bitmaps with unaligned sizes.
	 */
	@Test
	public void unalignedOr() {
		IBitmap bitmap1 = createBitmap();
		bitmap1.set(0);
		bitmap1.set(111);

		IBitmap bitmap2 = createBitmap();
		bitmap2.set(10_000);
		bitmap2.set(100_000);

		Set<Integer> rowSet = new HashSet<>();
		rowSet.add(0);
		rowSet.add(111);
		rowSet.add(10_000);
		rowSet.add(100_000);

		IBitmap or = bitmap1.or(bitmap2);
		match(or, rowSet);

		or = bitmap2.or(bitmap1);
		match(or, rowSet);

		bitmap1 = createBitmap();
		bitmap1.set(0);
		bitmap1.set(111);
		bitmap1.set(777_777_777);

		bitmap2 = createBitmap();
		bitmap2.set(0);
		bitmap2.set(100_000);

		rowSet = new HashSet<>();
		rowSet.add(0);
		rowSet.add(111);
		rowSet.add(100_000);
		rowSet.add(777_777_777);

		or = bitmap1.or(bitmap2);
		match(or, rowSet);
	}

	@Test
	public void testStream() {
		IBitmap bitmap = createBitmap();
		assertEquals(0, bitmap.stream().count());

		for (int i = 64; i < 2 * 64; ++i) {
			bitmap.set(i);
		}
		for (int i = 2*64+1; i < 3 * 64; ++i) {
			bitmap.set(i);
		}
		bitmap.set(64 * 3);
		bitmap.set(64 * 3 + 1);
		bitmap.set(64 * 3 + 10);
		bitmap.set(64 * 3 + 11);

		bitmap.set(64 * 5);
		bitmap.set(64 * 7);
		bitmap.set(64 * 8);

		bitmap.set(777_777_777);
		bitmap.set(777_777_778);
		bitmap.set(777_777_779);

		Set<Integer> rowSet = new HashSet<>();
		for (int i = 64; i < 2 * 64; ++i) {
			rowSet.add(i);
		}
		for (int i = 2 * 64 + 1; i < 3 * 64; ++i) {
			rowSet.add(i);
		}
		rowSet.add(64 * 3);
		rowSet.add(64 * 3 + 1);
		rowSet.add(64 * 3 + 10);
		rowSet.add(64 * 3 + 11);

		rowSet.add(64 * 5);
		rowSet.add(64 * 7);
		rowSet.add(64 * 8);

		rowSet.add(777_777_777);
		rowSet.add(777_777_778);
		rowSet.add(777_777_779);

		match(bitmap, rowSet);
	}

	protected static void match(final IBitmap bitmap, final Set<Integer> rowSet) {
		long matches = bitmap.stream().peek(row -> {
			assertTrue(rowSet.contains(row), "Unexpected row: " + row);
		}).count();
		assertEquals(rowSet.size(), matches, "Matches");
	}

}

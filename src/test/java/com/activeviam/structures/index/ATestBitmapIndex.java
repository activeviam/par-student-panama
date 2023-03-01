package com.activeviam.structures.index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.activeviam.structures.bitmap.IBitmap;
import org.junit.jupiter.api.Test;

/**
 * Generic tests on {@link IWritableBitmapIndex}.
 *
 * @author ActiveViam
 * @param <B> the type of the bitmap index
 */
public abstract class ATestBitmapIndex<B extends IWritableBitmapIndex> {

	// ANY markup
	protected static final int ANY = IWritableBitmapIndex.ANY;

	// Currencies
	protected static final int EUR = 0;
	protected static final int USD = 1;
	protected static final int GBP = 2;
	protected static final int JPY = 3;

	// Portfolios
	protected static final int PORTFOLIO_A = 0;
	protected static final int PORTFOLIO_B = 1;
	protected static final int PORTFOLIO_C = 2;
	protected static final int PORTFOLIO_D = 3;

	// Maturities
	protected static final int DATE_1 = 0;
	protected static final int DATE_2 = 1;

	protected abstract B createBitmapIndex(int levels);

	@Test
	public void testMatch() {
		IWritableBitmapIndex index = createBitmapIndex(3);

		index.append(new int[]{EUR, PORTFOLIO_A, DATE_1});
		index.append(new int[]{EUR, PORTFOLIO_B, DATE_1});
		index.append(new int[]{USD, PORTFOLIO_A, DATE_1});
		index.append(new int[]{USD, PORTFOLIO_C, DATE_1});
		index.append(new int[]{GBP, PORTFOLIO_A, DATE_1});
		index.append(new int[]{GBP, PORTFOLIO_D, DATE_2});
		index.append(new int[]{JPY, PORTFOLIO_B, DATE_1});
		index.append(new int[]{JPY, PORTFOLIO_D, DATE_2});
		index.append(new int[]{EUR, PORTFOLIO_D, DATE_2});
		index.append(new int[]{EUR, PORTFOLIO_C, DATE_2});

		assertEquals(10, index.size(), "Index tuple count");

		// Exact matches
		exactMatch(index, new int[]{EUR, PORTFOLIO_A, DATE_1}, 0);
		exactMatch(index, new int[]{EUR, PORTFOLIO_B, DATE_1}, 1);
		exactMatch(index, new int[]{GBP, PORTFOLIO_D, DATE_2}, 5);
		exactMatch(index, new int[]{EUR, PORTFOLIO_C, DATE_2}, 9);

		// No match
		rangeMatch(index, new int[]{EUR, PORTFOLIO_A, DATE_2});
		rangeMatch(index, new int[]{USD, PORTFOLIO_B, DATE_1});
		rangeMatch(index, new int[]{GBP, PORTFOLIO_C, DATE_2});
		rangeMatch(index, new int[]{JPY, PORTFOLIO_D, DATE_1});

		// Range match
		rangeMatch(index, new int[]{EUR, ANY, ANY}, 0, 1, 8, 9);
		rangeMatch(index, new int[]{USD, ANY, ANY}, 2, 3);
		rangeMatch(index, new int[]{ANY, PORTFOLIO_A, DATE_1}, 0, 2, 4);
		rangeMatch(index, new int[]{ANY, PORTFOLIO_B, DATE_1}, 1, 6);
		rangeMatch(index, new int[]{ANY, PORTFOLIO_C, ANY}, 3, 9);
		rangeMatch(index, new int[]{ANY, ANY, DATE_1}, 0, 1, 2, 3, 4, 6);
		rangeMatch(index, new int[]{ANY, ANY, DATE_2}, 5, 7, 8, 9);
		rangeMatch(index, new int[]{ANY, ANY, ANY}, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

		// Composite patterns
		rangeMatch(index, new int[][]{{ANY}, {ANY}, {ANY}}, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		rangeMatch(index, new int[][]{{EUR, USD}, {ANY}, {ANY}}, 0, 1, 2, 3, 8, 9);
		rangeMatch(index, new int[][]{{EUR, USD}, {PORTFOLIO_A}, {ANY}}, 0, 2);
		rangeMatch(index, new int[][]{{ANY}, {PORTFOLIO_A}, {ANY}}, 0, 2, 4);
		rangeMatch(index, new int[][]{{EUR, USD, GBP}, {PORTFOLIO_A, PORTFOLIO_B}, {DATE_1}}, 0, 1, 2, 4);
		rangeMatch(index, new int[][]{{EUR, USD, GBP, JPY},
				{PORTFOLIO_A, PORTFOLIO_B, PORTFOLIO_C, PORTFOLIO_D},
				{DATE_1, DATE_2}}, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

	}

	protected void exactMatch(IWritableBitmapIndex index, int[] pattern, int row) {
		int[] rows = match(index, pattern);
		assertEquals(1, rows.length, "Exact match should return a single row");
		assertEquals(row, rows[0], "Match row");
	}

	protected void rangeMatch(IWritableBitmapIndex index, int[] pattern, int... expected) {
		int[] rows = match(index, pattern);

		if (expected == null) {
			expected = new int[0];
		}
		assertEquals(expected.length, rows.length, "Range match row count");
		for (int row = 0; row < expected.length; row++) {
			assertEquals(expected[row], rows[row], "Match row");
		}
	}

	protected void rangeMatch(IWritableBitmapIndex index, int[][] pattern, int... expected) {
		int[] rows = match(index, pattern);
		if (expected == null) {
			expected = new int[0];
		}
		assertEquals(expected.length, rows.length, "Range match row count");
		for (int row = 0; row < expected.length; row++) {
			assertEquals(expected[row], rows[row], "Match row");
		}
	}

	/**
	 * Performs a search on the index and return a list of rows that match the pattern.
	 *
	 * @param pattern the search pattern, for each level it specifies the searched value or ANY
	 *        value.
	 * @return the rows that match the pattern
	 */
	protected int[] match(IWritableBitmapIndex index, int[] pattern) {
		IBitmap bitmap = index.matchBitmap(pattern);
		if (bitmap == null) {
			return new int[0];
		}

		return bitmap.stream().toArray();
	}

	/**
	 * Performs a search on the index and return a list of rows that match the pattern.
	 *
	 * @param compositePattern a composite pattern that for each level specifies a collection of possible
	 *        matches, instead of an exact match.
	 * @return the rows that match the pattern
	 */
	protected int[] match(IWritableBitmapIndex index, int[][] compositePattern) {
		IBitmap bitmap = index.matchBitmap(compositePattern);
		if (bitmap == null) {
			return new int[0];
		}

		return bitmap.stream().toArray();
	}

}

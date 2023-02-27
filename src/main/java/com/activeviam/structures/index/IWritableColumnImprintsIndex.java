package com.activeviam.structures.index;

public interface IWritableColumnImprintsIndex extends IColumnImprintsIndex {

	/**
	 * Appends a tuple in the index.
	 *
	 * @param tuple The tuple to consider
	 * @return new size of this index
	 */
	int append(final int[] tuple);

	/**
	 * Rebuilds some index levels if necessary.
	 */
	void rebuild();

	/**
	 * Truncates the index.
	 *
	 * @param newSize The new size of the index
	 * @param imprintVectorsSizes The number of imprint vectors of each level
	 * @param lastImprintVectors The last imprint vector of each level
	 * @param cachelineDictionariesSizes The number of cacheline dictionaries of each level
	 * @param lastCachelineDictionaries The last cacheline dictionary of each level
	 */
	void truncate(final int newSize,
			final int[] imprintVectorsSizes,
			final long[] lastImprintVectors,
			final int[] cachelineDictionariesSizes,
			final int[] lastCachelineDictionaries);

}

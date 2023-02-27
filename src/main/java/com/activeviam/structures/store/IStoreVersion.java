package com.activeviam.structures.store;

import com.activeviam.mvcc.IVersion;
import com.activeviam.structures.bitmap.IBitmap;
import java.util.stream.Stream;

/**
 * {@link IVersion} for {@link IMultiVersionStore}.
 *
 * @author ActiveViam
 */
public interface IStoreVersion extends IVersion, ITable {

	/**
	 * @return the number of records alive at this version
	 */
	@Override
	int size();

	IBitmap findRows(final int[] pattern);
	IBitmap findRows(final int[][] compositePattern);

	default Stream<IRecord> scan(int[] pattern) {
		return findRows(pattern).stream().mapToObj(this::getRecord);
	}

	default Stream<IRecord> scan(int[][] compositePattern) {
		return findRows(compositePattern).stream().mapToObj(this::getRecord);
	}

}

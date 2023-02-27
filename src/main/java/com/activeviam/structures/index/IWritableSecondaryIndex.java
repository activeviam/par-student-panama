package com.activeviam.structures.index;

import com.activeviam.structures.store.IRecord;

/**
 * A writable {@link ISecondaryIndex}
 *
 * @author ActiveViam
 */
public interface IWritableSecondaryIndex extends ISecondaryIndex {

	void index(IRecord record, int row);

	void remove(IRecord record, int row);

}

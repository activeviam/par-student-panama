package com.activeviam.structures.index;

import com.activeviam.mvcc.ITransaction;

/**
 * Transaction for {@link IMultiVersionSecondaryIndex}.
 *
 * @author ActiveViam
 */
public interface ISecondaryIndexTransaction extends ITransaction, IWritableSecondaryIndex {

}

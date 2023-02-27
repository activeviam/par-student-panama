package com.activeviam;

import com.activeviam.mvcc.IEpoch;
import com.activeviam.mvcc.impl.Epoch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * @author ActiveViam
 */
public class MvccTestUtil {

	public static Supplier<IEpoch> createSupplier() {
		return new Supplier<>() {

			private final AtomicLong epoch = new AtomicLong(IEpoch.INITIAL_EPOCH_ID);
			@Override
			public IEpoch get() {
				return new Epoch(epoch.incrementAndGet());
			}

		};
	}

}

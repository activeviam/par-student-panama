package com.activeviam.structures.store.impl;

import static com.activeviam.structures.index.IBitmapIndex.ANY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.activeviam.MvccTestUtil;
import com.activeviam.mvcc.IEpoch;
import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.store.IMultiVersionStore;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.IStoreTransaction;
import com.activeviam.structures.store.IStoreVersion;
import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for implementations of {@link AMultiVersionStore}.
 *
 * @author ActiveViam
 */
public abstract class ATestMultiVersionStore {

	protected Supplier<IEpoch> epochSupplier;

	@BeforeEach
	public void before() {
		epochSupplier = MvccTestUtil.createSupplier();
	}

	@AfterEach
	public void after() {
		epochSupplier = null;
	}

	protected StoreFormat defaultFormat() {
		return new StoreFormat(4, 2, new int[] {0}, 128);
	}

	protected AMultiVersionStore create() {
		return create(defaultFormat());
	}

	protected abstract AMultiVersionStore create(StoreFormat format);

	@Test
	public void test() {
		final IMultiVersionStore mv = create();

		IStoreTransaction t = mv.getOrCreateTransaction();
		t.submitRecord(create(0, 0, 0, 0, 10, 100));
		t.submitRecord(create(1, 1, 1, 1, 20, 200));
		t.submitRecord(create(2, 2, 2, 2, 30, 300));
		final IStoreVersion v1 = mv.commit(epochSupplier.get());
		assertEquals(3, v1.size());

		t = mv.getOrCreateTransaction();
		t.submitRecord(create(1, 1, 1, 1, 20, 201));
		t.submitRecord(create(2, 3, 3, 3, 30, 301));
		t.submitRecord(create(3, 4, 4, 4, 40, 400));
		final IStoreVersion v2 = mv.commit(epochSupplier.get());

		assertEquals(3, v1.size());
		assertEquals(4, v2.size());

		t = mv.getOrCreateTransaction();
		t.deleteRecord(createKey(1));
		final IStoreVersion v3 = mv.commit(epochSupplier.get());

		assertEquals(3, v1.size());
		assertEquals(4, v2.size());
		assertEquals(3, v3.size());

		// Tests on IStoreVersion.findRows

		// patterns
		assertArrayEquals(new int[] {2}, v1.findRows(new int[] {2, ANY, ANY}).stream().toArray());
		assertArrayEquals(new int[0], v2.findRows(new int[] {2, ANY, ANY}).stream().toArray());
		assertArrayEquals(new int[0], v3.findRows(new int[] {1, ANY, ANY}).stream().toArray());

		// composite patterns
		assertArrayEquals(new int[] {1, 2}, v1.findRows(new int[][] {{1, 2}, {ANY}, {ANY}}).stream().toArray());
		assertArrayEquals(new int[] {3}, v2.findRows(new int[][] {{1, 2}, {ANY}, {ANY}}).stream().toArray());
		assertArrayEquals(new int[0], v3.findRows(new int[][] {{1, 2}, {ANY}, {ANY}}).stream().toArray());
	}

	@Test
	public void testBigTransaction() {
		final StoreFormat format = defaultFormat();
		final IMultiVersionStore mv = create(format);
		final int chunkSize = format.chunkSize;
		final int size = chunkSize * 100;
		final List<IRecord> records = new ArrayList<>(size);

		for (int i = 0; i < size; ++i) {
			records.add(create(i, i / chunkSize, i % chunkSize, i % (chunkSize / 2), i, 42D));
		}

		IStoreTransaction t = mv.getOrCreateTransaction();
		t.submitRecords(records);
		final IStoreVersion v1 = mv.commit(epochSupplier.get());

		assertEquals(size, v1.size());

		// Tests on IStoreVersion.findRows

		assertArrayEquals(IntStream.range(chunkSize, 2 * chunkSize).toArray(),
				v1.findRows(new int[] {1, ANY, ANY}).stream().toArray());

		assertArrayEquals(new int[] {chunkSize, chunkSize + chunkSize / 2},
				v1.findRows(new int[] {1, ANY, 0}).stream().toArray());

		assertArrayEquals(new int[] {1, 2 + chunkSize / 2, chunkSize + 1, chunkSize + 2 + chunkSize / 2},
				v1.findRows(new int[][] {{0, 1}, {0, 1, 2 + chunkSize / 2}, {1, 2}}).stream().toArray());

		// Remove all
		t = mv.getOrCreateTransaction();
		for (int i = 0; i < size; ++i) {
			t.deleteRecord(createKey(i));
		}

		assertArrayEquals(IntStream.range(chunkSize, 2 * chunkSize).toArray(),
				v1.findRows(new int[] {1, ANY, ANY}).stream().toArray());

		final IStoreVersion v2 = mv.commit(epochSupplier.get());

		assertArrayEquals(IntStream.range(chunkSize, 2 * chunkSize).toArray(),
				v1.findRows(new int[] {1, ANY, ANY}).stream().toArray());

		assertArrayEquals(new int[0], v2.findRows(new int[] {ANY, ANY, ANY}).stream().toArray());
	}

	@Test
	public void testDelete() {
		final AMultiVersionStore mv = create();

		IStoreTransaction t = mv.getOrCreateTransaction();
		t.submitRecord(create(0, 0, 0, 0, 10, 100));
		t.submitRecord(create(1, 1, 1, 1, 30, 300));
		final IStoreVersion v1 = mv.commit(epochSupplier.get());

		assertEquals(2, v1.size());

		t = mv.getOrCreateTransaction();
		t.deleteRecord(createKey(1));
		final IStoreVersion v2 = mv.commit(epochSupplier.get());

		assertEquals(1, v2.size());
	}

	@Test
	public void testConcurrency() {
		final IMultiVersionStore mv = create();

		IStoreTransaction t = mv.getOrCreateTransaction();
		for (int i = 0; i < 256; ++i) {
			t.submitRecord(create(i, i / 4, i % 128, i, 10D, 100D));
		}
		final IStoreVersion v1 = mv.commit(epochSupplier.get());

		final int[] pattern = new int[] {ANY, 100, 100};
		final IBitmap expected = v1.findRows(pattern);

		final RecursiveAction writes = new RecursiveAction() {

			/** For serialization (from superclass) */
			private static final long serialVersionUID = 1L;

			@Override
			protected void compute() {
				System.out.println("Start writting");
				int i = 256;

				for (int j = 0; j < 1_000; ++j) {
					if (isCancelled()) {
						System.out.println("Writting cancelled");
						return;
					}
					IStoreTransaction t = mv.getOrCreateTransaction();
					for (int end = i + 80; i < end; ++i) {
						t.submitRecord(create(i, 0, i - 128, i, 10D, 100D));
					}
					mv.commit(epochSupplier.get());
				}

				System.out.println("End writting");
			}
		};

		final long start = System.nanoTime();
		writes.fork();

		System.out.println("Start reading");
		try {
			while (!writes.isDone()) {
				IBitmap actual = v1.findRows(pattern);
				assertEquals(expected, actual);
			}
		} catch (Throwable e) {
			writes.cancel(true);
			throw e;
		}
		System.out.println("End reading");
		final long end = System.nanoTime();
		System.out.println(TimeUnit.NANOSECONDS.toSeconds(end - start) + "s");
		writes.join();
	}

	protected IRecord create(int key, int f2, int f3, int f4, double v1, double v2) {
		return new Record(new int[] {key, f2, f3, f4}, new double[] {v1, v2});
	}

	protected IRecord createKey(int key) {
		return new Record(new int[] {key}, null);
	}

}

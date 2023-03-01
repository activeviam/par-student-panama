package com.activeviam.structures.array.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.activeviam.mvcc.impl.Epoch;
import com.activeviam.structures.array.IIntegerArray;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import org.junit.jupiter.api.Test;

/**
 * Tests on {@link MultiVersionIntegerArray}.
 *
 * @author ActiveViam
 */
public class TestMultiVersionIntegerArray {

	@Test
	public void test() {
		final int emptyValue = -1;
		final MultiVersionIntegerArray mv = new MultiVersionIntegerArray(emptyValue);

		final IIntegerArray v0 = mv.getMostRecentVersion();
		assertEquals(emptyValue, v0.getEmptyValue());
		assertEquals(0, v0.size());

		IntegerArrayTransaction t1 = mv.getOrCreateTransaction();
		t1.set(0, 5);
		assertEquals(emptyValue, t1.getEmptyValue());
		final IIntegerArray v1 = mv.commit(new Epoch(1));

		assertEquals(1, v1.size());
		assertEquals(5, v1.getValue(0));
		assertEquals(emptyValue, v1.getEmptyValue());

		IntegerArrayTransaction t2 = mv.getOrCreateTransaction();
		t2.set(1, 10);
		t2.set(0, 1);
		final IIntegerArray v2 = mv.commit(new Epoch(2));

		assertEquals(2, v2.size());
		assertEquals(1, v2.getValue(0));
		assertEquals(10, v2.getValue(1));

		assertEquals(1, v1.size());
		assertEquals(5, v1.getValue(0));
		assertEquals(emptyValue, v1.getValue(1));
}

	@Test
	public void testTransactionIsolation() {
		final int emptyValue = 5;
		final MultiVersionIntegerArray mv = new MultiVersionIntegerArray(emptyValue);
		final int v1Size = 20;

		for (int i = 0; i < v1Size; ++i) {
			mv.getOrCreateTransaction().set(i, i);
		}

		final IIntegerArray v1 = mv.commit(new Epoch(1));

		// Change data in transaction
		for (int i = 0; i < 2 * v1Size; ++i) {
			mv.getOrCreateTransaction().set(i, i + 1);
			assertEquals(Math.max(v1Size, i + 1), mv.getOrCreateTransaction().size());
		}

		// Check the version
		assertEquals(v1Size, v1.size());
		for (int i = 0; i < v1Size; ++i) {
			assertEquals(i, v1.getValue(i));
		}

		// Change again data in transaction
		for (int i = 2 * v1Size - 1; i >= 0; --i) {
			mv.getOrCreateTransaction().set(i, emptyValue);
		}

		// Check agin the version
		assertEquals(v1Size, v1.size());
		for (int i = 0; i < v1Size; ++i) {
			assertEquals(i, v1.getValue(i));
		}
	}

	@Test
	public void testEmptyValue() {
		final int emptyValue = new TIntIntHashMap().getNoEntryValue();
		final MultiVersionIntegerArray mv = new MultiVersionIntegerArray(emptyValue);

		mv.getOrCreateTransaction().set(0, emptyValue + 100);
		mv.commit(new Epoch(1));

		IntegerArrayTransaction t = mv.getOrCreateTransaction();
		t.set(0, emptyValue);
		assertEquals(emptyValue, t.getValue(0));

		IntegerArrayVersion v2 = mv.commit(new Epoch(2));
		assertEquals(emptyValue, v2.getValue(0));

		t = mv.getOrCreateTransaction();
		t.set(0, emptyValue + 200);
		mv.commit(new Epoch(3));

		assertEquals(emptyValue, v2.getValue(0));
	}

	/**
	 * Parallel reads and writes
	 */
	@Test
	public void testConcurrency() {
		final int emptyValue = 5;
		final MultiVersionIntegerArray mv = new MultiVersionIntegerArray(emptyValue);

		ForkJoinTask<?> reader = ForkJoinPool.commonPool().submit(() -> {
			for (int i = 0; i < 1000; i++) {
				final IntegerArrayVersion v = mv.getMostRecentVersion();
				final int epochId = (int) v.getEpoch().getId();
//				System.out.println("Reader " + epochId);

				if (epochId == 0) {
					assertEquals(0, v.size());
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				} else if ((epochId & 1) == 1) {
					int size = epochId;
					assertEquals(size, v.size());
					for (int j = 0; j < size; j++) {
						if ((j & 1) == 0) {
							assertEquals(epochId, v.getValue(j), v.getEpoch().toString() + " " + j);
						} else {
							if (j < size /2) {
								assertEquals(epochId - 1, v.getValue(j), v.getEpoch().toString() + " " + j);
							} else {
								assertEquals(emptyValue, v.getValue(j), v.getEpoch().toString() + " " + j);
							}
						}
					}

				} else {
					int size = epochId - 1;
					assertEquals(size, v.size());
					for (int j = 0; j < epochId / 2; j++) {
						assertEquals(epochId, v.getValue(j), v.getEpoch().toString() + " " + j);
					}

					for (int j = epochId / 2; j < size; j++) {
						if ((j & 1) == 1) {
							assertEquals(emptyValue, v.getValue(j), v.getEpoch().toString() + " " + j);
						} else {
							assertEquals(epochId - 1, v.getValue(j), v.getEpoch().toString() + " " + j);
						}
					}

				}
				Thread.yield();
			}
		});

		ForkJoinTask<?> writer = ForkJoinPool.commonPool().submit(() -> {
			for (int i = 1; i < 1000; i++) {
//				System.out.println("Writer " + i);
				IntegerArrayTransaction t = mv.getOrCreateTransaction();
				if ((i & 1) == 1) {
					for (int j = 0; j <= i; j+=2) {
						t.set(j, i);
					}

				} else {
					for (int j = 0; j < i/2; ++j) {
						t.set(j, i);
					}
				}
				mv.commit(new Epoch(i));

				Thread.yield();
			}
		});

		reader.join();
		writer.join();
	}

	/**
	 * Parallel reads and writes on a single index to spot concurrency issues in
	 * {@link IntegerArrayVersion#getValue(int)}
	 */
	@Test
	public void testGetValue() {
		final int emptyValue = -1;
		final MultiVersionIntegerArray mv = new MultiVersionIntegerArray(emptyValue);

		ForkJoinTask<?> reader = ForkJoinPool.commonPool().submit(() -> {
			for (int i = 0; i < 1000; i++) {
				final IntegerArrayVersion v = mv.getMostRecentVersion();
				final int epochId = (int) v.getEpoch().getId();
				// System.out.println("Reader " + epochId);

				if (epochId == 0) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				} else {
					for (int j = 0; j < 100; j++) {
						assertEquals(epochId, v.getValue(0));
					}
				}
				Thread.yield();
			}
		});

		ForkJoinTask<?> writer = ForkJoinPool.commonPool().submit(() -> {
			for (int i = 1; i < 1000; i++) {
				//System.out.println("Writer " + i);
				IntegerArrayTransaction t = mv.getOrCreateTransaction();
				t.set(0, i);
				mv.commit(new Epoch(i));
				Thread.yield();
			}
		});

		reader.join();
		writer.join();
	}

	/**
	 * Parallel reads and writes on a single index to spot concurrency issues when iterating on the
	 * version chain in {@link IntegerArrayVersion#getValue(int)}
	 */
	@Test
	public void testGetValue2() {
		final int emptyValue = -1;
		final MultiVersionIntegerArray mv = new MultiVersionIntegerArray(emptyValue);

		ForkJoinTask<?> reader = ForkJoinPool.commonPool().submit(() -> {
			for (int i = 0; i < 1000; i++) {
				final IntegerArrayVersion v = mv.getMostRecentVersion();
				final int epochId = (int) v.getEpoch().getId();
//				System.out.println("Reader " + epochId);

				if (epochId == 0) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				} else if ((epochId & 1) == 1) {
					for (int j = 0; j < 100; j++) {
						assertEquals(epochId, v.getValue(0));
					}
				} else {
					for (int j = 0; j < 100; j++) {
						assertEquals(epochId - 1, v.getValue(0));
					}
				}
				Thread.yield();
			}
		});

		ForkJoinTask<?> writer = ForkJoinPool.commonPool().submit(() -> {
			for (int i = 1; i < 1000; i++) {
//				System.out.println("Writer " + i);
				IntegerArrayTransaction t = mv.getOrCreateTransaction();
				if ((i & 1) == 1) {
					t.set(0, i);
				} else {
					t.set(1, i);
				}
				mv.commit(new Epoch(i));
				Thread.yield();
			}
		});

		reader.join();
		writer.join();
	}

}

package com.activeviam.structures.store.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.activeviam.MvccTestUtil;
import com.activeviam.mvcc.IEpoch;
import com.activeviam.structures.store.IMultiVersionStore;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.impl.AMultiVersionStore.StoreFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestMultiVersionStoreMemory {

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
    return new StoreFormat(4, 2, new int[] {0}, 2 * 1024);
  }

  protected AMultiVersionStore createV1(StoreFormat format) {
    return new MultiVersionStoreV1(format);
  }

  protected AMultiVersionStore createV2(StoreFormat format) {
    return new MultiVersionStoreV2(format);
  }

  protected AMultiVersionStore createV3(StoreFormat format) {
    return new MultiVersionStoreV3(format);
  }

  protected IRecord create(int key, int f2, int f3, int f4, double v1, double v2) {
    return new Record(new int[] {key, f2, f3, f4}, new double[] {v1, v2});
  }

  protected IRecord createKey(int key) {
    return new Record(new int[] {key}, null);
  }

	@Test
	public void testMemoryConsumption() {
		final StoreFormat format = defaultFormat();
		final IMultiVersionStore mv1 = createV1(format);
		final IMultiVersionStore mv2 = createV2(format);
		final IMultiVersionStore mv3 = createV3(format);
		final int chunkSize = format.chunkSize;
		final int size = chunkSize * 100;
		final List<IRecord> records = new ArrayList<>(size);

		final boolean random = false;
		if (random) {
			System.out.println("Generating random data");
			Random r = new Random(123);
			for (int i = 0; i < size; ++i) {
				records.add(create(i, i / chunkSize, r.nextInt(100), r.nextInt(1000), i, 42D));
			}
		} else {
			System.out.println("Generating data");
			for (int i = 0; i < size; ++i) {
				records.add(create(i, i / chunkSize, i % chunkSize, i % (chunkSize / 2), i, 42D));
			}
		}


		System.out.println("Submitting data");
		Stream.of(mv1, mv2, mv3).parallel().forEach(mv -> {
			mv.getOrCreateTransaction().submitRecords(records);
		});

		System.out.println("Committing data");
		Stream.of(mv1, mv2, mv3).parallel().forEach(mv -> {
			mv.commit(epochSupplier.get());
		});
		System.out.println();

		printSize(mv1.sizeInBytes());
		printSize(mv2.sizeInBytes());
		printSize(mv3.sizeInBytes());

		assertTrue(mv1.sizeInBytes() > mv2.sizeInBytes());
	}

	@Test
	public void testMemoryConsumptionAfterDiscard() {
		final StoreFormat format = defaultFormat();
		final IMultiVersionStore mv2 = createV2(format);
		final int chunkSize = format.chunkSize;
		final int size = chunkSize * 100;
		final List<IRecord> records = new ArrayList<>(size);

		final boolean random = false;
		if (random) {
			System.out.println("Generating random data");
			Random r = new Random(123);
			for (int i = 0; i < size; ++i) {
				records.add(create(i, i / chunkSize, r.nextInt(100), r.nextInt(1000), i, 42D));
			}
		} else {
			System.out.println("Generating data");
			for (int i = 0; i < size; ++i) {
				records.add(create(i, i / chunkSize, i % chunkSize, i % (chunkSize / 2), i, 42D));
			}
		}

		System.out.println("Submitting data");
		mv2.getOrCreateTransaction().submitRecords(records);
		System.out.println("Committing data");
		mv2.commit(epochSupplier.get());
		System.out.println();
		long sizeAfterAdds = mv2.sizeInBytes();

		System.out.println("Removing data");
		// Remove all the records in the chunks whose id is even
		IntStream.range(0, size / chunkSize).filter(chunk -> (chunk % 2) == 0)
			.flatMap(chunk -> IntStream.range(chunk * chunkSize, (chunk + 1) * chunkSize))
			.mapToObj(this::createKey)
			.forEach(mv2.getOrCreateTransaction()::deleteRecord);

		System.out.println("Committing data");
		mv2.commit(epochSupplier.get());
		long sizeAfterRemovals = mv2.sizeInBytes();

		mv2.discardBefore(mv2.getMostRecentVersion().getEpoch().getId());
		long sizeAfterDiscard = mv2.sizeInBytes();

		System.out.println();
		printSize(sizeAfterAdds);
		printSize(sizeAfterRemovals);
		printSize(sizeAfterDiscard);

		assertTrue(sizeAfterDiscard < sizeAfterAdds);
	}

	protected void printSize(long bytes) {
		System.out.println(String.format("%,11d KB", bytes / 1024));
	}
}

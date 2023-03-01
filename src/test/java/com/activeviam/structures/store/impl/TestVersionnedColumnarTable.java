package com.activeviam.structures.store.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.impl.ColumnarTable.TableFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VersionedColumnarTable}.
 *
 * @author ActiveViam
 */
public class TestVersionnedColumnarTable extends TestColumnarTable {

	@Override
	protected VersionedColumnarTable create() {
		return new VersionedColumnarTable(defaultFormat());
	}


	/**
	 * Test the {@link VersionedColumnarTable#discardBefore(long)} method
	 */
	@Test
	public void testDiscardBefore() {
		final int chunkSize = 4;

		final VersionedColumnarTable table = new VersionedColumnarTable(new TableFormat(2, 1, 4));
		final IntFunction<IRecord> recordFacory = key -> new Record(new int[] {key, key / 3}, new double[] {2D * key});

		int v1Size = 3 * chunkSize - 1;
		int v2Size = 4 * chunkSize;

		for (int i = 0; i < v1Size; i++) {
			table.append(recordFacory.apply(i));
		}
		table.commit(0, 1, Collections.emptySet());


		table.discardBefore(0);
		table.discardBefore(1);
		for (int c = 0; c < v1Size % chunkSize; c++) {
			assertNotNull(table.chunks[c], String.valueOf(c));
		}

		Set<Integer> deletions = new HashSet<>();
		// Remove all the rows of the first chunk and one of the second chunk
		for (int i = 0; i < chunkSize + 1; i++) {
			deletions.add(i);
		}

		for (int i = v1Size; i < v2Size; i++) {
			table.append(recordFacory.apply(i));
		}
		table.commit(v1Size, 2, deletions);

		table.discardBefore(2);
		assertNull(table.chunks[0]);
		for (int c = 1; c < v2Size % chunkSize; c++) {
			assertNotNull(table.chunks[c], String.valueOf(c));
		}

		assertEquals(table.deletedRows[0], chunkSize);
		assertEquals(table.deletedRows[1], 1);

		long epoch = 2;
		for (int i = chunkSize + 1; i < 2 * chunkSize; i++) {
			table.commit(v2Size, ++epoch, Collections.singleton(i));
		}

		for (long i = 2; i < epoch; i++) {
			table.discardBefore(i);
			assertNotNull(table.chunks[1]);
		}

		table.discardBefore(2 * epoch);
		assertNull(table.chunks[1]);
	}

}

package com.activeviam.structures.table.impl;

import com.activeviam.chunk.IChunk;
import com.activeviam.chunk.IChunkAllocator;
import com.activeviam.chunk.SegmentMemoryAllocator;
import com.activeviam.structures.bitmap.IBitmap;
import com.activeviam.structures.bitmap.impl.BitSetBitmap;
import com.activeviam.structures.store.impl.ColumnarTable;
import com.activeviam.structures.store.impl.ColumnarTable.TableFormat;
import com.activeviam.structures.store.impl.Record;
import com.activeviam.vector.IVectorAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySession;

import static com.activeviam.Types.INTEGER;

public class TestColumnarTable {

	@Test
	public void testFindRows() {
		int chunkSize = 4;
		final ColumnarTable table = new ColumnarTable(new TableFormat(3, 2, 4));
		for (int i = 0; i < 2 * chunkSize + 1; i++) {
			table.append(new Record(new int[] {i,  i *2, i % 4}, new double[] {i * 1D, 1D}));
		}

		table.print();

		IBitmap expected = new BitSetBitmap();
		expected.set(1);
		Assertions.assertEquals(expected, table.findRows(new int[] {1, 2, 1}));

		expected.clear();
		expected.set(2);
		expected.set(6);
		Assertions.assertEquals(expected, table.findRows(new int[] {-1, -1, 2}));
	}

	@Test
	public void testFindRowsSIMD() {
		MemorySession memorySession = MemorySession.openConfined();
		IChunkAllocator VECTOR_ALLOCATOR = new SegmentMemoryAllocator(memorySession);
		int chunkSize = 4;
		final ColumnarTable table = new ColumnarTable(new TableFormat(3, 2, 4), VECTOR_ALLOCATOR);
		for (int i = 0; i < 2 * chunkSize + 1; i++) {
			table.append(new Record(new int[] {i,  i *2, i % 4}, new double[] {i * 1D, 1D}));
		}

		table.print();

		IBitmap expected = new BitSetBitmap();
		expected.set(1);
		Assertions.assertEquals(expected, table.findRowsSIMD(new int[] {1, 2, 1}));

		expected.clear();
		expected.set(2);
		expected.set(6);
		Assertions.assertEquals(expected, table.findRowsSIMD(new int[] {-1, -1, 2}));
	}

}

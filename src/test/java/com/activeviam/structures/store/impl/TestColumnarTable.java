package com.activeviam.structures.store.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.impl.ColumnarTable.TableFormat;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ColumnarTable}.
 *
 * @author ActiveViam
 */
public class TestColumnarTable {

	@Test
	public void test() {
		final ColumnarTable t = create();
		assertEquals(0, t.size());

		final IRecord r1 = create(0, 0, 0, 0, 10, 100);
		t.append(r1);
		assertEquals(1, t.size());

		final IRecord r2 = create(1, 1, 1, 1, 20, 200);
		t.append(r2);
		assertEquals(2, t.size());

		final IRecord r3 = create(0, 0, 0, 0, 10, 100);
		t.append(r3);
		assertEquals(3, t.size());

		assertEquals(r1, t.getRecord(0));
		assertEquals(r2, t.getRecord(1));
		assertEquals(r3, t.getRecord(2));

		for (int i = 0; i < r1.getAttributes().length; i++) {
			assertEquals(r1.readInt(i), t.readInt(0, i));
		}
		for (int i = 0; i < r1.getValues().length; i++) {
			assertEquals(r1.readDouble(i), t.readDouble(0, i));
		}

		t.ensureCapacity(1024);
		assertEquals(3, t.size());
		assertEquals(r1, t.getRecord(0));
		assertEquals(r2, t.getRecord(1));
		assertEquals(r3, t.getRecord(2));
	}

	protected TableFormat defaultFormat() {
		return new TableFormat(4, 2, 16);
	}

	protected ColumnarTable create() {
		return new ColumnarTable(defaultFormat());
	}

	protected IRecord create(int key, int f2, int f3, int f4, double v1, double v2) {
		return new Record(new int[] {key, f2, f3, f4}, new double[] {v1, v2});
	}

}

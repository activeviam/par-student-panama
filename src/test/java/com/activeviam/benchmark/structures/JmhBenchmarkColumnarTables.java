/*
 * (C) ActiveViam 2021
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.benchmark.structures;

import com.activeviam.chunk.IChunkAllocator;
import com.activeviam.chunk.SegmentMemoryAllocator;
import com.activeviam.structures.store.IRecord;
import com.activeviam.structures.store.impl.ColumnarTable;
import com.activeviam.structures.store.impl.Record;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.foreign.MemorySession;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 20, time = 100, timeUnit = MILLISECONDS)
@Measurement(iterations = 20, time = 100, timeUnit = MILLISECONDS)
@Fork(1)
public class JmhBenchmarkColumnarTables {
	protected IChunkAllocator CHUNK_ALLOCATOR;

	@Param({"random", /* "zero" */})
	protected static String TABLE_CONTENT;

	@Param({"1000", "100000"})
	protected static int TABLE_SIZE;

	@Param({"8", /* "32", "64", "128"*/})
	protected int CHUNK_SIZE;

	@Param({"10", "100", "1000"})
	protected int NB_OF_ATTRIBUTES;

	@Param({"10", /* "100000", "10000000", "100000000" */})
	protected int NB_OF_VALUES;

	protected static ColumnarTable TABLE;

	protected int[] PREDICATE;
	@Setup(Level.Trial)
	public void setChunkAllocator() {
		MemorySession memorySession = MemorySession.openConfined();
		CHUNK_ALLOCATOR = new SegmentMemoryAllocator(memorySession);
	}
	@Setup(Level.Trial)
	public void setTable() {
		TABLE = new ColumnarTable(new ColumnarTable.TableFormat(NB_OF_ATTRIBUTES, NB_OF_VALUES, CHUNK_SIZE), CHUNK_ALLOCATOR);
		for (int i = 0; i < TABLE_SIZE; i++) {
			TABLE.append(Record());
		}
		PREDICATE = new int[NB_OF_ATTRIBUTES];
		if (TABLE_CONTENT.equals("random")) {
			Random rand = new Random();
			for (int i = 0; i < NB_OF_ATTRIBUTES; i++) {
				int r = rand.nextInt(2);
				PREDICATE[i] = rand.nextInt(10)*(1-r) - r;
			}
		} else if (TABLE_CONTENT.equals("zero")) {
			Arrays.fill(PREDICATE, 0);
		}
	}

	@Benchmark
	public void findRows(Blackhole blackhole) {
		blackhole.consume(TABLE.findRows(PREDICATE));
	}

	@Benchmark
	public void findRowsSIMD(Blackhole blackhole) {
		blackhole.consume(TABLE.findRowsSIMD(PREDICATE));
	}

	protected IRecord Record() {
		int attributes[] = new int[NB_OF_ATTRIBUTES];
		double values[] = new double[NB_OF_VALUES];

		if (TABLE_CONTENT.equals("random")) {
			Random rand = new Random();
			for (int i = 0; i < NB_OF_ATTRIBUTES; i++) {
				attributes[i] = rand.nextInt(10);
			}
		} else if (TABLE_CONTENT.equals("zero")) {
			Arrays.fill(attributes, 0);
		}
		Arrays.fill(values, 0);
		return new Record(attributes, values);
	}
}

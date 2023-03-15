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
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 20, time = 100, timeUnit = MILLISECONDS)
@Measurement(iterations = 20, time = 100, timeUnit = MILLISECONDS)
@Fork(1)
public class JmhBenchmarkColumnarTables {
	@Param({"1000", /* "100000", "10000000", "100000000" */})
	protected static int TABLE_SIZE;
	protected MemorySession memorySession;

	protected IChunkAllocator CHUNK_ALLOCATOR;

	@Param({"16", /* "32", "64", "128"*/})
	protected int CHUNK_SIZE;

	@Param({"10", /* "100000", "10000000", "100000000" */})
	protected int NB_OF_ATTRIBUTES;

	@Param({"10", /* "100000", "10000000", "100000000" */})
	protected int NB_OF_VALUES;

	protected static ColumnarTable ZERO_TABLE;

	@Setup(Level.Trial)
	public void setupChunkAllocator() {
		memorySession = MemorySession.openConfined();
		CHUNK_ALLOCATOR = new SegmentMemoryAllocator(memorySession);
	}

	@Setup(Level.Trial)
	public void setZeroTable() {
		ZERO_TABLE = new ColumnarTable(new ColumnarTable.TableFormat(NB_OF_ATTRIBUTES, NB_OF_VALUES, CHUNK_SIZE), CHUNK_ALLOCATOR);
		for (int i = 0; i < TABLE_SIZE; i++) {
			ZERO_TABLE.append(zeroRecord());
		}
	}

	@Benchmark
	public void findRows(Blackhole blackhole) {
		int predicate[] = new int[NB_OF_ATTRIBUTES];
		Arrays.fill(predicate, 0);
		blackhole.consume(ZERO_TABLE.findRows(predicate));
	}

	@Benchmark
	public void findRowsSIMD(Blackhole blackhole) {
		int predicate[] = new int[NB_OF_ATTRIBUTES];
		Arrays.fill(predicate, 0);
		blackhole.consume(ZERO_TABLE.findRowsSIMD(predicate));
	}

	protected IRecord zeroRecord() {
		int attributes[] = new int[NB_OF_ATTRIBUTES];
		double values[] = new double[NB_OF_VALUES];
		Arrays.fill(attributes, 0);
		Arrays.fill(values, 0);
		return new Record(attributes, values);
	}
}

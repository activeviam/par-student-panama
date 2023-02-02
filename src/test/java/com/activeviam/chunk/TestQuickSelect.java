package com.activeviam.chunk;

import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySession;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestQuickSelect {
	private void testPartition(int[] arr, int[] expectedArr, int expectedPartition) {
		int partitionIdx = SegmentIntegerBlock.partition(arr, 0, arr.length);
		assertArrayEquals(arr, expectedArr);
		assertEquals(partitionIdx, expectedPartition);
	}
	
	@Test
	public void testPartition1() {
		testPartition(
			new int[] { 1, 2, 3, 4, 5, 6, 7 },
			new int[] { 1, 2, 3, 4, /**/ 5, 6, 7 },
			4
		);
	}
	@Test
	public void testPartition2() {
		testPartition(
			new int[] { 7, 6, 5, 4, 3, 2, 1 },
			new int[] { 1, 2, 3, 4, /**/ 5, 6, 7 },
			4
		);
	}
	@Test
	public void testPartition3() {
		testPartition(
			new int[] { 1, 2, 3, 4, 4, 4, 1, 2, 3 },
			new int[] { 1, 2, 3, 3, 2, 1, /**/ 4, 4, 4 },
			6
		);
	}
	@Test
	public void testPartition4() {
		testPartition(
			new int[] { 1, 2 },
			new int[] { 1, /**/ 2 },
			1
		);
	}
	@Test
	public void testPartition5() {
		testPartition(
				new int[] { 0, 0, 0, 2, 1, 0, 2, 0, 0 },
				new int[] { 0, 0, 0, 0, 0, 0, /**/ 2, 1, 2 },
				6
		);
	}
	
	
	private void testPartitionSimd(int[] arr, int[] expectedArr, int expectedPartition) {
		var buf = new int[arr.length];
		int partitionIdx = SegmentIntegerBlock.partitionSimd(arr, buf, 0, arr.length);
		assertArrayEquals(arr, expectedArr);
		assertEquals(partitionIdx, expectedPartition);
	}
	
	@Test
	public void testPartitionSimd1() {
		testPartitionSimd(
			new int[] { 1, 2, 3, 4, 5, 6, 7 },
			new int[] { 1, 2, 3, /**/ 4, 7, 5, 6 },
			3
		);
	}
	@Test
	public void testPartitionSimd2() {
		testPartitionSimd(
			new int[] { 7, 6, 5, 4, 3, 2, 1 },
			new int[] { 1, 3, 2, /**/ 4, 7, 6, 5 },
			3
		);
	}
	@Test
	public void testPartitionSimd3() {
		testPartitionSimd(
			new int[] { 1, 2, 3, 4, 4, 4, 1, 2, 3 },
			new int[] { 1, 2, 3, 3, 1, 2, /**/ 4, 4, 4 },
			6
		);
	}
	@Test
	public void testPartitionSimd4() {
		testPartitionSimd(
			new int[] { 1, 2 },
			new int[] { 1, /**/ 2 },
			1
		);
	}
	@Test
	public void testPartitionSimd5() {
		testPartitionSimd(
			new int[] { 0, 0, 0, 2, 1, 0, 2, 0, 0 },
			new int[] { 0, 0, 0, 0, 0, 0, /**/ 1, 2, 2 },
			6
		);
	}
	
	private void testQuickTopK(int[] src, int k, int[] expectedTopK) {
		try(var session = MemorySession.openConfined()) {
			var block = new SegmentIntegerBlock(session, src.length);
			block.write(0, src);
			
			int[] topK = block.quickTopK(0, src.length, k);
			// Sort values before comparing, as any order is considered valid.
			Arrays.sort(topK);
			assertArrayEquals(topK, expectedTopK);
		}
	}
	
	
	private void testQuickTopKRandom(int n, int k, int scale) {
		int[] src = new int[n];
		for(int i = 0; i < n; i++) {
			src[i] = (int) Math.floor(Math.random() * scale);
		}
		
		int[] copy = Arrays.copyOf(src, n);
		Arrays.sort(copy);
		int[] expectedTopK = Arrays.copyOfRange(copy, n - k, n);
		
		testQuickTopK(src, k, expectedTopK);
	}
	
	@Test
	public void testQuickTopK1() {
		testQuickTopKRandom(20, 3, 40);
	}
	@Test
	public void testQuickTopK2() {
		testQuickTopKRandom(20, 3, 5);
	}
	@Test
	public void testQuickTopK3() {
		testQuickTopKRandom(1000, 100, 2000);
	}
	@Test
	public void testQuickTopK4() {
		testQuickTopK(new int[] { 1,2,0,3,4 }, 3, new int[] { 2,3,4 });
	}
	
	private void testQuickQuantileInt(int n, double r, int scale) {
		int[] src = new int[n];
		for(int i = 0; i < n; i++) {
			src[i] = (int) Math.floor(Math.random() * scale);
		}
		
		int[] copy = Arrays.copyOf(src, n);
		Arrays.sort(copy);
		int expectedQuantile = copy[(int) Math.ceil(n * r)];
		
		try(var session = MemorySession.openConfined()) {
			var block = new SegmentIntegerBlock(session, src.length);
			block.write(0, src);
			
			int quantile = block.quickQuantileInt(0, src.length, r);
			assertEquals(quantile, expectedQuantile);
		}
	}
	
	@Test
	public void testQuickQuantileInt1() {
		testQuickQuantileInt(20, 0.25, 40);
	}
	@Test
	public void testQuickQuantileInt2() {
		testQuickQuantileInt(20, 0.5, 5);
	}
	@Test
	public void testQuickQuantileInt3() {
		testQuickQuantileInt(1000, 0.9, 2000);
	}
}

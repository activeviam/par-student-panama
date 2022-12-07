package com.activeviam.chunk;

import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySession;
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestQuickSelect {
	private void testPartition(int[] arr, int[] expectedArr, int expectedPartition) {
		int pivotIdx = SegmentIntegerBlock.partition(arr, 0, arr.length);
		assertThat(Arrays.equals(arr, expectedArr) && pivotIdx == expectedPartition);
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
	
	private void testQuickTopK(int[] src, int k, int[] expectedTopK) {
		try(var session = MemorySession.openConfined()) {
			var block = new SegmentIntegerBlock(session, src.length);
			block.write(0, src);
			int[] topK = block.quickTopK(0, src.length, k);
			Arrays.sort(topK);
			assertThat(Arrays.equals(topK, expectedTopK));
		}
	}
	
	private void testQuickTopKRandom(int n, int k, int scale) {
		int[] src = new int[n];
		for(int i = 0; i < n; i++) {
			src[i] = (int) Math.floor(Math.random() * scale);
		}
		
		int[] copy = Arrays.copyOf(src, n);
		Arrays.sort(copy);
		int[] topK = Arrays.copyOfRange(copy, n - k, n);
		
		testQuickTopK(src, k, topK);
	}
	
	@Test
	public void testQuickTopK1() {
		testQuickTopK(
			new int[] { 1, 2, 3, 4, 5, 6, 7 },
			3,
			new int[] { 5, 6, 7 }
		);
	}
	@Test
	public void testQuickTopK2() {
		testQuickTopKRandom(20, 3, 40);
	}
	@Test
	public void testQuickTopK3() {
		testQuickTopKRandom(1000, 100, 2000);
	}
	@Test
	public void testQuickTopK4() {
		testQuickTopKRandom(20, 3, 5);
	}
}

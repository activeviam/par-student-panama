package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.heap.MaxHeapInteger;
import com.activeviam.heap.MaxHeapIntegerWithIndices;
import com.activeviam.heap.MinHeapInteger;
import com.activeviam.heap.MinHeapIntegerWithIndices;
import com.activeviam.iterator.IPrimitiveIterator;
import jdk.incubator.vector.*;

import java.lang.foreign.MemorySession;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class SegmentIntegerBlock extends ASegmentBlock implements IntegerChunk{
	public SegmentIntegerBlock(SegmentAllocator allocator, int capacity) {
		super(allocator, Types.INTEGER, capacity);
	}
	
	@Override
	public Object read(int position) {
		return readInt(position);
	}
	
	@Override
	public int readInt(int position) {
		return segment.get(ValueLayout.JAVA_INT, (long) position * 4);
	}


	@Override
	public void write(int position, Object value) {
		if(value instanceof Integer) {
			writeInt(position, (Integer) value);
		}
	}
	
	@Override
	public void writeInt(int position, int value) {
		segment.set(ValueLayout.JAVA_INT, (long) position * 4, value);
	}

	@Override
	public void addInt(int position, int value) {
		writeInt(position, readInt(position) + value);
	}
	
	@Override
	public void transfer(int position, int[] dest) {
		for(int i = 0; i < dest.length; i++) {
			dest[i] = readInt(position + i);
		}
	}
	
	public void transferSimd(int position, int[] dest) {
		int endIdx = position + dest.length;
		for(int i = 0; i < dest.length; i += VECTOR_LANES) {
			VectorMask<Integer> mask = VECTOR_SPECIES.maskAll(true);
			if(i + VECTOR_LANES > endIdx) {
				mask = VECTOR_SPECIES.indexInRange(i, endIdx);
			}
			IntVector.fromMemorySegment(VECTOR_SPECIES, segment,
				(long) position * 4, ByteOrder.nativeOrder(), mask)
				.intoArray(dest, i, mask);
		}
	}
	
	@Override
	public void write(int position, int[] src) {
		for(int i = 0; i < src.length; i++) {
			writeInt(position + i, src[i]);
		}
	}
	
	@Override
	public void fillInt(int position, int lgth, int v) {
		for(int i = position; i < position + lgth; i++) {
			writeInt(i, v);
		}
	}
	
	@Override
	public void scale(int position, int lgth, int v) {
		for(int i = position; i < position + lgth; i++) {
			writeInt(i, readInt(i) * v);
		}
	}
	
	@Override
	public void divide(int position, int lgth, int v) {
		for(int i = position; i < position + lgth; i++) {
			writeInt(i, readInt(i) / v);
		}
	}
	
	@Override
	public void translate(int position, int lgth, int v) {
		for(int i = position; i < position + lgth; i++) {
			writeInt(i, readInt(i) + v);
		}
	}
	
	@Override
	public int hashCode(int position, int length) {
		int result = 1;
		for(int i = position; i < position + length; i++) {
			result = result * 31 + readInt(i);
		}
		return result;
	}
	
	@Override
	public IPrimitiveIterator topK(int position, int lgth, int k) {
		var heap = new MinHeapInteger(k);
		for(int i = 0; i < lgth; i++) {
			int val = readInt(position + i);
			if(heap.size() < k) { // Initial fill
				heap.add(val);
			} else if(val > heap.peek()) { // If in top K so far
				heap.poll(); // Remove minimum
				heap.add(val);
			}
		}
		return heap;
	}
	
	/**
	 * Partitions a given subarray of {@code arr} into two subarrays.
	 * A pivot value {@code p} is chosen and elements are reordered,
	 * such that elements in the left subarray are {@code <= p},
	 * and elements in the right subarray are {@code >= p}.
	 * (Elements equal to the pivot can end up in either subarray.)
	 * @param arr an array of integers
	 * @param startIdx start index of the source subarray
	 * @param endIdx past-the-end index of the source subarray
	 * @return end index of the left / start index of the right subarray
	 */
	public static int partition(int[] arr, int startIdx, int endIdx) {
		int pivot = arr[(startIdx+endIdx-1)/2];
		int left = startIdx - 1;
		int right = endIdx;
		while(true) {
			do { left++; } while(arr[left] < pivot);
			do { right--; } while(arr[right] > pivot);
			if(left >= right) return right + 1;
			int tmp = arr[left];
			arr[left] = arr[right];
			arr[right] = tmp;
		}
	}
	
	public static final VectorSpecies<Integer> VECTOR_SPECIES = IntVector.SPECIES_PREFERRED;
	public static final int VECTOR_LANES = VECTOR_SPECIES.length();
	private static final int intMask = (1 << VECTOR_LANES) - 1;
	private static final IntVector[] PERM_TABLE = new IntVector[1 << VECTOR_LANES];
	static {
		for(int mask = 0; mask <= intMask; mask++) {
			var indices = new int[VECTOR_LANES];
			int j = 0;
			for(int i = 0; i < VECTOR_LANES; i++) {
				if(((mask >> i) & 1) == 1) {
					indices[j++] = i;
				}
			}
			PERM_TABLE[mask] = IntVector.fromArray(VECTOR_SPECIES, indices, 0);
		}
	}
	
	/**
	 * Same as {@code partition}, but uses Lomuto partition scheme for comparison.
	 */
	public static int partitionLomuto(int[] arr, int startIdx, int endIdx) {
		int pivotIdx = (startIdx+endIdx-1)/2;
		int pivot = arr[pivotIdx];
		arr[pivotIdx] = arr[endIdx-1];
		
		int lesserCnt = 0;
		int greaterCnt = 0;
		int[] greater = new int[endIdx - startIdx];
		
		for(int i = startIdx; i < endIdx - 1; i++) {
			int x = arr[i];
			
			if(x >= pivot) {
				greater[greaterCnt] = x;
				greaterCnt++;
			} else {
				arr[startIdx + lesserCnt] = x;
				lesserCnt++;
			}
		}
		
		arr[startIdx + lesserCnt] = pivot;
		
		for(int i = 0; i < greaterCnt; i++) {
			arr[startIdx + lesserCnt + 1 + i] = greater[i];
		}
		
		if(lesserCnt == 0) // Pivot was smallest element; skip it to avoid infinite recursion
			return startIdx + 1;
		return startIdx + lesserCnt;
	}
	
	/**
	 * Same as {@code partitionLomuto}, but uses SIMD instructions to speed up the partitioning.
	 * {@code buf} should be temporary storage with at least the size of {@code arr}
	 */
	public static int partitionSimd(int[] arr, int[] buf, int startIdx, int endIdx) {
		int pivotIdx = (startIdx+endIdx-1)/2;
		int pivot = arr[pivotIdx];
		arr[pivotIdx] = arr[endIdx-1];
		IntVector pivotV = IntVector.broadcast(VECTOR_SPECIES, pivot);
		
		int lesserCnt = 0;
		int greaterCnt = 0;
		int[] greater = new int[endIdx - startIdx];
		
		for(int i = startIdx; i < endIdx - 1; i += VECTOR_LANES) {
			VectorMask<Integer> mask = VECTOR_SPECIES.maskAll(true);
			if(i + VECTOR_LANES > endIdx - 1) {
				mask = VECTOR_SPECIES.indexInRange(i, endIdx - 1);
			}
			IntVector v = IntVector.fromArray(VECTOR_SPECIES, arr, i, mask);
			
			int cmpGreater = (int) v.compare(VectorOperators.GE, pivotV, mask).toLong();
			v.rearrange(PERM_TABLE[cmpGreater].toShuffle()).intoArray(greater, greaterCnt, mask);
			greaterCnt += Integer.bitCount(cmpGreater);
			
			int cmpLesser = ~cmpGreater & intMask;
			if(i + VECTOR_LANES > startIdx) {
				cmpLesser = (int) v.compare(VectorOperators.LT, pivotV, mask).toLong();
			}
			v.rearrange(PERM_TABLE[cmpLesser].toShuffle()).intoArray(arr, startIdx + lesserCnt, mask);
			lesserCnt += Integer.bitCount(cmpLesser);
		}
		
		arr[startIdx + lesserCnt] = pivot;
		
		for(int i = 0; i < greaterCnt; i += VECTOR_LANES) {
			VectorMask<Integer> mask = VECTOR_SPECIES.maskAll(true);
			if(i + VECTOR_LANES > greaterCnt) {
				mask = VECTOR_SPECIES.indexInRange(i, greaterCnt);
			}
			IntVector.fromArray(VECTOR_SPECIES, greater, i, mask)
				.intoArray(arr, startIdx + lesserCnt + 1 + i, mask);
		}
		
		if(lesserCnt == 0) // Pivot was smallest element; skip it to avoid infinite recursion
			return startIdx + 1;
		return startIdx + lesserCnt;
	}
	
	/**
	 * Returns an array composed of the k biggest elements in the
	 * block between {@code position} and {@code position + lgth}.
	 * Unlike, {@code topK}, the result of this method is in no particular order.
	 *
	 * @param position the starting position
	 * @param lgth the number of elements
	 * @param k the number of elements to return
	 * @return an array containing the k biggest elements
	 */
	public int[] quickTopK(int position, int lgth, int k) {
		var arr = new int[lgth];
		transfer(position, arr);
		int startIdx = 0;
		int endIdx = lgth;
		while(true) {
			int partitionIdx = partition(arr, startIdx, endIdx);
			if(partitionIdx < lgth - k) {
				startIdx = partitionIdx;
			} else if(partitionIdx > lgth - k) {
				endIdx = partitionIdx;
			} else {
				return Arrays.copyOfRange(arr, lgth - k, lgth);
			}
		}
	}
	
	/**
	 * Identical to {@code quickTopK}, but uses Lomuto partition scheme
	 * for comparison with quickTopKSimd
	 */
	public int[] quickTopKLomuto(int position, int lgth, int k) {
		var arr = new int[lgth];
		transfer(position, arr);
		int startIdx = 0;
		int endIdx = lgth;
		while(true) {
			int partitionIdx = partitionLomuto(arr, startIdx, endIdx);
			if(partitionIdx < lgth - k) {
				startIdx = partitionIdx;
			} else if(partitionIdx > lgth - k) {
				endIdx = partitionIdx;
			} else {
				return Arrays.copyOfRange(arr, lgth - k, lgth);
			}
		}
	}
	
	/**
	 * Identical to {@code quickTopKLomuto}, but uses SIMD instructions.
	 */
	public int[] quickTopKSimd(int position, int lgth, int k) {
		var arr = new int[lgth];
		transfer(position, arr);
		int startIdx = 0;
		int endIdx = lgth;
		while(true) {
			var buf = new int[endIdx - startIdx];
			int partitionIdx = partitionSimd(arr, buf, startIdx, endIdx);
			if(partitionIdx < lgth - k) {
				startIdx = partitionIdx;
			} else if(partitionIdx > lgth - k) {
				endIdx = partitionIdx;
			} else {
				return Arrays.copyOfRange(arr, lgth - k, lgth);
			}
		}
	}
	
	/**
	 * Identical to {@code quickTopKSimd}, but does fewer allocations.
	 */
	public int[] quickTopKSimdFewAllocs(int position, int lgth, int k) {
		var arr = new int[lgth];
		var buf = new int[lgth];
		transfer(position, arr);
		int startIdx = 0;
		int endIdx = lgth;
		while(true) {
			int partitionIdx = partitionSimd(arr, buf, startIdx, endIdx);
			if(partitionIdx < lgth - k) {
				startIdx = partitionIdx;
			} else if(partitionIdx > lgth - k) {
				endIdx = partitionIdx;
			} else {
				return Arrays.copyOfRange(arr, lgth - k, lgth);
			}
		}
	}
	
	protected MinHeapIntegerWithIndices topKIndicesHeap(int position, int lgth, int k) {
		var heap = new MinHeapIntegerWithIndices(k);
		for(int i = 0; i < lgth; i++) {
			int val = readInt(position + i);
			if(heap.size() < k) { // Initial fill
				heap.add(val, i);
			} else if(val > heap.peek()) { // If in top K so far
				heap.poll(); // Remove minimum
				heap.add(val, i);
			}
		}
		return heap;
	}
	
	@Override
	public int[] topKIndices(int position, int lgth, int k) {
		var heap = topKIndicesHeap(position, lgth, k);
		heap.sort();
		return heap.getArrayIndices();
	}
	
	@Override
	public IPrimitiveIterator bottomK(int position, int lgth, int k) {
		var heap = new MaxHeapInteger(k);
		for(int i = 0; i < lgth; i++) {
			int val = readInt(position + i);
			if(heap.size() < k) { // Initial fill
				heap.add(val);
			} else if(val < heap.peek()) { // If in bottom K so far
				heap.poll(); // Remove maximum
				heap.add(val);
			}
		}
		return heap;
	}
	
	public MaxHeapIntegerWithIndices bottomKIndicesHeap(int position, int lgth, int k) {
		var heap = new MaxHeapIntegerWithIndices(k);
		for(int i = 0; i < lgth; i++) {
			int val = readInt(position + i);
			if(heap.size() < k) { // Initial fill
				heap.add(val, i);
			} else if(val < heap.peek()) { // If in bottom K so far
				heap.poll(); // Remove maximum
				heap.add(val, i);
			}
		}
		return heap;
	}
	
	@Override
	public int[] bottomKIndices(int position, int lgth, int k) {
		var heap = bottomKIndicesHeap(position, lgth, k);
		heap.sort();
		return heap.getArrayIndices();
	}
	
	protected int nearestRank(final int lgth, final double r) {
		return (int) Math.ceil(lgth * r);
	}
	
	@Override
	public int quantileInt(int position, int lgth, double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException("Order of the quantile should be greater than zero and less than 1.");
		}
		if (r >= 0.5) {
			return topK(position, lgth, lgth - nearestRank(lgth, r) + 1).nextInt();
		} else {
			return bottomK(position, lgth, nearestRank(lgth, r)).nextInt();
		}
	}
	
	public int quickQuantileInt(int position, int lgth, double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException("Order of the quantile should be greater than zero and less than 1.");
		}
		int k = nearestRank(lgth, r);
		var arr = new int[lgth];
		transfer(position, arr);
		int startIdx = 0;
		int endIdx = lgth;
		while(true) {
			int partitionIdx = partition(arr, startIdx, endIdx);
			if(partitionIdx <= k) {
				startIdx = partitionIdx;
			} else {
				endIdx = partitionIdx;
			}
			if(startIdx == k && endIdx == k+1) {
				return arr[k];
			}
		}
	}
	
	public int quickQuantileIntSimd(int position, int lgth, double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException("Order of the quantile should be greater than zero and less than 1.");
		}
		int k = nearestRank(lgth, r);
		var arr = new int[lgth];
		var buf = new int[lgth];
		transfer(position, arr);
		int startIdx = 0;
		int endIdx = lgth;
		while(true) {
			int partitionIdx = partitionSimd(arr, buf, startIdx, endIdx);
			if(partitionIdx <= k) {
				startIdx = partitionIdx;
			} else {
				endIdx = partitionIdx;
			}
			if(startIdx == k && endIdx == k+1) {
				return arr[k];
			}
		}
	}
	
	@Override
	public int quantileIndex(int position, int lgth, double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException("Order of the quantile should be greater than zero and less than 1.");
		}
		if (r >= 0.5) {
			return topKIndicesHeap(position, lgth, lgth - nearestRank(lgth, r) + 1).peekIndex();
		} else {
			return bottomKIndicesHeap(position, lgth, nearestRank(lgth, r)).peekIndex();
		}
	}
	
	public IntVector getSimd(int position, int maxPosition) {
		if(position + VECTOR_SPECIES.length() <= maxPosition) {
			return IntVector.fromMemorySegment(VECTOR_SPECIES, segment,
				(long) position * 4, ByteOrder.nativeOrder());
		} else {
			var mask = VECTOR_SPECIES.indexInRange(position, maxPosition);
			return IntVector.fromMemorySegment(VECTOR_SPECIES, segment,
					(long) position * 4, ByteOrder.nativeOrder(), mask);
		}
	}
	
	public void putSimd(int position, int maxPosition, IntVector vec) {
		if(position + VECTOR_SPECIES.length() <= maxPosition) {
			vec.intoMemorySegment(segment, (long) position * 4, ByteOrder.nativeOrder());
		} else {
			var mask = VECTOR_SPECIES.indexInRange(position, maxPosition);
			vec.intoMemorySegment(segment, (long) position * 4, ByteOrder.nativeOrder(), mask);
		}
	}
}

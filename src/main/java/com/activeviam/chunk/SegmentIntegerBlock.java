package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.heap.MaxHeapInteger;
import com.activeviam.heap.MaxHeapIntegerWithIndices;
import com.activeviam.heap.MinHeapInteger;
import com.activeviam.heap.MinHeapIntegerWithIndices;
import com.activeviam.iterator.IPrimitiveIterator;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.Arrays;

public class SegmentIntegerBlock extends ASegmentBlock {
	public SegmentIntegerBlock(MemorySession session, int capacity) {
		super(session, Types.INTEGER, capacity);
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
	 * such that elements of the left subarray are {@code <= p},
	 * and elements of the right subarray are {@code >= p}.
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
	
	/**
	 * Identical to {@code partition}, but elements in {@code arr} are
	 * indices into the block, and are compared based on the corresponding values.
	 * @param arr an array of indices into the block
	 * @param startIdx start index of the source subarray
	 * @param endIdx past-the-end index of the source subarray
	 * @return end index of the left / start index of the right subarray
	 */
	public int partitionIndices(int[] arr, int startIdx, int endIdx) {
		int pivot = readInt(arr[(startIdx+endIdx-1)/2]);
		int left = startIdx - 1;
		int right = endIdx;
		while(true) {
			do { left++; } while(readInt(arr[left]) < pivot);
			do { right--; } while(readInt(arr[right]) > pivot);
			if(left >= right) return right + 1;
			int tmp = arr[left];
			arr[left] = arr[right];
			arr[right] = tmp;
		}
	}
	
	/**
	 * Returns an array composed of the indices of the k biggest elements in the
	 * block between {@code position} and {@code position + lgth}.
	 * Unlike, {@code topK}, the result of this method is in no particular order.
	 *
	 * @param position the starting position
	 * @param lgth the number of elements
	 * @param k the number of elements to return
	 * @return an array containing the k biggest elements
	 */
	public int[] quickTopKIndices(int position, int lgth, int k) {
		var arr = new int[lgth];
		for(int i = 0; i < lgth; i++) {
			arr[i] = position + i;
		}
		int startIdx = 0;
		int endIdx = lgth;
		while(true) {
			int partitionIdx = partitionIndices(arr, startIdx, endIdx);
			if(partitionIdx < lgth - k) {
				startIdx = partitionIdx;
			} else if(partitionIdx > lgth - k) {
				endIdx = partitionIdx;
			} else {
				return Arrays.copyOfRange(arr, lgth - k, lgth);
			}
		}
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
		int k = nearestRank(lgth, r);
		var arr = new int[lgth];
		transfer(position, arr);
		int startIdx = 0;
		int endIdx = lgth;
		while(true) {
			int partitionIdx = partition(arr, startIdx, endIdx);
			if(partitionIdx < k) {
				startIdx = partitionIdx;
			} else if(partitionIdx > k) {
				endIdx = partitionIdx;
			} else {
				return arr[k];
			}
		}
	}
	
	@Override
	public int quantileIndex(int position, int lgth, double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException("Order of the quantile should be greater than zero and less than 1.");
		}
		int k = nearestRank(lgth, r);
		var arr = new int[lgth];
		for(int i = 0; i < lgth; i++) {
			arr[i] = position + i;
		}
		int startIdx = 0;
		int endIdx = lgth;
		while(true) {
			int partitionIdx = partitionIndices(arr, startIdx, endIdx);
			if(partitionIdx < k) {
				startIdx = partitionIdx;
			} else if(partitionIdx > k) {
				endIdx = partitionIdx;
			} else {
				return arr[k];
			}
		}
	}
	
	public static final VectorSpecies<Integer> VECTOR_SPECIES = IntVector.SPECIES_PREFERRED;
	
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

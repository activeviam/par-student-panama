package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.heap.MaxHeapInteger;
import com.activeviam.heap.MaxHeapIntegerWithIndices;
import com.activeviam.heap.MinHeapInteger;
import com.activeviam.heap.MinHeapIntegerWithIndices;
import com.activeviam.iterator.IPrimitiveIterator;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

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

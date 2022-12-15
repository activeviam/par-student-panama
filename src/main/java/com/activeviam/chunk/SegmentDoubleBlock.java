package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.heap.*;
import com.activeviam.iterator.IPrimitiveIterator;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

public class SegmentDoubleBlock extends ASegmentBlock implements DoubleChunk{
	protected SegmentDoubleBlock(MemorySession session, int capacity) {
		super(session, Types.DOUBLE, capacity);
	}
	
	@Override
	public Object read(int position) {
		return readDouble(position);
	}
	
	@Override
	public double readDouble(int position) {
		return segment.get(ValueLayout.JAVA_DOUBLE, (long) position * 8);
	}


	@Override
	public void write(int position, Object value) {
		if(value instanceof Double) {
			writeDouble(position, (Double) value);
		}
	}
	
	@Override
	public void writeDouble(int position, double value) {
		segment.set(ValueLayout.JAVA_DOUBLE, (long) position * 8, value);
	}
	
	@Override
	public void write(int position, double[] src) {
		for(int i = 0; i < src.length; i++) {
			writeDouble(position + i, src[i]);
		}
	}
	
	@Override
	public void scale(int position, int lgth, double v) {
		for(int i = position; i < position + lgth; i++) {
			writeDouble(i, readDouble(i) * v);
		}
	}

	@Override
	public void translate(int position, int lgth, double v) {
		for(int i = position; i < position + lgth; i++) {
			writeDouble(i, readDouble(i) + v);
		}
	}
	
	@Override
	public int hashCode(int position, int length) {
		int result = 1;
		for(int i = position; i < position + length; i++) {
			result = result * 31 + (int) readDouble(i);
		}
		return result;
	}

	@Override
	public IPrimitiveIterator topK(int position, int lgth, int k) {
		var heap = new MinHeapDouble(k);
		for(int i = 0; i < lgth; i++) {
			double val = readDouble(position + i);
			if(heap.size() < k) { // Initial fill
				heap.add(val);
			} else if(val > heap.peek()) { // If in top K so far
				heap.poll(); // Remove minimum
				heap.add(val);
			}
		}
		return heap;
	}
	
	protected MinHeapDoubleWithIndices topKIndicesHeap(int position, int lgth, int k) {
		var heap = new MinHeapDoubleWithIndices(k);
		for(int i = 0; i < lgth; i++) {
			double val = readDouble(position + i);
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
		var heap = new MaxHeapDouble(k);
		for(int i = 0; i < lgth; i++) {
			double val = readDouble(position + i);
			if(heap.size() < k) { // Initial fill
				heap.add(val);
			} else if(val < heap.peek()) { // If in bottom K so far
				heap.poll(); // Remove maximum
				heap.add(val);
			}
		}
		return heap;
	}
	
	public MaxHeapDoubleWithIndices bottomKIndicesHeap(int position, int lgth, int k) {
		var heap = new MaxHeapDoubleWithIndices(k);
		for(int i = 0; i < lgth; i++) {
			double val = readDouble(position + i);
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
	public double quantileDouble(int position, int lgth, double r) {
		if (r <= 0d || r > 1d) {
			throw new UnsupportedOperationException("Order of the quantile should be greater than zero and less than 1.");
		}
		if (r >= 0.5) {
			return topK(position, lgth, lgth - nearestRank(lgth, r) + 1).nextDouble();
		} else {
			return bottomK(position, lgth, nearestRank(lgth, r)).nextDouble();
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
	
	public static final VectorSpecies<Double> VECTOR_SPECIES = DoubleVector.SPECIES_PREFERRED;
	
	public DoubleVector getSimd(int position, int maxPosition) {
		if(position + VECTOR_SPECIES.length() <= maxPosition) {
			return DoubleVector.fromMemorySegment(VECTOR_SPECIES, segment,
				(long) position * 8, ByteOrder.nativeOrder());
		} else {
			var mask = VECTOR_SPECIES.indexInRange(position, maxPosition);
			return DoubleVector.fromMemorySegment(VECTOR_SPECIES, segment,
					(long) position * 8, ByteOrder.nativeOrder(), mask);
		}
	}
	
	public void putSimd(int position, int maxPosition, DoubleVector vec) {
		if(position + VECTOR_SPECIES.length() <= maxPosition) {
			vec.intoMemorySegment(segment, (long) position * 8, ByteOrder.nativeOrder());
		} else {
			var mask = VECTOR_SPECIES.indexInRange(position, maxPosition);
			vec.intoMemorySegment(segment, (long) position * 8, ByteOrder.nativeOrder(), mask);
		}
	}
}

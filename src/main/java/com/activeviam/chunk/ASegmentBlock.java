package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.allocator.AllocationType;
import com.activeviam.iterator.IPrimitiveIterator;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

public class ASegmentBlock implements IBlock {
	protected final MemorySegment segment;
	private final Types type;
	private final int capacity;
	
	protected ASegmentBlock(MemorySession session, Types type, int capacity) {
		this.type = type;
		this.capacity = capacity;
		this.segment = MemorySegment.allocateNative(
				(long) capacity * Types.getSize(type), session);
	}
	
	@Override
	public AllocationType getAllocation() {
		return AllocationType.SEGMENT;
	}
	
	public MemorySegment getSegment() {
		return segment;
	}
	
	@Override
	public Types getComponentType() {
		return type;
	}
	
	@Override
	public int capacity() {
		return capacity;
	}
	
	@Override
	public boolean isNull(int position) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Object read(int position) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void write(int position, Object value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void transfer(int position, double[] dest) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void transfer(int position, float[] dest) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void transfer(int position, long[] dest) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void transfer(int position, int[] dest) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void write(int position, double[] src) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void write(int position, float[] src) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void write(int position, long[] src) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void write(int position, int[] src) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void fillDouble(int position, int lgth, double v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void fillFloat(int position, int lgth, float v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void fillLong(int position, int lgth, long v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void fillInt(int position, int lgth, int v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void scale(int position, int lgth, double v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void scale(int position, int lgth, float v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void scale(int position, int lgth, long v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void scale(int position, int lgth, int v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void divide(int position, int lgth, long v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void divide(int position, int lgth, int v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void translate(int position, int lgth, double v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void translate(int position, int lgth, float v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void translate(int position, int lgth, long v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void translate(int position, int lgth, int v) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int hashCode(int position, int length) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public IPrimitiveIterator topK(int position, int lgth, int k) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public IPrimitiveIterator bottomK(int position, int lgth, int k) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int[] topKIndices(int position, int lgth, int k) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int[] bottomKIndices(int position, int lgth, int k) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public double quantileDouble(int position, int lgth, double r) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public float quantileFloat(int position, int lgth, double r) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public long quantileLong(int position, int lgth, double r) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int quantileInt(int position, int lgth, double r) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int quantileIndex(int position, int lgth, double r) {
		throw new UnsupportedOperationException();
	}
}

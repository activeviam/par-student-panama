/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.vector;

import com.activeviam.Types;
import com.activeviam.allocator.AllocationType;
import com.activeviam.chunk.ADirectVectorBlock;
import com.activeviam.chunk.IBlock;
import com.activeviam.chunk.SegmentDoubleBlock;
import com.activeviam.chunk.SegmentIntegerBlock;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;

/**
 * Basic implementation for an integer segment block vector.
 *
 * @author ActiveViam
 */
public class SegmentDoubleVector extends ASegmentVector {

    /**
     * Constructor.
     *
     * @param block the block on which to create the vector
     * @param position the position in the block
     * @param length the length of the vector
     */
    public SegmentDoubleVector(SegmentDoubleBlock block, int position, int length) {
        super(block, position, length);
    }

    @Override
    public AllocationType getAllocation() {
        return this.block.getAllocation();
    }

    @Override
    public ITransientVector sort() {
        final double[] a = toDoubleArray();
        Arrays.sort(a);
        return new ArrayDoubleVector(a);
    }

    @Override
    public Types getComponentType() {
        return Types.DOUBLE;
    }

    @Override
    public void copyFrom(double[] src) {
        checkIndex(0, src.length);
        this.block.write(position, src);
    }

    @Override
    public void copyFrom(IVector vector) {
        final int length = vector.size();
        checkIndex(0, length);
        var segVector = (SegmentDoubleVector) vector;
        MemorySegment.copy(
                segVector.block.getSegment(),
                (long) segVector.position * 8,
                block.getSegment(),
                (long) position * 8,
                (long) length * 8);
    }

    @Override
    public void copyTo(double[] dst) {
        checkIndex(0, dst.length);
        this.block.transfer(position, dst);
    }

    @Override
    public Double read(final int index) {
        return readDouble(index);
    }

    @Override
    public int readInt(final int index) {
        return (int) readDouble(index);
    }

    @Override
    public long readLong(final int index) {
        return (long) readDouble(index);
    }

    @Override
    public float readFloat(final int index) {
        return (float) readDouble(index);
    }

    @Override
    public double readDouble(final int index) {
        checkIndex(index);
        return this.block.readDouble(this.position + index);
    }

    @Override
    public void write(final int index, final Object value) {
        if (value instanceof Number) {
            writeDouble(index, ((Number) value).doubleValue());
        }
    }

    @Override
    public void writeDouble(final int index, final double value) {
        checkIndex(index);
        this.block.writeDouble(this.position + index, value);
    }

    @Override
    public void addDouble(final int position, final double addedValue) {
        writeDouble(position, readDouble(position) + addedValue);
    }

    @Override
    public double average() {
        return (double) sumDouble() / this.length;
    }

    @Override
    protected double squaredEuclideanDistance(final IBlock block, final int position) {
        double s = 0;
        for (int i = 0; i < this.length; i++) {
            final double a = block.readDouble(position + i);
            s += a * a;
        }
        return s;
    }

    @Override
    public IVector cloneOnHeap() {
        return new ArrayDoubleVector(toDoubleArray());
    }

    public static final int VECTOR_STEP = SegmentDoubleBlock.VECTOR_SPECIES.length();

    protected DoubleVector getSimd(int i) {
        return ((SegmentDoubleBlock) block).getSimd(position + i, position + length);
    }
    protected void putSimd(int i, DoubleVector vec) {
        ((SegmentDoubleBlock) block).putSimd(position + i, position + length, vec);
    }

    @Override
    public void plus(IVector vector) {
        final int length = vector.size();
        checkIndex(0, length);
        var right = (SegmentDoubleVector) vector;
        for(int i = 0; i < length; i += VECTOR_STEP) {
            putSimd(i, getSimd(i).add(right.getSimd(i)));
        }
    }

    @Override
    public void plusPositiveValues(IVector vector) {
        final int length = vector.size();
        checkIndex(0, length);
        var right = (SegmentDoubleVector) vector;
        for(int i = 0; i < length; i += VECTOR_STEP) {
            putSimd(i, getSimd(i).add(right.getSimd(i).max(0)));
        }
    }

    @Override
    public void plusNegativeValues(IVector vector) {
        final int length = vector.size();
        checkIndex(0, length);
        var right = (SegmentDoubleVector) vector;
        for(int i = 0; i < length; i += VECTOR_STEP) {
            putSimd(i, getSimd(i).add(right.getSimd(i).min(0)));
        }
    }

    @Override
    public void minus(IVector vector) {
        final int length = vector.size();
        checkIndex(0, length);
        var right = (SegmentDoubleVector) vector;
        for(int i = 0; i < length; i += VECTOR_STEP) {
            putSimd(i, getSimd(i).sub(right.getSimd(i)));
        }
    }

    @Override
    public void minusPositiveValues(IVector vector) {
        final int length = vector.size();
        checkIndex(0, length);
        var right = (SegmentDoubleVector) vector;
        for(int i = 0; i < length; i += VECTOR_STEP) {
            putSimd(i, getSimd(i).sub(right.getSimd(i).max(0)));
        }
    }

    @Override
    public void minusNegativeValues(IVector vector) {
        final int length = vector.size();
        checkIndex(0, length);
        var right = (SegmentDoubleVector) vector;
        for(int i = 0; i < length; i += VECTOR_STEP) {
            putSimd(i, getSimd(i).sub(right.getSimd(i).min(0)));
        }
    }

    @Override
    public double sumDouble() {
        double sum = 0;
        for(int i = 0; i < length; i += VECTOR_STEP) {
            sum += getSimd(i).reduceLanes(VectorOperators.ADD);
        }
        return sum;
    }
}

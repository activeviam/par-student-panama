package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.vector.EmptyVector;
import com.activeviam.vector.IVector;
import com.activeviam.vector.IVectorAllocator;
import com.activeviam.vector.SegmentDoubleVector;
import com.activeviam.vector.SegmentIntegerVector;

import java.lang.foreign.SegmentAllocator;

public class SegmentMemoryAllocator implements IChunkAllocator {

    private final SegmentAllocator allocator;
    public SegmentMemoryAllocator(SegmentAllocator allocator) {
        this.allocator = allocator;
    }
    @Override
    public IntegerChunk allocateIntegerChunk(int size) {
        return new SegmentIntegerBlock(allocator, size);
    }

    @Override
    public DoubleChunk allocateDoubleChunk(int size) {
        return new SegmentDoubleBlock(allocator, size);
    }

    @Override
    public IVectorAllocator getVectorAllocator(Types type) {
        switch (type) {
            case DOUBLE:
                return new SegmentDoubleVectorAllocator();
            case INTEGER:
                return new SegmentIntegerVectorAllocator();
            default:
                throw new IllegalStateException(
                        "Unexpected type: " + type.name());
        }
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    public class SegmentIntegerVectorAllocator implements IVectorAllocator {

        @Override
        public IVector allocateNewVector(int length) {
            if (length == 0) {
                return EmptyVector.emptyVector(Types.INTEGER);
            }
            final var block = new SegmentIntegerBlock(SegmentMemoryAllocator.this.allocator, length);
            return new SegmentIntegerVector(block, 0, length);
        }

        @Override
        public void reallocateVector(IVector vector) {

        }

        @Override
        public IVector copy(IVector toCopy) {
            if (toCopy == null) {
                return null;
            }
            final IVector clone = allocateNewVector(toCopy.size());
            clone.copyFrom(toCopy);
            return clone;
        }

        @Override
        public Types getComponentType() {
            return Types.INTEGER;
        }
    }

    public class SegmentDoubleVectorAllocator implements IVectorAllocator {


        @Override
        public IVector allocateNewVector(int length) {
            if (length == 0) {
                return EmptyVector.emptyVector(Types.DOUBLE);
            }
            final var block = new SegmentDoubleBlock(SegmentMemoryAllocator.this.allocator, length);
            return new SegmentDoubleVector(block, 0, length);
        }

        @Override
        public void reallocateVector(IVector vector) {

        }

        @Override
        public IVector copy(IVector toCopy) {
            if (toCopy == null) {
                return null;
            }
            final IVector clone = allocateNewVector(toCopy.size());
            clone.copyFrom(toCopy);
            return clone;
        }

        @Override
        public Types getComponentType() {
            return Types.DOUBLE;
        }
    }
}

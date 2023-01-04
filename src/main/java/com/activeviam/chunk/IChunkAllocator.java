package com.activeviam.chunk;

import com.activeviam.Types;
import com.activeviam.vector.IVectorAllocator;

/**
 * @author ActiveViam
 */
public interface IChunkAllocator {

	IntegerChunk allocateIntegerChunk(int size);

	DoubleChunk allocateDoubleChunk(int size);

	default IVectorChunk allocateVectorChunk(int size, Types type) {
		return new ChunkVector(size, type, this);
	}

	IVectorAllocator getVectorAllocator(Types type);

	boolean isTransient();
}

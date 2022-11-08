/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.chunk.vector;

import static org.assertj.core.api.Assertions.assertThat;

import com.activeviam.chunk.IVectorChunk;
import com.activeviam.vector.IVector;
import org.junit.jupiter.api.Test;

public interface SpecTestDoubleVector {

  IVectorChunk createChunk(final int capacity);

  @Test
  default void testReadWrite() throws Exception {
    try (IVectorChunk chunk = createChunk(8)) {
      final double[] doubleArray = new double[]{-58d, 80d};
      chunk.write(7, doubleArray);
      final IVector allocatedVector = (IVector) chunk.read(7);
      assertThat(allocatedVector.size()).isEqualTo(2);
      assertThat(allocatedVector.readDouble(0)).isEqualTo(-58d);
      assertThat(allocatedVector.readDouble(1)).isEqualTo(80d);
    }
  }

  @Test
  default void testPlusVector() throws Exception {
    try (IVectorChunk chunk = createChunk(8)) {
      final double[] doubleArray = new double[]{-58d, 80d};
      chunk.write(7, doubleArray);
      IVector allocatedVector = (IVector) chunk.read(7);
      chunk.write(6, doubleArray);
      final IVector deepClonedVector = (IVector) chunk.read(6);
      allocatedVector.plus(deepClonedVector);
      allocatedVector = (IVector) chunk.read(7);
      assertThat(allocatedVector.size()).isEqualTo(2);
      assertThat(allocatedVector.readDouble(1)).isEqualTo(160d);
      assertThat(allocatedVector.readDouble(0)).isEqualTo(-116d);
    }
  }

}

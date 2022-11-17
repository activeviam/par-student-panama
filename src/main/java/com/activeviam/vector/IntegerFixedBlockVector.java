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
import java.util.Arrays;

/**
 * Basic implementation for an integer fixed block vector.
 *
 * @author ActiveViam
 */
public class IntegerFixedBlockVector extends AFixedBlockVector {

	/**
	 * Constructor.
	 *
	 * @param block the block on which to create the vector
	 * @param position the position in the block
	 * @param length the length of the vector
	 */
	public IntegerFixedBlockVector(ADirectVectorBlock block, int position, int length) {
		super(block, position, length);
	}

	@Override
	public AllocationType getAllocation() {
		return this.block.getAllocation();
	}

	@Override
	public ITransientVector sort() {
		final int[] a = toIntArray();
		Arrays.sort(a);
		return new ArrayIntegerVector(a);
	}

	@Override
	public Types getComponentType() {
		return Types.INTEGER;
	}

	@Override
	public void copyFrom(int[] src) {
		checkIndex(0, src.length);
		this.block.write(position, src);
	}

	@Override
	public void copyFrom(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final IBlock rightBlock = ((IntegerFixedBlockVector) vector).block;
		final int rghtLen = vector.size();
		final IBlock leftBlock = this.block;
		long rghtPos = getPos((ADirectVectorBlock) rightBlock, 2);
		long lftPos = getPos((ADirectVectorBlock) leftBlock, 2);
		final long maxPos = getMaxPos(rghtPos, rghtLen, 2);
		final long maxUnroll = getMaxUnroll(rghtPos, rghtLen, 2, 16);
		for (; rghtPos < maxUnroll; rghtPos += 16, lftPos += 16) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							- Math.min(0, UNSAFE.getInt(rghtPos)));
			UNSAFE.putInt(
					lftPos + (1 * (1 << 2)),
					UNSAFE.getInt(lftPos + (1 * (1 << 2)))
							- Math.min(0, UNSAFE.getInt(rghtPos + (1 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (2 * (1 << 2)),
					UNSAFE.getInt(lftPos + (2 * (1 << 2)))
							- Math.min(0, UNSAFE.getInt(rghtPos + (2 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (3 * (1 << 2)),
					UNSAFE.getInt(lftPos + (3 * (1 << 2)))
							- Math.min(0, UNSAFE.getInt(rghtPos + (3 * (1 << 2)))));
		}

		for (; rghtPos < maxPos; rghtPos += 1 << 2, lftPos += 1 << 2) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							- Math.min(0, UNSAFE.getInt(rghtPos)));
		}

	}
	
	@Override
	public void copyTo(final int[] dst) {
		checkIndex(0, dst.length);
		this.block.transfer(position, dst);
	}

	@Override
	public Integer read(final int index) {
		return readInt(index);
	}

	@Override
	public int readInt(final int index) {
		checkIndex(index);
		return this.block.readInt(this.position + index);
	}

	@Override
	public long readLong(final int index) {
		return readInt(index);
	}

	@Override
	public float readFloat(final int index) {
		return readInt(index);
	}

	@Override
	public double readDouble(final int index) {
		return readInt(index);
	}

	@Override
	public void write(final int index, final Object value) {
		if (value instanceof Number) {
			writeInt(index, ((Number) value).intValue());
		}
	}

	@Override
	public void writeInt(final int index, final int value) {
		checkIndex(index);
		this.block.writeInt(this.position + index, value);
	}

	@Override
	public void addInt(final int position, final int addedValue) {
		writeInt(position, readInt(position) + addedValue);
	}

	@Override
	public double average() {
		return (double) sumInt() / this.length;
	}

	@Override
	protected double squaredEuclideanDistance(final IBlock block, final int position) {
		int s = 0;
		for (int i = 0; i < this.length; i++) {
			final int a = block.readInt(position + i);
			s += a * a;
		}
		return s;
	}

	@Override
	public IVector cloneOnHeap() {
		return new ArrayIntegerVector(toIntArray());
	}

	@Override
	public void plus(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);
		final IBlock rightBlock = ((IntegerFixedBlockVector) vector).block;
		final int rghtLen = vector.size();
		final IBlock leftBlock = this.block;
		long rghtPos = getPos((ADirectVectorBlock) rightBlock, 2);
		long lftPos = getPos((ADirectVectorBlock) leftBlock, 2);
		final long maxPos = getMaxPos(rghtPos, rghtLen, 2);
		final long maxUnroll = getMaxUnroll(rghtPos, rghtLen, 2, 16);
		for (; rghtPos < maxUnroll; rghtPos += 16, lftPos += 16) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							+ UNSAFE.getInt(rghtPos));
			UNSAFE.putInt(
					lftPos + (1 * (1 << 2)),
					UNSAFE.getInt(lftPos + (1 * (1 << 2)))
							+ UNSAFE.getInt(rghtPos + (1 * (1 << 2))));
			UNSAFE.putInt(
					lftPos + (2 * (1 << 2)),
					UNSAFE.getInt(lftPos + (2 * (1 << 2)))
							+ UNSAFE.getInt(rghtPos + (2 * (1 << 2))));
			UNSAFE.putInt(
					lftPos + (3 * (1 << 2)),
					UNSAFE.getInt(lftPos + (3 * (1 << 2)))
							+ UNSAFE.getInt(rghtPos + (3 * (1 << 2))));
		}

		for (; rghtPos < maxPos; rghtPos += 1 << 2, lftPos += 1 << 2) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							+ UNSAFE.getInt(rghtPos));
		}

	}

	@Override
	public void plusPositiveValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final IBlock rightBlock = ((IntegerFixedBlockVector) vector).block;
		final int rghtLen = vector.size();
		final IBlock leftBlock = this.block;
		long rghtPos = getPos((ADirectVectorBlock) rightBlock, 2);
		long lftPos = getPos((ADirectVectorBlock) leftBlock, 2);
		final long maxPos = getMaxPos(rghtPos, rghtLen, 2);
		final long maxUnroll = getMaxUnroll(rghtPos, rghtLen, 2, 16);
		for (; rghtPos < maxUnroll; rghtPos += 16, lftPos += 16) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							+ Math.max(0, UNSAFE.getInt(rghtPos)));
			UNSAFE.putInt(
					lftPos + (1 * (1 << 2)),
					UNSAFE.getInt(lftPos + (1 * (1 << 2)))
							+ Math.max(0, UNSAFE.getInt(rghtPos + (1 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (2 * (1 << 2)),
					UNSAFE.getInt(lftPos + (2 * (1 << 2)))
							+ Math.max(0, UNSAFE.getInt(rghtPos + (2 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (3 * (1 << 2)),
					UNSAFE.getInt(lftPos + (3 * (1 << 2)))
							+ Math.max(0, UNSAFE.getInt(rghtPos + (3 * (1 << 2)))));
		}

		for (; rghtPos < maxPos; rghtPos += 1 << 2, lftPos += 1 << 2) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							+ Math.max(0, UNSAFE.getInt(rghtPos)));
		}

	}

	@Override
	public void plusNegativeValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final IBlock rightBlock = ((IntegerFixedBlockVector) vector).block;
		final int rghtLen = vector.size();
		final IBlock leftBlock = this.block;
		long rghtPos = getPos((ADirectVectorBlock) rightBlock, 2);
		long lftPos = getPos((ADirectVectorBlock) leftBlock, 2);
		final long maxPos = getMaxPos(rghtPos, rghtLen, 2);
		final long maxUnroll = getMaxUnroll(rghtPos, rghtLen, 2, 16);
		for (; rghtPos < maxUnroll; rghtPos += 16, lftPos += 16) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							+ Math.min(0, UNSAFE.getInt(rghtPos)));
			UNSAFE.putInt(
					lftPos + (1 * (1 << 2)),
					UNSAFE.getInt(lftPos + (1 * (1 << 2)))
							+ Math.min(0, UNSAFE.getInt(rghtPos + (1 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (2 * (1 << 2)),
					UNSAFE.getInt(lftPos + (2 * (1 << 2)))
							+ Math.min(0, UNSAFE.getInt(rghtPos + (2 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (3 * (1 << 2)),
					UNSAFE.getInt(lftPos + (3 * (1 << 2)))
							+ Math.min(0, UNSAFE.getInt(rghtPos + (3 * (1 << 2)))));
		}

		for (; rghtPos < maxPos; rghtPos += 1 << 2, lftPos += 1 << 2) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							+ Math.min(0, UNSAFE.getInt(rghtPos)));
		}

	}

	@Override
	public void minus(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final IBlock rightBlock = ((IntegerFixedBlockVector) vector).block;
		final int rghtLen = vector.size();
		final IBlock leftBlock = this.block;
		long rghtPos = getPos((ADirectVectorBlock) rightBlock, 2);
		long lftPos = getPos((ADirectVectorBlock) leftBlock, 2);
		final long maxPos = getMaxPos(rghtPos, rghtLen, 2);
		final long maxUnroll = getMaxUnroll(rghtPos, rghtLen, 2, 16);
		for (; rghtPos < maxUnroll; rghtPos += 16, lftPos += 16) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							- UNSAFE.getInt(rghtPos));
			UNSAFE.putInt(
					lftPos + (1 * (1 << 2)),
					UNSAFE.getInt(lftPos + (1 * (1 << 2)))
							- UNSAFE.getInt(rghtPos + (1 * (1 << 2))));
			UNSAFE.putInt(
					lftPos + (2 * (1 << 2)),
					UNSAFE.getInt(lftPos + (2 * (1 << 2)))
							- UNSAFE.getInt(rghtPos + (2 * (1 << 2))));
			UNSAFE.putInt(
					lftPos + (3 * (1 << 2)),
					UNSAFE.getInt(lftPos + (3 * (1 << 2)))
							- UNSAFE.getInt(rghtPos + (3 * (1 << 2))));
		}

		for (; rghtPos < maxPos; rghtPos += 1 << 2, lftPos += 1 << 2) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							- UNSAFE.getInt(rghtPos));
		}

	}

	@Override
	public void minusPositiveValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final IBlock rightBlock = ((IntegerFixedBlockVector) vector).block;
		final int rghtLen = vector.size();
		final IBlock leftBlock = this.block;
		long rghtPos = getPos((ADirectVectorBlock) rightBlock, 2);
		long lftPos = getPos((ADirectVectorBlock) leftBlock, 2);
		final long maxPos = getMaxPos(rghtPos, rghtLen, 2);
		final long maxUnroll = getMaxUnroll(rghtPos, rghtLen, 2, 16);
		for (; rghtPos < maxUnroll; rghtPos += 16, lftPos += 16) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							- Math.max(0, UNSAFE.getInt(rghtPos)));
			UNSAFE.putInt(
					lftPos + (1 * (1 << 2)),
					UNSAFE.getInt(lftPos + (1 * (1 << 2)))
							- Math.max(0, UNSAFE.getInt(rghtPos + (1 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (2 * (1 << 2)),
					UNSAFE.getInt(lftPos + (2 * (1 << 2)))
							- Math.max(0, UNSAFE.getInt(rghtPos + (2 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (3 * (1 << 2)),
					UNSAFE.getInt(lftPos + (3 * (1 << 2)))
							- Math.max(0, UNSAFE.getInt(rghtPos + (3 * (1 << 2)))));
		}

		for (; rghtPos < maxPos; rghtPos += 1 << 2, lftPos += 1 << 2) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							- Math.max(0, UNSAFE.getInt(rghtPos)));
		}

	}

	@Override
	public void minusNegativeValues(IVector vector) {
		final int length = vector.size();
		checkIndex(0, length);

		final IBlock rightBlock = ((IntegerFixedBlockVector) vector).block;
		final int rghtLen = vector.size();
		final IBlock leftBlock = this.block;
		long rghtPos = getPos((ADirectVectorBlock) rightBlock, 2);
		long lftPos = getPos((ADirectVectorBlock) leftBlock, 2);
		final long maxPos = getMaxPos(rghtPos, rghtLen, 2);
		final long maxUnroll = getMaxUnroll(rghtPos, rghtLen, 2, 16);
		for (; rghtPos < maxUnroll; rghtPos += 16, lftPos += 16) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							- Math.min(0, UNSAFE.getInt(rghtPos)));
			UNSAFE.putInt(
					lftPos + (1 * (1 << 2)),
					UNSAFE.getInt(lftPos + (1 * (1 << 2)))
							- Math.min(0, UNSAFE.getInt(rghtPos + (1 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (2 * (1 << 2)),
					UNSAFE.getInt(lftPos + (2 * (1 << 2)))
							- Math.min(0, UNSAFE.getInt(rghtPos + (2 * (1 << 2)))));
			UNSAFE.putInt(
					lftPos + (3 * (1 << 2)),
					UNSAFE.getInt(lftPos + (3 * (1 << 2)))
							- Math.min(0, UNSAFE.getInt(rghtPos + (3 * (1 << 2)))));
		}

		for (; rghtPos < maxPos; rghtPos += 1 << 2, lftPos += 1 << 2) {
			UNSAFE.putInt(
					lftPos,
					UNSAFE.getInt(lftPos)
							- Math.min(0, UNSAFE.getInt(rghtPos)));
		}

	}

}

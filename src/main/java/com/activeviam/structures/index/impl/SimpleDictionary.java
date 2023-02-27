package com.activeviam.structures.index.impl;

import com.activeviam.structures.index.IDictionary;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Very simple implementation of {@link IDictionary} supporting multiple readers but a single writer.
 *
 * @author ActiveViam
 * @param <K> the type of the object stored in this dictionary
 */
public class SimpleDictionary<K> implements IDictionary<K> {

	protected final Map<K, Integer> mapping;

	/**
	 * The objects stored in this dictionary
	 */
	protected K[] objects;

	/**
	 * @see #size()
	 */
	protected volatile int size;

	public SimpleDictionary() {
		this(16);
	}

	@SuppressWarnings("unchecked")
	public SimpleDictionary(int initialCapacity) {
		this.mapping = new HashMap<>(initialCapacity);
		this.objects = (K[]) new Object[initialCapacity];
	}

	@Override
	public K read(int position) {
		return objects[position];
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int getPosition(K object) {
		return mapping.getOrDefault(object, -1);
	}

	@Override
	public int map(K object) {
		final int previousSize = size;
		final int pos = mapping.computeIfAbsent(object, __ -> previousSize);
		if (pos == previousSize) {
			if (objects.length <= previousSize + 1) {
				objects = Arrays.copyOf(objects, Math.multiplyExact(2, objects.length));
			}
			objects[pos] = object;
			size = 1 + previousSize;
		}
		return pos;
	}

}

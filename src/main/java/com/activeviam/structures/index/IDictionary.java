package com.activeviam.structures.index;

/**
 * A dictionary is a strictly monotone function assigning integers to objects.
 * <p>
 * The first value is 0.
 *
 * @author ActiveViam
 */
public interface IDictionary<K> {

	/**
	 * Assigns a value to the given object
	 *
	 * @param object
	 * @return the position of the object in the dictionary
	 * @see #getPosition(Object)
	 */
	int map(K object);

	/**
	 * Gets the position of the given object
	 *
	 * @param object
	 * @return the position of the object in the dictionary, or -1 if the object is not in the
	 *         dictionary
	 * @see #map(Object)
	 */
	int getPosition(K object);

	/**
	 * Gets the object with the given position
	 *
	 * @param position the position of the object to retrieve
	 * @return the object with the given position
	 */
	K read(int position);

	/**
	 * @return the number of objects in the dictionary
	 */
	int size();

}

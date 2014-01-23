/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dma.ais.abnormal.stat.db.data;

/**
 * This is an interface to a multi-key map with three keys to one value.
 */
public interface ThreeKeyMap {

    /**
     * Return a String containing a single-worded English description of the semantics of key1
     * @return
     */
    String getMeaningOfKey1();

    /**
     * Return a String containing a single-worded English description of the semantics of key2
     * @return
     */
    String getMeaningOfKey2();

    /**
     * Return a String containing a single-worded English description of the semantics of key3
     * @return
     */
    String getMeaningOfKey3();

    /**
     * Increment the value pointed to by (key1, key2, key3) by one. If the value has not previously
     * been set, then set it to 1.
     *
     * @param key1
     * @param key2
     * @param key3
     */
    void incrementValue(int key1, int key2, String key3);

    /**
     * Set the value pointed to by (key1, key2, key3) to the given value.
     *
     * @param key1
     * @param key2
     * @param key3
     * @param value
     */
    void setValue(int key1, int key2, String key3, int value);

    /**
     * Get the value pointed to by (key1, key2, key3).
     *
     * @param key1
     * @param key2
     * @param key3
     */
    Integer getValue(int key1, int key2, String key3);

    /**
     * Iterate over all combinations of (key1, key2) and sum the values for the given key3.
     * Return the summed value.
     *
     * @param key3
     * @return
     */
    int getSumFor(String key3);

}

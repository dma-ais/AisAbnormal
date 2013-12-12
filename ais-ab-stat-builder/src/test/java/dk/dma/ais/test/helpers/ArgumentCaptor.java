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

package dk.dma.ais.test.helpers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class ArgumentCaptor<T> {
    T capturedObject;

    public static <T> ArgumentCaptor<T> forClass(Class<T> clazz) {
        return new ArgumentCaptor<T>();
    }

    public Matcher<T> getMatcher() {
        return new BaseMatcher<T>() {
            @SuppressWarnings("unchecked")
            public boolean matches(Object item) {
                capturedObject = (T) item;
                return true;
            }

            public void describeTo(Description paramDescription) {
            }
        };
    }

    public T getCapturedObject() {
        return capturedObject;
    }
}

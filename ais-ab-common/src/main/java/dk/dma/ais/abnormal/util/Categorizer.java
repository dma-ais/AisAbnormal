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

package dk.dma.ais.abnormal.util;

public final class Categorizer {

    // TODO changing the rules in this categorizer invalidates the featureData files. Retraining needed.

    public static short mapShipTypeToCategory(int shipType) {
        short bucket = 8;

        if (shipType > 79 && shipType < 90) {
            bucket = 1;
        } else if (shipType > 69 && shipType < 80) {
            bucket = 2;
        } else if ((shipType > 39 && shipType < 50) || (shipType > 59 && shipType < 70)) {
            bucket = 3;
        } else if ((shipType > 30 && shipType < 36) || (shipType > 49 && shipType < 56)) {
            bucket = 4;
        } else if (shipType == 30) {
            bucket = 5;
        } else if (shipType == 36 || shipType == 37) {    // TODO Class B
            bucket = 6;
        } else if ((shipType > 0 && shipType < 30) || (shipType > 89 && shipType < 100)) {
            bucket = 7;
        } else if (shipType == 0) {
            bucket = 8;
        }

        return bucket;
    }

    public static short mapShipLengthToCategory(int shipLength) {
        short bucket;

        if (shipLength >= 0 && shipLength < 1) {
            bucket = 1;
        } else if (shipLength >= 1 && shipLength < 50) {
            bucket = 2;
        } else if (shipLength >= 50 && shipLength < 100) {
            bucket = 3;
        } else if (shipLength >= 100 && shipLength < 200) {
            bucket = 4;
        } else if (shipLength >= 200 && shipLength < 999) {
            bucket = 5;
        } else {
            bucket = 6;
        }

        return bucket;
    }
}

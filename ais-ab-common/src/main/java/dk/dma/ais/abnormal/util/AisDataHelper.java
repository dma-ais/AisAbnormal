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

import com.google.common.base.Strings;

import static org.apache.commons.lang.StringUtils.isBlank;

public final class AisDataHelper {

    private AisDataHelper() {
    }

    /**
     * Trim AIS string to Java String by converting all '@'s to spaces, and then
     * trimming all leading and trailing spaces away.
     *
     * @param name
     * @return
     */
    public static String trimAisString(String name) {
        if (!Strings.isNullOrEmpty(name)) {
            name = name.replace('@', ' ').trim();
        }  else {
            name = "";
        }
        return name;
    }

    /**
     * If name is blank return the MMSI no. Otherwise return the name.
     * @param name
     * @param mmsi
     * @return
     */
    public static String nameOrMmsi(String name, int mmsi) {
        String trimmedName = trimAisString(name);
        return isBlank(trimmedName) ? "MMSI " + Integer.toString(mmsi) : trimmedName;
    }

    /**
     * Return true if speed over ground is available as per ITU-R M.1371-4 (table 45)
     * @param sog
     * @return
     */
    public static boolean isSpeedOverGroundAvailable(Float sog) {
        return sog != null && sog < 102.3;
    }

    /**
     * Return true if course over ground is available as per ITU-R M.1371-4 (table 45)
     * @param cog
     * @return
     */
    public static boolean isCourseOverGroundAvailable(Float cog) {
        return cog != null && cog < 360.0;
    }

    /**
     * Return true if true heading is available as per ITU-R M.1371-4 (table 45)
     * @param hdg
     * @return
     */
    public static boolean isTrueHeadingAvailable(Float hdg) {
        return hdg != null && hdg < 360.0;
    }

}

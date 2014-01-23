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

package dk.dma.ais.abnormal.application;

import org.slf4j.Logger;

import java.util.Arrays;

/**
 * This class holds a few very generic methods which provide common support to
 * applications.
 */

public final class ApplicationSupport {

    public static void logJavaSystemProperties(Logger log) {
        Arrays.asList(
                "java.version",
                "java.specification.version",
                "java.vm.version",
                "java.home",
                "java.vendor",
                "os.arch",
                "os.name",
                "os.version",
                "user.name"
        ).forEach(s -> log.info(s + ": " + System.getProperty(s)));
    }

}

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

package dk.dma.ais.abnormal.analyzer.userargs;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.net.MalformedURLException;
import java.net.URL;

public class AisDataSourceUrlValidator implements IParameterValidator {
    /**
     * Validate the url for the AIS data source.
     * Valid formats are:
     *
     * @param name
     * @param value
     * @throws ParameterException
     */
    @Override
    public void validate(String name, String value) throws ParameterException {
        try {
            URL url = new URL(value);
            String protocol = url.getProtocol();
            if (!"file".equalsIgnoreCase(protocol) && !"tcp".equalsIgnoreCase(protocol)) {
                throw new ParameterException("Only 'file' and 'tcp' protocols supported; not " + protocol + ".");
            }
            if ("tcp".equalsIgnoreCase(protocol)) {
                if (url.getPort() < 0) {
                    throw new ParameterException("Port no required. Expected format: tcp://host:port/");
                }
            }
        } catch (MalformedURLException e) {
            throw new ParameterException(e);
        }
    }
}

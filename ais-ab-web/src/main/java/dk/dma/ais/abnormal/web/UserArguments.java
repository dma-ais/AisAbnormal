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

package dk.dma.ais.abnormal.web;

import com.beust.jcommander.Parameter;

public class UserArguments {

    @Parameter(names = "-help", help = true, description = "Print this help", hidden = true)
    protected boolean help;

    @Parameter(names = "-port", description = "Port no. for listening to HTTP.")
    private int port = 8080;

    @Parameter(names = "-featureDirectory", description = "Directory to scan for feature files to read", required = true)
    private String inputDirectory;

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean isHelp() {
        return help;
    }

    public int getPort() {
        return port;
    }

    public String getInputDirectory() {
        return inputDirectory;
    }

}

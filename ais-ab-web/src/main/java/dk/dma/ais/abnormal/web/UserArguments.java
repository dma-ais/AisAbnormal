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

@SuppressWarnings("FieldCanBeLocal")
public class UserArguments {

    @Parameter(names = "-help", help = true, description = "Print this help.", hidden = true)
    protected boolean help;

    @Parameter(names = "-port", description = "Port no. for listening to HTTP.")
    private int port = 8080;

    @Parameter(names = "-statistics", description = "Filename of statistic data file to read.", required = true)
    private String pathToStatisticData;

    // -- Repository type choice

    @Parameter(names = "-eventDataRepositoryType", description = "Type of repository used to hold event data ('h2', 'pgsql').", required = true)
    private String eventDataRepositoryType;

    // -- Postgres specific args

    @Parameter(names = "-eventDataDbHost", description = "Name of the event database RDBMS host.", required = false)
    private String eventDataDbHost;

    @Parameter(names = "-eventDataDbPort", description = "Remote port of the event database RDBMS host.", required = false)
    private Integer eventDataDbPort;

    @Parameter(names = "-eventDataDbUsername", description = "Username to connect to the event database RDBMS host.", required = false)
    private String eventDataDbUsername;

    @Parameter(names = "-eventDataDbPassword", description = "Password to connect to the event database RDBMS host.", required = false)
    private String eventDataDbPassword;

    @Parameter(names = "-eventDataDbName", description = "Database name to use for the event database with the RDBMS host.", required = false)
    private String eventDataDbName;

    // -- H2 specific args

    @Parameter(names = "-eventDataDbFile", description = "Name of RDBMS file to hold event data.", required = false)
    private String eventDataDbFile;

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean isHelp() {
        return help;
    }

    public int getPort() {
        return port;
    }

    public String getPathToStatisticData() {
        return pathToStatisticData;
    }

    public String getEventDataRepositoryType() {
        return eventDataRepositoryType;
    }

    public String getEventDataDbHost() {
        return eventDataDbHost;
    }

    public Integer getEventDataDbPort() {
        return eventDataDbPort;
    }

    public String getEventDataDbUsername() {
        return eventDataDbUsername;
    }

    public String getEventDataDbPassword() {
        return eventDataDbPassword;
    }

    public String getEventDataDbName() {
        return eventDataDbName;
    }

    public String getEventDataDbFile() {
        return eventDataDbFile;
    }
}

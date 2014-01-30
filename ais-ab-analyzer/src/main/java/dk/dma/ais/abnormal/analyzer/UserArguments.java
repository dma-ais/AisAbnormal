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

package dk.dma.ais.abnormal.analyzer;

import com.beust.jcommander.Parameter;
import com.google.common.base.Strings;

@SuppressWarnings("FieldCanBeLocal")
public class UserArguments {

    @Parameter(names = "-help", help = true, description = "Print this help", hidden = true)
    protected boolean help;

    @Parameter(names = "-inputDirectory", description = "Directory to scan for files to read", required = true)
    private String inputDirectory = ".";

    @Parameter(names = "-input", description = "Glob pattern for files to read. '.zip' and '.gz' files are decompressed automatically.", required = true)
    private String inputFilenamePattern;

    @Parameter(names = "-r", description = "Recursive directory scan")
    private boolean recursive;

    @Parameter(names = "-featureData", description = "Name of file containing feature data statistics.", required = true)
    private String featureData;

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

    @Parameter(names = "-eventDataDbFile", description = "Name of file to hold event data.", required = false)
    private String eventDataDbFile;

    // -- AIS stream args

    @Parameter(names = "-downsampling", description = "Downsampling period (in secs).")
    private Integer downSampling = 10;

    // --

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean isHelp() {
        return help;
    }

    public String getInputDirectory() {
        return inputDirectory;
    }

    public String getInputFilenamePattern() {
        return inputFilenamePattern;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public String getFeatureData() {
        return featureData;
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

    public Integer getDownSampling() {
        return downSampling;
    }

    public boolean paramsValidForH2() {
        return "h2".equalsIgnoreCase(eventDataRepositoryType) &&
                !Strings.isNullOrEmpty(eventDataDbFile);
    }

    public boolean paramsValidForPgsql() {
        return "pgsql".equalsIgnoreCase(eventDataRepositoryType) &&
                !Strings.isNullOrEmpty(eventDataDbHost) &&
                !(eventDataDbPort == null) &&
                !Strings.isNullOrEmpty(eventDataDbUsername) &&
                !Strings.isNullOrEmpty(eventDataDbPassword) &&
                !Strings.isNullOrEmpty(eventDataDbName);
    }
}

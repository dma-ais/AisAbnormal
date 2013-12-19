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

    @Parameter(names = "-eventData", description = "Name of file to hold event data.", required = true)
    private String pathToEventDatabase;

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

    public String getPathToEventDatabase() {
        return pathToEventDatabase;
    }
}

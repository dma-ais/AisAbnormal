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

package dk.dma.ais.abnormal.stat;

import com.beust.jcommander.Parameter;

public class UserArguments {

    @Parameter(names = "-help", help = true, description = "Print this help", hidden = true)
    protected boolean help;

    @Parameter(names = "-mt", description = "Run multi-threaded.")
    private boolean multiThreaded;

    @Parameter(names = "-inputDirectory", description = "Directory to scan for files to read")
    private String inputDirectory = ".";

    @Parameter(names = "-r", description = "Recursive directory scan")
    private boolean recursive;

    @Parameter(names = "-input", description = "Glob pattern for files to read. '.zip' and '.gz' files are decompressed automatically.", required = true)
    private String inputFilenamePattern;

    @Parameter(names = "-output", description = "Name of output file.", required = true)
    private String outputFilename;

    @Parameter(names = "-gridsize", description = "Grid resolution (approx. cell size in meters).")
    private Integer gridSize = 200;

    @Parameter(names = "-downsampling", description = "Downsampling period (in secs).")
    private Integer downSampling = 60;

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isMultiThreaded() {
        return multiThreaded;
    }

    public Integer getGridSize() {
        return gridSize;
    }

    public String getInputDirectory() {
        return inputDirectory;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public String getInputFilenamePattern() {
        return inputFilenamePattern;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public Integer getDownSampling() {
        return downSampling;
    }
}

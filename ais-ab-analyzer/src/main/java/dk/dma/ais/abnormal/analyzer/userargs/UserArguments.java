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

import com.beust.jcommander.Parameter;

import java.io.File;
import java.nio.file.Path;

/**
 * The UserArguments class defines names, help texts, and default values for all
 * command line arguments accepted by the application.
 *
 * It also holds typed copies of the command line arguments after application launch.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
@SuppressWarnings("FieldCanBeLocal")
public class UserArguments {

    @Parameter(names = "-help", help = true, description = "Print this help", hidden = true)
    protected boolean help;

    @Parameter(names = "-config", description = "Name of configuration file to use.", required = true)
    private File configFile;

    // --

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean isHelp() {
        return help;
    }

    public Path getConfigFile() {
        return configFile.toPath();
    }

}

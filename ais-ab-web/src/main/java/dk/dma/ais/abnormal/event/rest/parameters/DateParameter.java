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

package dk.dma.ais.abnormal.event.rest.parameters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateParameter {

    private static final String DATE_FORMAT_STRING = "dd/MM/yyyy HH:mm";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);

    private Date date;

    public DateParameter(String dateStr) {
        try {
            if (!dateStr.contains(":")) {
                dateStr += " 00:00";
            }
            this.date = DATE_FORMAT.parse(dateStr);
        } catch (ParseException pe) {
            throw new IllegalArgumentException("Illegal format of date parameter: \"" + dateStr + "\". Expected format is: \"" + DATE_FORMAT_STRING + "\".");
        }
    }

    public Date value() {
        return this.date;
    }

}

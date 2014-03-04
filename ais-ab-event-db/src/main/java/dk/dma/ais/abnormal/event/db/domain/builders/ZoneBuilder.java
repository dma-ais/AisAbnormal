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

package dk.dma.ais.abnormal.event.db.domain.builders;

import dk.dma.ais.abnormal.event.db.domain.Zone;

public class ZoneBuilder {

    Zone zone;
    CloseEncounterEventBuilder closeEncounterEventBuilder;

    public ZoneBuilder(CloseEncounterEventBuilder closeEncounterEventBuilder) {
        this.closeEncounterEventBuilder = closeEncounterEventBuilder;
        zone = new Zone();
    }

    public Zone getZone() {
        return zone;
    }

    public ZoneBuilder centerLatitude(double centerLatitude) {
        zone.setCenterLatitude(centerLatitude);
        return this;
    }

    public ZoneBuilder centerLongitude(double centerLongitude) {
        zone.setCenterLongitude(centerLongitude);
        return this;
    }

    public ZoneBuilder majorAxisHeading(double majorAxisHeading) {
        zone.setMajorAxisHeading(majorAxisHeading);
        return this;
    }

    public ZoneBuilder majorSemiAxisLength(double majorSemiAxisLength) {
        zone.setMajorSemiAxisLength(majorSemiAxisLength);
        return this;
    }

    public CloseEncounterEventBuilder minorSemiAxisLength(double minorSemiAxisLength) {
        zone.setMinorSemiAxisLength(minorSemiAxisLength);
        return closeEncounterEventBuilder;
    }

}

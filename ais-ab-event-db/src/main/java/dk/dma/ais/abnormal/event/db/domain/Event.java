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

package dk.dma.ais.abnormal.event.db.domain;

import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.util.Date;

/**
 * This abstract class is the common class of all Events. An event describes some sort of abnormal behaviour
 * detected among moving vessels in the maritime domain.
 *
 * An Event is also related to at least one vessel. Some event types can be related to further vessels or to
 * other Events.
 */
public abstract class Event {
    /**
     * Time on which the event started.
     */
    @NotNull
    @Past
    private Date startTime;

    /**
     * Time on which the event ended.
     */
    @Past
    private Date endTime;
    /**
     * MMSI no. of the primary involved vessel.
     */

    /**
     * The primary vessel involved the event and its behaviour.
     */
    @NotNull
    private Vessel vessel;

    /**
     * A textual description of the event in English language.
     */
    private String description;

    protected Event() {
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Vessel getVessel() {
        return vessel;
    }

    public void setVessel(Vessel vessel) {
        this.vessel = vessel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("startTime", startTime)
                .add("endTime", endTime)
                .add("vessel", vessel)
                .add("description", description)
                .toString();
    }
}

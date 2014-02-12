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

package dk.dma.ais.abnormal.analyzer.behaviour.events;

import dk.dma.ais.abnormal.analyzer.behaviour.EventCertainty;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.tracker.Track;

public abstract class AbnormalAbstractEvent {

    private final Class<? extends Event> eventClass;
    private final Track track;
    private final EventCertainty eventCertainty;

    protected AbnormalAbstractEvent(Class<? extends Event> eventClass, Track track, EventCertainty eventCertainty) {
        this.eventClass = eventClass;
        this.track = track;
        this.eventCertainty = eventCertainty;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    public Track getTrack() {
        return track;
    }

    public EventCertainty getEventCertainty() {
        return eventCertainty;
    }
}

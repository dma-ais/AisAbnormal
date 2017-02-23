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
import dk.dma.ais.tracker.eventEmittingTracker.Track;

public abstract class AbnormalAbstractEvent {

    private final Class<? extends Event> eventClass;
    private final EventCertainty eventCertainty;
    private final Track track;
    private final Track[] otherTracks;

    protected AbnormalAbstractEvent(Class<? extends Event> eventClass, EventCertainty eventCertainty, Track track, Track... otherTracks) {
        this.eventClass = eventClass;
        this.eventCertainty = eventCertainty;
        this.track = track;
        this.otherTracks = otherTracks;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    public EventCertainty getEventCertainty() {
        return eventCertainty;
    }

    public Track getTrack() {
        return track;
    }

    public Track[] getOtherTracks() {
        return otherTracks;
    }
}

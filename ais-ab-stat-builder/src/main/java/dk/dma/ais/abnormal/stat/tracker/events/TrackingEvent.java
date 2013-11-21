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

package dk.dma.ais.abnormal.stat.tracker.events;

import com.google.common.base.Objects;
import dk.dma.ais.abnormal.stat.tracker.Track;

public abstract class TrackingEvent {

    private final Track track;

    TrackingEvent(Track track) {
        this.track = track;
    }

    public final Track getTrack() {
        return track;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("track", track)
                .toString();
    }
}

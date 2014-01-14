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

import com.google.common.collect.ImmutableSortedSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static java.util.Collections.sort;

@Entity
public class Behaviour {

    public Behaviour() {
        trackingPoints = new LinkedList<>();
    }

    public SortedSet<TrackingPoint> getTrackingPoints() {
        sort(trackingPoints);
        return ImmutableSortedSet.copyOf(trackingPoints);
    }

    public void addTrackingPoint(TrackingPoint TrackingPoint) {
        trackingPoints.add(TrackingPoint);
    }

    public void addTrackingPoints(Set<TrackingPoint> TrackingPoints) {
        this.trackingPoints.addAll(TrackingPoints);
    }

    public Vessel getVessel() {
        return vessel;
    }

    public void setVessel(Vessel vessel) {
        this.vessel = vessel;
    }

    public long getId() {
        return id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    /**
     * The vessel involved in this behaviour.
     */
    @NotNull
    @OneToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Vessel vessel;

    @NotNull
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @OrderBy("timestamp")
    private List<TrackingPoint> trackingPoints;

}

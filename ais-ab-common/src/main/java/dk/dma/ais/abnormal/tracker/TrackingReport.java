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

package dk.dma.ais.abnormal.tracker;

import com.rits.cloning.Cloner;
import dk.dma.enav.model.geometry.Position;
import net.jcip.annotations.NotThreadSafe;

import java.util.HashMap;
import java.util.Map;

@NotThreadSafe
public final class TrackingReport implements Cloneable {
    private final long timestamp;
    private final Position position;
    private final Float courseOverGround;
    private final Float speedOverGround;
    private final boolean interpolated;

    private final Map<String, Object> properties = new HashMap<>(1);

    public static TrackingReport create(long timestamp, Position position, Float courseOverGround, Float speedOverGround, boolean interpolated) {
        return new TrackingReport(timestamp, position, courseOverGround, speedOverGround, interpolated);
    }

    private TrackingReport(long timestamp, Position position, Float courseOverGround, Float speedOverGround, boolean interpolated) {
        this.timestamp = timestamp;
        this.position = position;
        this.speedOverGround = speedOverGround;
        this.courseOverGround = courseOverGround;
        this.interpolated = interpolated;
    }

    public Object getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    public void setProperty(String propertyName, Object propertyValue) {
        properties.put(propertyName, propertyValue);
    }

    public void removeProperty(String propertyName) {
        properties.remove(propertyName);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Position getPosition() {
        return position;
    }

    public Float getCourseOverGround() {
        return courseOverGround;
    }

    public Float getSpeedOverGround() {
        return speedOverGround;
    }

    public boolean isInterpolated() {
        return interpolated;
    }

    @Override
    public TrackingReport clone() {
        return new Cloner().deepClone(this);
    }
}

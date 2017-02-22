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

package dk.dma.ais.abnormal.event.db.csv;

import com.google.inject.Inject;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Behaviour;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CsvEventRepository is an implementation of the EventRepository interface which
 * manages persistent Event objects in a flat CSV file.
 */
@ThreadSafe
public class CsvEventRepository implements EventRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CsvEventRepository.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final ReentrantLock lock = new ReentrantLock();

    private final CSVPrinter printer;

    private final boolean readonly;

    @GuardedBy("lock")
    private final Map<Integer, Map<Class<? extends Event>, Event>> ongoingEvents = new HashMap<>();

    @Inject
    public CsvEventRepository(OutputStream out, boolean readonly) throws IOException {
        Writer writer = new OutputStreamWriter(out);

        printer = CSVFormat.RFC4180
                    .withHeader(
                        "eventId", "eventType", "startTime", "endTime", "title",
                        "description", "mmsis",
                        "pMmsi", "pName", "pCallsign", "pType", "pLength", "pLat", "pLon",
                        "sMmsi", "sName", "sCallsign", "sType", "sLength", "sLat", "sLon"
                    ).print(writer);

        this.readonly = readonly;
    }

    @Override
    public List<String> getEventTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(Event event) {
        if (readonly == false) {
            if (event.getState() == Event.State.ONGOING) {
                insertOngoingEvent(event);

                try {
                    Behaviour primaryBehaviour = event.primaryBehaviour();
                    Behaviour secondaryBehaviour = event.arbitraryNonPrimaryBehaviour();

                    TrackingPoint primaryLastTrackingPoint = primaryBehaviour.getTrackingPoints().last();
                    TrackingPoint secondaryLastTrackingPoint = secondaryBehaviour == null ? null : secondaryBehaviour.getTrackingPoints().last();

                    printer.printRecord(
                        event.getId(),
                        event.getEventType(),
                        event.getStartTime() == null ? null : LocalDateTime.ofInstant(event.getStartTime().toInstant(), ZoneOffset.UTC),
                        event.getEndTime() == null ? null : LocalDateTime.ofInstant(event.getEndTime().toInstant(), ZoneOffset.UTC),
                        event.getTitle(),
                        event.getDescription(),
                        event.involvedMmsis(),
                        primaryBehaviour.getVessel().getMmsi(),
                        primaryBehaviour.getVessel().getName(),
                        primaryBehaviour.getVessel().getCallsign(),
                        primaryBehaviour.getVessel().getType(),
                        primaryBehaviour.getVessel().getLength(),
                        primaryLastTrackingPoint.getLatitude(),
                        primaryLastTrackingPoint.getLongitude(),
                        secondaryBehaviour == null ? null : secondaryBehaviour.getVessel().getMmsi(),
                        secondaryBehaviour == null ? null : secondaryBehaviour.getVessel().getName(),
                        secondaryBehaviour == null ? null : secondaryBehaviour.getVessel().getCallsign(),
                        secondaryBehaviour == null ? null : secondaryBehaviour.getVessel().getType(),
                        secondaryBehaviour == null ? null : secondaryBehaviour.getVessel().getLength(),
                        secondaryLastTrackingPoint == null ? null : secondaryLastTrackingPoint.getLatitude(),
                        secondaryLastTrackingPoint == null ? null : secondaryLastTrackingPoint.getLongitude()
                    );

                    printer.flush();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            } else {
                removeOngoingEvent(event);
            }
        }
    }

    @Override
    public Event getEvent(long eventId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Event> findEventsByFromAndToAndTypeAndVesselAndArea(Date from, Date to, String type, String vessel, Double north, Double east, Double south, Double west) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Event> findEventsByFromAndTo(Date from, Date to) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Event> findRecentEvents(int numberOfEvents) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Event> T findOngoingEventByVessel(int mmsi, Class<T> eventClass) {
        Event event = null;

        lock.lock();
        try {
            Map<Class<? extends Event>, Event> eventMap = ongoingEvents.get(mmsi);
            if (eventMap != null)
                event = eventMap.get(eventClass);
        } finally {
            lock.unlock();
        }

        return event == null ? null : (T) event;
    }

    private void insertOngoingEvent(Event event) {
        lock.lock();
        try {
            if (event.getState() == Event.State.ONGOING) {
                event.involvedMmsis().forEach( mmsi -> {
                    Map<Class<? extends Event>, Event> eventMap = ongoingEvents.get(mmsi);

                    if (eventMap == null) {
                        eventMap = new HashMap<>();
                        ongoingEvents.put(mmsi, eventMap);
                    }

                    eventMap.put(event.getClass(), event);
                });
            }
        } finally {
            lock.unlock();
        }
    }

    private void removeOngoingEvent(Event event) {
        lock.lock();
        try {
            event.involvedMmsis().forEach( mmsi -> {
                Map<Class<? extends Event>, Event> eventMap = ongoingEvents.get(mmsi);
                if (eventMap != null) {
                    eventMap.remove(event.getClass());
                }
            });
        } finally {
            lock.unlock();
        }
    }

}

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

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import dk.dma.ais.abnormal.tracker.events.CellIdChangedEvent;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisPosition;
import dk.dma.ais.message.AisTargetType;
import dk.dma.ais.message.IPositionMessage;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.grid.Cell;
import dk.dma.enav.model.geometry.grid.Grid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

public class TrackingServiceImpl implements TrackingService {

    static final Logger LOG = LoggerFactory.getLogger(TrackingServiceImpl.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private EventBus eventBus = new EventBus();
    final HashMap<Integer, Track> tracks = new HashMap<>(256);
    final Grid grid;

    @Inject
    public TrackingServiceImpl(Grid grid) {
        eventBus.register(this);
        this.grid = grid;
    }

    @Override
    public void update(Date timestamp, AisMessage aisMessage) {
        final AisTargetType targetType = aisMessage.getTargetType();

        int mmsi = aisMessage.getUserId();
        if (targetType == AisTargetType.A || targetType == AisTargetType.B) {
            Track track = getOrCreateTrack(mmsi);

            // Manage timestamps
            Long lastUpdate = (Long) track.getProperty(Track.TIMESTAMP);
            if (lastUpdate == null) {
                lastUpdate = 0L;
            }
            Long currentUpdate = timestamp.getTime();
            updateTimestamp(track, currentUpdate);

            // Perform track updates
            if (currentUpdate >= lastUpdate) {
                updateVesselName(track, aisMessage);
                updateImo(track, aisMessage);
                updateCallsign(track, aisMessage);
                updatePosition(track, aisMessage);
                updateCellId(track, aisMessage);
                updateShipType(track, aisMessage);
                updateVesselLength(track, aisMessage);
            } else {
                Long timeDelta = lastUpdate - currentUpdate;
                LOG.warn("Message of type " + aisMessage.getMsgId() + " ignored because it is out of sequence by " + timeDelta + " msecs (mmsi " + mmsi + ")");
            }
        } else {
            LOG.debug("Tracker does not support target type " + targetType + " (mmsi " + mmsi + ")");
        }
    }

    private void updateTimestamp(Track track, Long timestampMillis) {
        track.setProperty(Track.TIMESTAMP, timestampMillis);
    }

    private void updateVesselName(Track track, AisMessage aisMessage) {
        if (aisMessage instanceof AisMessage5) {
            AisMessage5 aisMessage5 = (AisMessage5) aisMessage;
            String name = aisMessage5.getName();
            track.setProperty(Track.SHIP_NAME, name);
        }
    }

    private void updateImo(Track track, AisMessage aisMessage) {
        if (aisMessage instanceof AisMessage5) {
            AisMessage5 aisMessage5 = (AisMessage5) aisMessage;
            long imo = aisMessage5.getImo();
            track.setProperty(Track.IMO, imo);
        }
    }

    private void updateCallsign(Track track, AisMessage aisMessage) {
        if (aisMessage instanceof AisMessage5) {
            AisMessage5 aisMessage5 = (AisMessage5) aisMessage;
            String callsign = aisMessage5.getCallsign();
            track.setProperty(Track.CALLSIGN, callsign);
        }
    }

    private void updatePosition(Track track, AisMessage aisMessage) {
        if (aisMessage instanceof IPositionMessage) {
            IPositionMessage positionMessage = (IPositionMessage) aisMessage;
            AisPosition aisPosition = positionMessage.getPos();
            Position position = aisPosition.getGeoLocation();
            if (position != null) {
                track.setProperty(Track.POSITION, position);
            } else {
                track.removeProperty(Track.POSITION);
            }
        }
    }

    private void updateCellId(Track track, AisMessage aisMessage) {
        if (aisMessage instanceof IPositionMessage) {
            Long oldCellId = (Long) track.getProperty(Track.CELL_ID);
            Long newCellId;

            IPositionMessage positionMessage = (IPositionMessage) aisMessage;
            AisPosition aisPosition = positionMessage.getPos();
            Position position = aisPosition.getGeoLocation();
            if (position != null) {
                Cell cell = grid.getCell(aisPosition.getGeoLocation());
                newCellId = cell.getCellId();
                track.setProperty(Track.CELL_ID, newCellId);
                if ( (oldCellId == null && newCellId != null) ||
                        (oldCellId != null && newCellId == null) ||
                        (oldCellId != null && newCellId != null && !oldCellId.equals(newCellId))) {
                    eventBus.post(new CellIdChangedEvent(track, oldCellId));
                }
            } else {
                track.removeProperty(Track.CELL_ID);
                if (oldCellId != null) {
                    eventBus.post(new CellIdChangedEvent(track, oldCellId));
                }
                LOG.warn("Message type " + aisMessage.getMsgId()  + " contained no valid position: " + aisPosition.getRawLatitude() + ", " + aisPosition.getRawLongitude() + " (mmsi " + track.getMmsi() + ")");
            }
        }
    }

    private void updateShipType(Track track, AisMessage aisMessage) {
        if (aisMessage instanceof AisMessage5) {
            AisMessage5 aisMessage5 = (AisMessage5) aisMessage;
            Integer shipType = aisMessage5.getShipType();
            track.setProperty(Track.SHIP_TYPE, shipType);
        }
    }

    private void updateVesselLength(Track track, AisMessage aisMessage) {
        if (aisMessage instanceof AisMessage5) {
            AisMessage5 aisMessage5 = (AisMessage5) aisMessage;

            Integer vesselLength;
            vesselLength = aisMessage5.getDimBow() + aisMessage5.getDimStern();

            track.setProperty(Track.VESSEL_LENGTH, vesselLength);
        }
    }

    private Track getOrCreateTrack(int mmsi) {
        Track track = tracks.get(mmsi);
        if (track == null) {
            track = new Track(mmsi);
            tracks.put(mmsi, track);
        }
        return track;
    }

    @Subscribe
    public void listen(DeadEvent event) {
        LOG.warn("No subscribers were interested in this event: " + event.getEvent());
    }

    @Override
    public void registerSubscriber(Object subscriber) {
        eventBus.register(subscriber);
    }

    @Override
    public Integer getNumberOfTracks() {
        return tracks.size();
    }
}

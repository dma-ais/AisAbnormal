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
package dk.dma.ais.abnormal.stat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.data.AisTargetDimensions;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisPositionMessage;
import dk.dma.ais.message.ShipTypeCargo;
import dk.dma.ais.packet.AisPacket;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.grid.Cell;
import dk.dma.enav.model.geometry.grid.Grid;
import dk.dma.enav.util.function.Consumer;

/**
 * Handler for read AIS packets
 */
public class PacketHandler implements Consumer<AisPacket> {
    
    static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);

    private final String db;
    private final StatBuilderStatistics buildStats = new StatBuilderStatistics(1, TimeUnit.MINUTES);
    private final Map<Cell, Object> cellCache = new HashMap<>();
    private volatile boolean cancel;
    
    private final Grid grid;

    public PacketHandler(String db) {
        this.db = db;
        
        this.grid = Grid.createSize(200);
        
        // TODO configuration encapsulation and maybe properties        

        // TODO initialization
    }

    public void accept(AisPacket packet) {
        if (cancel) {
            return;
        }
                
        buildStats.incPacketCount();

        // Just lots of example code

        // Get AisMessage from packet or drop
        AisMessage message = packet.tryGetAisMessage();
        if (message == null) {
            return;
        }
        buildStats.incMessageCount();

        // Handle class A position message (to include class B use IVesselPosMessage)
        if (message instanceof AisPositionMessage) {            
            handlePos((AisPositionMessage) message);
        }
        // Handler class A static message
        else if (message instanceof AisMessage5) {
            handleStatic((AisMessage5) message);
        }

        
        buildStats.log();        
    }

    private void handlePos(AisPositionMessage posMsg) {
        buildStats.incPosMsgCount();
        
        // Get position
        Position pos = posMsg.getValidPosition();
        if (pos == null) {
            return;
        }
        
        // boolean validators that can be used
        posMsg.isCogValid();
        posMsg.isSogValid();
        posMsg.isHeadingValid();
        
        // Get cell of position
        Cell cell = grid.getCell(pos);

        if (!cellCache.containsKey(cell)) {
            cellCache.put(cell, new Object());
        }
        
        buildStats.setCellCount(cellCache.size());                

    }

    private void handleStatic(AisMessage5 msg5) {
        buildStats.incStatMsgCount();
        
        ShipTypeCargo shipTypeCarge = new ShipTypeCargo(msg5.getShipType());
        shipTypeCarge.getShipType();
        
        AisTargetDimensions dimensions = new AisTargetDimensions(msg5);
        dimensions.getDimBow();
        
    }

    public void cancel() {
        cancel = true;
        // TODO close down and clean up
    }
    
    public StatBuilderStatistics getBuildStats() {
        return buildStats;
    }

}

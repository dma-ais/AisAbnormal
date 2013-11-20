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

package dk.dma.ais.abnormal.stat.features;

import dk.dma.ais.data.AisTargetDimensions;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisPositionMessage;
import dk.dma.ais.message.ShipTypeCargo;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.grid.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShipTypeAndSizeFeature extends Feature {

    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger(ShipTypeAndSizeFeature.class);

    public ShipTypeAndSizeFeature() {
    }

    @Override
    public void trainFrom(AisMessage aisMessage) {
        // Handle class A position message (to include class B use IVesselPosMessage)
        if (aisMessage instanceof AisPositionMessage) {
            handlePos((AisPositionMessage) aisMessage);
        }
        // Handler class A static message
        else if (aisMessage instanceof AisMessage5) {
            handleStatic((AisMessage5) aisMessage);
        }
    }


    private void handlePos(AisPositionMessage posMsg) {
        appStatisticsService.incPosMsgCount();

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

        appStatisticsService.setCellCount(cellCache.size());

    }

    private void handleStatic(AisMessage5 msg5) {
        appStatisticsService.incStatMsgCount();

        ShipTypeCargo shipTypeCargo = new ShipTypeCargo(msg5.getShipType());
        shipTypeCargo.getShipType();

        AisTargetDimensions dimensions = new AisTargetDimensions(msg5);
        dimensions.getDimBow();

    }
}

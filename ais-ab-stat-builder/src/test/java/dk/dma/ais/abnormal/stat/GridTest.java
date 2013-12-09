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

import dk.dma.ais.filter.DownSampleFilter;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.grid.Cell;
import dk.dma.enav.model.geometry.grid.Grid;
import dk.dma.enav.util.function.Consumer;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class GridTest {
    
    @Test
    public void countCellsFromFeed() throws IOException, InterruptedException {        
        final Set<Integer> mmsis = new HashSet<>();
        final Set<Cell> cells = new HashSet<>();
        final Grid grid = Grid.createSize(200);
        final AtomicReference<Long> mCount = new AtomicReference<>(0L);
        long start = System.currentTimeMillis();
        final DownSampleFilter df = new DownSampleFilter(0);
        
        //AisReader reader = AisReaders.createReaderFromFile("/tmp/ais_dk_76h.txt.gz");
        AisReader reader = AisReaders.createReaderFromFile("src/test/resources/ais-sample.txt.gz");
        reader.registerPacketHandler(new Consumer<AisPacket>() {            
            @Override
            public void accept(AisPacket p) {
                if (df.rejectedByFilter(p)) {
                    return;
                }
                AisMessage m = p.tryGetAisMessage();
                if (m == null) {
                    return;
                }
                mCount.set(mCount.get() + 1);                
                // Only look at class a position reports
                if (!(m instanceof AisPositionMessage)) {
                    return;
                }                
                mmsis.add(m.getUserId());                
                AisPositionMessage posM = (AisPositionMessage)m;
                Position pos = posM.getValidPosition();
                if (pos == null) {
                    return;
                }
                cells.add(grid.getCell(pos));
            }            
        });
        
        reader.start();
        reader.join();
        
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        double rate = (double)mCount.get() / (double)elapsed;
        System.out.println(String.format("%-30s %9d", "Elapsed s", elapsed));
        System.out.println(String.format("%-30s %9.0f msg/sec", "Message rate", rate));
        System.out.println(String.format("%-30s %9d", "Unique mmsi", mmsis.size()));
        System.out.println(String.format("%-30s %9d", "Unique cells", cells.size()));
    }

}

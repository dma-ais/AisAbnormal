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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import dk.dma.ais.abnormal.stat.features.Feature;
import dk.dma.ais.abnormal.stat.features.ShipTypeAndSizeFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.filter.DownSampleFilter;
import dk.dma.ais.filter.DuplicateFilter;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.packet.AisPacket;

import java.util.Set;

/**
 * Handler for read AIS packets
 */
public class PacketHandlerImpl implements PacketHandler {

    static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    
    @Inject
    private AppStatisticsService appStatisticsService; // = new AppStatisticsServiceImpl(1, TimeUnit.MINUTES);
    private volatile boolean cancel;

    private final DuplicateFilter duplicateFilter;
    private final DownSampleFilter downSampleFilter;

    @Inject
    private ShipTypeAndSizeFeature shipTypeAndSizeFeature;

    private Set<Feature> features;

    public PacketHandlerImpl() {
        this.duplicateFilter = new DuplicateFilter();
        this.downSampleFilter = new DownSampleFilter(0);

        // TODO configuration encapsulation and maybe properties

        // TODO initialization
    }

    public void accept(AisPacket packet) {
        if (cancel) {
            return;
        }

        // Duplicate and down sampling filtering
        if (duplicateFilter.rejectedByFilter(packet) || downSampleFilter.rejectedByFilter(packet)) {
            return;
        }

        appStatisticsService.incPacketCount();

        // Get AisMessage from packet or drop
        AisMessage message = packet.tryGetAisMessage();
        if (message == null) {
            return;
        }
        appStatisticsService.incMessageCount();

        if (features == null) {
            initFeatures();
        }

        for (Feature feature: features) {
            feature.trainFrom(message);
        }

        appStatisticsService.log();
    }
    @Override
    public void cancel() {
        cancel = true;
        // TODO close down and clean up
    }

    @Override
    public AppStatisticsService getBuildStats() {
        return appStatisticsService;
    }

    private void initFeatures() {
        // TODO figure out how to do @PostConstruct with Guice
        this.features = new ImmutableSet.Builder<Feature>()
            .add(shipTypeAndSizeFeature)
            .build();
    }
}
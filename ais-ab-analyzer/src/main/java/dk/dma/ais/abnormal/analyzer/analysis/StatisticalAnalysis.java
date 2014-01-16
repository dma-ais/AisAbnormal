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

package dk.dma.ais.abnormal.analyzer.analysis;

import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.tracker.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StatisticalAnalysis implements Analysis {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticalAnalysis.class);
    private final FeatureDataRepository featureDataRepository;
    private final TrackingService trackingService;

    protected StatisticalAnalysis(FeatureDataRepository featureDataRepository, TrackingService trackingService) {
        this.featureDataRepository = featureDataRepository;
        this.trackingService = trackingService;
    }

    @Override
    public void start() {
        LOG.info(this.getClass().getSimpleName() + " starts to listen for tracking events.");
        trackingService.registerSubscriber(this);
    }

    protected final FeatureDataRepository getFeatureDataRepository() {
        return featureDataRepository;
    }

}

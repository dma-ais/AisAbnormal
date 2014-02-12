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

import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManager;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.tracker.TrackingService;

/**
 * This class provides basic and common functionality to Analysis-es which are based
 * on comparison of current track behaviour with feature data based on history behaviours.
 */
public abstract class FeatureDataBasedAnalysis extends Analysis {

    private final FeatureDataRepository featureDataRepository;

    protected FeatureDataBasedAnalysis(EventRepository eventRepository, FeatureDataRepository featureDataRepository, TrackingService trackingService, BehaviourManager behaviourManager) {
        super(eventRepository, trackingService, behaviourManager);
        this.featureDataRepository = featureDataRepository;
    }

    protected final FeatureDataRepository getFeatureDataRepository() {
        return featureDataRepository;
    }
}

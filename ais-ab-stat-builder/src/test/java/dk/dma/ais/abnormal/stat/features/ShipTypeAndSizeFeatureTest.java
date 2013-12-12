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

import dk.dma.ais.abnormal.stat.AppStatisticsService;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData2Key;
import dk.dma.ais.abnormal.stat.tracker.Track;
import dk.dma.ais.abnormal.stat.tracker.TrackingService;
import dk.dma.ais.abnormal.stat.tracker.events.CellIdChangedEvent;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class ShipTypeAndSizeFeatureTest {
    final JUnit4Mockery context = new JUnit4Mockery();

    @Test
    public void testNewShipCountIsCreated() {
        // Mock dependencies
        final TrackingService trackingService = context.mock(TrackingService.class);
        final AppStatisticsService statisticsService = context.mock(AppStatisticsService.class);
        final FeatureDataRepository featureDataRepository = context.mock(FeatureDataRepository.class);

        // Setup test data
        final Track track = new Track(1386832929000L, 1234567);
        track.setProperty(Track.CELL_ID, 5674365784L);
        track.setProperty(Track.SHIP_TYPE, 40);
        track.setProperty(Track.VESSEL_LENGTH, 75);
        Long oldCellId = null;
        CellIdChangedEvent event = new CellIdChangedEvent(track, oldCellId);

        // Setup expectations
        final ShipTypeAndSizeFeature feature = new ShipTypeAndSizeFeature(statisticsService, trackingService, featureDataRepository);
        final ArgumentCaptor<FeatureData> featureData = ArgumentCaptor.forClass(FeatureData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(feature);
            ignoring(statisticsService).incFeatureStatistics(with(feature.FEATURE_NAME), with(any(String.class)));

            oneOf(featureDataRepository).getFeatureData(with(feature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            oneOf(featureDataRepository).putFeatureData(with(feature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(featureData.getMatcher()));
        }});

        // Execute
        feature.start();
        feature.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
        assertEquals(feature.FEATURE_NAME, featureData.getCapturedObject().getFeatureName());
        assertEquals(FeatureData2Key.class, featureData.getCapturedObject().getClass());
        FeatureData2Key capturedFeatureData = (FeatureData2Key) featureData.getCapturedObject();
        assertEquals(ShipTypeAndSizeFeature.STATISTICS_KEY_1, capturedFeatureData.getMeaningOfKey1()); // shipType
        assertEquals(ShipTypeAndSizeFeature.STATISTICS_KEY_2, capturedFeatureData.getMeaningOfKey2()); // shipSize
        assertEquals(TreeMap.class, featureData.getCapturedObject().getData().getClass());
        TreeMap<Short, TreeMap<Short, HashMap<String,Object>>> data = (TreeMap<Short, TreeMap<Short, HashMap<String,Object>>>) capturedFeatureData.getData();
        assertEquals(1, data.size()); // Assert one statistic recorded
        short shipType = data.firstKey();
        assertEquals(3, shipType);
        short shipSize = data.get(shipType).firstKey();
        assertEquals(3, shipSize);
        int numberOfStatsForShipTypeAndShipSize = data.get(shipType).get(shipSize).size();
        assertEquals(1, numberOfStatsForShipTypeAndShipSize);
        String statName = data.get(shipType).get(shipSize).keySet().iterator().next();
        assertEquals(ShipTypeAndSizeFeature.STATISTICS_NAME, statName);
        Object statValue = data.get(shipType).get(shipSize).get(statName);
        assertEquals(Integer.class, statValue.getClass());
        assertEquals(1, statValue);
    }

    @Test
    public void testExistingShipCountIsUpdated() {
        // Mock dependencies
        final TrackingService trackingService = context.mock(TrackingService.class);
        final AppStatisticsService statisticsService = context.mock(AppStatisticsService.class);
        final FeatureDataRepository featureDataRepository = context.mock(FeatureDataRepository.class);

        // Setup test data
        final Track track = new Track(1386832929000L, 1234567);
        track.setProperty(Track.CELL_ID, 5674365784L);
        track.setProperty(Track.SHIP_TYPE, 40);
        track.setProperty(Track.VESSEL_LENGTH, 75);
        Long oldCellId = null;
        CellIdChangedEvent event = new CellIdChangedEvent(track, oldCellId);

        // Setup expectations
        final ShipTypeAndSizeFeature feature = new ShipTypeAndSizeFeature(statisticsService, trackingService, featureDataRepository);

        final FeatureData existingFeatureData = new FeatureData2Key(ShipTypeAndSizeFeature.class, ShipTypeAndSizeFeature.STATISTICS_KEY_1, ShipTypeAndSizeFeature.STATISTICS_KEY_2);
        final TreeMap<Short, TreeMap<Short, HashMap<String,Object>>> existingData = (TreeMap<Short, TreeMap<Short, HashMap<String, Object>>>) existingFeatureData.getData();
        HashMap<String, Object> existingStatistics = new HashMap<String, Object>();
        existingStatistics.put(ShipTypeAndSizeFeature.STATISTICS_NAME, Integer.valueOf(1));
        TreeMap<Short, HashMap<String, Object>> key2Value = new TreeMap<Short, HashMap<String, Object>>();
        key2Value.put((short) 3, existingStatistics);
        existingData.put((short) 3, key2Value);

        final ArgumentCaptor<FeatureData> featureData2 = ArgumentCaptor.forClass(FeatureData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(feature);
            ignoring(statisticsService).incFeatureStatistics(with(feature.FEATURE_NAME), with(any(String.class)));

            oneOf(featureDataRepository).getFeatureData(with(feature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID))); will(returnValue(null));
            oneOf(featureDataRepository).putFeatureData(with(feature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(featureData2.getMatcher()));

            oneOf(featureDataRepository).getFeatureData(with(feature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID))); will(returnValue(existingFeatureData));
            oneOf(featureDataRepository).putFeatureData(with(feature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(featureData2.getMatcher()));
        }});

        // Execute
        feature.start();
        feature.onCellIdChanged(event);
        feature.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
        assertEquals(feature.FEATURE_NAME, featureData2.getCapturedObject().getFeatureName());
        assertEquals(FeatureData2Key.class, featureData2.getCapturedObject().getClass());
        FeatureData2Key capturedFeatureData = (FeatureData2Key) featureData2.getCapturedObject();
        assertEquals(ShipTypeAndSizeFeature.STATISTICS_KEY_1, capturedFeatureData.getMeaningOfKey1()); // shipType
        assertEquals(ShipTypeAndSizeFeature.STATISTICS_KEY_2, capturedFeatureData.getMeaningOfKey2()); // shipSize
        assertEquals(TreeMap.class, featureData2.getCapturedObject().getData().getClass());
        TreeMap<Short, TreeMap<Short, HashMap<String,Object>>> data = (TreeMap<Short, TreeMap<Short, HashMap<String,Object>>>) capturedFeatureData.getData();
        assertEquals(1, data.size()); // Assert one statistic recorded
        short shipType = data.firstKey();
        assertEquals(3, shipType);
        short shipSize = data.get(shipType).firstKey();
        assertEquals(3, shipSize);
        int numberOfStatsForShipTypeAndShipSize = data.get(shipType).get(shipSize).size();
        assertEquals(1, numberOfStatsForShipTypeAndShipSize);
        String statName = data.get(shipType).get(shipSize).keySet().iterator().next();
        assertEquals(ShipTypeAndSizeFeature.STATISTICS_NAME, statName);
        Object statValue = data.get(shipType).get(shipSize).get(statName);
        assertEquals(2, statValue);
    }

    @Test @Ignore
    public void testVesselPassingGreatBeltBridge() {
    }

}

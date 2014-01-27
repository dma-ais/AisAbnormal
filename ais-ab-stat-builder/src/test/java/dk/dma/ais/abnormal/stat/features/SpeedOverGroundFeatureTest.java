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
import dk.dma.ais.abnormal.stat.db.data.SpeedOverGroundFeatureData;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellIdChangedEvent;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class SpeedOverGroundFeatureTest {
    final JUnit4Mockery context = new JUnit4Mockery();

    TrackingService trackingService;
    AppStatisticsService statisticsService;
    FeatureDataRepository featureDataRepository;

    Track track;
    CellIdChangedEvent event;

    SpeedOverGroundFeature feature;

    @Before
    public void beforeTest() {
        // Mock dependencies
        trackingService = context.mock(TrackingService.class);
        statisticsService = context.mock(AppStatisticsService.class);
        featureDataRepository = context.mock(FeatureDataRepository.class);

        // Setup test data
        track = new Track(1234567);
        track.setProperty(Track.CELL_ID, 5674365784L);
        track.setProperty(Track.SHIP_TYPE, 40);     /* bucket 3 */
        track.setProperty(Track.VESSEL_LENGTH, 75); /* bucket 3 */
        track.setProperty(Track.SPEED_OVER_GROUND, Float.valueOf((float) 15.0)); /* bucket 5 */
        event = new CellIdChangedEvent(track, null);

        feature = new SpeedOverGroundFeature(statisticsService, trackingService, featureDataRepository);
    }

    @Test
    public void testNewShipCountIsCreated() {
        // Setup expectations
        final ArgumentCaptor<FeatureData> featureData = ArgumentCaptor.forClass(FeatureData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(feature);
            ignoring(statisticsService).incFeatureStatistics(with(SpeedOverGroundFeature.FEATURE_NAME), with(any(String.class)));

            oneOf(featureDataRepository).getFeatureData(with(SpeedOverGroundFeature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            oneOf(featureDataRepository).putFeatureData(with(SpeedOverGroundFeature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(featureData.getMatcher()));
        }});

        // Execute
        feature.start();
        feature.onCellIdChanged(event);

        // Main assertations
        SpeedOverGroundFeatureData capturedFeatureData = (SpeedOverGroundFeatureData) featureData.getCapturedObject();

        context.assertIsSatisfied();

        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> data = capturedFeatureData.getData();

        assertEquals(1, data.size()); // Assert one statistic recorded
        int shipTypeBucket = data.firstKey();
        assertEquals(3 - 1 /* -1 because idx counts from zero */, shipTypeBucket);
        int shipSizeBucket = data.get(shipTypeBucket).firstKey();
        assertEquals(3 - 1 /* -1 because idx counts from zero */, shipSizeBucket);
        int sogBucket = data.get(shipTypeBucket).get(shipSizeBucket).firstKey();
        assertEquals(5 - 1 /* -1 because idx counts from zero */, sogBucket);

        int numberOfStats = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).size();
        assertEquals(1, numberOfStats);
        String statName = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).keySet().iterator().next();
        Object statValue = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).get(statName);
        assertEquals(Integer.class, statValue.getClass());
        assertEquals(1, statValue);

        // Other assertations now we're here
        assertEquals(SpeedOverGroundFeatureData.class, featureData.getCapturedObject().getClass());
        assertEquals("shipType", capturedFeatureData.getMeaningOfKey1());
        assertEquals("shipSize", capturedFeatureData.getMeaningOfKey2());
        assertEquals(TreeMap.class, featureData.getCapturedObject().getData().getClass());
        assertEquals(SpeedOverGroundFeatureData.STAT_SHIP_COUNT, statName);
    }

    @Test
    public void testExistingShipCountIsUpdated() {
        final SpeedOverGroundFeatureData existingFeatureData = SpeedOverGroundFeatureData.create();
        existingFeatureData.setValue(3 - 1 /* -1 because idx counts from zero */, 3 - 1 /* -1 because idx counts from zero */, 5 - 1 /* -1 because idx counts from zero */, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);

        final ArgumentCaptor<FeatureData> featureData1 = ArgumentCaptor.forClass(FeatureData.class);
        final ArgumentCaptor<FeatureData> featureData2 = ArgumentCaptor.forClass(FeatureData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(feature);
            ignoring(statisticsService).incFeatureStatistics(with(SpeedOverGroundFeature.FEATURE_NAME), with(any(String.class)));

            oneOf(featureDataRepository).getFeatureData(with(SpeedOverGroundFeature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            will(returnValue(null));
            oneOf(featureDataRepository).putFeatureData(with(SpeedOverGroundFeature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(featureData1.getMatcher()));

            oneOf(featureDataRepository).getFeatureData(with(SpeedOverGroundFeature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            will(returnValue(existingFeatureData));
            oneOf(featureDataRepository).putFeatureData(with(SpeedOverGroundFeature.FEATURE_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(featureData2.getMatcher()));
        }});

        // Execute
        feature.start();
        feature.onCellIdChanged(event);
        feature.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
        // TODO assertEquals(SpeedOverGroundFeature.FEATURE_NAME, featureData2.getCapturedObject().getFeatureName());
        assertEquals(SpeedOverGroundFeatureData.class, featureData2.getCapturedObject().getClass());
        SpeedOverGroundFeatureData capturedFeatureData = (SpeedOverGroundFeatureData) featureData2.getCapturedObject();
        assertEquals("shipType", capturedFeatureData.getMeaningOfKey1());
        assertEquals("shipSize", capturedFeatureData.getMeaningOfKey2());
        assertEquals(TreeMap.class, featureData2.getCapturedObject().getData().getClass());
        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> data = capturedFeatureData.getData();
        assertEquals(1, data.size()); // Assert one statistic recorded
        int shipTypeBucket = data.firstKey();
        assertEquals(3 - 1 /* -1 because idx counts from zero */, shipTypeBucket);
        int shipSizeBucket = data.get(shipTypeBucket).firstKey();
        assertEquals(3 - 1 /* -1 because idx counts from zero */, shipSizeBucket);
        int sogBucket = data.get(shipTypeBucket).get(shipSizeBucket).firstKey();
        assertEquals(5 - 1 /* -1 because idx counts from zero */, sogBucket);
        int numberOfStatsForShipTypeAndShipSize = data.get(shipTypeBucket).get(shipSizeBucket).size();
        assertEquals(1, numberOfStatsForShipTypeAndShipSize);
        String statName = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).keySet().iterator().next();
        assertEquals(SpeedOverGroundFeatureData.STAT_SHIP_COUNT, statName);
        Object statValue = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).get(statName);
        assertEquals(2, statValue);
    }
}

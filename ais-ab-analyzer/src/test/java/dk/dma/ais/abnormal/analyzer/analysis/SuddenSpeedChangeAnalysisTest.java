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

import dk.dma.ais.abnormal.analyzer.AppStatisticsService;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.SuddenSpeedChangeEvent;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingReport;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.PositionChangedEvent;
import dk.dma.ais.abnormal.tracker.events.TrackStaleEvent;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import dk.dma.enav.model.geometry.Position;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SuddenSpeedChangeAnalysisTest {

    private JUnit4Mockery context;

    private long testCellId = 24930669189L;

    private TrackingService trackingService;
    private AppStatisticsService statisticsService;
    private EventRepository eventRepository;
    private Track track;

    @Before
    public void prepareTest() {
        context = new JUnit4Mockery();

        // Mock dependencies
        trackingService = context.mock(TrackingService.class);
        statisticsService = context.mock(AppStatisticsService.class);
        eventRepository = context.mock(EventRepository.class);

        // Create test data
        track = new Track(123456);

        // These are needed to create an event object in the database:
        track.updatePosition(TrackingReport.create(1370589743L, Position.create(56, 12), 45.0f, 10.1f, false));
    }

    @Test
    public void eventIsRaisedForSuddenSpeedChange() {
        // Create object under test
        final SuddenSpeedChangeAnalysis analysis = new SuddenSpeedChangeAnalysis(statisticsService, trackingService, eventRepository);

        // Perform test - none of the required data are there
        final ArgumentCaptor<SuddenSpeedChangeEvent> eventCaptor = ArgumentCaptor.forClass(SuddenSpeedChangeEvent.class);

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            exactly(2).of(eventRepository).findOngoingEventByVessel(123456, SuddenSpeedChangeEvent.class);
            oneOf(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 12.2f, false));

        analysis.onSpeedOverGroundUpdated(event);

        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, false));
        track.setProperty(Track.SHIP_TYPE, 11);
        analysis.onSpeedOverGroundUpdated(event);

        SuddenSpeedChangeEvent capturedEvent = eventCaptor.getCapturedObject();
        assertEquals("SuddenSpeedChangeEvent", capturedEvent.getEventType());
        assertEquals(123456, capturedEvent.getBehaviour(track.getMmsi()).getVessel().getMmsi());
        assertTrue(capturedEvent.getStartTime().before(capturedEvent.getEndTime()));
        assertEquals(2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().size());
        assertEquals(12.2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().first().getSpeedOverGround(), 1e-6);
        assertEquals(0.1, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().last().getSpeedOverGround(), 1e-6);
        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedForSlowSpeedChange() {
        // Create object under test
        final SuddenSpeedChangeAnalysis analysis = new SuddenSpeedChangeAnalysis(statisticsService, trackingService, eventRepository);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();

        int deltaSecs = 16;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 12.2f, false));
        analysis.onSpeedOverGroundUpdated(event);

        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 1) * 2000, Position.create(56, 12), 45.0f, 0.1f, false));
        analysis.onSpeedOverGroundUpdated(event);

        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedForFastSpeedChangeAboveEightKnots() {
        // Create object under test
        final SuddenSpeedChangeAnalysis analysis = new SuddenSpeedChangeAnalysis(statisticsService, trackingService, eventRepository);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 22.2f, false));
        analysis.onSpeedOverGroundUpdated(event);

        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 9.0f, false));
        analysis.onSpeedOverGroundUpdated(event);

        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedWhenTrackIsFirstSeen() {
        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp(), Position.create(56, 12), 45.0f, 12.2f, false));

        // Create object under test
        final SuddenSpeedChangeAnalysis analysis = new SuddenSpeedChangeAnalysis(statisticsService, trackingService, eventRepository);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();
        analysis.onSpeedOverGroundUpdated(event);
        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedWhenTrackHasBeenStale() {
        // Create object under test
        final SuddenSpeedChangeAnalysis analysis = new SuddenSpeedChangeAnalysis(statisticsService, trackingService, eventRepository);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 12.2f, false));
        analysis.onSpeedOverGroundUpdated(event);

        TrackStaleEvent staleEvent = new TrackStaleEvent(track);
        analysis.onTrackStale(staleEvent);

        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, false));
        analysis.onSpeedOverGroundUpdated(event);

        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedForWhenSpeedIsUndefined() {
        // Create object under test
        final SuddenSpeedChangeAnalysis analysis = new SuddenSpeedChangeAnalysis(statisticsService, trackingService, eventRepository);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 102.3f /* 1023 int */, false));
        analysis.onSpeedOverGroundUpdated(event);

        track.updatePosition(TrackingReport.create(track.getPositionReportTimestamp() + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, false));
        analysis.onSpeedOverGroundUpdated(event);

        context.assertIsSatisfied();
    }

}

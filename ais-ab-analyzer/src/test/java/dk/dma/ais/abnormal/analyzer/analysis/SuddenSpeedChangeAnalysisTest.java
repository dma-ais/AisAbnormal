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
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.abnormal.tracker.events.PositionChangedEvent;
import dk.dma.ais.abnormal.tracker.events.TrackStaleEvent;
import dk.dma.ais.packet.AisPacket;
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

    private Tracker trackingService;
    private AppStatisticsService statisticsService;
    private EventRepository eventRepository;
    private Track track;

    // GatehouseSourceTag [baseMmsi=2190067, country=DK, region=, timestamp=Thu Apr 10 15:30:29 CEST 2014]
    // [msgId=5, repeat=0, userId=219000606, callsign=OWNM@@@, dest=BOEJDEN-FYNSHAV@@@@@, dimBow=12, dimPort=8, dimStarboard=4, dimStern=58, draught=30, dte=0, eta=67584, imo=8222824, name=FRIGG SYDFYEN@@@@@@@, posType=1, shipType=61, spare=0, version=0]
    final AisPacket msg5 = AisPacket.from(
        "$PGHP,1,2014,4,10,13,30,29,165,219,,2190067,1,28*22\r\n" +
        "!BSVDM,2,1,1,A,53@ng7P1uN6PuLpl000I8TLN1=T@ITDp0000000u1Pr844@P07PSiBQ1,0*7B\r\n" +
        "!BSVDM,2,2,1,A,CcAVCTj0EP00000,2*53");

    @Before
    public void prepareTest() {
        context = new JUnit4Mockery();

        // Mock dependencies
        trackingService = context.mock(Tracker.class);
        statisticsService = context.mock(AppStatisticsService.class);
        eventRepository = context.mock(EventRepository.class);

        // Create test data
        track = new Track(219000606);
        track.update(msg5); // Init static part

        // These are needed to create an event object in the database:
        track.update(1370589743L, Position.create(56, 12), 45.0f, 10.1f);
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
            exactly(2).of(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            oneOf(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);

        track.update(track.getTimeOfLastPositionReport() + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 12.2f);
        analysis.onSpeedOverGroundUpdated(event);

        track.update(track.getTimeOfLastPositionReport() + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f);
        analysis.onSpeedOverGroundUpdated(event);

        SuddenSpeedChangeEvent capturedEvent = eventCaptor.getCapturedObject();
        assertEquals("SuddenSpeedChangeEvent", capturedEvent.getEventType());
        assertEquals(219000606, capturedEvent.getBehaviour(track.getMmsi()).getVessel().getMmsi());
        assertTrue(capturedEvent.getStartTime().before(capturedEvent.getEndTime()));
        assertEquals(2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().size());
        assertEquals(12.2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().first().getSpeedOverGround(), 1e-6);
        assertEquals(0.1, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().last().getSpeedOverGround(), 1e-6);
        context.assertIsSatisfied();
    }

    @Test
    public void eventIsRaisedForSuddenSpeedChangeSpanningSeveralMessages() {
        // Create object under test
        final SuddenSpeedChangeAnalysis analysis = new SuddenSpeedChangeAnalysis(statisticsService, trackingService, eventRepository);

        // Perform test - none of the required data are there
        final ArgumentCaptor<SuddenSpeedChangeEvent> eventCaptor = ArgumentCaptor.forClass(SuddenSpeedChangeEvent.class);

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.start();

        int deltaSecs = 10;

        /* Grounding of 314234000 on Jul 03 2009 - 20:44:18 - 20:45:18 */

        track.update(track.getTimeOfLastPositionReport() + deltaSecs * 1000, Position.create(56, 12), 45.0f, 13.9f);
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            never(trackingService).registerSubscriber(analysis);
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(track.getTimeOfLastPositionReport() + deltaSecs * 1000, Position.create(56, 12), 45.0f, 13.3f);
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            never(trackingService).registerSubscriber(analysis);
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(track.getTimeOfLastPositionReport() + deltaSecs * 1000, Position.create(56, 12), 45.0f, 11.7f);
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            never(trackingService).registerSubscriber(analysis);
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(track.getTimeOfLastPositionReport() + deltaSecs * 1000, Position.create(56, 12), 45.0f, 8.3f);
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            never(trackingService).registerSubscriber(analysis);
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(track.getTimeOfLastPositionReport() + deltaSecs * 1000, Position.create(56, 12), 45.0f, 5.0f);
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            never(trackingService).registerSubscriber(analysis);
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(track.getTimeOfLastPositionReport() + deltaSecs * 1000, Position.create(56, 12), 45.0f, 1.9f);
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            never(trackingService).registerSubscriber(analysis);
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(track.getTimeOfLastPositionReport() + deltaSecs * 1000, Position.create(56, 12), 45.0f,  0.0f);
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            never(trackingService).registerSubscriber(analysis);
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            oneOf(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        SuddenSpeedChangeEvent capturedEvent = eventCaptor.getCapturedObject();
        assertEquals("SuddenSpeedChangeEvent", capturedEvent.getEventType());
        assertEquals(219000606, capturedEvent.getBehaviour(track.getMmsi()).getVessel().getMmsi());
        assertTrue(capturedEvent.getStartTime().before(capturedEvent.getEndTime()));
        assertEquals(2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().size());
    }

    @Test
    public void noEventIsRaisedForSlowSpeedChange() {
        // Create object under test
        final SuddenSpeedChangeAnalysis analysis = new SuddenSpeedChangeAnalysis(statisticsService, trackingService, eventRepository);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(SuddenSpeedChangeAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            never(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            oneOf(trackingService).registerSubscriber(analysis);
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();

        int deltaSecs = 61;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.update(track.getTimeOfLastPositionReport() + deltaSecs * 1000, Position.create(56, 12), 45.0f, 12.2f);
        analysis.onSpeedOverGroundUpdated(event);

        track.update(track.getTimeOfLastPositionReport() + deltaSecs * 1000, Position.create(56, 12), 45.0f, 0.1f);
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
        track.update(track.getTimeOfLastPositionReport() + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 22.2f);
        analysis.onSpeedOverGroundUpdated(event);

        track.update(track.getTimeOfLastPositionReport() + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 9.0f);
        analysis.onSpeedOverGroundUpdated(event);

        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedWhenTrackIsFirstSeen() {
        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.update(track.getTimeOfLastPositionReport(), Position.create(56, 12), 45.0f, 12.2f);

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
        track.update(track.getTimeOfLastPositionReport() + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 12.2f);
        analysis.onSpeedOverGroundUpdated(event);

        TrackStaleEvent staleEvent = new TrackStaleEvent(track);
        analysis.onTrackStale(staleEvent);

        track.update(track.getTimeOfLastPositionReport() + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f);
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
        track.update(track.getTimeOfLastPositionReport() + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 102.3f /* 1023 int */);
        analysis.onSpeedOverGroundUpdated(event);

        track.update(track.getTimeOfLastPositionReport() + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f);
        analysis.onSpeedOverGroundUpdated(event);

        context.assertIsSatisfied();
    }

}

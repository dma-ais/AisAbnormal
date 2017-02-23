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

import com.google.inject.Guice;
import com.google.inject.Injector;
import dk.dma.ais.abnormal.analyzer.AbnormalAnalyzerAppTestModule;
import dk.dma.ais.abnormal.analyzer.AppStatisticsService;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.SuddenSpeedChangeEvent;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import dk.dma.ais.tracker.Tracker;
import dk.dma.ais.tracker.eventEmittingTracker.EventEmittingTracker;
import dk.dma.ais.tracker.eventEmittingTracker.Track;
import dk.dma.ais.tracker.eventEmittingTracker.events.PositionChangedEvent;
import dk.dma.ais.tracker.eventEmittingTracker.events.TrackStaleEvent;
import dk.dma.enav.model.geometry.Position;
import org.apache.commons.configuration.Configuration;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static dk.dma.commons.util.DateTimeUtil.MILLIS_TO_LOCALDATETIME_UTC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SuddenSpeedChangeAnalysisTest {

    private JUnit4Mockery context;
    private Injector injector;

    private SuddenSpeedChangeAnalysis analysis;
    private Configuration configuration;
    private AppStatisticsService statisticsService;
    private Tracker trackingService;
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

        injector = Guice.createInjector(
            new AbnormalAnalyzerAppTestModule(context)
        );

        analysis = injector.getInstance(SuddenSpeedChangeAnalysis.class);
        statisticsService = injector.getInstance(AppStatisticsService.class);
        trackingService = injector.getInstance(EventEmittingTracker.class);
        eventRepository = injector.getInstance(EventRepository.class);

        // Create test data
        track = new Track(219000606);
        track.update(msg5); // Init static part
    }

    @Test
    public void eventIsRaisedForSuddenSpeedChange() {
        // Perform test - none of the required data are there
        final ArgumentCaptor<SuddenSpeedChangeEvent> eventCaptor = ArgumentCaptor.forClass(SuddenSpeedChangeEvent.class);

        context.checking(new Expectations() {{
            exactly(2).of(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            oneOf(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 12.2f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        for (int i=0; i<8; i++) {
            track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);
            analysis.onSpeedOverGroundUpdated(event);
        }

        SuddenSpeedChangeEvent capturedEvent = eventCaptor.getCapturedObject();
        assertEquals("SuddenSpeedChangeEvent", capturedEvent.getEventType());
        assertEquals(219000606, capturedEvent.getBehaviour(track.getMmsi()).getVessel().getMmsi());
        assertTrue(capturedEvent.getStartTime().isBefore(capturedEvent.getEndTime()));
        assertEquals(2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().size());
        assertEquals(12.2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().first().getSpeedOverGround(), 1e-6);
        assertEquals(0.1, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().last().getSpeedOverGround(), 1e-6);
        context.assertIsSatisfied();
    }

    @Test
    public void speedMustStaySustainedBelowThresholdBeforeEventIsRaised() {
        // Perform test - none of the required data are there
        final ArgumentCaptor<SuddenSpeedChangeEvent> eventCaptor = ArgumentCaptor.forClass(SuddenSpeedChangeEvent.class);

        context.checking(new Expectations() {{
            exactly(2).of(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            oneOf(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 12.2f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);   // No event
        analysis.onSpeedOverGroundUpdated(event);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);   // No event
        analysis.onSpeedOverGroundUpdated(event);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);   // No event
        analysis.onSpeedOverGroundUpdated(event);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);   // No event
        analysis.onSpeedOverGroundUpdated(event);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);   // No event
        analysis.onSpeedOverGroundUpdated(event);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);   // No event
        analysis.onSpeedOverGroundUpdated(event);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);   // No event
        analysis.onSpeedOverGroundUpdated(event);

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);   // Event
        analysis.onSpeedOverGroundUpdated(event);

        SuddenSpeedChangeEvent capturedEvent = eventCaptor.getCapturedObject();
        assertEquals("SuddenSpeedChangeEvent", capturedEvent.getEventType());
        assertEquals(219000606, capturedEvent.getBehaviour(track.getMmsi()).getVessel().getMmsi());
        assertTrue(capturedEvent.getStartTime().isBefore(capturedEvent.getEndTime()));
        assertEquals(2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().size());
        assertEquals(12.2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().first().getSpeedOverGround(), 1e-6);
        assertEquals(0.1, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().last().getSpeedOverGround(), 1e-6);
        context.assertIsSatisfied();
    }

    @Test
    public void eventIsRaisedForSuddenSpeedChangeSpanningSeveralMessages() {
        // Perform test - none of the required data are there
        final ArgumentCaptor<SuddenSpeedChangeEvent> eventCaptor = ArgumentCaptor.forClass(SuddenSpeedChangeEvent.class);

        context.checking(new Expectations() {{
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.start();

        int deltaSecs = 10;

        /* Grounding of 314234000 on Jul 03 2009 - 20:44:18 - 20:45:18 */

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 13.9f, 45.0f);
        context.checking(new Expectations() {{
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 13.3f, 45.0f);
        context.checking(new Expectations() {{
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 11.7f, 45.0f);
        context.checking(new Expectations() {{
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 8.3f, 45.0f);
        context.checking(new Expectations() {{
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 5.0f, 45.0f);
        context.checking(new Expectations() {{
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 1.9f, 45.0f);
        context.checking(new Expectations() {{
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        for (int i=0; i<6; i++) { // Sustain low speed
            track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 0.0f, 45.0f);
            context.checking(new Expectations() {{
                ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
                never(eventRepository).save(with(eventCaptor.getMatcher()));
            }});
            analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
            context.assertIsSatisfied();
        }

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 0.2f, 45.0f);
        context.checking(new Expectations() {{
            ignoring(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            oneOf(eventRepository).save(with(eventCaptor.getMatcher()));
        }});
        analysis.onSpeedOverGroundUpdated(new PositionChangedEvent(track, null));
        context.assertIsSatisfied();

        SuddenSpeedChangeEvent capturedEvent = eventCaptor.getCapturedObject();
        assertEquals("SuddenSpeedChangeEvent", capturedEvent.getEventType());
        assertEquals(219000606, capturedEvent.getBehaviour(track.getMmsi()).getVessel().getMmsi());
        assertTrue(capturedEvent.getStartTime().isBefore(capturedEvent.getEndTime()));
        assertEquals(2, capturedEvent.getBehaviour(track.getMmsi()).getTrackingPoints().size());
    }

    @Test
    public void noEventIsRaisedForSlowSpeedChange() {
        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            never(eventRepository).findOngoingEventByVessel(219000606, SuddenSpeedChangeEvent.class);
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();

        int deltaSecs = 61;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 12.2f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + deltaSecs * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedForFastSpeedChangeAboveEightKnots() {
        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 22.2f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 9.0f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedWhenTrackIsFirstSeenHigh() {
        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.update(track.getTimeOfLastPositionReport(), Position.create(56, 12), 45.0f, 12.2f, 45.0f);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();
        analysis.onSpeedOverGroundUpdated(event);
        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedWhenTrackIsFirstSeenLow() {
        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.update(track.getTimeOfLastPositionReport(), Position.create(56, 12), 45.0f, 0.4f, 45.0f);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();
        analysis.onSpeedOverGroundUpdated(event);
        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedWhenTrackHasBeenStale() {
        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 12.2f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);
        TrackStaleEvent staleEvent = new TrackStaleEvent(track);
        analysis.onTrackStale(staleEvent);

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 24*60*60) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        context.assertIsSatisfied();
    }

    @Test
    public void noEventIsRaisedWhenSpeedIsUndefined() {
        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.start();

        int deltaSecs = 7;

        PositionChangedEvent event = new PositionChangedEvent(track, null);
        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 0) * 1000, Position.create(56, 12), 45.0f, 102.3f /* 1023 int */, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 1) * 1000, Position.create(56, 12), 45.0f, 0.1f, 45.0f);
        analysis.onSpeedOverGroundUpdated(event);

        for (int t=0; t < analysis.SPEED_SUSTAIN_SECS*2; t += 10) {
            track.update(Analysis.toEpochMillis(track.getTimeOfLastPositionReport()) + (deltaSecs + 2) * 1000 + t, Position.create(56, 12), 45.0f, 0.1f, 45.0f);
            analysis.onSpeedOverGroundUpdated(event);
        }

        context.assertIsSatisfied();
    }

    @Test
    public void canDetectFaxborgsGroundingInVejleFjordOnAug08_2014() {

        final String[] NMEA_TEST_STRINGS = {
            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,37,24,435,219,,2190076,1,6D*6D\r\n" +
            "!BSVDM,2,1,5,A,5:02Ih01WrRsEH57J20H5P8u8N222222222222167H66663k085QBS1H,0*55\r\n" +
            "!BSVDM,2,2,5,A,888888888888880,2*38",

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,42,6,278,219,,2190076,1,3B*53\r\n" +
            "!BSVDM,1,1,,A,1:02Ih001U0d=V:Op85<2aT>0<0F,0*3B",         // 10.1 knots

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,42,6,278,219,,2190067,1,3B*53\r\n" +
            "!BSVDM,1,1,,A,1:02Ih001U0d=V:Op85<2aT>0<0F,0*3B",         // 10.1 knots

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,42,15,825,219,,2190076,1,5D*63\r\n" +
            "!BSVDM,1,1,,B,1:02Ih001T0d=IjOp8bsvqTR089@,0*5D",         // 10.0 knots

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,42,25,279,219,,2190067,1,30*11\r\n" +
            "!BSVDM,1,1,,A,1:02Ih0PAM0d=?POp9=ct9Nl0<0F,0*30",         //  9.3 knots  t = 0

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,42,36,121,219,,2190076,1,0E*6B\r\n" +
            "!BSVDM,1,1,,B,1:02Ih000A0d==VOp9DcpIM80HE9,0*0E",         //  1.7 knots  t = 10.842 sec

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,42,45,372,219,,2190067,1,45*1F\r\n" +
            "!BSVDM,1,1,,A,1:02Ih00040d==bOp9E;oqMJ0D0E,0*45",         //  0.4 knots  t = 20.093 sec

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,42,56,432,219,,2190076,1,68*11\r\n" +
            "!BSVDM,1,1,,B,1:02Ih00010d==`Op9E;eqMh0D0D,0*68",         //  0.1 knots  t = 31.153 sec

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,43,6,276,219,,2190076,1,37*29\r\n" +
            "!BSVDM,1,1,,A,1:02Ih00010d==TOp9DcHaL<083b,0*37",         //  0.1 knots  t = 40.997 sec

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,43,15,823,219,,2190076,1,13*17\r\n" +
            "!BSVDM,1,1,,B,1:02Ih00010d==LOp9CrwILP05jd,0*13",         //  0.1 knots  t = 50.544 sec

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,43,24,465,219,,2190076,1,6D*6B\r\n" + // Static report t = 59.186 sec
            "!BSVDM,2,1,8,B,5:02Ih01WrRsEH57J20H5P8u8N222222222222167H66663k085QBS1H,0*5B\r\n" +
            "!BSVDM,2,2,8,B,888888888888880,2*36",

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,43,25,292,219,,2190067,1,0B*64\r\n" +
            "!BSVDM,1,1,,A,1:02Ih00000d==BOp9C:oaLj08>k,0*0B",         //  0.0 knots  t = 60.013 sec 

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,43,36,118,219,,2190076,1,2C*64\r\n" +
            "!BSVDM,1,1,,B,1:02Ih00000d==6Op9ArkqM80D0D,0*2C",         //  0.0 knots  t = 70.839 sec

            "\\si:AISD*3F\\\r\n" +
            "$PGHP,1,2014,8,7,14,43,45,369,219,,2190076,1,75*17\r\n" +
            "!BSVDM,1,1,,A,1:02Ih00000d=<tOp9@JWqMJ0@JT,0*75"          //  0.0 knots  t = 80.090 sec
        };

        // Set expectations
        context.checking(new Expectations() {{
            exactly(2).of(eventRepository).findOngoingEventByVessel(with(671128000), with(SuddenSpeedChangeEvent.class));
            exactly(1).of(eventRepository).save(with(any(SuddenSpeedChangeEvent.class)));
        }});

        // Run test
        analysis.start();

        for (int i = 0; i < NMEA_TEST_STRINGS.length; i++) {
            trackingService.update(AisPacket.from(NMEA_TEST_STRINGS[i]));
        }

        // Assert expectations met
        context.assertIsSatisfied();
    }

    @Test
    public void canDetectLotusSuddenSpeedChangeOnOct05_2014() {
        Tracker tracker = injector.getInstance(EventEmittingTracker.class);
        EventRepository eventRepository = injector.getInstance(EventRepository.class);

        InputStream testDataStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("ais/219165000_ssc_1.ais");
        AisReader aisReader = AisReaders.createReaderFromInputStream(testDataStream);

        final ArgumentCaptor<SuddenSpeedChangeEvent> eventCaptor = ArgumentCaptor.forClass(SuddenSpeedChangeEvent.class);
        context.checking(new Expectations() {{
            atLeast(1).of(eventRepository).findOngoingEventByVessel(219165000, SuddenSpeedChangeEvent.class);
            atLeast(1).of(eventRepository).save(with(eventCaptor.getMatcher()));
        }});

        analysis.start();
        aisReader.registerPacketHandler(aisPacket -> {
            tracker.update(aisPacket);
        });
        aisReader.run();

        context.assertIsSatisfied();
        assertTrue(eventCaptor.getCapturedObject().getDescription().startsWith("Sudden speed change of LOTUS"));
        assertEquals(MILLIS_TO_LOCALDATETIME_UTC.apply(1412525411424L), eventCaptor.getCapturedObject().getStartTime());
    }
}

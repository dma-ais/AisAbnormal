package dk.dma.ais.abnormal.tracker;

import dk.dma.ais.packet.AisPacket;
import dk.dma.enav.model.geometry.Position;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TrackTest {

    Track track;

    // GatehouseSourceTag [baseMmsi=2190067, country=DK, region=, timestamp=Thu Apr 10 15:30:29 CEST 2014]
    // [msgId=5, repeat=0, userId=219000606, callsign=OWNM@@@, dest=BOEJDEN-FYNSHAV@@@@@, dimBow=12, dimPort=8, dimStarboard=4, dimStern=58, draught=30, dte=0, eta=67584, imo=8222824, name=FRIGG SYDFYEN@@@@@@@, posType=1, shipType=61, spare=0, version=0]
    AisPacket msg5 = AisPacket.from(
        "$PGHP,1,2014,4,10,13,30,29,165,219,,2190067,1,28*22\r\n" +
        "!BSVDM,2,1,1,A,53@ng7P1uN6PuLpl000I8TLN1=T@ITDp0000000u1Pr844@P07PSiBQ1,0*7B\r\n" +
        "!BSVDM,2,2,1,A,CcAVCTj0EP00000,2*53");

    @Before
    public void setUp() throws Exception {
        track = new Track(2345);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetMmsi() throws Exception {
        assertEquals(2345, track.getMmsi());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotUpdateTrackWithWrongMMSI() throws Exception {
        Track track = new Track(2191000);
        track.update(msg5);
    }

    @Test
    public void canGetStaticInformationFromTrack() throws Exception {
        Track track = new Track(219000606);

        assertNull(track.getCallsign());
        assertNull(track.getShipName());
        assertNull(track.getIMO());
        assertNull(track.getShipDimensionBow());
        assertNull(track.getShipDimensionStern());
        assertNull(track.getShipDimensionPort());
        assertNull(track.getShipDimensionStarboard());
        assertNull(track.getVesselLength());
        assertNull(track.getVesselBeam());
        assertEquals(0, track.getTimeOfLastUpdate());

        track.update(msg5);

        assertEquals("OWNM@@@", track.getCallsign());
        assertEquals("FRIGG SYDFYEN@@@@@@@", track.getShipName());
        assertEquals(8222824, track.getIMO().intValue());
        assertEquals(12, track.getShipDimensionBow().intValue());
        assertEquals(58, track.getShipDimensionStern().intValue());
        assertEquals(8, track.getShipDimensionPort().intValue());
        assertEquals(4, track.getShipDimensionStarboard().intValue());
        assertEquals(12 + 58, track.getVesselLength().intValue());
        assertEquals(8 + 4, track.getVesselBeam().intValue());
        assertEquals(1397136629165L, track.getTimeOfLastUpdate());
    }

    @Test
    public void testGetNewestTrackingReport() throws Exception {
        assertNull(track.getNewestTrackingReport());

        track.update(1000000000, Position.create(56, 12), 45.0f, 10.1f);
        assertNotNull(track.getNewestTrackingReport());
        assertEquals(1000000000, track.getNewestTrackingReport().getTimestamp());

        track.update(1000010000, Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertNotNull(track.getNewestTrackingReport());
        assertEquals(1000010000, track.getNewestTrackingReport().getTimestamp());

        track.update(1000020000, Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertNotNull(track.getNewestTrackingReport());
        assertEquals(1000020000, track.getNewestTrackingReport().getTimestamp());

        track.update(1000, Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertNotNull(track.getNewestTrackingReport());
        assertEquals(1000020000, track.getNewestTrackingReport().getTimestamp());
    }

    @Test
    public void testGetTimeOfLastPositionReport() throws Exception {
        assertEquals(0, track.getTimeOfLastPositionReport());

        track.update(1000000000, Position.create(56, 12), 45.0f, 10.1f);
        assertEquals(1000000000L, track.getTimeOfLastPositionReport());

        track.update(1000010000, Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(1000010000L, track.getTimeOfLastPositionReport());

        track.update(1000020000, Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(1000020000L, track.getTimeOfLastPositionReport());

        track.update(1000, Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(1000020000L, track.getTimeOfLastPositionReport());
    }

    @Test
    public void testPurgeOldPositionReports() {
        assertEquals(0, track.getTrackingReports().size());
        assertEquals(10, track.MAX_AGE_POSITION_REPORTS_MINUTES);

        track.update(new GregorianCalendar(2014, 02, 11, 12, 32, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(1, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 34, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(2, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 36, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(3, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 38, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(4, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 38, 59).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(5, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 40, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(6, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 42, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(7, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 44, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(7, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 46, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(7, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 48, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(7, track.getTrackingReports().size());
        track.update(new GregorianCalendar(2014, 02, 11, 12, 50, 00).getTimeInMillis(), Position.create(56.01, 12.01), 45.0f, 10.1f);
        assertEquals(6, track.getTrackingReports().size());

        List<TrackingReport> trackingReports = track.getTrackingReports();
        assertEquals(6, trackingReports.size());
        long oldestKept = new GregorianCalendar(2014, 02, 11, 12, 50-track.MAX_AGE_POSITION_REPORTS_MINUTES, 00).getTimeInMillis();
        trackingReports.forEach(p -> assertTrue(p.getTimestamp() >=  oldestKept));
    }

    @Test
    public void testPredictEast() {
        track.update(new GregorianCalendar(2014, 02, 11, 12, 32, 00).getTimeInMillis(), Position.create(56.00, 12.00), 90.0f, 1.0f);
        track.predict(new GregorianCalendar(2014, 02, 11, 12, 33, 00).getTimeInMillis());
        assertEquals(56.000000, track.getNewestTrackingReport().getPosition().getLatitude(), 1e-6);
        assertEquals(12.000496, track.getNewestTrackingReport().getPosition().getLongitude(), 1e-6);
        assertEquals(new GregorianCalendar(2014, 02, 11, 12, 33, 00).getTimeInMillis(), track.getTimeOfLastPositionReport(), 1e-10);
    }

    @Test
    public void testPredictNorth() {
        track.update(new GregorianCalendar(2014, 02, 11, 12, 32, 00).getTimeInMillis(), Position.create(56.00, 12.00), 0.0f, 1.0f);
        track.predict(new GregorianCalendar(2014, 02, 11, 12, 33, 00).getTimeInMillis());
        assertEquals(56.000277, track.getNewestTrackingReport().getPosition().getLatitude(), 1e-6);
        assertEquals(12.000000, track.getNewestTrackingReport().getPosition().getLongitude(), 1e-6);
        assertEquals(new GregorianCalendar(2014, 02, 11, 12, 33, 00).getTimeInMillis(), track.getTimeOfLastPositionReport(), 1e-10);
    }

    @Test
    public void testPredictLongAndFastNE() {
        track.update(new GregorianCalendar(2014, 02, 11, 12, 32, 00).getTimeInMillis(), Position.create(56.00, 12.00), 45.0f, 20.0f);
        track.predict(new GregorianCalendar(2014, 02, 11, 12, 42, 00).getTimeInMillis());
        assertEquals(56.039237, track.getNewestTrackingReport().getPosition().getLatitude(), 1e-6);
        assertEquals(12.070274, track.getNewestTrackingReport().getPosition().getLongitude(), 1e-6);
        assertEquals(new GregorianCalendar(2014, 02, 11, 12, 42, 00).getTimeInMillis(), track.getTimeOfLastPositionReport(), 1e-10);
    }

    @Test
    public void testPredictLongAndFastSW() {
        track.update(new GregorianCalendar(2014, 02, 11, 12, 32, 00).getTimeInMillis(), Position.create(56.00, 12.00), 180.0f+45.0f, 20.0f);
        track.predict(new GregorianCalendar(2014, 02, 11, 12, 42, 00).getTimeInMillis());
        assertEquals(55.960722, track.getNewestTrackingReport().getPosition().getLatitude(), 1e-6);
        assertEquals(11.929867, track.getNewestTrackingReport().getPosition().getLongitude(), 1e-6);
        assertEquals(new GregorianCalendar(2014, 02, 11, 12, 42, 00).getTimeInMillis(), track.getTimeOfLastPositionReport(), 1e-10);
    }
}

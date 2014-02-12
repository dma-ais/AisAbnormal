package dk.dma.ais.abnormal.tracker;

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

/**
 * Created by tbsalling on 11/02/14.
 */
public class TrackTest {

    Track track;

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

    @Test
    public void testGetPositionReport() throws Exception {
        assertNull(track.getPositionReport());

        track.updatePosition(PositionReport.create(1000000000, Position.create(56, 12), false));
        assertNotNull(track.getPositionReport());
        assertEquals(1000000000, track.getPositionReport().getTimestamp());

        track.updatePosition(PositionReport.create(1000010000, Position.create(56.01, 12.01), false));
        assertNotNull(track.getPositionReport());
        assertEquals(1000010000, track.getPositionReport().getTimestamp());

        track.updatePosition(PositionReport.create(1000020000, Position.create(56.01, 12.01), false));
        assertNotNull(track.getPositionReport());
        assertEquals(1000020000, track.getPositionReport().getTimestamp());

        track.updatePosition(PositionReport.create(1000, Position.create(56.01, 12.01), false));
        assertNotNull(track.getPositionReport());
        assertEquals(1000020000, track.getPositionReport().getTimestamp());
    }

    @Test
    public void testGetPositionReportTimestamp() throws Exception {
        assertNull(track.getPositionReportTimestamp());

        track.updatePosition(PositionReport.create(1000000000, Position.create(56, 12), false));
        assertEquals(Long.valueOf(1000000000), track.getPositionReportTimestamp());

        track.updatePosition(PositionReport.create(1000010000, Position.create(56.01, 12.01), false));
        assertEquals(Long.valueOf(1000010000), track.getPositionReportTimestamp());

        track.updatePosition(PositionReport.create(1000020000, Position.create(56.01, 12.01), false));
        assertEquals(Long.valueOf(1000020000), track.getPositionReportTimestamp());

        track.updatePosition(PositionReport.create(1000, Position.create(56.01, 12.01), false));
        assertEquals(Long.valueOf(1000020000), track.getPositionReportTimestamp());
    }

    @Test
    public void testPurgeOldPositionReports() {
        assertEquals(0, track.positionReports.size());
        assertEquals(10, track.MAX_AGE_POSITION_REPORTS_MINUTES);

        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 32, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(1, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 34, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(2, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 36, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(3, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 38, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(4, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 38, 59).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(5, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 40, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(6, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 42, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(7, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 44, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(7, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 46, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(7, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 48, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(7, track.positionReports.size());
        track.updatePosition(PositionReport.create(new GregorianCalendar(2014, 02, 11, 12, 50, 00).getTimeInMillis(), Position.create(56.01, 12.01), false));
        assertEquals(6, track.positionReports.size());

        List<PositionReport> positionReports = track.getPositionReports();
        assertEquals(6, positionReports.size());
        long oldestKept = new GregorianCalendar(2014, 02, 11, 12, 50-track.MAX_AGE_POSITION_REPORTS_MINUTES, 00).getTimeInMillis();
        positionReports.forEach(p -> assertTrue(p.getTimestamp() >=  oldestKept));
    }
}

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
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.enav.model.geometry.Position;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DriftAnalysisTest {

    private DriftAnalysis driftAnalysis;

    public JUnit4Mockery context;

    @Before
    public void initDriftAnalysis() {
        context = new JUnit4Mockery();
        Injector injector = Guice.createInjector(
                new AbnormalAnalyzerAppTestModule(context)
        );
        driftAnalysis = injector.getInstance(DriftAnalysis.class);
    }

    @Test
    public void ensureValidConfiguration() {
        assertTrue(Track.MAX_AGE_POSITION_REPORTS_MINUTES > driftAnalysis.OBSERVATION_PERIOD_MINUTES);
    }

    @Test
    public void testIsSignificantDeviation() {
        testIsCourseHeadingDeviationIndicatingDrift(0f);
    }

    @Test
    public void testIsSignificantDeviationDriftingBackwards() {
        testIsCourseHeadingDeviationIndicatingDrift(180f);
    }

    private void testIsCourseHeadingDeviationIndicatingDrift(final float cogOffset) {
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 0.0f, 0.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES - 1.0f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 0.0f, 0.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES));
        assertTrue(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 0.0f, 0.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES + 1.0f));

        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 90.0f, 90.0f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 90.0f, 90.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES - 1.0f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 90.0f, 90.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES));
        assertTrue(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 90.0f, 90.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES + 1.0f));

        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 180.0f, 180.0f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 180.0f, 180.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES - 1.0f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 180.0f, 180.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES));
        assertTrue(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 180.0f, 180.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES + 1.0f));

        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 270.0f, 270.0f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 270.0f, 270.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES - 1.0f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 270.0f, 270.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES));
        assertTrue(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 270.0f, 270.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES + 1.0f));

        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 0.0f, 0.0f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 0.0f, 359.9f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 0.1f, 359.9f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 0.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES / 2.0f, 0.0f - driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES / 2.0f));
        assertFalse(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 0.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES / 2.0f - 1, 0.0f - driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES / 2.0f));
        assertTrue(driftAnalysis.isCourseHeadingDeviationIndicatingDrift(cogOffset + 0.0f + driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES / 2.0f, 0.0f - driftAnalysis.MIN_HDG_COG_DEVIATION_DEGREES / 2.0f - 1));
    }

    @Test
    public void testIsDriftPeriodLongEnough() throws Exception {
        final long t0 = System.currentTimeMillis();

        final long t1 = t0 + driftAnalysis.OBSERVATION_PERIOD_MINUTES*60*1000;
        final long dt = 7000;

        // nonDriftingTrack (speed normal, cog/hdg deviation normal)
        System.out.println("Testing normally moving, non-drifting track");
        Track track = new Track(219000001);
        for (long t = t0; t < t1+60000; t += dt) {
            track.update(t, Position.create(56.0, 12.0), 45.0f, 12.0f, 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        assertFalse(driftAnalysis.isDriftPeriodLongEnough(track));

        // slowButNonDriftingTrack (speed indicates drift, cog/hdg deviation does not)
        System.out.println("Testing track moving in observed speed interval, but not drifting");
        track = new Track(219000001);
        for (long t = t0; t < t1+60000; t += dt) {
            track.update(t, Position.create(56.0, 12.0), 45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        assertFalse(driftAnalysis.isDriftPeriodLongEnough(track));

        // slowButNonDriftingTrack (speed does not indicate drift, cog/hdg deviation does)
        System.out.println("Testing track moving in normal speed, but cog/hdg deviation indicates drift");
        track = new Track(219000001);
        for (long t = t0; t < t1+60000; t += dt) {
            track.update(t, Position.create(56.0, 12.0), -45.0f, (float) (driftAnalysis.SPEED_HIGH_MARK + 0.5), 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        assertFalse(driftAnalysis.isDriftPeriodLongEnough(track));

        // verySlowButNonDriftingTrack (neither speed nor cog/hdg deviation indicate drift)
        System.out.println("Testing track moving below observed speed interval and not drifting");
        track = new Track(219000001);
        for (long t = t0; t < t1+60000; t += dt) {
            track.update(t, Position.create(56.0, 12.0), 45.0f, (float) (driftAnalysis.SPEED_LOW_MARK - 0.5), 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        assertFalse(driftAnalysis.isDriftPeriodLongEnough(track));

        // driftingTrackButNotForLongEnough
        System.out.println("Testing track drifting, but not drifting for long enough");
        track = new Track(219000001);
        for (long t = t0; t < t1-60000; t += dt) {    // Non-drifting
            track.update(t, Position.create(56.0, 12.0), 45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        for (long t = t1-60000+1000; t < t1+60000; t += dt) { // Drifting
            track.update(t, Position.create(56.0, 12.0), -45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        assertFalse(driftAnalysis.isDriftPeriodLongEnough(track));

        // driftingTrackGettingOutOfDrift
        System.out.println("Testing track drifting, but eventually getting out of drift");
        track = new Track(219000001);
        for (long t = t0; t < t1-60000; t += dt) {    // Drifting
            track.update(t, Position.create(56.0, 12.0), -45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        for (long t = t1-60000+1000; t < t1+60000; t += dt) { // Non-drifting
            track.update(t, Position.create(56.0, 12.0), 45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        assertFalse(driftAnalysis.isDriftPeriodLongEnough(track));

        //
        System.out.println("Testing drifting track 1");
        track = new Track(219000001);
        for (long t = t0; t < t1+60000; t += dt) {
            track.update(t, Position.create(56.0, 12.0), -45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        assertTrue(driftAnalysis.isDriftPeriodLongEnough(track));

        System.out.println("Testing drifting track 2");
        track = new Track(219000001);
        for (long t = t0; t < t1+60000; t += dt) {
            track.update(t, Position.create(56.0, 12.0), 353.3f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            //System.out.println("t:" + t + " pos:" + track.getPosition() + " sog:" + track.getSpeedOverGround() + " cog:" + track.getCourseOverGround() + " hdg:" + track.getTrueHeading());
        }
        assertTrue(driftAnalysis.isDriftPeriodLongEnough(track));
    }

    @Test
    public void testIsDriftDistanceLongEnough() {
        final long t0 = System.currentTimeMillis();
        final long dt = 7000;

        // nonDriftingTrack (speed normal, cog/hdg deviation normal)
        Track track = new Track(219000001);
        track.update(t0, Position.create(56.0, 12.0), 45.0f, 12.0f, 45.1f);
        assertFalse(driftAnalysis.isDriftDistanceLongEnough(track));
        track.update(t0+dt, Position.create(56.0001, 12.0001), 45.0f, 12.0f, 45.1f);
        assertFalse(driftAnalysis.isDriftDistanceLongEnough(track));

        // driftingTrack
        track = new Track(219000001);
        track.update(t0, Position.create(56.0000, 11.0000), -45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
        assertFalse(driftAnalysis.isDriftDistanceLongEnough(track));
        track.update(t0+dt, Position.create(56.0001, 11.0001), -45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f); // 12 m - http://www.csgnetwork.com/gpsdistcalc.html
        assertFalse(driftAnalysis.isDriftDistanceLongEnough(track));
        track.update(t0+2*dt, Position.create(56.0010, 11.0010), -45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f); // 155 m
        assertFalse(driftAnalysis.isDriftDistanceLongEnough(track));
        track.update(t0+3*dt, Position.create(56.0100, 11.0100), -45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f); // 1273 m
        assertTrue(driftAnalysis.isDriftDistanceLongEnough(track));
    }

    @Test
    public void testIsSustainedDrift() {
        final long t0 = System.currentTimeMillis();
        final long dt = 7000;

        // nonDriftingTrack (speed normal, cog/hdg deviation normal)
        Track track = new Track(219000001);
        for (int i=0; i < 100; i++) {
            track.update(t0+i*dt, Position.create(56.0 + 0.001*i, 12.0 + 0.001*i), 45.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            assertFalse(driftAnalysis.isSustainedDrift(track));
        }

        // drifting track, but not for long enough time
        track = new Track(219000001);
        int i=0;
        while (i*dt < driftAnalysis.OBSERVATION_PERIOD_MINUTES*60*1000) {
            track.update(t0 + i*dt, Position.create(56.0 + i*0.0001, 12.0 + i*0.0001), -54.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            assertFalse(driftAnalysis.isSustainedDrift(track));
            i++;
        }

        // drifting track, but not for long enough distance
        track = new Track(219000001);
        i=0;
        while (i*dt < driftAnalysis.OBSERVATION_PERIOD_MINUTES*60*1000) {
            track.update(t0 + i*dt, Position.create(56.0 + i*0.00001, 12.0 + i*0.00001), -54.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            assertFalse(driftAnalysis.isSustainedDrift(track));
            i++;
        }
        track.update(t0 + i*dt, Position.create(56.0 + i*0.00001, 12.0 + i*0.00001), -54.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
        System.out.println("Track drifted " + track.getNewestTrackingReport().getPosition().rhumbLineDistanceTo(track.getOldestTrackingReport().getPosition()) + " meters in " + i*dt/1000/60 + " minutes");
        assertFalse(driftAnalysis.isSustainedDrift(track));

        // drifting track
        track = new Track(219000001);
        i=0;
        while (i*dt < driftAnalysis.OBSERVATION_PERIOD_MINUTES*60*1000) {
            track.update(t0 + i*dt, Position.create(56.0 + i*0.0001, 12.0 + i*0.0001), -54.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
            assertFalse(driftAnalysis.isSustainedDrift(track));
            i++;
        }
        track.update(t0 + i*dt, Position.create(56.0 + i*0.0001, 12.0 + i*0.0001), -54.0f, (float) (driftAnalysis.SPEED_LOW_MARK + 0.5), 45.1f);
        System.out.println("Track drifted " + track.getNewestTrackingReport().getPosition().rhumbLineDistanceTo(track.getOldestTrackingReport().getPosition()) + " meters in " + i*dt/1000/60 + " minutes");
        assertTrue(driftAnalysis.isSustainedDrift(track));
    }
}
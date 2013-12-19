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

package dk.dma.ais.abnormal.event.db.h2;

import dk.dma.ais.abnormal.event.db.domain.AbnormalShipSizeOrTypeEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.SuddenSpeedChangeEvent;
import dk.dma.ais.abnormal.event.db.domain.Vessel;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class H2EventRepositoryTest {

    private H2EventRepository h2EventRepository;

    @Before
    public void init() {
        String workingDir = System.getProperty("user.dir");
        File dbFile = new File(workingDir + "/src/test/db/repository");
        System.out.println(dbFile.getAbsolutePath());
        h2EventRepository = new H2EventRepository(dbFile, true);
    }

    @Test
    public void testFindOngoingEventsByVessel() {
        Vessel vessel = new Vessel();
        vessel.getId().setCallsign("OVUA2");
        vessel.getId().setName("VT ELECTRON");
        vessel.getId().setImo(8207379);
        vessel.getId().setMmsi(219886000);

        List<Event> events = h2EventRepository.findOngoingEventsByVessel(vessel);

        assertEquals(17, events.size());
    }

    @Test
    public void testFindOngoingEventByVessel() {
        Vessel vessel = new Vessel();
        vessel.getId().setCallsign("OVUA2");
        vessel.getId().setName("VT ELECTRON");
        vessel.getId().setImo(8207379);
        vessel.getId().setMmsi(219886000);

        AbnormalShipSizeOrTypeEvent event1 = h2EventRepository.findOngoingEventByVessel(vessel, AbnormalShipSizeOrTypeEvent.class);
        assertNotNull(event1);

        SuddenSpeedChangeEvent event2 = h2EventRepository.findOngoingEventByVessel(vessel, SuddenSpeedChangeEvent.class);
        assertNull(event2);
    }

}

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
import dk.dma.ais.abnormal.event.db.domain.SuddenSpeedChangeEvent;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

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


    @Test @Ignore
    public void testFindOngoingEventByVessel() {
        AbnormalShipSizeOrTypeEvent event1 = h2EventRepository.findOngoingEventByVessel(219886000, AbnormalShipSizeOrTypeEvent.class);
        assertNotNull(event1);

        SuddenSpeedChangeEvent event2 = h2EventRepository.findOngoingEventByVessel(219886000, SuddenSpeedChangeEvent.class);
        assertNull(event2);
    }

}

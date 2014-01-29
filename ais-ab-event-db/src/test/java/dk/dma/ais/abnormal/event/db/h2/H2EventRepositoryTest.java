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

import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.ShipSizeOrTypeEvent;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

public class H2EventRepositoryTest {

    private JUnit4Mockery context;
    private SessionFactory sessionFactory;
    private Session session;

    private H2EventRepository h2EventRepository;

    @Before
    public void init() {
        context = new JUnit4Mockery();

        sessionFactory = context.mock(SessionFactory.class);
        session = context.mock(Session.class);
    }

    @Test
    public void callsSessionSetDefaultReadOnlyWithTrue() {
        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession(); will(returnValue(session));
            oneOf(session).setDefaultReadOnly(true);
            oneOf(session).get(Event.class, 1L); will(returnValue(null));
            oneOf(session).close();
        }});

        h2EventRepository = new H2EventRepository(sessionFactory, true);
        h2EventRepository.getEvent(1);
    }

    @Test
    public void callsSessionSetDefaultReadOnlyWithFalse() {
        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession(); will(returnValue(session));
            oneOf(session).setDefaultReadOnly(false);
            oneOf(session).get(Event.class, 1L); will(returnValue(null));
            oneOf(session).close();
        }});

        h2EventRepository = new H2EventRepository(sessionFactory, false);
        h2EventRepository.getEvent(1);
    }

    @Test
    public void testFindOngoingEventByVessel() {
        ShipSizeOrTypeEvent event = new ShipSizeOrTypeEvent();

        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession(); will(returnValue(session));
            oneOf(session).getTransaction();
            oneOf(session).beginTransaction();
            oneOf(session).createQuery(with(any(String.class)));
            oneOf(session).close();
        }});

        h2EventRepository = new H2EventRepository(sessionFactory, false);
        h2EventRepository.findOngoingEventByVessel(219886000, ShipSizeOrTypeEvent.class);
    }

}

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

package dk.dma.ais.abnormal.event.db.jpa;

import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.ShipSizeOrTypeEvent;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertTrue;

public class JpaEventRepositoryTest {

    private JUnit4Mockery context;
    private SessionFactory sessionFactory;
    private Session session;
    private Query query;
    private JpaEventRepository eventRepository;

    @Before
    public void init() {
        context = new JUnit4Mockery();

        sessionFactory = context.mock(SessionFactory.class);
        session = context.mock(Session.class);
        query = context.mock(Query.class);

        eventRepository = new JpaEventRepository(sessionFactory, false);
    }

    @After
    public void after() {
        context.assertIsSatisfied();
    }

    @Test
    public void callsSessionSetDefaultReadOnlyWithTrue() {
        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession(); will(returnValue(session));
            oneOf(session).setDefaultReadOnly(true);
            oneOf(session).get(Event.class, 1L); will(returnValue(null));
            oneOf(session).close();
        }});

        eventRepository = new JpaEventRepository(sessionFactory, true);
        eventRepository.getEvent(1);

        context.assertIsSatisfied();
    }

    @Test
    public void callsSessionSetDefaultReadOnlyWithFalse() {
        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession(); will(returnValue(session));
            never(session).setDefaultReadOnly(true);
            oneOf(session).get(Event.class, 1L); will(returnValue(null));
            oneOf(session).close();
        }});

        eventRepository = new JpaEventRepository(sessionFactory, false);
        eventRepository.getEvent(1);

        context.assertIsSatisfied();
    }

    @Test
    public void testFindOngoingEventByVessel() {
        ShipSizeOrTypeEvent event = new ShipSizeOrTypeEvent();

        final ArgumentCaptor<String> queryString = ArgumentCaptor.forClass(String.class);

        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession(); will(returnValue(session));
            oneOf(session).createQuery(with(queryString.getMatcher())); will(returnValue(query));
            allowing(query).setCacheable(with(any(Boolean.class)));
            allowing(query).setParameter(with(aNonNull(String.class)), with(aNonNull(Object.class)));
            allowing(query).setString(with(aNonNull(String.class)), with(aNonNull(String.class)));
            allowing(query).setInteger(with(aNonNull(String.class)), with(aNonNull(Integer.class)));
            oneOf(query).list();
            oneOf(session).close();
        }});

        eventRepository.findOngoingEventByVessel(219886000, ShipSizeOrTypeEvent.class);

        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*TYPE[(]e[)] = [:]clazz.*"));
        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*vessel.mmsi.*=.*"));

        context.assertIsSatisfied();
    }

    @Test
    public void testFindEventsByFromAndTo() {
        final ArgumentCaptor<String> queryString = ArgumentCaptor.forClass(String.class);

        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession();
            will(returnValue(session));
            oneOf(session).createQuery(with(queryString.getMatcher())); will(returnValue(query));
            allowing(query).setCacheable(with(any(Boolean.class)));
            allowing(query).setParameter(with(aNonNull(String.class)), with(aNonNull(Object.class)));
            allowing(query).setString(with(aNonNull(String.class)), with(aNonNull(String.class)));
            allowing(query).setInteger(with(aNonNull(String.class)), with(aNonNull(Integer.class)));
            oneOf(query).list();
            oneOf(session).close();
        }});

        Calendar calendar = Calendar.getInstance();

        calendar.set(2014, 03, 27, 0, 0, 0);
        Date from = calendar.getTime();

        calendar.set(2014, 03, 27, 14, 12, 10);
        Date to = calendar.getTime();

        eventRepository.findEventsByFromAndTo(from, to);

        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*(e.startTime >= :from AND e.startTime <= :to).*"));
        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*(e.endTime >= :from AND e.endTime <= :to).*"));

        context.assertIsSatisfied();
    }

    @Test
    public void testFindEventsByFromAndToAndTypeAndVesselAndArea() {
        final ArgumentCaptor<String> queryString = ArgumentCaptor.forClass(String.class);

        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession();
            will(returnValue(session));
            oneOf(session).createQuery(with(queryString.getMatcher())); will(returnValue(query));
            allowing(query).setCacheable(with(any(Boolean.class)));
            allowing(query).setParameter(with(aNonNull(String.class)), with(aNonNull(Object.class)));
            allowing(query).setString(with(aNonNull(String.class)), with(aNonNull(String.class)));
            allowing(query).setInteger(with(aNonNull(String.class)), with(aNonNull(Integer.class)));
            oneOf(query).list();
            oneOf(session).close();
        }});

        Calendar calendar = Calendar.getInstance();

        calendar.set(2014, 03, 27, 0, 0, 0);
        Date from = calendar.getTime();

        calendar.set(2014, 03, 27, 14, 12, 10);
        Date to = calendar.getTime();

        eventRepository.findEventsByFromAndToAndTypeAndVesselAndArea(from, to, "SpeedOverGroundEvent", "vessel", 56.1, 12.1, 56.0, 12.0);

        assertTrue(queryString.getCapturedObject().toString().matches(".*suppressed=false.*"));
        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*latitude<:north.*"));
        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*latitude>:south.*"));
        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*longitude<:east.*"));
        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*longitude>:west.*"));
        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*[(]e.startTime >= :from OR e.endTime >= :from[)] AND [(]e.startTime <= :to OR e.endTime <= :to[)].*"));
        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*TYPE[(]e[)] IN [(].*[)].*"));
        assertTrue(queryString.getCapturedObject().toString().matches(".*WHERE.*b.vessel.callsign LIKE :vessel OR b.vessel.name LIKE :vessel.*"));

        context.assertIsSatisfied();
    }

    @Test
    public void testFindRecentEvents() {
        final ArgumentCaptor<String> queryString = ArgumentCaptor.forClass(String.class);

        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession(); will(returnValue(session));
            oneOf(session).createQuery(with(queryString.getMatcher())); will(returnValue(query));
            oneOf(query).setMaxResults(3);
            oneOf(query).list();
            oneOf(session).close();
        }});

        eventRepository.findRecentEvents(3);

        assertTrue(queryString.getCapturedObject().toString().matches("SELECT e FROM Event e WHERE e.suppressed=false ORDER BY e.startTime DESC"));

        context.assertIsSatisfied();
    }

    @Test
    public void testGetEventTypes() {
        final ArgumentCaptor<String> queryString = ArgumentCaptor.forClass(String.class);

        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession(); will(returnValue(session));
            oneOf(session).createQuery(with(queryString.getMatcher())); will(returnValue(query));
            oneOf(query).list();
            oneOf(session).close();
        }});

        eventRepository.getEventTypes();

        assertTrue(queryString.getCapturedObject().toString().matches("SELECT DISTINCT e.class AS c FROM Event e WHERE e.suppressed=false ORDER BY c"));

        context.assertIsSatisfied();
    }
}

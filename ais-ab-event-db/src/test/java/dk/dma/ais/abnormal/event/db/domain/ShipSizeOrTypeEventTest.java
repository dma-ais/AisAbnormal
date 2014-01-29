package dk.dma.ais.abnormal.event.db.domain;

import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.h2.H2EventRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

public class ShipSizeOrTypeEventTest {

    private JUnit4Mockery context;
    private SessionFactory sessionFactory;
    private Session session;

    @Before
    public void setUp() throws Exception {
        context = new JUnit4Mockery();

        sessionFactory = context.mock(SessionFactory.class);
        session = context.mock(Session.class);
    }

    @Test
    public void canSaveShipSizeOrTypeEvent() {
        EventRepository eventRepository = new H2EventRepository(sessionFactory, false);
        ShipSizeOrTypeEvent event = new ShipSizeOrTypeEvent();

        context.checking(new Expectations() {{
            oneOf(sessionFactory).openSession(); will(returnValue(session));
            oneOf(session).getTransaction();
            oneOf(session).beginTransaction();
            oneOf(session).saveOrUpdate(event);
            oneOf(session).close();
        }});

        eventRepository.save(event);
    }
}

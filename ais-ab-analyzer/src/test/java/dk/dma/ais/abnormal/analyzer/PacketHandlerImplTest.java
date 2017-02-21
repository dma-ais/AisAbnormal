package dk.dma.ais.abnormal.analyzer;

import com.google.inject.Injector;
import dk.dma.ais.abnormal.analyzer.analysis.Analysis;
import dk.dma.ais.abnormal.analyzer.analysis.CloseEncounterAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.CourseOverGroundAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.DriftAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.FreeFlowAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.ShipTypeAndSizeAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.SpeedOverGroundAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.SuddenSpeedChangeAnalysis;
import dk.dma.ais.abnormal.analyzer.services.SafetyZoneService;
import dk.dma.ais.tracker.eventEmittingTracker.EventEmittingTracker;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import java.util.Set;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_CLOSEENCOUNTER_ENABLED;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_COG_ENABLED;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_DRIFT_ENABLED;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_BBOX;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_ENABLED;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_SOG_ENABLED;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_SUDDENSPEEDCHANGE_ENABLED;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_TYPESIZE_ENABLED;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BEHIND;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BREADTH;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_LENGTH;
import static org.junit.Assert.assertEquals;

public class PacketHandlerImplTest {

    @Test
    public void initNoAnalyses() throws Exception {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(CONFKEY_ANALYSIS_COG_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_SOG_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_TYPESIZE_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_DRIFT_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_SUDDENSPEEDCHANGE_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_CLOSEENCOUNTER_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_FREEFLOW_ENABLED, false);

        final JUnit4Mockery context = new JUnit4Mockery();
        Injector injectorMock = context.mock(Injector.class);

        context.checking(new Expectations() {{
        }});

        PacketHandlerImpl sut = new PacketHandlerImpl(configuration, injectorMock, null, null, null, null);
        Set<Analysis> analyses = sut.getAnalyses();

        assertEquals(0, analyses.size());
    }

    @Test
    public void initAllAnalyses() throws Exception {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(CONFKEY_ANALYSIS_COG_ENABLED, true);
        configuration.addProperty(CONFKEY_ANALYSIS_SOG_ENABLED, true);
        configuration.addProperty(CONFKEY_ANALYSIS_TYPESIZE_ENABLED, true);
        configuration.addProperty(CONFKEY_ANALYSIS_DRIFT_ENABLED, true);
        configuration.addProperty(CONFKEY_ANALYSIS_SUDDENSPEEDCHANGE_ENABLED, true);
        configuration.addProperty(CONFKEY_ANALYSIS_CLOSEENCOUNTER_ENABLED, true);
        configuration.addProperty(CONFKEY_ANALYSIS_FREEFLOW_ENABLED, true);
        configuration.addProperty(CONFKEY_ANALYSIS_FREEFLOW_BBOX, new Integer[] {1, 2, 3, 4});
        configuration.addProperty(CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_LENGTH, 2.0);
        configuration.addProperty(CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BREADTH, 3.0);
        configuration.addProperty(CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BEHIND, 0.25);

        final JUnit4Mockery context = new JUnit4Mockery();
        Injector injectorMock = context.mock(Injector.class);
        EventEmittingTracker trackingServiceMock = context.mock(EventEmittingTracker.class);

        SafetyZoneService safetyZoneService = new SafetyZoneService(configuration);

        context.checking(new Expectations() {{
            ignoring(trackingServiceMock);
            oneOf(injectorMock).getInstance(with(CourseOverGroundAnalysis.class)); will(returnValue(new CourseOverGroundAnalysis(configuration, null, null, trackingServiceMock, null, null)));
            oneOf(injectorMock).getInstance(with(SpeedOverGroundAnalysis.class)); will(returnValue(new SpeedOverGroundAnalysis(configuration, null, null, trackingServiceMock, null, null)));
            oneOf(injectorMock).getInstance(with(ShipTypeAndSizeAnalysis.class)); will(returnValue(new ShipTypeAndSizeAnalysis(configuration, null, null, trackingServiceMock, null, null)));
            oneOf(injectorMock).getInstance(with(DriftAnalysis.class)); will(returnValue(new DriftAnalysis(configuration, null, trackingServiceMock, null)));
            oneOf(injectorMock).getInstance(with(SuddenSpeedChangeAnalysis.class)); will(returnValue(new SuddenSpeedChangeAnalysis(configuration, null, trackingServiceMock, null)));
            oneOf(injectorMock).getInstance(with(CloseEncounterAnalysis.class)); will(returnValue(new CloseEncounterAnalysis(configuration, null, trackingServiceMock, null, safetyZoneService)));
            oneOf(injectorMock).getInstance(with(FreeFlowAnalysis.class)); will(returnValue(new FreeFlowAnalysis(configuration, null, trackingServiceMock, null)));
        }});

        PacketHandlerImpl sut = new PacketHandlerImpl(configuration, injectorMock, null, null, null, null);

        Set<Analysis> analyses = sut.getAnalyses();
        assertEquals(7, analyses.size());
        assertEquals(true, analyses.stream().anyMatch(a -> a instanceof CourseOverGroundAnalysis));
        assertEquals(true, analyses.stream().anyMatch(a -> a instanceof SpeedOverGroundAnalysis));
        assertEquals(true, analyses.stream().anyMatch(a -> a instanceof ShipTypeAndSizeAnalysis));
        assertEquals(true, analyses.stream().anyMatch(a -> a instanceof DriftAnalysis));
        assertEquals(true, analyses.stream().anyMatch(a -> a instanceof SuddenSpeedChangeAnalysis));
        assertEquals(true, analyses.stream().anyMatch(a -> a instanceof CloseEncounterAnalysis));
        assertEquals(true, analyses.stream().anyMatch(a -> a instanceof FreeFlowAnalysis));
    }

    @Test
    public void initSelectedAnalyses() throws Exception {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(CONFKEY_ANALYSIS_COG_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_SOG_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_TYPESIZE_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_DRIFT_ENABLED, false);
        configuration.addProperty(CONFKEY_ANALYSIS_SUDDENSPEEDCHANGE_ENABLED, true);
        configuration.addProperty(CONFKEY_ANALYSIS_CLOSEENCOUNTER_ENABLED, true);
        configuration.addProperty(CONFKEY_ANALYSIS_FREEFLOW_ENABLED, false);
        configuration.addProperty(CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_LENGTH, 2.0);
        configuration.addProperty(CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BREADTH, 3.0);
        configuration.addProperty(CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BEHIND, 0.25);

        final JUnit4Mockery context = new JUnit4Mockery();
        Injector injectorMock = context.mock(Injector.class);
        EventEmittingTracker trackingServiceMock = context.mock(EventEmittingTracker.class);

        SafetyZoneService safetyZoneService = new SafetyZoneService(configuration);

        context.checking(new Expectations() {{
            ignoring(trackingServiceMock);
            oneOf(injectorMock).getInstance(with(SuddenSpeedChangeAnalysis.class)); will(returnValue(new SuddenSpeedChangeAnalysis(configuration, null, trackingServiceMock, null)));
            oneOf(injectorMock).getInstance(with(CloseEncounterAnalysis.class)); will(returnValue(new CloseEncounterAnalysis(configuration, null, trackingServiceMock, null, safetyZoneService)));
        }});

        PacketHandlerImpl sut = new PacketHandlerImpl(configuration, injectorMock, null, null, null, null);

        Set<Analysis> analyses = sut.getAnalyses();
        assertEquals(2, analyses.size());
        assertEquals(false, analyses.stream().anyMatch(a -> a instanceof CourseOverGroundAnalysis));
        assertEquals(false, analyses.stream().anyMatch(a -> a instanceof SpeedOverGroundAnalysis));
        assertEquals(false, analyses.stream().anyMatch(a -> a instanceof ShipTypeAndSizeAnalysis));
        assertEquals(false, analyses.stream().anyMatch(a -> a instanceof DriftAnalysis));
        assertEquals(true, analyses.stream().anyMatch(a -> a instanceof SuddenSpeedChangeAnalysis));
        assertEquals(true, analyses.stream().anyMatch(a -> a instanceof CloseEncounterAnalysis));
        assertEquals(false, analyses.stream().anyMatch(a -> a instanceof FreeFlowAnalysis));
    }

}
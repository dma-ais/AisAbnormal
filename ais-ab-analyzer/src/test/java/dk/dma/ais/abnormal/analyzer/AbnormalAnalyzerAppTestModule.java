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

package dk.dma.ais.abnormal.analyzer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import dk.dma.ais.abnormal.analyzer.analysis.DriftAnalysis;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.tracker.EventEmittingTracker;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.enav.model.geometry.grid.Grid;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_DRIFT_COGHDG;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_DRIFT_DISTANCE;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_DRIFT_PERIOD;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_DRIFT_SOG_MAX;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_DRIFT_SOG_MIN;

/**
 * This is the Google Guice module class which defines creates objects to be injected by Guice.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
public final class AbnormalAnalyzerAppTestModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(AbnormalAnalyzerAppTestModule.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final JUnit4Mockery context;

    public AbnormalAnalyzerAppTestModule(JUnit4Mockery context) {
        this.context = context;
    }

    @Override
    public void configure() {
        bind(DriftAnalysis.class).in(Scopes.SINGLETON);
        bind(Tracker.class).to(EventEmittingTracker.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    Configuration provideConfiguration() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty(CONFKEY_ANALYSIS_DRIFT_PERIOD, 10);
        configuration.setProperty(CONFKEY_ANALYSIS_DRIFT_DISTANCE, 500);
        configuration.setProperty(CONFKEY_ANALYSIS_DRIFT_SOG_MIN, 1);
        configuration.setProperty(CONFKEY_ANALYSIS_DRIFT_SOG_MAX, 5);
        configuration.setProperty(CONFKEY_ANALYSIS_DRIFT_COGHDG, 45);
        return configuration;
    }

    @Provides
    @Singleton
    Grid provideGrid() {
        return Grid.create(200);
    }

    @Provides
    @Singleton
    dk.dma.ais.abnormal.analyzer.AppStatisticsService provideStatisticsService1() {
        return new dk.dma.ais.abnormal.analyzer.AppStatisticsServiceImpl();
    }

    @Provides
    @Singleton
    dk.dma.ais.abnormal.application.statistics.AppStatisticsService provideStatisticsService2() {
        return new dk.dma.ais.abnormal.application.statistics.AppStatisticsServiceImpl();
    }

    @Provides
    @Singleton
    EventRepository provideEventRepository() {
        return context.mock(EventRepository.class);
    }
}

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
package dk.dma.ais.abnormal.stat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AbnormalStatBuilderAppTest {

    @Test
    public void appTest() throws Exception {
        String[] args = new String[]{"-dir" ,"ais-ab-stat-builder/src/test/resources", "-name", "*.gz"};

        Injector injector = Guice.createInjector(new AbnormalStatBuilderAppTestInjector());
        AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);
        app.execute(args);
    }

    /*
     * Test that the appStatistics bean injected in app and features is indeed the same instance.
     * If dependency injection is done wrong, different instances may be used.
     */
    @Test
    public void testAppStatisticsIsSingleton() throws Exception {
        String[] args = new String[]{"-dir" ,"ais-ab-stat-builder/src/test/resources", "-name", "ais-sample-micro.txt.gz"};

        Injector injector = Guice.createInjector(new AbnormalStatBuilderAppTestInjector());
        AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);
        app.execute(args);

        AppStatisticsService appStatistics = app.getAppStatisticsService();

        assertEquals(9, appStatistics.getMessageCount());
        assertEquals(8, appStatistics.getPosMsgCount());
        assertEquals(1, appStatistics.getStatMsgCount());
        assertTrue("Cell count must be positive for Features to have been trained", appStatistics.getCellCount() > 0);
    }

}

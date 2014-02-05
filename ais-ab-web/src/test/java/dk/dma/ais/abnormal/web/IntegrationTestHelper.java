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
package dk.dma.ais.abnormal.web;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;

public class IntegrationTestHelper {

    private final static long runNo = System.currentTimeMillis();
    private final static File screenShotParentDir;
    static {
        screenShotParentDir = new File(System.getProperty("user.dir") + File.separator + "target" + File.separator + "selenium");
        screenShotParentDir.mkdirs();
    }

    public static void takeScreenshot(WebDriver browser, String integrationTestCaseName, String screenshotName) {
        if (browser instanceof TakesScreenshot) {
            File tempfile = ((TakesScreenshot) browser).getScreenshotAs(OutputType.FILE);
            File dumpfile = new File(screenShotParentDir.getAbsolutePath() + File.separator  + "scrshot-" + runNo + "-" + integrationTestCaseName + "-" + screenshotName + ".png");
            tempfile.renameTo(dumpfile);
            System.err.println("Screen shot dumped to: " + dumpfile.getAbsolutePath());
        } else {
            System.err.println("Browser does not support screen shots.");
        }
    }

}

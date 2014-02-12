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

import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class IntegrationTestHelper {

    private final static long runNo = System.currentTimeMillis();
    private final static File screenShotParentDir;
    static {
        screenShotParentDir = new File(System.getProperty("user.dir") + File.separator + "target" + File.separator + "selenium");
        screenShotParentDir.mkdirs();
    }

    public static PhantomJSDriver createPhantomJSWebDriver() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        PhantomJSDriverService driverService = PhantomJSDriverService.createDefaultService(capabilities);

        PhantomJSDriver driver;
        try {
            driver = new PhantomJSDriver(driverService, capabilities);
            driver.getFileDetector()
            driver.manage().window().setSize(new Dimension(1280, 1024));
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        } catch (WebDriverException e) {
            System.err.println("Message - " + e.getMessage());
            System.err.println("Additional info - " + e.getAdditionalInformation());
            System.err.println("Support url - " + e.getSupportUrl());
            System.err.println("System info - " + e.getSystemInformation());
            System.err.println("Build info - " + e.getBuildInformation());
            throw e;
        } catch (Throwable t) {
            System.err.println("Class - " + t.getClass());
            System.err.println("Message - " + t.getMessage());
            System.err.println("--------");
            t.printStackTrace(System.err);
            System.err.println("--------");
            throw t;
        }

        return driver;
    }

    public static void takeScreenshot(WebDriver browser, String screenshotName) {
        if (browser instanceof TakesScreenshot) {
            String testCaseClass  = calledByClass();
            String testCaseMethod = calledByMethod();

            File tempfile = ((TakesScreenshot) browser).getScreenshotAs(OutputType.FILE);
            File dumpfile = new File(screenShotParentDir.getAbsolutePath() + File.separator  + "scrshot-" + runNo + "-" + testCaseClass + "-" + testCaseMethod + "-" + screenshotName + ".png");
            tempfile.renameTo(dumpfile);
            System.err.println("Screen shot dumped to: " + dumpfile.getAbsolutePath());
        } else {
            System.err.println("Browser does not support screen shots.");
        }
    }

    private static String calledByClass() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement e = stacktrace[3];
        String className = e.getClassName();
        className = className.substring(className.lastIndexOf('.')+1);
        return className;
    }

    private static String calledByMethod() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement e = stacktrace[3];
        String methodName = e.getMethodName();
        return methodName;
    }

}

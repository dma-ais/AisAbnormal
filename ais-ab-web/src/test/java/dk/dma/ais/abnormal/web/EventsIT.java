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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventsIT {

    private static WebDriver browser;
    private static WebDriverWait wait;
    private final static String TEST_NAME = EventsIT.class.getSimpleName();

    @BeforeClass
    public static void setUp() {
        browser = new PhantomJSDriver();
        browser.manage().window().setSize(new Dimension(1280, 1024));
        wait = new WebDriverWait(browser, 120);
    }

    @Test
    public void canDisplayEventsViaTicker() throws InterruptedException {
        browser.get("http://127.0.0.1:8080/abnormal");
        Thread.sleep(1000);
        IntegrationTestHelper.takeScreenshot(browser, TEST_NAME, "init");

        try {
            // Assert there are 5 events in the ticker
            List<WebElement> tickerElements = browser.findElements(By.cssSelector("#ticker li"));
            assertEquals(5, tickerElements.size());

            // Assert that the ticker rolls between events
            WebElement ticker0Element = tickerElements.get(0);
            WebElement ticker1Element = tickerElements.get(1);

            System.out.println("ticker0="+ticker0Element.getText());
            wait.until(ExpectedConditions.visibilityOf(ticker0Element));
            System.out.println("ticker0="+ticker0Element.getText());
            assertTrue(ticker0Element.getText().matches(".*LEHMANN SOUND.*"));

            System.out.println("ticker1=" + ticker1Element.getText());
            wait.until(ExpectedConditions.visibilityOf(ticker1Element));
            System.out.println("ticker1=" + ticker1Element.getText());
            assertTrue(ticker1Element.getText().matches(".*EIDE FIGHTER.*"));

            // Assert that event in ticker can be clicked
            ticker1Element.findElement(By.cssSelector("span.glyphicon")).click();
        } catch (AssertionError e) {
            IntegrationTestHelper.takeScreenshot(browser, TEST_NAME, "error");
            throw e;
        } catch (WebDriverException e) {
            IntegrationTestHelper.takeScreenshot(browser, TEST_NAME, "error");
            throw e;
        }
        IntegrationTestHelper.takeScreenshot(browser, TEST_NAME, "success");
    }

    @AfterClass
    public static void tearDown() {
        browser.close();
    }

}

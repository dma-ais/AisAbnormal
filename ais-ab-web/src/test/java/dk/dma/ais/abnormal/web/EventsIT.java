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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElement;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

public class EventsIT {

    private static WebDriver browser;
    private static WebDriverWait wait;

    @BeforeClass
    public static void setUp() {
        browser = IntegrationTestHelper.createPhantomJSWebDriver();
        wait = new WebDriverWait(browser, 120);
        IntegrationTestHelper.reloadWebApplication(browser);
    }

    @Before
    public void reloadWebApplication() {
        IntegrationTestHelper.reloadWebApplication(browser);
    }

    @Test
    public void canDisplayEventsViaTicker() throws InterruptedException {
        try {
            // Assert there are 5 events in the ticker
            List<WebElement> tickerElements = browser.findElements(cssSelector("#ticker li"));
            assertEquals(5, tickerElements.size());

            // Assert that the ticker rolls between events
            WebElement ticker0Element = tickerElements.get(0);
            WebElement ticker1Element = tickerElements.get(1);

            System.out.println("ticker0=" + ticker0Element.getText());
            wait.until(ExpectedConditions.visibilityOf(ticker0Element));
            System.out.println("ticker0=" + ticker0Element.getText());
            assertTrue(ticker0Element.getText().matches(".*HAMLET.*"));

            System.out.println("ticker1=" + ticker1Element.getText());
            wait.until(ExpectedConditions.visibilityOf(ticker1Element));
            System.out.println("ticker1=" + ticker1Element.getText());
            assertTrue(ticker1Element.getText().matches(".*VESTA.*"));

            // Assert that event in ticker can be clicked
            ticker1Element.findElement(cssSelector("span.glyphicon")).click();
        } catch (AssertionError e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        } catch (WebDriverException e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        }
    }

    @Test
    public void navigateToSearchDialog() {
        try {
            // Navigate to search dialog
            WebElement searchModal = browser.findElement(id("event-search-modal"));
            assertFalse(searchModal.isDisplayed());

            // Tab
            WebElement eventsTab = browser.findElement(id("tab-events"));
            eventsTab.click();

            // Button
            wait.until(visibilityOfElementLocated(id("events-search")));
            WebElement searchButton = browser.findElement(id("events-search"));
            searchButton.click();

            // Popup
            wait.until(visibilityOfElementLocated(id("event-search-modal")));
            wait.until(visibilityOfElementLocated(id("event-search-by-other")));
            assertTrue(searchModal.isDisplayed());

            // Ensure that "search-by-other" tab on search dialog is displayed
            WebElement searchByOtherButton = browser.findElement(id("event-search-by-other"));
            WebElement searchByIdButton = browser.findElement(id("event-search-by-id"));
            assertTrue(searchByOtherButton.isDisplayed());
            assertFalse(searchByIdButton.isDisplayed());
        } catch (AssertionError e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        } catch (WebDriverException e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        }
    }

    @Test
    public void canShowSearchAllEvents() throws InterruptedException {
        navigateToSearchDialog();
        try {
            WebElement searchByOtherButton = browser.findElement(id("event-search-by-other"));
            searchByOtherButton.click();
            assertNumberOfSearchResults(20);
        } catch (AssertionError e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        } catch (WebDriverException e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        }
    }

    @Test
    public void canShowSearchByVesselName() throws InterruptedException {
        navigateToSearchDialog();
        try {
            WebElement vesselNameCallsignImoField = browser.findElement(id("search-event-vessel"));
            vesselNameCallsignImoField.sendKeys("L");
            WebElement searchByOtherButton = browser.findElement(id("event-search-by-other"));
            searchByOtherButton.click();
            assertNumberOfSearchResults(12);
        } catch (AssertionError e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        } catch (WebDriverException e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        }
    }

    @Test
    public void canDisplayEventOnMapFromSearchResults() throws InterruptedException {
        canShowSearchByVesselName();
        try {
            // Click on search result
            WebElement glyphIcon = browser.findElement(cssSelector("div.search-data span#result-9.glyphicon"));
            glyphIcon.click();

            // Assert search modal closes
            WebElement searchModal = browser.findElement(id("event-search-modal"));
            wait.until(not(visibilityOfElementLocated(id("event-search-modal"))));

            // Assert event drawn by OpenLayers in SVG
            List<WebElement> svgEventLabel = browser.findElements(cssSelector("div#map text tspan"));
            assertEquals(4, svgEventLabel.size());
        } catch (AssertionError e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        } catch (WebDriverException e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        }
    }

    @Test
    public void addsShownEventsToEventList() {
        try {
            // Assert events list empty
            List<WebElement> eventsShown = browser.findElements(By.cssSelector("table#events-shown tbody > tr"));
            assertEquals(1, eventsShown.size());
            WebElement noElementsShown = browser.findElement(By.cssSelector("table#events-shown tbody > tr:nth-child(1) td"));
            assertEquals("No data available in table", noElementsShown.getText());

            // Search event by vessel and display it on map
            navigateToSearchDialogAndSearchEventsByVessel();
            WebElement icon = browser.findElement(By.cssSelector("span#result-9.glyphicon"));
            icon.click();
            WebElement close = browser.findElement(By.id("search-close"));
            close.click();
            wait.until( not(visibilityOf(browser.findElement(id("event-search-modal")))) );

            // Assert that event list was updated with event
            eventsShown = browser.findElements(By.cssSelector("table#events-shown tbody > tr"));
            assertEquals(1, eventsShown.size());
            List<WebElement> elementShown = browser.findElements(By.cssSelector("table#events-shown tbody > tr td"));
            assertEquals(3, elementShown.size());
            assertEquals("SEABASS", elementShown.get(1).getText());

            // Assert that showing same event again does not cause duplicate on list
            navigateToSearchDialogAndSearchEventsByVessel();
            icon = browser.findElement(By.cssSelector("span#result-9.glyphicon"));
            icon.click();
            close = browser.findElement(By.id("search-close"));
            close.click();
            wait.until( not(visibilityOf(browser.findElement(id("event-search-modal")))) );

            eventsShown = browser.findElements(By.cssSelector("table#events-shown tbody > tr"));
            assertEquals(1, eventsShown.size());
            elementShown = browser.findElements(By.cssSelector("table#events-shown tbody > tr td"));
            assertEquals(3, elementShown.size());
            assertEquals("SEABASS", elementShown.get(1).getText());

            // Assert that adding another event shows up on list
            navigateToSearchDialogAndSearchEventsByVessel();
            icon = browser.findElement(By.cssSelector("span#result-6.glyphicon"));
            icon.click();
            close = browser.findElement(By.id("search-close"));
            close.click();
            wait.until( not(visibilityOf(browser.findElement(id("event-search-modal")))) );

            eventsShown = browser.findElements(By.cssSelector("table#events-shown tbody > tr"));
            assertEquals(2, eventsShown.size());

            // Assert that removing all events from the map clears the event list
            WebElement button = browser.findElement(By.id("events-remove"));
            button.click();

            eventsShown = browser.findElements(By.cssSelector("table#events-shown tbody > tr"));
            assertEquals(1, eventsShown.size());
            noElementsShown = browser.findElement(By.cssSelector("table#events-shown tbody > tr:nth-child(1) td"));
            assertEquals("No data available in table", noElementsShown.getText());
        } catch (AssertionError e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        } catch (WebDriverException e) {
            IntegrationTestHelper.takeScreenshot(browser, "error");
            throw e;
        }
    }

    private void navigateToSearchDialogAndSearchEventsByVessel() {
        navigateToSearchDialog();
        WebElement vesselNameCallsignImoField = browser.findElement(id("search-event-vessel"));
        vesselNameCallsignImoField.clear();
        vesselNameCallsignImoField.sendKeys("L");
        WebElement searchByOtherButton = browser.findElement(id("event-search-by-other"));
        searchByOtherButton.click();
        assertNumberOfSearchResults(12);
    }

    private void assertNumberOfSearchResults(int expectedNumberOfSearchResults) {
        WebElement searchStatus = browser.findElement(cssSelector("div.search-status"));
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        wait.until(not(textToBePresentInElement(id("event-search-modal"), "Searching...")));
        assertEquals("Found " + String.valueOf(expectedNumberOfSearchResults)+ " matching events.", searchStatus.getText());
    }

    @AfterClass
    public static void tearDown() {
        browser.close();
    }

}

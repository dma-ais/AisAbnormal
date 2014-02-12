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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

public class EventsIT {

    private WebDriver browser;
    private WebDriverWait wait;

    @Before
    public void setUp() {
        browser = IntegrationTestHelper.createPhantomJSWebDriver();
        wait = new WebDriverWait(browser, 120);

        try {
            browser.get("http://127.0.0.1:8080/abnormal");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

            System.out.println("ticker0="+ticker0Element.getText());
            wait.until(ExpectedConditions.visibilityOf(ticker0Element));
            System.out.println("ticker0="+ticker0Element.getText());
            assertTrue(ticker0Element.getText().matches(".*LOTUS.*"));

            System.out.println("ticker1=" + ticker1Element.getText());
            wait.until(ExpectedConditions.visibilityOf(ticker1Element));
            System.out.println("ticker1=" + ticker1Element.getText());
            assertTrue(ticker1Element.getText().matches(".*FINNSEA.*"));

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

            WebElement eventsTab = browser.findElement(id("tab-events"));
            eventsTab.click();
            WebElement searchButton = browser.findElement(id("events-search"));
            searchButton.click();
            wait.until(visibilityOfElementLocated(id("event-search-modal")));
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
            assertNumberOfSearchResults(18);
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
            vesselNameCallsignImoField.sendKeys("FINNSEA");
            WebElement searchByOtherButton = browser.findElement(id("event-search-by-other"));
            searchByOtherButton.click();
            assertNumberOfSearchResults(2);
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
            WebElement glyphIcon = browser.findElement(cssSelector("div.search-data span#result-17.glyphicon"));
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

    private void assertNumberOfSearchResults(int expectedNumberOfSearchResults) {
        WebElement searchStatus = browser.findElement(cssSelector("div.search-status"));
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        wait.until(not(textToBePresentInElement(id("event-search-modal"), "Searching...")));
        assertEquals("Found " + String.valueOf(expectedNumberOfSearchResults)+ " matching events.", searchStatus.getText());
    }

    @After
    public void tearDown() {
        browser.close();
    }

}

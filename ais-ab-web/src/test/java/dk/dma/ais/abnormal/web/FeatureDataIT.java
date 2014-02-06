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

import com.google.common.base.Predicate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FeatureDataIT {

    private static WebDriver browser;
    private static WebDriverWait wait;

    @BeforeClass
    public static void setUp() {
        browser = new PhantomJSDriver();
        browser.manage().window().setSize(new Dimension(1280, 1024));
        browser.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        wait = new WebDriverWait(browser, 120);
    }

    @Test
    public void testCanZoomInToSelectCellAndDisplayFeatureData() throws InterruptedException {
        browser.get("http://127.0.0.1:8080/abnormal");
        Thread.sleep(1000);
        IntegrationTestHelper.takeScreenshot(browser, "init");

        try {
            checkFeatureSetMetadata();
            checkCellsAreDisplayedWhenZoomingIn();
            clickOnACell();
            checkFeatureDataDisplayedCorrectlyForClickedCell();
        } catch (AssertionError e) {
            IntegrationTestHelper.takeScreenshot(browser,"error");
            throw e;
        } catch (WebDriverException e) {
            IntegrationTestHelper.takeScreenshot(browser,"error");
            throw e;
        }
        IntegrationTestHelper.takeScreenshot(browser,"success");
    }

    private void checkFeatureDataDisplayedCorrectlyForClickedCell() {
        // Check that a tab is shown for each feature
        List<WebElement> tabs = browser.findElements(By.cssSelector("div#cell-data-tabs.tabs li"));
        assertNotNull(tabs);
        assertEquals(3, tabs.size());
        assertEquals("CourseOverGround", tabs.get(0).getText());
        assertEquals("ShipTypeAndSize", tabs.get(1).getText());
        assertEquals("SpeedOverGround", tabs.get(2).getText());

        // Check course over ground data
        browser.findElement(By.cssSelector("a#tab-CourseOverGround.ui-tabs-anchor")).click();
        WebElement cogTab = browser.findElement(By.cssSelector("div[aria-labelledby=\"tab-CourseOverGround\"]"));
        List<WebElement> cogTabDivs = cogTab.findElements(By.tagName("div"));
        assertEquals("Total ship count is 8.", cogTabDivs.get(1).getText());

        // Check ship type and size data
        browser.findElement(By.cssSelector("a#tab-ShipTypeAndSize.ui-tabs-anchor")).click();
        WebElement stsTab = browser.findElement(By.cssSelector("div[aria-labelledby=\"tab-ShipTypeAndSize\"]"));
        List<WebElement> stsTabDivs = stsTab.findElements(By.tagName("div"));
        assertEquals("Total ship count is 8.", stsTabDivs.get(1).getText());

        // Check speed over ground data
        browser.findElement(By.cssSelector("a#tab-SpeedOverGround.ui-tabs-anchor")).click();
        WebElement sogTab = browser.findElement(By.cssSelector("div[aria-labelledby=\"tab-SpeedOverGround\"]"));
        List<WebElement> sogTabDivs = sogTab.findElements(By.tagName("div"));
        assertEquals("Total ship count is 8.", sogTabDivs.get(1).getText());
    }

    private void checkFeatureSetMetadata() {
        browser.findElement(By.id("tab-stats")).click();
        assertEquals("200 m", browser.findElement(By.cssSelector("div#gridsize.useroutput span.data")).getText());
        assertEquals("10 secs", browser.findElement(By.cssSelector("div#downsampling.useroutput span.data")).getText());
        IntegrationTestHelper.takeScreenshot(browser,"metadata");
    }

    private void clickOnACell() throws InterruptedException {
        zoomIntoHelsinore();
        waitForCellsToBeDisplayed();

        WebElement map = getMap();

        Actions actions = new Actions(browser);
        actions.moveToElement(map, map.getSize().getWidth() / 2, map.getSize().getHeight() / 2);
        actions.click();
        actions.perform();

        // Cursor position is exactly in the center of the map
        browser.findElement(By.id("tab-map")).click();
        assertEquals("(56°02'05.7\"N, 12°38'59.7\"E)", browser.findElement(By.cssSelector("div#cursorpos.useroutput p")).getText());

        // Assert feature data for correct cell displayed
        final String expectedCellInfo = "Cell id 6249302540 (56°02'06.5\"N,12°38'53.8\"E) - (56°02'00\"N,12°39'00.3\"E)";
        By actualCellInfoElement = By.cssSelector("div.cell-data-contents > h5");
        wait.until(ExpectedConditions.textToBePresentInElement(actualCellInfoElement, expectedCellInfo));
    }

    private void waitForCellsToBeDisplayed() {
        browser.findElement(By.id("tab-map")).click();
        wait.until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(@Nullable WebDriver webDriver) {
                return webDriver.findElement(By.id("cell-layer-load-status")).getText().matches(".*cells loaded.*");
            }
        });
    }

    private static WebElement getMap() {
        // WebElement map = browser.findElement(By.cssSelector("svg#OpenLayers_Layer_Vector_27_svgRoot"));
        WebElement map = browser.findElement(By.cssSelector("div#map"));
        assertEquals(55, map.getLocation().getX());
        assertEquals(169, map.getLocation().getY());
        assertEquals(870, map.getSize().getWidth());
        assertEquals(524, map.getSize().getHeight());
        return map;
    }

    private void zoomIntoHelsinore() throws InterruptedException {
        if (browser instanceof JavascriptExecutor) {
            ((JavascriptExecutor) browser).executeScript("mapModule.map.setCenter(new OpenLayers.LonLat(12.65,56.035).transform(new OpenLayers.Projection(\"EPSG:4326\"), new OpenLayers.Projection(\"EPSG:900913\")), 12)");
        }

        browser.findElement(By.id("tab-map")).click();
        final String expectedViewPortInfo = "(56°05'06.8\"N, 12°30'02.4\"E)\n(55°59'05\"N, 12°47'57.6\"E)";
        By actualViewPortInfoElement = By.cssSelector("div#viewport.useroutput p");
        wait.until(ExpectedConditions.textToBePresentInElement(actualViewPortInfoElement, expectedViewPortInfo));
    }

    private void checkCellsAreDisplayedWhenZoomingIn() throws InterruptedException {
        assertCellLayerLoadStatusNoCellsLoaded();

        try {
            WebElement zoomIn = browser.findElement(By.cssSelector("a.olControlZoomIn.olButton"));
            zoomIn.click();
            Thread.sleep(300);
            assertCellLayerLoadStatusNoCellsLoaded();
            zoomIn.click();
            Thread.sleep(300);
            assertCellLayerLoadStatusNoCellsLoaded();
            zoomIn.click();
            Thread.sleep(300);
            assertCellLayerLoadStatusNoCellsLoaded();
            zoomIn.click();
            Thread.sleep(300);
            assertCellLayerLoadStatusNoCellsLoaded();
            zoomIn.click();
            wait.until(ExpectedConditions.textToBePresentInElement(By.id("cell-layer-load-status"), "61 cells loaded, 61 added to map."));
        } catch (Throwable e) {
            if (browser instanceof TakesScreenshot) {
                IntegrationTestHelper.takeScreenshot(browser,"error");
            }
            throw e;
        }
    }

    private static void assertCellLayerLoadStatusNoCellsLoaded() {
        WebElement cellLayerLoadStatus = browser.findElement(By.id("cell-layer-load-status"));
        assertEquals("No cells loaded.", cellLayerLoadStatus.getText());
    }

    @AfterClass
    public static void tearDown() {
        browser.close();
    }

}

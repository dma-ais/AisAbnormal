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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import static org.junit.Assert.assertEquals;

public class FeatureDataIT {

    private static WebDriver browser;

    @BeforeClass
    public static void setup() {
        browser = new PhantomJSDriver();
        browser.manage().window().setSize(new Dimension(1280, 1024));
    }

    @Test
    public void testCanZoomInToSelectCellAndDisplayFeatureData() throws InterruptedException {
        browser.get("http://127.0.0.1:8080/abnormal");
        Thread.sleep(1000);
        IntegrationTestHelper.takeScreenshot(browser, "init");

        zoomInUntilCellsLoadedAndDisplayed();
        clickOnACell();
    }



    private void clickOnACell() throws InterruptedException {
        if (browser instanceof JavascriptExecutor) {
            ((JavascriptExecutor)browser).executeScript("mapModule.map.setCenter(new OpenLayers.LonLat(12.65,56.035).transform(new OpenLayers.Projection(\"EPSG:4326\"), new OpenLayers.Projection(\"EPSG:900913\")), 16)");
        }
        Thread.sleep(1000);

        WebElement map = getMap();

        Actions actions = new Actions(browser);
        actions.moveToElement(map);
        actions.click();
        actions.perform();
        assertEquals("Cell id 6249703285 (56°02'19.4\"N,12°38'21.5\"E) - (56°02'13\"N,12°38'28\"E)", browser.findElement(By.cssSelector("div.cell-data-contents > h5")).getText());

        IntegrationTestHelper.takeScreenshot(browser, "debug");
    }

    private static WebElement getMap() {
        return browser.findElement(By.cssSelector("svg#OpenLayers_Layer_Vector_27_svgRoot"));
    }

    private void zoomInUntilCellsLoadedAndDisplayed() throws InterruptedException {
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
            Thread.sleep(1000);

            WebElement cellLayerLoadStatus = browser.findElement(By.id("cell-layer-load-status"));
            assertEquals("61 cells loaded, 61 added to map.", cellLayerLoadStatus.getText());

        } catch (Throwable e) {
            if (browser instanceof TakesScreenshot) {
                IntegrationTestHelper.takeScreenshot(browser, "error");
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

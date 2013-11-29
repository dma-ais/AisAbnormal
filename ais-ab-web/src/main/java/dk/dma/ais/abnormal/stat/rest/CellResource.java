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
package dk.dma.ais.abnormal.stat.rest;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData2Key;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.grid.Cell;
import dk.dma.enav.model.geometry.grid.Grid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@RequestScoped
@Path("/cell")
public class CellResource {

    private static final Logger LOG = LoggerFactory.getLogger(CellResource.class);
    static {
        LOG.debug("CellResource loaded.");
    }
    {
        LOG.debug(this.getClass().getSimpleName() + " created (" + this + " ).");
    }

    private FeatureDataRepository featureDataRepository;

    @Inject
    public CellResource(FeatureDataRepository featureDataRepository) {
        this.featureDataRepository = featureDataRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<CellWrapper> getCellIdsWithinBoundaries(@QueryParam("north") Double north, @QueryParam("east") Double east, @QueryParam("south") Double south, @QueryParam("west") Double west) {
        // http://localhost:8080/abnormal/featuredata/cell?north=55&east=11&south=54.91&west=10.91

        LOG.debug("Attempting get id's of cells inside boundary");

        if (north == null) {
            throw new IllegalArgumentException("Missing 'north' parameter");
        }
        if (east == null) {
            throw new IllegalArgumentException("Missing 'east' parameter");
        }
        if (west == null) {
            throw new IllegalArgumentException("Missing 'west' parameter");
        }
        if (south == null) {
            throw new IllegalArgumentException("Missing 'south' parameter");
        }
        if (north <= south) {
            throw new IllegalArgumentException("'north' parameter must be > 'south' parameter");
        }
        if (east <= west) {
            throw new IllegalArgumentException("'east' parameter must be > 'west' parameter");
        }
        if (north - south > 0.1) {
            throw new IllegalArgumentException("'north' and 'south' parameters must be within 0.1. The current difference is " + (north - south));
        }
        if (east - west > 0.1) {
            throw new IllegalArgumentException("'east' and 'west' parameters must be within 0.1. The current difference is " + (east - west));
        }
        LOG.debug("parameters are ok");

        Double gridResolution = featureDataRepository.getMetaData().getGridResolution();

        LOG.debug("Feature set uses grid resolution of " + gridResolution);
        Grid grid = Grid.create(gridResolution);
        LOG.debug("Created grid with resolution " + gridResolution);

        Position northWest = Position.create(north, west);
        Position southEast = Position.create(south, east);
        Area area = BoundingBox.create(northWest, southEast, CoordinateSystem.GEODETIC);
        LOG.debug("Looking for cells touching area bounded by " + north + " north, " + east + " east, " + south + " south, and " + west + " west.");

        //Set<Cell> cells = grid.getCells(area);
        //LOG.debug("Found " + cells.size() + " cells inside area [" + northWest + "," + southEast + "]");
        Set<CellWrapper> cells = loadDummyCells(grid);

        return cells;
    }

    private Set<CellWrapper> loadDummyCells(Grid grid) {
        Set<CellWrapper> cells = new LinkedHashSet<>();

        for (double lon = 12.0; lon < 12.50; lon += 0.05) {
            for (double lat = 56.0; lat < 56.50; lat += 0.05) {
                FeatureData2Key feature1Data = new FeatureData2Key(this.getClass(), "shipType", "shipSize");
                feature1Data.setStatistic((short) 1, (short) 1, "stat1", (Integer) 7);

                FeatureData2Key feature2Data = new FeatureData2Key(Integer.class, "prime", "square");
                feature2Data.setStatistic((short) 1, (short) 1, "statA", (Integer) 9);
                feature2Data.setStatistic((short) 1, (short) 2, "statA", (Integer) 8);
                feature2Data.setStatistic((short) 2, (short) 1, "statA", (Integer) 7);

                Cell cell = grid.getCell(lat, lon);
                CellWrapper cellWrapper = new CellWrapper(cell, lat+0.02, lon+0.02, lat-0.02, lon-0.02, feature1Data, feature2Data);
                cells.add(cellWrapper);
            }
        }

        return cells;
    }

    public class CellWrapper {
        private final Cell cell;
        private final double north;
        private final double east;
        private final double south;
        private final double west;
        private final Set<FeatureData> featureData;

        public int getCellId() {
            return cell.getCellId();
        }

        public double getNorth() {
            return north;
        }

        public double getEast() {
            return east;
        }

        public double getSouth() {
            return south;
        }

        public double getWest() {
            return west;
        }

        public Set<FeatureData> getFeatureData() {
            return featureData;
        }

        public CellWrapper(Cell cell, double north, double east, double south, double west, FeatureData... featureData) {
            this.cell = cell;
            this.north = north;
            this.east = east;
            this.south = south;
            this.west = west;
            this.featureData = new HashSet<>(Arrays.asList(featureData));
        }
    }
}

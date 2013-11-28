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
    public Set<Cell> geCellIdsWithinBoundaries(@QueryParam("north") Double north, @QueryParam("east") Double east, @QueryParam("south") Double south, @QueryParam("west") Double west) {
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

        Set<Cell> cells = grid.getCells(area);
        LOG.debug("Found " + cells.size() + " cells inside area [" + northWest + "," + southEast + "]");

        return cells;
    }

}

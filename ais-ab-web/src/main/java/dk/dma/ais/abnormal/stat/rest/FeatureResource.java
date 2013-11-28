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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@RequestScoped
@Path("/featureset/feature/{featureName}")
public class FeatureResource {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureResource.class);

    private FeatureDataRepository featureDataRepository;

    @Inject
    public FeatureResource(FeatureDataRepository featureDataRepository) {
        this.featureDataRepository = featureDataRepository;
    }

    @Path("/cell")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Long> geCellIdsForFeature(@PathParam("featureName") String featureName) {
        LOG.debug("Attempting get id's of cells with data feature "+ featureName);

        // http://localhost:8080/abnormal/feature/featureset/feature/ShipTypeAndSizeFeature/cell
        Set<Long> cellIds = featureDataRepository.getCellIds(featureName);

        LOG.debug("There are" + (cellIds == null ? " no ":" ") + "matching cell ids");
        if (cellIds == null) {
            throw new IllegalArgumentException("Feature " + featureName + " has no data");
        }

        return cellIds;
    }

    @Path("/cell/{cellId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public FeatureData getFeatureDataForCell(@PathParam("featureName") String featureName, @PathParam("cellId") Integer cellId) {
        LOG.debug("Attempting get feature data for feature "+ featureName + ", cell id " + cellId);

        if (cellId == null || cellId <= 0) {
            throw new IllegalArgumentException("Missing cellId parameter");
        }

        // http://localhost:8080/abnormal/feature/featureset/feature/ShipTypeAndSizeFeature/cell/2146894246
        FeatureData featureData = featureDataRepository.getFeatureData(featureName, cellId);

        LOG.debug("There are" + (featureData == null ? " no ":" ") + "feature data for cellId " + cellId);
        if (featureData == null) {
            throw new IllegalArgumentException("Feature " + featureName + " has no data for cellId " + cellId);
        }

        return featureData;
    }

}

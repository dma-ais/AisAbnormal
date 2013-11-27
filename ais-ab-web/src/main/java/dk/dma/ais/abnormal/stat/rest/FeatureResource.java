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
import dk.dma.ais.abnormal.stat.db.data.DatasetMetaData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData2Key;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.io.PrintStream;
import java.util.Set;

@RequestScoped
@Path("/feature/{featureName}")
public class FeatureResource {

    private FeatureDataRepository featureDataRepository;

    @Inject
    public FeatureResource(FeatureDataRepository featureDataRepository) {
        this.featureDataRepository = featureDataRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public FeatureData getFeatureDataForCell(@PathParam("featureName") String featureName, @QueryParam("cellId") Integer cellId) {
        // http://localhost:8080/abnormal/feature/feature/testFeature?cellId=9999
        // http://localhost:8080/abnormal/feature/feature/ShipTypeAndSizeFeature?cellId=67
       return featureDataRepository.getFeatureData(featureName, cellId);
    }

}

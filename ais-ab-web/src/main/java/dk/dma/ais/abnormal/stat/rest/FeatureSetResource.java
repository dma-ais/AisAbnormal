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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@RequestScoped
@Path("/featureset")
public class FeatureSetResource {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureSetResource.class);
    static {
        LOG.debug("FeatureSetResource loaded.");
    }
    {
        LOG.debug(this.getClass().getSimpleName() + " created (" + this + " ).");
    }

    private FeatureDataRepository featureDataRepository;

    @Inject
    public FeatureSetResource(FeatureDataRepository featureDataRepository) {
        this.featureDataRepository = featureDataRepository;
    }

    @GET
    @Path("/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public DatasetMetaData getMetadata() {
        // http://localhost:8080/abnormal/feature/featureset/metadata
        return featureDataRepository.getMetaData();
    }

    @GET
    @Path("/featureNames")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getFeatureNames() {
        // http://localhost:8080/abnormal/feature/featureset/featureNames
        return featureDataRepository.getFeatureNames();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List sayHello() {
        // http://localhost:8080/abnormal/featuredata/featureset/

        DatasetMetaData metaData = featureDataRepository.getMetaData();
        Set<String> featureNames = featureDataRepository.getFeatureNames();

        HashMap<String, Set<String>> featureNamesMap = new HashMap<>();
        featureNamesMap.put("featureNames", featureNames);

        ArrayList<Object> s = new ArrayList<>();
        Collections.addAll(s, metaData, featureNamesMap);
        return s; // output.toString();
    }

}

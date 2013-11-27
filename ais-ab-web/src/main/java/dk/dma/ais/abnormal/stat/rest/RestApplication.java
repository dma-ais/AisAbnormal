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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

@RequestScoped
@Path("/feature")
public class RestApplication extends Application {

    private FeatureDataRepository featureDataRepository;

    @Inject
    public RestApplication(FeatureDataRepository featureDataRepository) {
        this.featureDataRepository = featureDataRepository;
    }

    @GET
    @Produces("text/plain")
    public String sayHello() {
        return
               "The feature data in the repository were computed with\n" +
               "   grid resolution: " + featureDataRepository.getMetaData().getGridSize() + "\n" +
               "   down sampling:   " + featureDataRepository.getMetaData().getDownsampling() + " secs";
    }

}

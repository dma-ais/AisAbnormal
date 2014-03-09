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
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
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
@Path("/statistics")
public class StatisticsResource {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsResource.class);
    static {
        LOG.debug("StatisticsResource loaded.");
    }
    {
        LOG.debug(this.getClass().getSimpleName() + " created (" + this + " ).");
    }

    private StatisticDataRepository statisticsRepository;

    @Inject
    public StatisticsResource(StatisticDataRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    @GET
    @Path("/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public DatasetMetaData getMetadata() {
        // http://localhost:8080/abnormal/statistic/Statistics/metadata
        return statisticsRepository.getMetaData();
    }

    @GET
    @Path("/statisticNames")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getStatisticNames() {
        // http://localhost:8080/abnormal/statistic/Statistics/statisticNames
        return statisticsRepository.getStatisticNames();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List sayHello() {
        // http://localhost:8080/abnormal/statisticdata/Statistics/

        DatasetMetaData metaData = statisticsRepository.getMetaData();
        Set<String> statisticNames = statisticsRepository.getStatisticNames();

        HashMap<String, Set<String>> statisticNamesMap = new HashMap<>();
        statisticNamesMap.put("statisticNames", statisticNames);

        ArrayList<Object> s = new ArrayList<>();
        Collections.addAll(s, metaData, statisticNamesMap);
        return s; // output.toString();
    }

}

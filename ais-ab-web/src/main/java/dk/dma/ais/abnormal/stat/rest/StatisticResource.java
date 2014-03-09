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
import dk.dma.ais.abnormal.stat.db.data.StatisticData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@RequestScoped
@Path("/statistics/statistic/{statisticName}")
public class StatisticResource {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticResource.class);
    static {
        LOG.debug("StatisticResource loaded.");
    }
    {
        LOG.debug(this.getClass().getSimpleName() + " created (" + this + " ).");
    }

    private StatisticDataRepository statisticsRepository;

    @Inject
    public StatisticResource(StatisticDataRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    @Path("/cell")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Long> geCellIdsForStatistic(@PathParam("statisticName") String statisticName) {
        LOG.debug("Attempting get id's of cells with data statistic "+ statisticName);

        // http://localhost:8080/abnormal/statistic/Statistics/statistic/ShipTypeAndSizeStatistic/cell
        Set<Long> cellIds = statisticsRepository.getCellsWithData(statisticName);

        LOG.debug("There are" + (cellIds == null ? " no ":" ") + "matching cell ids");
        if (cellIds == null) {
            throw new IllegalArgumentException("Statistic " + statisticName + " has no data");
        }

        return cellIds;
    }

    @Path("/cell/{cellId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public StatisticData getStatisticDataForCell(@PathParam("statisticName") String statisticName, @PathParam("cellId") Integer cellId) {
        LOG.debug("Attempting get statistic data for statistic "+ statisticName + ", cell id " + cellId);

        if (cellId == null || cellId <= 0) {
            throw new IllegalArgumentException("Missing cellId parameter");
        }

        // http://localhost:8080/abnormal/statistic/Statistics/statistic/ShipTypeAndSizeStatistic/cell/2146894246
        StatisticData statistics = statisticsRepository.getStatisticData(statisticName, cellId);

        LOG.debug("There are" + (statistics == null ? " no ":" ") + "statistic data for cellId " + cellId);
        if (statistics == null) {
            throw new IllegalArgumentException("Statistic " + statisticName + " has no data for cellId " + cellId);
        }

        return statistics;
    }

}

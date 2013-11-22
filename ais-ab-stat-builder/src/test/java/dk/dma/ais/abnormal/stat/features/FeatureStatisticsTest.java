package dk.dma.ais.abnormal.stat.features;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class FeatureStatisticsTest {

    @Test
    public void canStoreAndRetrieveSingleStatistic() {
        FeatureStatistics featureStatistics = new FeatureStatistics();

        featureStatistics.setStatistic(7,3,1,"testStat", Integer.valueOf(42));

        assertEquals(Integer.valueOf(1), featureStatistics.getNumberOfLevel1Entries());
        assertEquals(Integer.valueOf(42), featureStatistics.getStatistic(7, 3, 1, "testStat"));
        assertNull(featureStatistics.getStatistic(8, 3, 1, "testStat"));
        assertNull(featureStatistics.getStatistic(7, 2, 1, "testStat"));
        assertNull(featureStatistics.getStatistic(7, 3, 2, "testStat"));
        assertNull(featureStatistics.getStatistic(7, 3, 1, "wrongTestStat"));
    }

    @Test
    public void canIncrementInitializedStatistic() {
        FeatureStatistics featureStatistics = new FeatureStatistics();

        featureStatistics.setStatistic(7,3,1,"testStat", Integer.valueOf(42));
        featureStatistics.incrementStatistic(7,3,1,"testStat");

        assertEquals(Integer.valueOf(43), featureStatistics.getStatistic(7, 3, 1, "testStat"));
    }

    @Test
    public void canIncrementUninitializedStatistic() {
        FeatureStatistics featureStatistics = new FeatureStatistics();
        featureStatistics.incrementStatistic(7,3,1,"testStat");
        assertEquals(Integer.valueOf(1), featureStatistics.getStatistic(7, 3, 1, "testStat"));

        featureStatistics = new FeatureStatistics();
        featureStatistics.incrementStatistic(7,3,1,"testStat");
        featureStatistics.incrementStatistic(7,3,1,"testStat");
        assertEquals(Integer.valueOf(2), featureStatistics.getStatistic(7, 3, 1, "testStat"));
    }
}

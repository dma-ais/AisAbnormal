package dk.dma.ais.abnormal.stat.db.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FeatureDataTest {

    @Test
    public void canStoreAndRetrieveSingleStatistic() {
        FeatureData featureData = new FeatureData();

        featureData.setStatistic((short) 3, (short) 1, "testStat", 42);

        assertEquals((Integer) 1, featureData.getNumberOfLevel1Entries());
        assertEquals(42, featureData.getStatistic((short) 3, (short) 1, "testStat"));
        assertNull(featureData.getStatistic((short) 2, (short) 1, "testStat"));
        assertNull(featureData.getStatistic((short) 3, (short) 2, "testStat"));
        assertNull(featureData.getStatistic((short) 3, (short) 1, "wrongTestStat"));
    }

    @Test
    public void canIncrementInitializedStatistic() {
        FeatureData featureData = new FeatureData();

        featureData.setStatistic((short) 3, (short) 1, "testStat", 42);
        featureData.incrementStatistic((short) 3, (short) 1,"testStat");

        assertEquals(43, featureData.getStatistic((short) 3, (short) 1, "testStat"));
    }

    @Test
    public void canIncrementUninitializedStatistic() {
        FeatureData featureData = new FeatureData();
        featureData.incrementStatistic((short) 3, (short) 1,"testStat");
        assertEquals(1, featureData.getStatistic((short) 3, (short) 1, "testStat"));

        featureData = new FeatureData();
        featureData.incrementStatistic((short) 3, (short) 1,"testStat");
        featureData.incrementStatistic((short) 3, (short) 1,"testStat");
        assertEquals(2, featureData.getStatistic((short) 3, (short) 1, "testStat"));
    }
}

package dk.dma.ais.abnormal.util;

import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.packet.AisPacket;
import org.junit.Test;

import static dk.dma.ais.abnormal.util.TrackPredicates.isUnknownTypeOrSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrackPredicatesTest {

    // GatehouseSourceTag [baseMmsi=2190067, country=DK, region=, timestamp=Thu Apr 10 15:30:29 CEST 2014]
    // [msgId=5, repeat=0, userId=219000606, callsign=OWNM@@@, dest=BOEJDEN-FYNSHAV@@@@@, dimBow=12, dimPort=8, dimStarboard=4, dimStern=58, draught=30, dte=0, eta=67584, imo=8222824, name=FRIGG SYDFYEN@@@@@@@, posType=1, shipType=61, spare=0, version=0]
    AisPacket msg5 = AisPacket.from(
        "$PGHP,1,2014,4,10,13,30,29,165,219,,2190067,1,28*22\r\n" +
        "!BSVDM,2,1,1,A,53@ng7P1uN6PuLpl000I8TLN1=T@ITDp0000000u1Pr844@P07PSiBQ1,0*7B\r\n" +
        "!BSVDM,2,2,1,A,CcAVCTj0EP00000,2*53");

    @Test
    public void detectsUnknownTypeOrSize() {
        Track track1 = new Track(219000606);
        assertTrue(isUnknownTypeOrSize.test(track1));
        track1.update(msg5);
        assertFalse(isUnknownTypeOrSize.test(track1));
    }

}
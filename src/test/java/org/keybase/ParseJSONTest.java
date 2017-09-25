// Purpose: To test the JSON Parser

package org.keybase;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.*;

public class ParseJSONTest {
    private static final Logger log = LoggerFactory.getLogger(ParseJSONTest.class);
    
    @Test
    /**
     * Tests JSON parser by comparing parsed Merkle Root with stored Merkle Roots
     * with root number 1, 10, 100, 1000, 10000, 100000, 1000000
     */
    public void testAgainstKnown() {
        log.info("Test: Parsing JSON");
        String[] roots = new String[7];
        roots[0] = "da88640b9e5c230849e138cf8b00d5afa13688dd28da21357a143fd91db09369ea96b2ead77c096f2b617e06b4db1403028448d315c62c462ea4bdd0844beaad"; // seqno 1
        roots[1] = "96630ebdfed54cf3d050fcce99b09928e7cbbe7140c9b79d7071fbe687fc1a44e05dde723f3730d1f601cb550f872c9ed4d482e7fe63f23a38f5af2c5fd3e388"; // seqno 10
        roots[2] = "7def1fe87611fcd3cdc09f71447683257c32d3ee1277a7cfc0b6efec10c7aa9089716adbf31d356d2e1cd042b605842e173269892bfb2bd25b966bb51b01514b"; // seqno 100
        roots[3] = "e6c4e11f698a180e6716a3d475f79f0c602458cdbc05372b8fb2c64de9d053d6b1cc284003d4463099e88a73b64361b18501d864a826bddecff954e6bf875351"; // seqno 1000
        roots[4] = "25ab72f94f869872f228aa4460190d917f8dedf0287ecbd5de7b0cf242451e7388185f053f4936fcae923cc78142f401a7744b6027b4fa4c2bd68ddf62683d6e"; // seqno 10000
        roots[5] = "a0c68d481eaee2a167a63520b6bfc69b4cc428c303670caf6370f3690934883afa11c4964cd972a442e24f66d75f629e1bf27db21bf6ebda28c716fcf10f7c4b"; // seqno 100000
        roots[6] = "037d832591f1ae2bbfffbbf6cccdf4170889ff58d2335baa625fd7e29d58a3ff00ecd30a83307ca1045a6915418f1bd2fc427c514de7f8c082dfc37a0b107369"; // seqno 1000000
        int rootNum = 1;
        for (int i=0; i < 7; i++) {
            try {
                assertEquals(roots[i], JSONParser.getMerkleRoot(rootNum));
            } catch (Exception e) {
                log.error("ERROR: Failed on merkle root " +  rootNum);
            }
            rootNum *= 10;
        }
    }
    


}

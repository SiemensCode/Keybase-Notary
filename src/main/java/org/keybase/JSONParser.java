// Purpose: To access the Keybase server and read the merkle root from
// JSON

package org.keybase;

import java.net.URL;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONParser {

    private static final Logger log = LoggerFactory.getLogger(JSONParser.class); 
    private static final String KEYBASE_URL = "https://keybase.io/_/api/1.0/merkle/root.json?seqno=";

    public static String getMerkleRoot(int num) throws IOException, IllegalArgumentException {
        URL url = null;
        if (num >= 1) {
            url = new URL(KEYBASE_URL + num);

        } else {
            log.error("ERROR: Not a valid URL");
            throw new IllegalArgumentException("Num must be a positive integer."); // num must be a positive integer
        }

        ObjectMapper objectMapper = new ObjectMapper();
        MerkleRoot root = null;

        Exception exception;
        int waitingTime = 1;
        
        do {
            try {
                root = objectMapper.readValue(url.openStream(), MerkleRoot.class);
                exception = null;
            } catch (Exception e) {
                exception = e;
                log.error("ERROR: Keybase server not working, waiting " + waitingTime + " seconds to retry");
                waitTime(waitingTime);
                
                waitingTime *= 2;
                if(waitingTime >= 600) {
                    waitingTime = 600;
                }
                
                
            }
        }while(exception != null);
       

        return root.getHash();
    }
    
    private static void waitTime(int seconds) {
        try {
            Thread.sleep(1000 * seconds);
        } catch (InterruptedException e) {      
            Thread.currentThread().interrupt();
        }
    }


    public static int getCurrentSeqno() {
        URL url;
        try {
            url = new URL("https://keybase.io/_/api/1.0/merkle/root.json");
        } catch (Exception e) {
            log.error("ERROR: Unable to access keybase merkle roots");
            e.printStackTrace();
            System.exit(1);
            return -1; // included to make compiler happy,
            // but this line of code should never be reached.
        }


        ObjectMapper objectMapper = new ObjectMapper();
        try {
            MerkleRoot root = objectMapper.readValue(url.openStream(), MerkleRoot.class);
            return root.getSeqno();
        } catch (IOException e) {
            log.error("ERROR: IO Exception unable to read Merkle root from JSON.");
            e.printStackTrace();
            System.exit(1);
            return -1; // included to make compiler happy,
            // but this line of code should never be reached.
        }

    }

}

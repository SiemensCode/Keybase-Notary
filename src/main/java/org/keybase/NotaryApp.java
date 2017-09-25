// Purpose: To run a catena server and create a notary that contains the keybase Merkle Roots

package org.keybase;

import org.bitcoinj.core.*;
import org.catena.server.CatenaServer;
import org.catena.common.CatenaApp;
import org.catena.common.CatenaUtils;
import org.catena.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import java.io.File;
import java.io.IOException;

public class NotaryApp extends CatenaApp {

    private static final Logger log = LoggerFactory.getLogger(NotaryApp.class);
    private static ECKey chainKey;
    private static CatenaServer server;
    private static final int SECONDS_BETWEEN = 1; // waits only 1 second.
    
    private static int seqno = 0;

    public static void main(String[] args) throws Exception {
        //System.out.println(NotaryApp.class.getClassLoader().getResource("logging.properties"));

        //BriefLogFormatter.init();
        //BriefLogFormatter.initVerbose();
        //BriefLogFormatter.initWithSilentBitcoinJ();

        if (args.length < 2) {
            // TODO: handle root-of-trust txid for new wallets with reused keys 
            log.error("Usage: <chain-secret-key> mainnet|testnet|regtest [<datadir>] [<root-of-trust-txid>]");
            return;
        }

        // Parse the Bitcoin network we're connecting to
        parseBitcoinNetwork(args, 1, "-server");
        if(args.length > 2) {
            directory = args[2];
        }

        maybeParseDataDirectory();

        // Read the chain's secret key from the file (we do this *after* we've set up the NetworkParameters)
        String chainKeyStr = args[0];
        try {
            chainKey = DumpedPrivateKey.fromBase58(params, chainKeyStr).getKey(); 
        } catch(Exception e) {
            log.error(Utils.fmt("Error decoding Bitcoin secret key '{}': {}.\n", chainKeyStr, e.getMessage()));
            log.error("Stack trace from exception: " + Throwables.getStackTraceAsString(e));
            System.exit(1);
            return;
        }

        // Server can tell if it needs the chainKey or not, by looking in its wallet
        server = new CatenaServer(params, new File(directory), chainKey, null);
        svc = server;
        //WalletAppKit server = new WalletAppKit(params, new File(directory), "-server");

        connectAndStart(new Runnable() {
            @Override
            public void run() {
                notarizeKeybase();
            }
        });
    }

    /** 
     * Creates transactions from Keybase Merkle Roots and thus
     * 'notarizes' Keybase. Run from the run() function in main.
     */
    private static void notarizeKeybase() {
        try {
            while (true) {
                if (!maybeIssueStatements())
                    log.debug("No new Merkle Roots to issue");
                waitTime();
            }
        } catch(Throwable e) {
            log.error("Exception occurred: " + Throwables.getStackTraceAsString(e));
        }
    }

    /** Waits SECONDS_BETWEEN seconds. This is called in between
     * attempts to issue a new Merkle Root.
     */
    private static void waitTime() {
        try {
            Thread.sleep(1000 * SECONDS_BETWEEN);
        } catch (InterruptedException e) {	    
            Thread.currentThread().interrupt();
        }
    }

    private static void issueStatementHandler(int sequenceNumber) throws InsufficientMoneyException, IOException, InterruptedException {
        // If there's no root-of-trust TXN, we gotta take care of that first
        if(ext.hasRootOfTrustTxid() == false) {
            issueRootOfTrustHandler();
            //return;
        } else {
            log.debug("Already have root-of-trust txid '{}'", ext.getRootOfTrustTxid());
        }

        String stmt = null;
        try {
            stmt = JSONParser.getMerkleRoot(sequenceNumber);
        } catch (IOException e) {
            log.error("Error " + e + " : " + Throwables.getStackTraceAsString(e));
            System.exit(1);
            // TODO
            // This probably means the networks is down, in which case we want to recheck.
        } catch (IllegalArgumentException e) {
            log.error("ERROR: Illegal Argument Exception");
            log.error("This most likely came from passing a nonpositive integer to JSONParer.getMerkleRoot()");
            log.error(Throwables.getStackTraceAsString(e));
            System.exit(1); // This should never happen, so exit!!
        }
        //        System.out.print("Please type in your next issued statement: ");
        //        String stmt = scanner.nextLine();
        //        if(stmt.trim().isEmpty()) {
        //            System.out.println("Cancelled due to empty statement. Please try again but type something in.");
        //            return;
        //        }
        //        System.out.println(stmt); // for debugging
        issueStatement(stmt);
    }    

    private static boolean maybeIssueStatements() {
        int currentSeqno = JSONParser.getCurrentSeqno();
        //    	if (currentSeqno == -1) {
        //	    System.err.println("ERROR: IO Exception while getting seqno");
        //	    System.exit(1);
        //    	}
        boolean issuedStatement = false;
        if (seqno < currentSeqno) {
            seqno = currentSeqno;
            try {
                issueStatementHandler(seqno);
                log.debug("Issued sequence number: " + seqno);
                issuedStatement = true;
            } catch (Exception e) {
                log.error("Exception occured while issuing statements: " + Throwables.getStackTraceAsString(e));
                System.exit(1);
            }
        }
        //    	while (seqno < currentSeqno) {
        //	    issuedStatement = true;
        //	    seqno++;
        //	    try {
        //		issueStatementHandler(seqno);
        //		System.out.println("Issued sequence number: " + seqno);
        //	    } catch (Exception e) {
        //		System.err.println(/"//Exception occurred while issuing statements: " + Throwables.getStackTraceAsString(e));
        //		System.exit(1);
        //	    }
        //    }
        return issuedStatement;
    }


    private static void issueStatement(String stmt)
            throws InsufficientMoneyException, IOException, InterruptedException {

        String seq = String.format("%010d", seqno);
        String tx = (stmt);

        if (seq.length() != 10) {
            log.error("ERROR: String length should be of size 10, but is of size " + seq.length());
            System.exit(1);;
        }
        if (tx.length() != 128)	{
            log.error("ERROR: Stmt length should be of size 128, but is of size " + stmt.length());
            System.exit(1);
        }

        Transaction txn = appendStatement(hexStringToByteArray(stmt));		

        Transaction prevTx = CatenaUtils.getPrevCatenaTx(wallet, txn.getHash());

        log.debug("Created tx '" + txn.getHash() + "' with statement '" + stmt + "' (prev tx is '" + prevTx.getHash() + "')");
        
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 == 1) {
            log.error("String of odd lenth cannot be hex.");
            System.exit(1); // This means there is a serious bug.
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static Transaction appendStatement(byte[] stmt) throws InsufficientMoneyException, IOException, 
    InterruptedException {
        Transaction txn = server.appendStatement(stmt);
        if(isRegtestEnv) {
            // We generate the block ourselves if we are in regtest mode
            CatenaUtils.generateBlockRegtest();
        }
        return txn;
    }


    private static void issueRootOfTrustHandler() throws InsufficientMoneyException, IOException, InterruptedException {
        Address chainAddr = wallet.getChainAddress();
        //System.out.printf("It looks like there is no Catena chain associated with the address from your wallet\n",
        //        chainAddr);

        log.debug("It looks like there is no Catena chain associated with the " + chainAddr + " address from your wallet");
        log.debug("Setting chain name to \'Keybase_notary\'");
        String chainName = "Keybase_notary";
        issueRootOfTrust(chainName);
    }

    private static void issueRootOfTrust(String chainName) throws InsufficientMoneyException, IOException, 
    InterruptedException {
        Transaction txn = appendStatement(chainName.getBytes());

        log.debug("Created root-of-trust tx '" + txn.getHash() + "' for chain '" + chainName);        
    }

}
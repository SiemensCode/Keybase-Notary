package org.keybase;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MerkleRoot {

    private Status status;
    private String hash;
    private int seqno;
    @JsonProperty("ctime_string")
    private String ctimeString;
    private long ctime;
    private String sig;
    @JsonProperty("payload_json")
    private String payloadJson;
    private String txid;
    private String hash160;
    @JsonProperty("payload_hash")
    private String payloadHash;
    @JsonProperty("hash_meta")
    private String hashMeta;
    //    private Sigs sigs;
    @JsonProperty("key_fingerprint")
    private String keyFingerprint;

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public String getHash() {
        return hash;
    }
    public void setHash(String hash) {
        this.hash = hash;
    }
    public int getSeqno() {
        return seqno;
    }
    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }
    public String getCtimeString() {
        return ctimeString;
    }
    public void setCtimeString(String ctimeString) {
        this.ctimeString = ctimeString;
    }
    public long getCtime() {
        return ctime;
    }
    public Date getDate() {
        return new Date(ctime * 1000);
    }
    public void setCtime(long ctime) {
        this.ctime = ctime;
    }
    public String getSig() {
        return sig;
    }
    public void setSig(String sig) {
        this.sig = sig;
    }
    public String getPayloadJson() {
        return payloadJson;
    }
    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
    public String getTxid() {
        return txid;
    }
    public void setTxid(String txid) {
        this.txid = txid;
    }   
    public String getPayloadHash() {
        return payloadHash;
    }
    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }
    public String getHash160() {
        return hash160;
    }
    public void setHash160(String hash160) {
        this.hash160 = hash160;
    }
    public String getHashMeta() {
        return hashMeta;
    }
    public void setHashMeta(String hashMeta) {
        this.hashMeta = hashMeta;
    }

    public String getKeyFingerprint() {
        return keyFingerprint;
    }
    public void setKeyFingerprint(String keyFingerprint) {
        this.keyFingerprint = keyFingerprint;
    }


}

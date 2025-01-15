package pt.ulisboa.tecnico.sec;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class AccountSnapshotValidated implements Serializable {
    private PublicKey key;
    private float balance;
    private Map<Integer, byte[]> signatures = new HashMap<Integer, byte[]>();

    public AccountSnapshotValidated(PublicKey key, float balance, Map<Integer, byte[]> signatures) {
        this.key = key;
        this.balance = balance;
        this.signatures = signatures;
    }

    public AccountSnapshotValidated(PublicKey key, float balance) {
        this.key = key;
        this.balance = balance;
    }

    public PublicKey getKey() {
        return key;
    }

    public void setKey(PublicKey key) {
        this.key = key;
    }

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }

    public Map<Integer, byte[]> getSignatures() {
        return signatures;
    }

    public void setSignatures(Map<Integer, byte[]> signatures) {
        this.signatures = signatures;
    }

    public void addSignature(int senderId, byte[] signature) {
        this.signatures.put(senderId, signature);
    }

}

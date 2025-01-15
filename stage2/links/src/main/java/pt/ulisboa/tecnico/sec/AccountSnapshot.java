package pt.ulisboa.tecnico.sec;

import java.io.Serializable;
import java.security.PublicKey;

public class AccountSnapshot implements Serializable {
    private PublicKey key;
    private float balance;
    private byte[] signature;

    public AccountSnapshot(PublicKey key, float balance, byte[] signature) {
        this.key = key;
        this.balance = balance;
        this.signature = signature;
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

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
}

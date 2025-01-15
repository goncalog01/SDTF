package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;
import java.security.PublicKey;

public class TransferMessage extends Message {

    private int sourceId;
    private PublicKey sourceAccountKey;
    private int destinationId;
    private PublicKey destinationAccountKey;
    private float amount;
    private boolean isFee;

    public TransferMessage(int senderId, int seqNum, InetAddress toAddress, int toPort,
                           InetAddress fromAddress, int fromPort, int sourceId, PublicKey sourceAccountKey, int destinationId, PublicKey destinationAcccountKey, float amount, boolean isFee) {
        super(MessageType.TRANSFER, senderId, seqNum, toAddress, toPort, fromAddress, fromPort);
        this.sourceId = sourceId;
        this.sourceAccountKey = sourceAccountKey;
        this.destinationId = destinationId;
        this.destinationAccountKey = destinationAcccountKey;
        this.amount = amount;
        this.isFee = isFee;
    }

    public int getSourceId() {
        return this.sourceId;
    }

    public PublicKey getSourceKey() {
        return this.sourceAccountKey;
    }

    public int getDestinationId() {
        return this.destinationId;
    }

    public PublicKey getDestinationKey() {
        return this.destinationAccountKey;
    }

    public float getAmount() {
        return this.amount;
    }

    public boolean isFee() {
        return this.isFee;
    }

}

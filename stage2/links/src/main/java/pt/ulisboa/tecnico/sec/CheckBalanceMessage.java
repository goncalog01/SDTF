package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;
import java.security.PublicKey;

public abstract class CheckBalanceMessage extends Message {

    private PublicKey accountKey;

    public CheckBalanceMessage(MessageType mode, int senderId, int seqNum, InetAddress toAddress, int toPort,
                               InetAddress fromAddress, int fromPort, PublicKey accountKey) {
        super(mode, senderId, seqNum, toAddress, toPort, fromAddress, fromPort);
        this.accountKey = accountKey;
    }

    public PublicKey getKey() {
        return this.accountKey;
    }
}

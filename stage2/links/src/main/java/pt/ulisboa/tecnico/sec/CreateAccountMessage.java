package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;
import java.security.PublicKey;

public class CreateAccountMessage extends Message {

    private PublicKey accountKey;

    public CreateAccountMessage(int senderId, int seqNum, InetAddress toAddress, int toPort,
                                InetAddress fromAddress, int fromPort, PublicKey accountKey) {
        super(MessageType.CREATE_ACCOUNT, senderId, seqNum, toAddress, toPort, fromAddress, fromPort);
        this.accountKey = accountKey;
    }

    public PublicKey getKey() {
        return this.accountKey;
    }
}

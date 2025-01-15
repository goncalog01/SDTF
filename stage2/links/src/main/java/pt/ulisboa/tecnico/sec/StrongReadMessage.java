package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;
import java.security.PublicKey;

public class StrongReadMessage extends CheckBalanceMessage {

    public StrongReadMessage(int senderId, int seqNum, InetAddress toAddress, int toPort,
                             InetAddress fromAddress, int fromPort, PublicKey accountKey) {
        super(MessageType.STRONG_READ, senderId, seqNum, toAddress, toPort, fromAddress, fromPort, accountKey);
    }

}

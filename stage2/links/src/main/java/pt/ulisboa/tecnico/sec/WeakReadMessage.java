package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;
import java.security.PublicKey;

public class WeakReadMessage extends CheckBalanceMessage {

    public WeakReadMessage(int senderId, int seqNum, InetAddress toAddress, int toPort,
                           InetAddress fromAddress, int fromPort, PublicKey accountKey) {
        super(MessageType.WEAK_READ, senderId, seqNum, toAddress, toPort, fromAddress, fromPort, accountKey);
    }

}

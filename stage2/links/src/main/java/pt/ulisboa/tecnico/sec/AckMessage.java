package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;

public class AckMessage extends Message {

    private int ackSenderId;

    public AckMessage(int senderId, int seqNum, InetAddress toAddress, int toPort,
                      InetAddress fromAddress, int fromPort, int ackSenderId) {
        super(MessageType.ACK, senderId, seqNum, toAddress, toPort, fromAddress, fromPort);
        this.ackSenderId = ackSenderId;
    }

    public int getAckSenderId() {
        return this.ackSenderId;
    }
}

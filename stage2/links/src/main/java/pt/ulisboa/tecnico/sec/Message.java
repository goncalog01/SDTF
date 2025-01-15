package pt.ulisboa.tecnico.sec;

import java.io.Serializable;
import java.net.*;

public abstract class Message implements Serializable {

    private MessageType type;
    private int senderId;
    private int seqNum;
    private InetAddress toAddress;
    private int toPort;
    private InetAddress fromAddress;
    private int fromPort;

    public Message(MessageType type, int senderId, int seqNum, InetAddress toAddress, int toPort,
                   InetAddress fromAddress, int fromPort) {
        this.type = type;
        this.senderId = senderId;
        this.toAddress = toAddress;
        this.toPort = toPort;
        this.fromAddress = fromAddress;
        this.fromPort = fromPort;
        this.seqNum = seqNum;
    }

    public MessageType getType() {
        return type;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public InetAddress getToAddress() {
        return toAddress;
    }

    public int getToPort() {
        return toPort;
    }

    public InetAddress getFromAddress() {
        return fromAddress;
    }

    public int getFromPort() {
        return fromPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Message msg = (Message) o;
        return senderId == msg.senderId && seqNum == msg.seqNum;
    }

}
package pt.ulisboa.tecnico.sec;

import java.io.Serializable;
import java.net.*;

public class Message implements Serializable {

    private MessageType type;
    private int senderId;
    private int seqNum;
    private InetAddress toAddress;
    private int toPort;
    private InetAddress fromAddress;
    private int fromPort;
    private String msg;

    public Message(MessageType type, int senderId, int seqNum, InetAddress toAddress, int toPort,
            InetAddress fromAddress, int fromPort, String msg) {
        this.type = type;
        this.senderId = senderId;
        this.toAddress = toAddress;
        this.toPort = toPort;
        this.fromAddress = fromAddress;
        this.fromPort = fromPort;
        this.seqNum = seqNum;
        this.msg = msg;
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

    public String getMsg() {
        return msg;
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
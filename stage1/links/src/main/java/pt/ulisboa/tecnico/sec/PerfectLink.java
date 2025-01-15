package pt.ulisboa.tecnico.sec;

import java.net.*;
import java.util.ArrayList;

public class PerfectLink {

    private StubbornLink sLink;
    private ArrayList<MessageWrapper> received = new ArrayList<MessageWrapper>();

    public PerfectLink() {
        try {
            this.sLink = new StubbornLink();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PerfectLink(int port) {
        try {
            this.sLink = new StubbornLink(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PerfectLink(int port, InetAddress address) {
        try {
            this.sLink = new StubbornLink(port, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(String msg, int senderId, int seqNum, InetAddress toAddress, int toPort, InetAddress fromAddress,
            int fromPort) {
        sLink.send_text(msg, senderId, seqNum, toAddress, toPort, fromAddress, fromPort);
    }

    public void send(Message msg, byte[] sign) {
        sLink.send_text(msg, sign);
    }

    public MessageWrapper receive() {
        MessageWrapper wrapper = sLink.receive();
        if (wrapper == null) {
            return null;
        }

        Message msg = wrapper.getMessage();
        if (msg.getType() == MessageType.TEXT) {
            sLink.send_ack(msg.getSenderId(), msg.getSeqNum(), msg.getFromAddress(),
                    msg.getFromPort(), msg.getToAddress(), msg.getToPort());
            if (!received.contains(wrapper)) {
                received.add(wrapper);
            } else {
                wrapper = null;
            }
        } else {
            wrapper = null;
        }
        return wrapper;
    }

    public void close() {
        sLink.close();
    }
}

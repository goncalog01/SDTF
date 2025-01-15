package pt.ulisboa.tecnico.sec;

import java.net.*;
import java.util.HashMap;

public class PerfectLink {

    private int id;
    private StubbornLink sLink;
    private HashMap<Integer, Integer> received = new HashMap<>();

    public PerfectLink(int id) {
        try {
            this.sLink = new StubbornLink(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.id = id;
    }

    public PerfectLink(int port, int id) {
        try {
            this.sLink = new StubbornLink(id, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.id = id;
    }

    public PerfectLink(int port, InetAddress address, int id) {
        try {
            this.sLink = new StubbornLink(id, port, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.id = id;
    }

    public void send(Message msg) {
        sLink.send(msg);
    }

    public MessageWrapper receive() {
        MessageWrapper wrapper = sLink.receive();
        if (wrapper == null || !wrapper.verifySignature()) {
            return null;
        }

        Message msg = wrapper.getMessage();
        if (!received.containsKey(msg.getSenderId())) {
            received.put(msg.getSenderId(), 0);
        }
        if (msg.getSeqNum() > received.get(msg.getSenderId())) {
            received.put(msg.getSenderId(), msg.getSeqNum());
            AckMessage m = new AckMessage(msg.getSenderId(), msg.getSeqNum(), msg.getToAddress(), msg.getToPort(), msg.getFromAddress(), msg.getFromPort(), this.id);
            sLink.send(m);
        } else {
            return null;
        }
        return wrapper;
    }

    public void close() {
        sLink.close();
    }
}

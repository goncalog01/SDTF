package pt.ulisboa.tecnico.sec;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class StubbornLink {

    int id;
    private DatagramSocket socket;
    private ConcurrentLinkedQueue<MessageWrapper> toSend = new ConcurrentLinkedQueue<>();

    public StubbornLink(int id) {
        try {
            this.id = id;
            this.socket = new DatagramSocket();
            socket.setSoTimeout(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runnable resendMessages = new Runnable() {
            public void run() {
                for (MessageWrapper msg : toSend) {
                    long delay = (long) Math.pow(2, msg.getResendCount()) * 1000;
                    if (msg.lastSentTime + delay <= System.currentTimeMillis()) {
                        msg.setLastSentTime(System.currentTimeMillis());
                        msg.incrementResendCount();
                        send(msg);
                    }
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(resendMessages, 0, 1, TimeUnit.SECONDS);
    }

    public StubbornLink(int id, int port) {
        try {
            this.id = id;
            this.socket = new DatagramSocket(port);
            socket.setSoTimeout(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runnable resendMessages = new Runnable() {
            public void run() {
                for (MessageWrapper msg : toSend) {
                    long delay = (long) Math.pow(2, msg.getResendCount()) * 1000;
                    if (msg.lastSentTime + delay <= System.currentTimeMillis()) {
                        msg.setLastSentTime(System.currentTimeMillis());
                        msg.incrementResendCount();
                        send(msg);
                    }
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(resendMessages, 0, 1, TimeUnit.SECONDS);
    }

    public StubbornLink(int id, int port, InetAddress address) {
        try {
            this.id = id;
            this.socket = new DatagramSocket(port, address);
            socket.setSoTimeout(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runnable resendMessages = new Runnable() {
            public void run() {
                for (MessageWrapper msg : toSend) {
                    long delay = (long) Math.pow(2, msg.getResendCount()) * 1000;
                    if (msg.lastSentTime + delay <= System.currentTimeMillis()) {
                        msg.setLastSentTime(System.currentTimeMillis());
                        msg.incrementResendCount();
                        send(msg);
                    }
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(resendMessages, 0, 1, TimeUnit.SECONDS);
    }

    public void send(MessageWrapper msg) {
        if (!toSend.contains(msg)) {
            toSend.add(msg);
        }
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(msg);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] data = bStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, msg.getMessage().getToAddress(),
                msg.getMessage().getToPort());
        try {
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(Message m) {
        MessageWrapper wrapper = new MessageWrapper(m, this.id);
        if (!toSend.contains(wrapper)) {
            toSend.add(wrapper);
        }
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(wrapper);
            oos.flush();
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] data = bStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, m.getToAddress(), m.getToPort());
        try {
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MessageWrapper receive() {
        MessageWrapper wrapper = null;
        byte[] buf = new byte[10240];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
            ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
            wrapper = (MessageWrapper) iStream.readObject();
            iStream.close();
            if (wrapper.getMessage().getType() == MessageType.ACK) {
                if (wrapper.verifySignature() && toSend.contains(wrapper)) {
                    toSend.remove(wrapper);
                }
                return null;
            }
        } catch (SocketTimeoutException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wrapper;
    }

    public void close() {
        socket.close();
    }
}

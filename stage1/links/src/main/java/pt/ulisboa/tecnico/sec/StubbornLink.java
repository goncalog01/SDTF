package pt.ulisboa.tecnico.sec;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class StubbornLink {

    private DatagramSocket socket;
    private ConcurrentLinkedQueue<MessageWrapper> toSend = new ConcurrentLinkedQueue<MessageWrapper>();

    public StubbornLink() {
        try {
            this.socket = new DatagramSocket();
            socket.setSoTimeout(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runnable resendMessages = new Runnable() {
            public void run() {
                for (MessageWrapper msg : toSend) {
                    send_text(msg);
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(resendMessages, 0, 5, TimeUnit.SECONDS);
    }

    public StubbornLink(int port) {
        try {
            this.socket = new DatagramSocket(port);
            socket.setSoTimeout(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runnable resendMessages = new Runnable() {
            public void run() {
                for (MessageWrapper msg : toSend) {
                    send_text(msg);
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(resendMessages, 0, 5, TimeUnit.SECONDS);
    }

    public StubbornLink(int port, InetAddress address) {
        try {
            this.socket = new DatagramSocket(port, address);
            socket.setSoTimeout(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runnable resendMessages = new Runnable() {
            public void run() {
                for (MessageWrapper msg : toSend) {
                    send_text(msg);
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(resendMessages, 0, 5, TimeUnit.SECONDS);
    }

    public void send_text(MessageWrapper msg) {
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

    public void send_text(Message msg, byte[] sign) {
        MessageWrapper wrapper = new MessageWrapper(msg, sign);
        if (!toSend.contains(wrapper)) {
            toSend.add(wrapper);
        }
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(wrapper);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] data = bStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, msg.getToAddress(), msg.getToPort());
        try {
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send_text(String msg, int senderId, int seqNum, InetAddress toAddress, int toPort,
            InetAddress fromAddress, int fromPort) {
        Message m = new Message(MessageType.TEXT, senderId, seqNum, toAddress, toPort, fromAddress,
                fromPort, msg);
        MessageWrapper wrapper = new MessageWrapper(m, null);
        if (!toSend.contains(wrapper)) {
            toSend.add(wrapper);
        }
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(wrapper);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] data = bStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, toAddress, toPort);
        try {
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send_ack(int senderId, int seqNum, InetAddress toAddress, int toPort, InetAddress fromAddress,
            int fromPort) {
        Message m = new Message(MessageType.ACK, senderId, seqNum, toAddress, toPort, fromAddress,
                fromPort, "");
        MessageWrapper wrapper = new MessageWrapper(m, null);
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
        DatagramPacket packet = new DatagramPacket(data, data.length, toAddress, toPort);
        try {
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MessageWrapper receive() {
        MessageWrapper wrapper = null;
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
            ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
            wrapper = (MessageWrapper) iStream.readObject();
            iStream.close();
            if (wrapper.getMessage().getType() == MessageType.ACK && toSend.contains(wrapper)) {
                toSend.remove(wrapper);
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

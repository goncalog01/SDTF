package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;

public class PrePrepareMessage extends Message {

    private int instance;
    private int round;
    private Block block;

    public PrePrepareMessage(int senderId, int seqNum, InetAddress toAddress, int toPort,
                             InetAddress fromAddress, int fromPort, int instance, int round, Block block) {
        super(MessageType.PRE_PREPARE, senderId, seqNum, toAddress, toPort, fromAddress, fromPort);
        this.instance = instance;
        this.round = round;
        this.block = block;
    }

    public int getInstance() {
        return this.instance;
    }

    public int getRound() {
        return this.round;
    }

    public Block getBlock() {
        return this.block;
    }
}

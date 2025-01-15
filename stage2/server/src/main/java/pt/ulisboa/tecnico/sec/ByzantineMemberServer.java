package pt.ulisboa.tecnico.sec;

import java.util.ArrayList;

public class ByzantineMemberServer extends MemberServer {

    private String mode;

    public ByzantineMemberServer(int serverId, int port, int isLeader, int leaderId, String configFilePath, String mode) {
        super(serverId, port, isLeader, leaderId, configFilePath);
        this.mode = mode;
    }

    @Override
    public void checkPrepareQuorum(PrepareMessage m) {
        switch (this.mode) {
            case "silent" -> System.out.println("Silent");
            case "spam" -> {
                for (int i = 0; i < 10; i++) {
                    broadcastCommit(m);
                }
            }
            case "mischief" -> {
                PrepareMessage byzantineMsg = new PrepareMessage(m.getSenderId(), m.getSeqNum(), m.getToAddress(), m.getToPort(), m.getFromAddress(), m.getFromPort(), m.getInstance(), m.getRound(), new Block(new ArrayList<>()));
                broadcastCommit(byzantineMsg);
            }
            case "chameleon" -> {
                for (int i = 1; i < 4; i++) {
                    PrepareMessage byzantineMsg = new PrepareMessage(i, m.getSeqNum(), m.getToAddress(), m.getToPort(), m.getFromAddress(), m.getFromPort(), m.getInstance(), m.getRound(), new Block(new ArrayList<>()));
                    broadcastCommit(byzantineMsg);
                }
            }
            default -> System.out.println("Unsupported byzantine type");
        }
    }

    @Override
    public void checkBalance(CheckBalanceMessage msg) {
        switch (this.mode) {
            case "silent" -> System.out.println("Silent");
            case "spam", "chameleon" -> {
                for (int i = 0; i < 10; i++) {
                    sendClientResponse(msg, "Check balance failed: account does not exist");
                }
            }
            case "mischief" -> sendClientResponse(msg, "Check balance failed: account does not exist");
            default -> System.out.println("Unsupported byzantine type");
        }
    }

    @Override
    public void checkBalanceWeak(WeakReadMessage msg) {
        switch (this.mode) {
            case "silent" -> System.out.println("Silent");
            case "spam", "chameleon" -> {
                AccountSnapshotValidated account = this.snapshotValidated.get(msg.getKey());
                WeakReadResponseMessage m = new WeakReadResponseMessage(this.serverId, this.seqNum, msg.getFromAddress(), msg.getFromPort(),
                        msg.getToAddress(), msg.getToPort(), "Balance: " + account.getBalance(), msg.getSeqNum(), account);
                for (int i = 0; i < 10; i++) {
                    this.socket.send(m);
                }
                this.seqNum++;
            }
            case "mischief" -> sendClientResponse(msg, "Check balance failed: account does not exist");
            default -> System.out.println("Unsupported byzantine type");
        }
    }

}
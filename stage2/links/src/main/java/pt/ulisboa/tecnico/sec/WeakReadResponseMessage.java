package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;

public class WeakReadResponseMessage extends ServerResponseMessage {

    private AccountSnapshotValidated snapshot;

    public WeakReadResponseMessage(int senderId, int seqNum, InetAddress toAddress, int toPort,
                                   InetAddress fromAddress, int fromPort, String message, int seqNumToRespond, AccountSnapshotValidated snapshot) {
        super(MessageType.WEAK_READ_RESPONSE, senderId, seqNum, toAddress, toPort, fromAddress, fromPort, message, seqNumToRespond);
        this.snapshot = snapshot;
    }


    public AccountSnapshotValidated getSnapshot() {
        return this.snapshot;
    }
}
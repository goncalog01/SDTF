package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;
import java.security.PublicKey;
import java.util.*;

public class SnapshotMessage extends Message {

    private Map<PublicKey, AccountSnapshot> snapshot;
    private int instance;

    public SnapshotMessage(int senderId, int seqNum, InetAddress toAddress, int toPort,
                           InetAddress fromAddress, int fromPort, Map<PublicKey, AccountSnapshot> snapshot, int instance) {
        super(MessageType.SNAPSHOT, senderId, seqNum, toAddress, toPort, fromAddress, fromPort);
        this.snapshot = snapshot;
        this.instance = instance;
    }

    public Map<PublicKey, AccountSnapshot> getSnapshot() {
        return this.snapshot;
    }

    public int getInstance() {
        return this.instance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SnapshotMessage msg = (SnapshotMessage) o;
        if (this.getInstance() != msg.getInstance()) {
            return false;
        }
        for (Map.Entry<PublicKey, AccountSnapshot> entry : snapshot.entrySet()) {
            if (entry.getValue().getBalance() != msg.getSnapshot().get(entry.getKey()).getBalance()) {
                return false;
            }
        }
        return true;
    }

}

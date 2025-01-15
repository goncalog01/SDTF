package pt.ulisboa.tecnico.sec;

import java.net.InetAddress;

public class ServerResponseMessage extends Message {

    private String message;
    private int seqNumToRespond;

    public ServerResponseMessage(int senderId, int seqNum, InetAddress toAddress, int toPort,
                                 InetAddress fromAddress, int fromPort, String message, int seqNumToRespond) {
        super(MessageType.SERVER_RESPONSE, senderId, seqNum, toAddress, toPort, fromAddress, fromPort);
        this.message = message;
        this.seqNumToRespond = seqNumToRespond;
    }

    public ServerResponseMessage(MessageType type, int senderId, int seqNum, InetAddress toAddress, int toPort,
                                 InetAddress fromAddress, int fromPort, String message, int seqNumToRespond) {
        super(type, senderId, seqNum, toAddress, toPort, fromAddress, fromPort);
        this.message = message;
        this.seqNumToRespond = seqNumToRespond;
    }

    public String getMessage() {
        return message;
    }

    public int getSeqNumToRespond() {
        return seqNumToRespond;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + seqNumToRespond;
        return result;
    }

}

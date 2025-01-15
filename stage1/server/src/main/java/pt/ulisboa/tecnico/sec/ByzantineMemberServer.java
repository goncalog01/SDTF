package pt.ulisboa.tecnico.sec;

import java.util.*;

public class ByzantineMemberServer extends MemberServer {

    private String mode;

    public ByzantineMemberServer(int serverId, int port, int isLeader, int leaderId, String mode) {
        super(serverId, port, isLeader, leaderId);
        this.mode = mode;
    }

    @Override
    public void commitPrepare(Message m) {
        // pegar na mensagem e atualizar o valor do pri e pvi
        String[] quorum_msg = m.getMsg().split(",");

        this.pri = Integer.parseInt(quorum_msg[2]);
        Integer[] id = { Integer.parseInt(quorum_msg[3]), Integer.parseInt(quorum_msg[4]) };
        this.inputValuei_Id = Collections.unmodifiableList(Arrays.asList(id));
        this.inputValuei_Val = quorum_msg[5];

        if (this.mode.equals("silent")) {
            System.out.println("silent");

        } else if (this.mode.equals("spam")) {
            String msg = "COMMIT," + quorum_msg[1] + "," + String.valueOf(this.pri)
                    + "," + String.valueOf(this.inputValuei_Id.get(0)) + ","
                    + String.valueOf(this.inputValuei_Id.get(1)) + "," + this.inputValuei_Val;

            System.out.println("spam:" + msg);
            broadcast(msg);
            broadcast(msg);
        } else if (this.mode.equals("mischief")) {
            String msg = "COMMIT," + quorum_msg[1] + "," + String.valueOf(this.pri)
                    + "," + String.valueOf(this.inputValuei_Id.get(0)) + ","
                    + String.valueOf(this.inputValuei_Id.get(1)) + "," + this.inputValuei_Val + "COMMIT_BYZ";

            System.out.println("replace val:" + msg);
            broadcast(msg);
        }

    }

}
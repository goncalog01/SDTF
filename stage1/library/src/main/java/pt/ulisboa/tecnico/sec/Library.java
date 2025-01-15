package pt.ulisboa.tecnico.sec;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Library {

    private int clientId;
    private int seqNum = 1;
    private int port;
    private InetAddress address;
    private PerfectLink socket;
    private ArrayList<Integer> membersPorts = new ArrayList<Integer>();

    public Library(int clientId, int port) {
        this.clientId = clientId;
        this.port = port;

        try {
            this.address = InetAddress.getByName("localhost");
            this.socket = new PerfectLink(port, address);
            File file = new File("config/configFile.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;

            while ((st = br.readLine()) != null) {
                String[] serverInfo = st.split(",");
                if (serverInfo[0].equals("member")) {
                    int memberPort = Integer.parseInt(serverInfo[2]);
                    membersPorts.add(memberPort);
                }
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Runnable receiveMessage = new Runnable() {
            public void run() {
                MessageWrapper wrapper = socket.receive();
                if (wrapper != null) {
                    //System.out.println();
                    System.out.println(wrapper.getMessage().getMsg());
                    //System.out.print("Enter string to append to the blockchain: ");
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(receiveMessage, 0, 1, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> socket.close()));
    }

    public void add_blockchain(String msg) {
        for (int p : membersPorts) {
            socket.send("ADDBLOCK," + msg, clientId, seqNum, address, p, address, port);
        }
        seqNum++;
    }
}

package pt.ulisboa.tecnico.sec;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import java.nio.file.*;
import java.security.*;
import java.security.spec.*;

public class MemberServer extends Thread {

    protected int consensusQuorum = 0;

    // server attributes
    protected PerfectLink socket;
    protected InetAddress address;
    protected int port;
    protected int seqNum = 1;
    protected boolean running;

    protected int serverId = 0;
    protected int isLeader = 0;
    protected int isByzantine = 0;
    protected int leaderId = 0;
    protected ArrayList<Integer> membersPorts = new ArrayList<Integer>();

    protected int consensusCnt = 0;
    protected Message currentConsensusMessage;

    Map<List<Integer>, String> blockchain_buffer = new HashMap<List<Integer>, String>();
    Map<List<Integer>, String> blockchain = Collections.synchronizedMap(blockchain_buffer);
    
    protected CopyOnWriteArrayList<Message> queue = new CopyOnWriteArrayList<Message>();

    protected CopyOnWriteArrayList<Message> prepareQuorumMessages = new CopyOnWriteArrayList<Message>();

    protected CopyOnWriteArrayList<Message> commitQuorumMessages = new CopyOnWriteArrayList<Message>();

    // consensus attributes
    protected int currentConsensusInstance = 0;
    protected int pri;
    protected String pvi;
    protected List<Integer> inputValuei_Id;
    protected String inputValuei_Val;

    protected boolean consensusRunning;

    protected long startTime;
    protected long endTime;

    public MemberServer(int serverId, int port, int isLeader, int leaderId) {
        try {
            this.serverId = serverId;
            this.isLeader = isLeader;
            this.leaderId = leaderId;

            this.address = InetAddress.getByName("localhost");
            this.port = port;
            this.socket = new PerfectLink(port, address);

            File file = new File("config/configFile.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;

            while ((st = br.readLine()) != null) {
                String[] processInfo = st.split(",");
                if (processInfo[0].equals("member")) {
                    int memberPort = Integer.parseInt(processInfo[2]);
                    membersPorts.add(memberPort);
                }
            }

            br.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> socket.close()));
    }

    public void run() {
        running = true;

        int numServers = membersPorts.size();

        int faults = (numServers - 1) / 3;

        consensusQuorum = 2 * faults + 1;

        Thread checkQueue = new Thread(() -> {
            if (this.isLeader == 1) {
                checkQueueHandling();
            }
        });
        checkQueue.start();

        while (running) {
            try {
                MessageWrapper msgWrapper = socket.receive();

                if (msgWrapper == null) {
                    continue;
                }

                Message m = msgWrapper.getMessage();

                String received = m.getMsg();

                // handle message received function
                Thread handleReceived = new Thread(() -> {
                    handleRequest(msgWrapper);
                });
                handleReceived.start();

                System.out.println("Server " + serverId + " received: " + received + " from server " + m.getSenderId());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }

    protected void checkQueueHandling() {
        int current_instance = this.currentConsensusInstance;

        while (this.isLeader == 1) {
            if (!this.consensusRunning) {
                this.consensusCnt++;

                if (queue.size() == 0) {
                    continue;
                }

                Message first_entry = queue.get(0);
                queue.remove(0);

                Integer[] id = { first_entry.getSenderId(), first_entry.getSeqNum() };

                startInstance(first_entry.getMsg().substring(9), Collections.unmodifiableList(Arrays.asList(id)),
                        first_entry);
            }
        }
    }

    // fazer processing de requests
    public void handleRequest(MessageWrapper msgWrapper) {
        Message m = msgWrapper.getMessage();
        String message = m.getMsg();

        String[] messageArgs = message.split(",");

        String command = messageArgs[0];

        switch (command) {
            case "ADDBLOCK":
                addToQueue(m);

                break;
            case "PRE-PREPARE":
                if (!verifySignature(m, msgWrapper.getSignature())) {
                    return;
                }

                checkPrePrepare(m);

                break;
            case "PREPARE":
                if (!verifySignature(m, msgWrapper.getSignature())) {
                    return;
                }

                addPrepareQuorum(m);

                break;
            case "COMMIT":
                if (!verifySignature(m, msgWrapper.getSignature())) {
                    return;
                }

                addCommitQuorum(m);

                break;
        }
    }

    public int compareMessageStrings(Message m1, Message m2) {
        String[] msg1 = m1.getMsg().split(",");
        String[] msg2 = m2.getMsg().split(",");

        for (int i = 0; i < msg1.length; i++) {
            if (!msg1[i].equals(msg2[i])) {
                return 0;
            }
        }

        return 1;
    }

    public boolean inBlockchain(Message m) {
        synchronized (this.blockchain) {
            Integer[] arr = { m.getSenderId(), m.getSeqNum() };
            List<Integer> id = Collections.unmodifiableList(Arrays.asList(arr));
            return this.blockchain.containsKey(id);
        }
    }

    public void addToQueue(Message m) {
        if (queue.contains(m) ||
                inBlockchain(m)) {
            return;
        }
        if (currentConsensusInstance > 0 && currentConsensusMessage != null && currentConsensusMessage.equals(m)) {
            return;
        }

        queue.add(m);
    }

    public boolean verifyPrepareSender(Message m) {
        for (Message msg : this.prepareQuorumMessages) {
            if (msg.getSenderId() == m.getSenderId() && msg.getMsg().equals(m.getMsg())) {
                return false;
            }
        }

        return true;
    }

    public void addPrepareQuorum(Message message) {
        if (!verifyPrepareSender(message)) {
            return;
        }


        prepareQuorumMessages.add(message);

        checkPrepare(message);
    }

    public boolean verifyCommitSender(Message m) {
        for (Message msg : this.commitQuorumMessages) {
            if (msg.getSenderId() == m.getSenderId() && msg.getMsg().equals(m.getMsg())) {
                return false;
            }
        }

        return true;
    }

    public void addCommitQuorum(Message message) {
        if (!verifyCommitSender(message)) {
            return;
        }

        commitQuorumMessages.add(message);

        checkCommit(message);
    }

    public synchronized void startInstance(String value, List<Integer> id, Message m) {

        this.currentConsensusInstance = this.consensusCnt;
        this.pri = 1;
        this.startTime = System.currentTimeMillis();
        this.endTime = exponentialRoundFunction(this.startTime);

        this.inputValuei_Val = value;
        this.inputValuei_Id = id;

        // maybe meter sincronizado
        this.consensusRunning = true;

        prepareQuorumMessages.clear();
        commitQuorumMessages.clear();

        this.currentConsensusMessage = m;

        if (this.isLeader == 1) {
            String msg = "PRE-PREPARE," + String.valueOf(this.currentConsensusInstance) + "," + String.valueOf(this.pri)
                    + "," + String.valueOf(this.inputValuei_Id.get(0)) + ","
                    + String.valueOf(this.inputValuei_Id.get(1)) + "," + this.inputValuei_Val;

            broadcast(msg);

            // TODO set timer talvez???

        }
    }

    public void checkPrePrepare(Message msg) {
        if (msg.getSenderId() != this.leaderId) {
            return;
        }

        // timer para running ??

        String[] msgArgs = msg.getMsg().split(",");

        String m = "PREPARE," + msgArgs[1] + "," + msgArgs[2]
                + "," + msgArgs[3] + "," + msgArgs[4] + "," + msgArgs[5];

        broadcast(m);
    }

    public void checkPrepare(Message m) {
        int counter = 0;

        for (Message message : this.prepareQuorumMessages) {
            int equal = compareMessageStrings(m, message);

            if (equal == 1) {
                counter++;
            }
        }

        if (counter == consensusQuorum) {
            commitPrepare(m);
        }
    }

    public void commitPrepare(Message m) {
        // pegar na mensagem e atualizar o valor do pri e pvi
        String[] quorum_msg = m.getMsg().split(",");

        this.pri = Integer.parseInt(quorum_msg[2]);
        Integer[] id = { Integer.parseInt(quorum_msg[3]), Integer.parseInt(quorum_msg[4]) };
        this.inputValuei_Id = Collections.unmodifiableList(Arrays.asList(id));
        this.inputValuei_Val = quorum_msg[5];

        String msg = "COMMIT," + quorum_msg[1] + "," + String.valueOf(this.pri)
                + "," + String.valueOf(this.inputValuei_Id.get(0)) + "," + String.valueOf(this.inputValuei_Id.get(1))
                + "," + this.inputValuei_Val;

        broadcast(msg);
    }

    public void checkCommit(Message m) {
        int counter = 0;

        for (Message message : this.commitQuorumMessages) {
            int equal = compareMessageStrings(m, message);

            if (equal == 1) {
                counter++;
            }
        }

        if (counter == consensusQuorum) {
            quorumCommit();
        }
    }

    public void quorumCommit() {

        decide(this.currentConsensusInstance, this.inputValuei_Val, this.inputValuei_Id);
    }

    public synchronized void decide(int currentConsensusInstance, String value_Val, List<Integer> value_Id) {

        blockchain.put(value_Id, value_Val);
        System.out.println("Server " + serverId + " added to blockchain: " + value_Val);

        String msg = "String \"" + value_Val + "\" added to the blockchain";

        if (this.isLeader == 1) {
            socket.send(msg, serverId, seqNum, currentConsensusMessage.getFromAddress(),
                    currentConsensusMessage.getFromPort(), address, port);
            seqNum++;
        }

        this.currentConsensusInstance = -1;
        this.consensusRunning = false;
    }

    protected long exponentialRoundFunction(long value) {
        return (long) Math.exp((double) value);
    }

    protected void broadcast(String msg) {
        for (int p : membersPorts) {
            Message m = new Message(MessageType.TEXT, serverId, seqNum, address, p, address, port, msg);

            byte[] signature = generateSignature(m);
            socket.send(m, signature);
        }
        seqNum++;
    }

    protected String getKeyPath(boolean isPrivateKey, int sendId) {

        switch (sendId) {
            case 1:

                if (isPrivateKey) {
                    return "keys/server1/private_key_server1.der";
                } else {
                    return "keys/server1/public_key_server1.der";
                }

            case 2:

                if (isPrivateKey) {
                    return "keys/server2/private_key_server2.der";
                } else {
                    return "keys/server2/public_key_server2.der";
                }

            case 3:

                if (isPrivateKey) {
                    return "keys/server3/private_key_server3.der";
                } else {
                    return "keys/server3/public_key_server3.der";
                }

            case 4:

                if (isPrivateKey) {
                    return "keys/server4/private_key_server4.der";
                } else {
                    return "keys/server4/public_key_server4.der";
                }

            default:
                return "";
        }
    }

    public static PrivateKey getPrivateKey(String filename) throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(spec);
    }

    protected PublicKey getPublicKey(String filename) throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(spec);
    }

    protected byte[] generateSignature(Message m) {
        byte[] signedData = null;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");

            String keyPath = getKeyPath(true, this.serverId);

            PrivateKey key = getPrivateKey(keyPath);

            // buscar chave do ficheiro
            signature.initSign(key);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(m);
            oos.close();

            byte[] data = bStream.toByteArray();

            // Add data to be signed
            signature.update(data);

            // Generate the signature
            signedData = signature.sign();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return signedData;
    }

    public boolean verifySignature(Message m, byte[] signedData) {
        boolean isValid = false;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");

            int sendId = m.getSenderId();

            String keyPath = getKeyPath(false, sendId);

            PublicKey key = getPublicKey(keyPath);

            signature.initVerify(key);

            ByteArrayOutputStream bStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bStream);
            oos.writeObject(m);
            oos.close();

            byte[] data = bStream.toByteArray();

            signature.update(data);

            isValid = signature.verify(signedData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isValid;
    }

}

package pt.ulisboa.tecnico.sec;

import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;

public class Library {

    private int clientId;
    private int seqNum = 1;
    private int port;
    private InetAddress address;
    private PerfectLink socket;
    private ArrayList<Integer> membersPorts = new ArrayList<Integer>();
    private int faults;
    private Map<Integer, List<ServerResponseMessage>> responses = new ConcurrentHashMap<>();
    private Map<Integer, Integer> readsSeqNums = new ConcurrentHashMap<>();

    public Library(int clientId, int port, String configFilePath) {
        this.clientId = clientId;
        this.port = port;

        try {
            this.address = InetAddress.getByName("localhost");
            this.socket = new PerfectLink(port, address, this.clientId);
            File file = new File(configFilePath);
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

        int numServers = membersPorts.size();

        this.faults = (numServers - 1) / 3;

        Runnable receiveMessage = new Runnable() {
            public void run() {
                while (true) {
                    MessageWrapper wrapper = socket.receive();
                    if (wrapper != null && (wrapper.getMessage().getType() == MessageType.SERVER_RESPONSE || wrapper.getMessage().getType() == MessageType.WEAK_READ_RESPONSE)) {
                        ServerResponseMessage serverResponseMessage = (ServerResponseMessage) wrapper.getMessage();
                        if (responses.containsKey(serverResponseMessage.getSeqNumToRespond())) {
                            responses.get(serverResponseMessage.getSeqNumToRespond()).add(serverResponseMessage);
                            if (!readsSeqNums.containsKey(serverResponseMessage.getSeqNumToRespond()))
                                checkResponses(serverResponseMessage);
                        }
                    }
                }
            }
        };
        Thread thread = new Thread(receiveMessage);
        thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> socket.close()));
    }

    public void checkResponses(ServerResponseMessage serverResponseMessage) {
        int counter = 0;
        for (ServerResponseMessage m : responses.get(serverResponseMessage.getSeqNumToRespond())) {
            if (m.getSenderId() != serverResponseMessage.getSenderId() && m.getMessage().equals(serverResponseMessage.getMessage())) {
                counter++;
            }
        }

        if (counter == this.faults + 1) {
            System.out.println("\n\n" + serverResponseMessage.getMessage() + "\n");
            System.out.print("Select operation\n1: Create account\n2: Transfer\n3: Check balance\n> ");
            responses.remove(serverResponseMessage.getSeqNumToRespond());
        }
    }

    public void createAccount() {
        String keyPath = Utils.getKeyPath(false, this.clientId);
        try {
            PublicKey key = Utils.getPublicKey(keyPath);
            for (int p : membersPorts) {
                CreateAccountMessage m = new CreateAccountMessage(this.clientId, this.seqNum, this.address, p, this.address, this.port, key);
                socket.send(m);
            }
            responses.put(this.seqNum, new ArrayList<>());
            seqNum++;
        } catch (IOException e) {
            System.out.println("Invalid id");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void transfer(float amount, int id) {
        if (id <= membersPorts.size()) {
            System.out.println("Invalid clientId");
            return;
        }
        String sourceKeyPath = Utils.getKeyPath(false, this.clientId);
        String destinationKeyPath = Utils.getKeyPath(false, id);
        try {
            PublicKey sourceKey = Utils.getPublicKey(sourceKeyPath);
            PublicKey destinationKey = Utils.getPublicKey(destinationKeyPath);
            for (int p : membersPorts) {
                TransferMessage m = new TransferMessage(this.clientId, this.seqNum, this.address, p, this.address, this.port, this.clientId, sourceKey, id, destinationKey, amount, false);
                socket.send(m);
            }
            responses.put(this.seqNum, new ArrayList<>());
            seqNum++;
        } catch (IOException e) {
            System.out.println("Invalid id");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void waitStrongReadResponses(int seqNum) {
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds
        while (true) {
            try {
                Map<String, ArrayList<Integer>> frequencyMap = new HashMap<>();
                for (ServerResponseMessage response : responses.get(seqNum)) {
                    if (frequencyMap.containsKey(response.getMessage())) {
                        if (!frequencyMap.get(response.getMessage()).contains(response.getSenderId()))
                            frequencyMap.get(response.getMessage()).add(response.getSenderId());
                    } else {
                        frequencyMap.put(response.getMessage(), new ArrayList<>());
                        frequencyMap.get(response.getMessage()).add(response.getSenderId());
                    }
                }
                for (Map.Entry<String, ArrayList<Integer>> entry : frequencyMap.entrySet()) {
                    String key = entry.getKey();
                    ArrayList<Integer> value = entry.getValue();
                    if (value.size() >= 2 * this.faults + 1) {
                        System.out.println("\n\n" + key + "\n");
                        System.out.print("Select operation\n1: Create account\n2: Transfer\n3: Check balance\n> ");
                        responses.remove(seqNum);
                        readsSeqNums.remove(seqNum);
                        return;
                    }
                }
                // Check if the timeout has been reached
                if (System.currentTimeMillis() - startTime > timeout) {
                    responses.remove(seqNum);
                    readsSeqNums.remove(seqNum);
                    stronglyConsistentRead();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stronglyConsistentRead() {
        String keyPath = Utils.getKeyPath(false, this.clientId);
        try {
            PublicKey key = Utils.getPublicKey(keyPath);
            for (int p : membersPorts) {
                StrongReadMessage m = new StrongReadMessage(this.clientId, this.seqNum, this.address, p, this.address, this.port, key);
                socket.send(m);
            }
            responses.put(this.seqNum, new CopyOnWriteArrayList<>());
            readsSeqNums.put(this.seqNum, this.seqNum);
            int seqNumber = this.seqNum;
            Runnable handleStrongReadResponses = new Runnable() {
                public void run() {
                    waitStrongReadResponses(seqNumber);
                }
            };
            Thread thread = new Thread(handleStrongReadResponses);
            thread.start();

            seqNum++;
        } catch (IOException e) {
            System.out.println("Invalid id");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void waitWeakReadResponses(int seqNum) {
        while (true) {
            try {
                if (responses.get(seqNum).size() > 0) {
                    ServerResponseMessage response = responses.get(seqNum).get(0);
                    if (response.getType() == MessageType.WEAK_READ_RESPONSE) {
                        WeakReadResponseMessage weakReadResponseMessage = (WeakReadResponseMessage) response;
                        if (weakReadResponseMessage.getSnapshot().getSignatures().size() < 2 * this.faults + 1) {
                            System.out.println("\n\nCan't verify Weakly Consistent Read\n");
                            return;
                        }
                        for (Map.Entry<Integer, byte[]> entry : weakReadResponseMessage.getSnapshot().getSignatures().entrySet()) {
                            if (!Utils.verifySignature(entry.getKey(), weakReadResponseMessage.getSnapshot().getKey(), weakReadResponseMessage.getSnapshot().getBalance(), entry.getValue())) {
                                System.out.println("\n\nCan't verify Weakly Consistent Read\n");
                                System.out.print("Select operation\n1: Create account\n2: Transfer\n3: Check balance\n> ");
                                responses.remove(seqNum);
                                readsSeqNums.remove(seqNum);
                                return;
                            }
                        }
                        System.out.println("\n\n" + weakReadResponseMessage.getMessage() + "\n");
                        System.out.print("Select operation\n1: Create account\n2: Transfer\n3: Check balance\n> ");
                        responses.remove(seqNum);
                        readsSeqNums.remove(seqNum);
                        return;
                    } else {
                        responses.get(seqNum).remove(0);
                        continue;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void weaklyConsistentRead() {
        String keyPath = Utils.getKeyPath(false, this.clientId);
        try {
            PublicKey key = Utils.getPublicKey(keyPath);
            for (int p : membersPorts) {
                WeakReadMessage m = new WeakReadMessage(this.clientId, this.seqNum, this.address, p, this.address, this.port, key);
                socket.send(m);
            }
            responses.put(this.seqNum, new CopyOnWriteArrayList<>());
            readsSeqNums.put(this.seqNum, this.seqNum);
            int seqNumber = this.seqNum;
            Runnable handleWeakReadResponses = new Runnable() {
                public void run() {
                    waitWeakReadResponses(seqNumber);
                }
            };
            Thread thread = new Thread(handleWeakReadResponses);
            thread.start();

            seqNum++;
        } catch (IOException e) {
            System.out.println("Invalid id");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

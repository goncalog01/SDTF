package pt.ulisboa.tecnico.sec;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class MemberServer extends Thread {

    // server attributes
    protected PerfectLink socket;
    private InetAddress address;
    private int port;
    protected int seqNum = 1;
    protected int serverId;
    private boolean isLeader;
    private int leaderId;
    private ArrayList<Integer> membersPorts = new ArrayList<Integer>();
    private float fee = 5;
    private int blockSize = 5;

    private Map<Integer, Block> blockchain_buffer = new HashMap<Integer, Block>();
    private Map<Integer, Block> blockchain = Collections.synchronizedMap(blockchain_buffer);

    private Map<PublicKey, Float> accounts_buffer = new HashMap<PublicKey, Float>();
    private Map<PublicKey, Float> accounts = Collections.synchronizedMap(accounts_buffer);

    private ArrayList<Message> queue = new ArrayList<Message>();

    // consensus attributes
    private int currentInstance = 0;
    private int currentRound = 1;
    private int consensusQuorum;
    private ArrayList<PrepareMessage> prepareQuorumMessages = new ArrayList<PrepareMessage>();
    private ArrayList<CommitMessage> commitQuorumMessages = new ArrayList<CommitMessage>();
    private Map<Integer, Timer> timers = new HashMap<Integer, Timer>();


    private Map<PublicKey, AccountSnapshot> currentSnapshot = new HashMap<PublicKey, AccountSnapshot>();
    protected Map<PublicKey, AccountSnapshotValidated> snapshotValidated = new HashMap<PublicKey, AccountSnapshotValidated>();

    private ArrayList<SnapshotMessage> snapshotQuorumMessages = new ArrayList<SnapshotMessage>();

    public MemberServer(int serverId, int port, int isLeader, int leaderId, String configFilePath) {
        try {
            this.serverId = serverId;
            this.isLeader = isLeader == 1;
            this.leaderId = leaderId;

            this.address = InetAddress.getByName("localhost");
            this.port = port;
            this.socket = new PerfectLink(port, address, this.serverId);

            File file = new File(configFilePath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;

            while ((st = br.readLine()) != null) {
                String[] processInfo = st.split(",");
                if (processInfo[0].equals("member")) {
                    int memberPort = Integer.parseInt(processInfo[2]);
                    this.membersPorts.add(memberPort);
                }
            }

            br.close();

            accounts.put(Utils.getPublicKey(Utils.getKeyPath(false, this.leaderId)), (float) 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }

        int numServers = membersPorts.size();

        int faults = (numServers - 1) / 3;

        this.consensusQuorum = 2 * faults + 1;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> socket.close()));
    }

    public void run() {
        if (this.isLeader) {
            Thread checkQueue = new Thread(() -> {
                checkQueueHandling();
            });
            checkQueue.start();
        }
        while (true) {
            try {
                MessageWrapper msgWrapper = socket.receive();

                if (msgWrapper == null) {
                    continue;
                }

                // handle message received function
                Thread handleReceived = new Thread(() -> {
                    handleRequest(msgWrapper);
                });
                handleReceived.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void checkQueueHandling() {
        while (true) {
            ArrayList<Message> operations;
            synchronized (this.queue) {
                if (this.queue.size() < this.blockSize) { //now 2 clients are needed to test block size = 2
                    continue;
                }

                operations = new ArrayList<>(this.queue.subList(0, this.blockSize)); //now 2 clients are needed to test block size = 2
                this.queue.removeAll(operations);
            }
            operations = orderOperations(operations);
            startInstance(operations);
        }
    }

    public ArrayList<Message> orderOperations(ArrayList<Message> operations) {
        ArrayList<Message> orderedOperations = new ArrayList<>();
        for (Message operation : operations) {
            switch (operation.getType()) {
                case CREATE_ACCOUNT -> orderedOperations.add(0, operation);
                case TRANSFER -> {
                    orderedOperations.add(operation);
                    TransferMessage o = (TransferMessage) operation;
                    try {
                        // add transfer to pay fee to leader
                        orderedOperations.add(new TransferMessage(o.getSenderId(), seqNum, address, o.getToPort(), address, o.getFromPort(), o.getSourceId(), o.getSourceKey(), this.leaderId, Utils.getPublicKey(Utils.getKeyPath(false, this.leaderId)), this.fee, true));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                default -> System.out.println("Unexpected message type while ordering operations");
            }
        }
        return orderedOperations;
    }

    // fazer processing de requests
    public void handleRequest(MessageWrapper msgWrapper) {
        Message m = msgWrapper.getMessage();

        switch (m.getType()) {
            case PRE_PREPARE -> {
                PrePrepareMessage prePrepareMsg = (PrePrepareMessage) m;
                checkPrePrepare(prePrepareMsg);
            }
            case PREPARE -> {
                PrepareMessage prepareMsg = (PrepareMessage) m;
                addPrepareQuorum(prepareMsg);
            }
            case COMMIT -> {
                CommitMessage commitMsg = (CommitMessage) m;
                addCommitQuorum(commitMsg);
            }
            case STRONG_READ -> {
                StrongReadMessage strongReadMessage = (StrongReadMessage) m;
                if (!checkSourceKey(strongReadMessage.getSenderId(), strongReadMessage.getKey())) {
                    break;
                }
                synchronized (this.accounts) {
                    checkBalance(strongReadMessage);
                }
            }
            case WEAK_READ -> {
                WeakReadMessage weakReadMessage = (WeakReadMessage) m;
                if (!checkSourceKey(weakReadMessage.getSenderId(), weakReadMessage.getKey())) {
                    break;
                }
                synchronized (this.snapshotValidated) {
                    checkBalanceWeak(weakReadMessage);
                }
            }
            case SNAPSHOT -> {
                SnapshotMessage snapshotMessage = (SnapshotMessage) m;
                addSnapshotQuorum(snapshotMessage);
            }
            case CREATE_ACCOUNT, TRANSFER -> addToQueue(m);
            default -> System.out.println("Unexpected message type while handling requests");
        }
    }

    public void addToQueue(Message m) {
        synchronized (this.queue) {
            if (this.queue.contains(m)) {
                return;
            }
            this.queue.add(m);
        }
    }

    public boolean verifyPrepareSender(PrepareMessage m) {
        for (PrepareMessage msg : this.prepareQuorumMessages) {
            if (m.getInstance() == msg.getInstance() && m.getRound() == msg.getRound() && m.getSenderId() == msg.getSenderId()) {
                return false;
            }
        }
        return true;
    }

    public void addPrepareQuorum(PrepareMessage message) {
        synchronized (this.prepareQuorumMessages) {
            if (!verifyPrepareSender(message)) {
                return;
            }

            this.prepareQuorumMessages.add(message);

            checkPrepareQuorum(message);
        }
    }

    public boolean verifyCommitSender(CommitMessage m) {
        for (CommitMessage msg : this.commitQuorumMessages) {
            if (m.getInstance() == msg.getInstance() && m.getRound() == msg.getRound() && m.getSenderId() == msg.getSenderId()) {
                return false;
            }
        }
        return true;
    }

    public void addCommitQuorum(CommitMessage message) {
        synchronized (this.commitQuorumMessages) {
            if (!verifyCommitSender(message)) {
                return;
            }

            commitQuorumMessages.add(message);

            checkCommitQuorum(message);
        }
    }

    public void startInstance(ArrayList<Message> operations) {
        synchronized (this) {
            this.currentInstance++;
            Block block;

            if (this.isLeader) {
                block = new Block(operations);

                broadcastPrePrepare(this.currentInstance, this.currentRound, block);

                Timer timer = new Timer();
                TimerTask task = new TimerTaskPrint(this.currentInstance);
                this.timers.put(this.currentInstance, timer);
                timer.schedule(task, 5000);
            }
        }
    }

    public void checkPrePrepare(PrePrepareMessage msg) {
        if (msg.getSenderId() != this.leaderId) {
            return;
        }

        if (this.timers.containsKey(msg.getInstance())) {
            this.timers.get(msg.getInstance()).cancel();
        }

        if (!this.isLeader) {
            Timer timer = new Timer();
            TimerTask task = new TimerTaskPrint(msg.getInstance());
            this.timers.put(msg.getInstance(), timer);
            timer.schedule(task, 5000);
        }

        broadcastPrepare(msg);
    }

    public void checkPrepareQuorum(PrepareMessage m) {
        int counter = 0;

        for (PrepareMessage message : this.prepareQuorumMessages) {
            if (m.getInstance() == message.getInstance() && m.getRound() == message.getRound() && m.getBlock().equals(message.getBlock())) {
                counter++;
            }
        }

        if (counter == this.consensusQuorum) {
            this.prepareQuorumMessages.removeIf(msg -> (msg.getInstance() == m.getInstance()));
            broadcastCommit(m);
        }
    }

    public void checkCommitQuorum(CommitMessage m) {
        int counter = 0;

        for (CommitMessage message : this.commitQuorumMessages) {
            if (m.getInstance() == message.getInstance() && m.getRound() == message.getRound() && m.getRound() == message.getRound() && m.getBlock().equals(message.getBlock())) {
                counter++;
            }
        }

        if (counter == this.consensusQuorum) {
            this.timers.get(m.getInstance()).cancel();
            this.timers.remove(m.getInstance());
            this.commitQuorumMessages.removeIf(msg -> (msg.getInstance() == m.getInstance()));
            decide(m.getInstance(), m.getBlock());
        }
    }

    public void decide(int instance, Block block) {
        blockchain.put(instance, block);

        System.out.println("Added to blockchain: instance " + instance);

        executeBlock(instance);

    }

    public void createSnapshot() {
        this.accounts.forEach((key, value) -> {
            // compute snapshot account and add to current snapshot
            AccountSnapshot acc = computeSnapshotAccount(key, value);

            synchronized (this.currentSnapshot) {
                this.currentSnapshot.put(key, acc);
            }
        });
    }

    public AccountSnapshot computeSnapshotAccount(PublicKey key, Float balance) {
        byte[] signature = Utils.generateSignatureBalance(key, balance, this.serverId);

        AccountSnapshot accountSnap = new AccountSnapshot(key, balance, signature);

        return accountSnap;
    }

    public boolean verifySnapshotSender(SnapshotMessage m) {
        for (SnapshotMessage msg : this.snapshotQuorumMessages) {
            if (m.getSenderId() == msg.getSenderId()) {
                return false;
            }
        }
        return true;
    }

    public void addSnapshotQuorum(SnapshotMessage message) {
        synchronized (this.snapshotQuorumMessages) {
            if (!verifySnapshotSender(message)) {
                return;
            }

            snapshotQuorumMessages.add(message);

            checkSnapshotQuorum(message);
        }
    }

    public void checkSnapshotQuorum(SnapshotMessage m) {
        int counter = 0;
        Map<PublicKey, AccountSnapshotValidated> accumulatedSignatures = new HashMap<>();

        for (SnapshotMessage message : this.snapshotQuorumMessages) {
            if (message.equals(m)) {
                counter++;
                for (Map.Entry<PublicKey, AccountSnapshot> entry : message.getSnapshot().entrySet()) {
                    if (!accumulatedSignatures.containsKey(entry.getKey()))
                        accumulatedSignatures.put(entry.getKey(), new AccountSnapshotValidated(entry.getKey(), entry.getValue().getBalance()));
                    accumulatedSignatures.get(entry.getKey()).addSignature(message.getSenderId(), entry.getValue().getSignature());
                }
            }
        }
        if (counter == this.consensusQuorum) {
            this.snapshotQuorumMessages.removeIf(msg -> (msg.getInstance() == m.getInstance()));
            this.snapshotValidated = accumulatedSignatures;
        }
    }

    public void executeBlock(int instance) {
        if (!blockchain.containsKey(instance) || (instance > 1 && (!blockchain.containsKey(instance - 1) || !blockchain.get(instance - 1).wasExecuted()))) {
            return;
        }
        boolean lastTransferValid = true;
        synchronized (accounts) {
            for (Message message : blockchain.get(instance).getOperations()) {
                switch (message.getType()) {
                    case CREATE_ACCOUNT -> {
                        CreateAccountMessage createAccountMessage = (CreateAccountMessage) message;
                        if (validateCreateAccount(createAccountMessage)) {
                            createAccount(createAccountMessage);
                        }
                    }
                    case TRANSFER -> {
                        TransferMessage transferMessage = (TransferMessage) message;
                        if (!transferMessage.isFee()) {
                            lastTransferValid = validateTransfer(transferMessage);
                        }
                        if (lastTransferValid) {
                            transfer(transferMessage);
                        }
                    }
                    default -> System.out.println("Unexpected message type while executing block");
                }
            }
            if ((instance + 2) % 3 == 0) {
                createSnapshot();
                broadcastSnapshot(instance);
            }
        }
        blockchain.get(instance).setExecuted();
        executeBlock(instance + 1);
    }

    public void createAccount(CreateAccountMessage msg) {
        accounts.put(msg.getKey(), (float) 100.0);

        sendClientResponse(msg, "Account created");
    }

    public void transfer(TransferMessage msg) {
        accounts.compute(msg.getSourceKey(), (k, v) -> v - msg.getAmount());
        accounts.compute(msg.getDestinationKey(), (k, v) -> v + msg.getAmount());

        sendClientResponse(msg, "Transfered " + msg.getAmount() + " from client " + msg.getSourceId() + " to client " + msg.getDestinationId());
    }

    public void checkBalance(CheckBalanceMessage msg) {
        if (this.accounts.containsKey(msg.getKey())) {
            sendClientResponse(msg, "Balance: " + this.accounts.get(msg.getKey()));
        } else {
            sendClientResponse(msg, "Check balance failed: account does not exist");
        }
    }

    public void checkBalanceWeak(WeakReadMessage msg) {
        AccountSnapshotValidated account = this.snapshotValidated.get(msg.getKey());
        WeakReadResponseMessage m = new WeakReadResponseMessage(this.serverId, this.seqNum, msg.getFromAddress(), msg.getFromPort(),
                msg.getToAddress(), msg.getToPort(), "Balance: " + account.getBalance(), msg.getSeqNum(), account);
        socket.send(m);
        this.seqNum++;
    }

    public boolean validateCreateAccount(CreateAccountMessage msg) {
        if (!checkSourceKey(msg.getSenderId(), msg.getKey())) {
            sendClientResponse(msg, "Create account failed: unauthorized");
            return false;
        } else if (this.accounts.containsKey(msg.getKey())) {
            sendClientResponse(msg, "Create account failed: account already exists");
            return false;
        } else {
            return true;
        }
    }

    public boolean validateTransfer(TransferMessage msg) {

        if (!checkSourceKey(msg.getSenderId(), msg.getSourceKey())) {
            sendClientResponse(msg, "Transfer failed: unauthorized");
            return false;
        } else if (!this.accounts.containsKey(msg.getSourceKey())) {
            sendClientResponse(msg, "Transfer failed: source account does not exist");
            return false;
        } else if (!this.accounts.containsKey(msg.getDestinationKey())) {
            sendClientResponse(msg, "Transfer failed: destination account does not exist");
            return false;
        }

        float sourceAccountBalance = this.accounts.get(msg.getSourceKey());

        if (sourceAccountBalance < msg.getAmount() + this.fee) {
            sendClientResponse(msg, "Transfer failed: balance too low");
            return false;
        }

        return true;

    }

    public boolean checkSourceKey(int senderId, PublicKey sourceKey) {
        try {
            PublicKey senderKey = Utils.getPublicKey(Utils.getKeyPath(false, senderId));

            if (senderKey.equals(sourceKey)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public void sendClientResponse(Message msg, String response) {
        ServerResponseMessage m = new ServerResponseMessage(this.serverId, this.seqNum, msg.getFromAddress(), msg.getFromPort(),
                msg.getToAddress(), msg.getToPort(), response, msg.getSeqNum());
        socket.send(m);
        this.seqNum++;
    }

    protected void broadcastPrePrepare(int instance, int round, Block block) {
        for (int p : membersPorts) {
            PrePrepareMessage m = new PrePrepareMessage(this.serverId, this.seqNum, this.address, p, this.address, this.port, instance, round, block);
            socket.send(m);
        }
        this.seqNum++;
    }

    protected void broadcastPrepare(PrePrepareMessage msg) {
        for (int p : membersPorts) {
            PrepareMessage m = new PrepareMessage(this.serverId, this.seqNum, this.address, p, this.address, this.port, msg.getInstance(), msg.getRound(), msg.getBlock());
            socket.send(m);
        }
        this.seqNum++;
    }

    protected void broadcastCommit(PrepareMessage msg) {
        for (int p : membersPorts) {
            CommitMessage m = new CommitMessage(this.serverId, this.seqNum, this.address, p, this.address, this.port, msg.getInstance(), msg.getRound(), msg.getBlock());
            socket.send(m);
        }
        this.seqNum++;
    }

    protected void broadcastSnapshot(int instance) {
        for (int p : membersPorts) {
            SnapshotMessage m = new SnapshotMessage(this.serverId, this.seqNum, this.address, p, this.address, this.port, this.currentSnapshot, instance);
            socket.send(m);
        }
        this.seqNum++;
    }

}

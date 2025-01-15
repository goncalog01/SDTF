package pt.ulisboa.tecnico.sec;

public enum MessageType {
    ACK,
    CREATE_ACCOUNT,
    TRANSFER,
    PRE_PREPARE,
    PREPARE,
    COMMIT,
    SERVER_RESPONSE,
    STRONG_READ,
    WEAK_READ,
    SNAPSHOT,
    WEAK_READ_RESPONSE
}

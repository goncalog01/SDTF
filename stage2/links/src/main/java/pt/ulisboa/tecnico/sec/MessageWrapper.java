package pt.ulisboa.tecnico.sec;

import java.io.*;

public class MessageWrapper implements Serializable {
    Message message;
    byte[] signature;
    int resendCount = 0;
    long lastSentTime = 0L;

    public MessageWrapper(Message m, int id) {
        this.message = m;
        this.signature = Utils.generateSignature(m, id);
    }

    public Message getMessage() {
        return this.message;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public int getResendCount() {
        return resendCount;
    }

    public void incrementResendCount() {
        this.resendCount++;
    }

    public long getLastSentTime() {
        return lastSentTime;
    }

    public void setLastSentTime(long lastSentTime) {
        this.lastSentTime = lastSentTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MessageWrapper msg = (MessageWrapper) o;
        return this.message.equals(msg.getMessage());
    }

    public boolean verifySignature() {
        return Utils.verifySignature(this.message, this.signature);
    }


}
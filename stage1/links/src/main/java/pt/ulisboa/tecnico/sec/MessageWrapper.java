package pt.ulisboa.tecnico.sec;

import java.io.Serializable;
import java.util.Arrays;

public class MessageWrapper implements Serializable {
    Message message;
    byte[] signature;

    public MessageWrapper(Message m, byte[] signature) {
        this.message = m;
        this.signature = signature;
    }

    public Message getMessage() {
        return this.message;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MessageWrapper msg = (MessageWrapper) o;
        return this.message.equals(msg.getMessage()) && Arrays.equals(this.signature, msg.getSignature());
    }
}
package pt.ulisboa.tecnico.sec;

import java.io.*;
import java.util.*;

public class Block implements Serializable {
    private ArrayList<Message> operations;
    private boolean executed = false;

    public Block(ArrayList<Message> operations) {
        this.operations = operations;
    }

    public ArrayList<Message> getOperations() {
        return this.operations;
    }

    public boolean wasExecuted() {
        return this.executed;
    }

    public void setExecuted() {
        this.executed = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Block block = (Block) o;
        return operations.equals(block.getOperations());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.operations, this.executed);
    }

}

package pt.ulisboa.tecnico.sec;

import java.util.TimerTask;

public class TimerTaskPrint extends TimerTask {

    private int instance;

    public TimerTaskPrint(int instance) {
        this.instance = instance;
    }

    public void run() {
        System.out.println("Timer for instance " + this.instance + " expired");
        cancel();
    }
}

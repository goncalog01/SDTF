package pt.ulisboa.tecnico.sec;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Wrong input");
            return;
        }

        try {
            int port = -1;
            File file = new File("config/" + args[1]);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;

            while ((st = br.readLine()) != null) {
                String[] processInfo = st.split(",");
                if (processInfo[1].equals(args[0])) {
                    if (!processInfo[0].equals("client")) {
                        System.out.println("Invalid client id");
                        return;
                    }
                    port = Integer.parseInt(processInfo[2]);
                }
            }

            br.close();

            if (port == -1) {
                System.out.println("Invalid client id");
                return;
            }

            Library library = new Library(Integer.parseInt(args[0]), port);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                //System.out.print("Enter string to append to the blockchain: ");
                String input = scanner.nextLine();
                if (input.length() == 0) {
                    System.out.println("Invalid string");
                    continue;
                }
                library.add_blockchain(input);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
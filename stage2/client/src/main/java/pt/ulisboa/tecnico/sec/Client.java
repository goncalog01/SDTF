package pt.ulisboa.tecnico.sec;

import java.io.*;
import java.util.*;

public class Client {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Wrong input");
            return;
        }

        try {
            int clientId = -1;
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
                    clientId = Integer.parseInt(processInfo[1]);
                    port = Integer.parseInt(processInfo[2]);
                }
            }

            br.close();

            if (port == -1) {
                System.out.println("Invalid client id");
                return;
            }

            Library library = new Library(Integer.parseInt(args[0]), port, "config/" + args[1]);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Select operation\n1: Create account\n2: Transfer\n3: Check balance\n> ");
                String input = scanner.nextLine();
                switch (input) {
                    case "1":
                        library.createAccount();
                        break;
                    case "2":
                        System.out.print("Amount:\n> ");
                        String aux = scanner.nextLine();
                        float amount = Float.parseFloat(aux);

                        if (amount < 0) {
                            System.out.println("Can't transfer negative amount");
                            break;
                        }

                        System.out.print("To what Client Id:\n> ");
                        aux = scanner.nextLine();
                        int id = Integer.parseInt(aux);

                        if (id == clientId) {
                            System.out.println("Invalid clientId");
                            break;
                        }

                        library.transfer(amount, id);
                        break;
                    case "3":
                        System.out.print("Select Mode\n1: Strongly Consistent Read\n2: Weakly Consistent Read\n> ");
                        String mode = scanner.nextLine();
                        switch (mode) {
                            case "1":
                                library.stronglyConsistentRead();
                                break;
                            case "2":
                                library.weaklyConsistentRead();
                                break;
                            default:
                                System.out.println("Invalid mode");
                                continue;
                        }
                        break;
                    default:
                        System.out.println("Invalid operation");
                        continue;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
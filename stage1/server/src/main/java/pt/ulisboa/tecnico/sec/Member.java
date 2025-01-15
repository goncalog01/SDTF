package pt.ulisboa.tecnico.sec;

import java.io.*;

public class Member {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Wrong input");
            return;
        }

        try {
            int port = -1;
            File file = new File("config/configFile.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;
            String[] serverInfo = new String[1];
            String leaderId = "";

            while ((st = br.readLine()) != null) {
                String[] processInfo = st.split(",");
                if (processInfo[0].equals("member") && Integer.parseInt(processInfo[3]) == 1) {
                    leaderId = processInfo[1];
                }

                if (processInfo[1].equals(args[0])) {
                    if (!processInfo[0].equals("member")) {
                        System.out.println("Invalid member id");
                        return;
                    }

                    port = Integer.parseInt(processInfo[2]);
                    serverInfo = processInfo;
                }
            }

            br.close();

            if (port == -1) {
                System.out.println("Invalid member id");
                return;
            } else if (leaderId.equals("")) {
                System.out.println("No leader defined");
                return;
            }

            // To ensure for this stage that no leaders can be byzantine
            if (serverInfo[4].equals("1") && serverInfo[3].equals("0")) {
                ByzantineMemberServer server = new ByzantineMemberServer(Integer.parseInt(serverInfo[1]),
                        Integer.parseInt(serverInfo[2]),
                        Integer.parseInt(serverInfo[3]),
                        Integer.parseInt(leaderId),
                        serverInfo[5]);

                server.start();
            } else {

                MemberServer server = new MemberServer(Integer.parseInt(serverInfo[1]),
                        Integer.parseInt(serverInfo[2]),
                        Integer.parseInt(serverInfo[3]),
                        Integer.parseInt(leaderId));

                server.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

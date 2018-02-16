/*
Developer: Hung Pham
 */

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        /*
        to run the slave program: java -jar ./JuliaNetwork.jar slave
         */
        if (args[0].equalsIgnoreCase("slave")) {
            System.out.println("This is slave application");
            System.out.println("Please specify the port listening.");
            System.out.println("Enter 0 to choose default port or larger than 0 to choose specific port.");
            Scanner in = new Scanner(System.in);
            String line = in.nextLine().trim();
            try {
                int port = Integer.parseInt(line);
                JuliaSlave.openConnection(port);
            }
            catch (Exception e) {
                System.out.println("Wrong port format.");
            }
        }
        /*
        to run the slave program: java -jar ./JuliaNetwork.jar master
         */
        else if (args[0].equalsIgnoreCase("master")) {
            System.out.println("This is master application");
            System.out.println("Please specify the address and port of slaves.");
            Scanner in = new Scanner(System.in);
            String line = in.nextLine();
            String[] slaves = line.split("\\s+");
            JuliaMaster.openConnection(slaves);
        }
    }
}

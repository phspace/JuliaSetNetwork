/*
Developer: Hung Pham
Reference to the book: Introduction to Programming Using Java, David J. Eck
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class JuliaSlave {
    private static final int PORT_LISTENING = 9001;

    public static void openConnection(int portIn) {
        int port = PORT_LISTENING;

        if (portIn > 0) {
            try {
                port = portIn;
                if (port < 0 || port > 65535)
                    throw new NumberFormatException();
            }
            catch (NumberFormatException e) {
                port = PORT_LISTENING;
            }
        }

        System.out.println("Starting with listening port number " + port);

        // always listen to the Master
        while (true) {
            ServerSocket listener = null;
            try {
                listener = new ServerSocket(port);
            }
            catch (Exception e) {
                System.out.println("ERROR: Can't create listening socket on port " + port);
                System.exit(1);
            }

            try {
                Socket connection = listener.accept();
                listener.close();
                System.out.println("Accepted connection from " + connection.getInetAddress());
                handleConnection(connection);
            }
            catch (Exception e) {
                System.out.println("ERROR: Server shut down with error:");
                System.out.println(e);
                System.exit(2);
            }
        }

    }

    // read the task information from Master
    private static JuliaSet receiveTask(String taskData) throws IOException {
        try {
            Scanner scanner = new Scanner(taskData);
            JuliaSet task = new JuliaSet();
            scanner.next();  // skip the command at the start of the line.
            task.id = scanner.nextInt();
            task.maxIterations = scanner.nextInt();
            task.rows = scanner.nextInt();
            task.columns = scanner.nextInt();
            return task;
        }
        catch (Exception e) {
            throw new IOException("There is a problem while reading data");
        }
    }

    // prepare result String
    private static String sendResult(JuliaSet task) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("result");
        buffer.append(' ');
        buffer.append(task.id);
        buffer.append(' ');
        buffer.append(task.columns);
        for (int i = 0; i < task.columns; i++) {
            buffer.append(' ');
            buffer.append(task.results[i]);
        }
        return buffer.toString();
    }

    // all the communications are inside here
    private static void handleConnection(Socket connection) {
        try {
            BufferedReader in = new BufferedReader( new InputStreamReader(
                    connection.getInputStream()) );
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            while (true) {
                String line = in.readLine();  // Message from the master.
                if (line == null) {
                    throw new Exception("Connection closed unexpectedly.");
                }
                if (line.startsWith("close")) {
                    System.out.println("Received close command.");
                    break;
                }
                else if (line.startsWith("task")) {
                    JuliaSet task = receiveTask(line);
                    task.compute();
                    out.println(sendResult(task));
                    out.flush();
                }
                else {
                    throw new Exception("Wrong command received");
                }
            }
        }
        catch (Exception e) {
            System.out.println("Client connection closed with error " + e);
        }
        finally {
            try {
                connection.close();
            }
            catch (Exception e) {
            }
        }
    }
}

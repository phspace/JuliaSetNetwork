/*
Developer: Hung Pham
Reference to the book: Introduction to Programming Using Java, David J. Eck
 */

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JuliaMaster implements Julia {
    private static final int PORT_LISTENING = 9001;

    private static ConcurrentLinkedQueue<JuliaSet> tasks;

    private static int tasksCompleted;

    private static int rows, columns;

    private static int[][] julia;

    public static void openConnection(String[] slaveHost) {

        long startTime = System.currentTimeMillis();

        createTask();

        if (slaveHost.length == 0) { // run on local machine

            System.out.println("Running on this computer only...");
            while (true) {
                JuliaSet task = tasks.poll();
                if (task == null)
                    break;
                task.compute();
                taskCompleted(task);
            }

        } else {  // run over network

            SlaveConnector[] slaves = new SlaveConnector[slaveHost.length];
            System.out.println("Number of slaves: " + slaveHost.length);

            // create connections to slave with input ip address and port
            // then send tasks to slave and get results
            for (int i = 0; i < slaveHost.length; i++) {
                String host = slaveHost[i];
                int port = PORT_LISTENING;
                // separate ip address and port
                int pos = host.indexOf(':');
                if (pos >= 0) {
                    String portString = host.substring(pos + 1);
                    host = host.substring(0, pos);
                    try {
                        port = Integer.parseInt(portString);
                    } catch (NumberFormatException e) {
                    }
                }
                // initialize SlaveConnector: communication class
                slaves[i] = new SlaveConnector(i + 1, host, port);
            }

            for (int i = 0; i < slaveHost.length; i++) {
                // Wait for all the threads to terminate.
                while (slaves[i].isAlive()) {
                    try {
                        slaves[i].join();
                    } catch (InterruptedException e) {
                    }
                }
            }

            // check if slaves do all task
            if (tasksCompleted != rows) {
                System.out.println(tasksCompleted + " finished while there are " + rows + " tasks.");
                System.exit(1);
            }

        }

        long runTime = System.currentTimeMillis() - startTime;
        System.out.println("Finished in " + (runTime / 1000.0) + " seconds ");

        saveImage();
    }

    // create Julia task
    private static void createTask() {
        rows = 720;
        columns = 1280;
        julia = new int[rows][columns];

        tasks = new ConcurrentLinkedQueue<JuliaSet>();
        for (int j = 0; j < rows; j++) {  // Add tasks to the task list.
            JuliaSet task;
            task = new JuliaSet();
            task.id = j;
            task.maxIterations = MAX_ITERATION;
            task.rows = rows;
            task.columns = columns;
            tasks.add(task);
        }
    }

    private static void addTask(JuliaSet task) {
        tasks.add(task);
    }

    // check number of completed tasks
    // synchronized to prevent race condition
    synchronized private static void taskCompleted(JuliaSet task) {
        int row = task.id;
        System.arraycopy(task.results, 0, julia[row], 0, columns);
        tasksCompleted++;
    }

    // the command String and information for tasks
    private static String sendTask(JuliaSet task) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("task");
        buffer.append(' ');
        buffer.append(task.id);
        buffer.append(' ');
        buffer.append(task.maxIterations);
        buffer.append(' ');
        buffer.append(task.rows);
        buffer.append(' ');
        buffer.append(task.columns);
        buffer.append(' ');
        return buffer.toString();
    }

    // read tasks result from the slaves
    private static void getTaskResults(String data, JuliaSet task) throws Exception {
        Scanner scanner = new Scanner(data);
        scanner.next();  // read "results" at beginning of line
        int id = scanner.nextInt();
        if (id != task.id)
            throw new IOException("Wrong task ID in results");
        int count = scanner.nextInt();
        if (count != task.columns)
            throw new IOException("Wrong data count in results");
        task.results = new int[count];
        for (int i = 0; i < count; i++)
            task.results[i] = scanner.nextInt();
    }

    private static void saveImage() {
        System.out.println("Saving image...");
        JFileChooser fileDialog = new JFileChooser();
        fileDialog.setSelectedFile(new File("JuliaSet.png"));
        fileDialog.setDialogTitle("Select File to be Saved");
        int option = fileDialog.showSaveDialog(null);
        if (option != JFileChooser.APPROVE_OPTION)
            return;
        File selectedFile = fileDialog.getSelectedFile();
        if (selectedFile.exists()) {
            int response = JOptionPane.showConfirmDialog(null,
                    "The file \"" + selectedFile.getName()
                            + "\" already exists.\nDo you want to replace it?",
                    "Confirm Save",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (response != JOptionPane.YES_OPTION)
                return;
        }
        try {
            int[] palette = new int[256];

            for (int i = 0; i < 256; i++)
                palette[i] = Color.getHSBColor(i / 255F, 1, 1).getRGB();

            BufferedImage OSI = new BufferedImage(columns, rows, BufferedImage.TYPE_INT_RGB);
            int[] rgb = new int[columns];
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    rgb[col] = palette[julia[row][col] % palette.length];
                }
                OSI.setRGB(0, row, columns, 1, rgb, 0, 1024);
            }
            boolean hasPNG = ImageIO.write(OSI, "PNG", selectedFile);
        } catch (Exception e) {
            System.out.println("Cannot save image.");
            e.printStackTrace();
        }

    }

    /*
    This class is for managing, sending, and getting results of task between Master and Slaves
     */
    private static class SlaveConnector extends Thread {

        int id; // track ID of Slaves
        String host;
        int port;

        SlaveConnector(int id, String host, int port) {
            this.id = id;
            this.host = host;
            this.port = port;
            start();
        }

        public void run() {

            int tasksCompleted = 0;
            Socket socket;

            try {
                socket = new Socket(host, port);  // open the connection.
            } catch (Exception e) {
                System.out.println("Slave " + id + " could not open connection to " +
                        host + ":" + port);
                System.out.println("   Error: " + e);
                return;
            }

            System.out.println("Connection established successfully with slave " + id + " at " + host + ":" + port);

            JuliaSet currentTask = null;
            JuliaSet nextTask = null;

            // send currentTask and prepare nextTask
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream());
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                currentTask = tasks.poll();
                if (currentTask != null) {
                    // Send first task
                    String taskString = sendTask(currentTask);
                    out.println(taskString);
                    out.flush();
                }
                while (currentTask != null) { // send tasks until there is no task left
                    String resultString = in.readLine(); // Get results for currentTask.
                    if (resultString == null)
                        throw new IOException("Connection closed unexpectedly.");
                    if (!resultString.startsWith("result"))
                        throw new IOException("Illegal string received from worker.");
                    nextTask = tasks.poll();  // Get next task from list
                    if (nextTask != null) {
                        // send next task
                        String taskString = sendTask(nextTask);
                        out.println(taskString);
                        out.flush();
                    }
                    getTaskResults(resultString, currentTask);
                    taskCompleted(currentTask);
                    tasksCompleted++;
                    currentTask = nextTask;
                    nextTask = null;
                }
                out.println("close");
                out.flush();
            } catch (Exception e) {
                System.out.println("Slave " + id + " terminated because of an error");
                System.out.println("   Error: " + e);
                e.printStackTrace();
                // check if there is any umcompleted task and send to slave again
                if (currentTask != null)
                    addTask(currentTask);
                if (nextTask != null)
                    addTask(nextTask);
            } finally {
                System.out.println("Slave " + id + " ending after completing " +
                        tasksCompleted + " tasks");
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }

        }

    }
}

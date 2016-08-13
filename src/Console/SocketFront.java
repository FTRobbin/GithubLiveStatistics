package Console;

import GLSUtility.LogService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/7/16.
 */
public class SocketFront implements ConsoleFront {

    public static final String ENTITY = "Console.SocketFront";

    public static final String HELP =
            "=== GithubLiveStatistics SocketFront Help ===\n" +
                    "List of Currently supported commands: \n" +
                    "help : Show this message\n" +
                    "stat : Show basic statistics\n" +
                    "exit : Close current connection\n" +
                    "shutdown : Shutdown GithubLivsStatistics\n" +
                    "====================== End of Help ======================\n" +
                    "" +
                    "";

    private boolean isShutdown;

    private boolean isClosed;

    private final int portNum;

    private ServerSocket serverSocket;
    private Socket connection;
    private BufferedWriter writer;
    private BufferedReader reader;

    private LogService logger;

    public SocketFront(int portNum, LogService logger) {
        this.isShutdown = false;
        this.portNum = portNum;
        this.logger = logger;
    }

    @Override
    public void close() {
        isClosed = true;
    }

    @Override
    public void shutdown() {
        isClosed = true;
        isShutdown = true;
        logger.log(Level.FINER, ENTITY, "SocketFront shutdown");
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }

    @Override
    public void writeLine(String s) {
        try {
            writer.write(s + "\n");
            writer.flush();
        } catch (IOException ie) {
            logger.logErr(Level.SEVERE, ENTITY, "Unexpected IO Exception when writing messages", ie);
        }
    }

    @Override
    public void writeHelpMessage() {
        writeLine(HELP);
    }

    public void waitForConnection() throws IOException {
        logger.log(Level.FINE, ENTITY, "Waiting for connections");
        connection = serverSocket.accept();
        logger.log(Level.FINE, ENTITY, "Connection established");
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
    }

    public void closeConnection() throws IOException {
        reader.close();
        writer.close();
        connection.close();
    }

    public void startMessageLoop(ConsoleBack back) throws IOException {
        isClosed = false;
        writeHelpMessage();
        while (!isClosed && !Thread.currentThread().isInterrupted()) {
            try {
                String command = reader.readLine();
                back.runCommand(command, this);
            } catch (IOException ie) {
                logger.logErr(Level.SEVERE, ENTITY, "Unexpected IO Exception when reading messages, close current connection", ie);
                close();
            }
        }
    }

    @Override
    public void startSocketServer(ConsoleBack back) {
        try {
            serverSocket = new ServerSocket(portNum);
        } catch (IOException ie) {
            logger.logErr(Level.SEVERE, ENTITY, "Unable to open port on " + portNum + ", console closed", ie);
            return;
        }
        logger.log(Level.INFO, ENTITY, "Successfully create port on " + portNum);
        while(!isShutdown && !Thread.currentThread().isInterrupted()) {
            try {
                waitForConnection();
                startMessageLoop(back);
            } catch (IOException ie) {
                logger.logErr(Level.SEVERE, ENTITY, "Unexpected IO Exception during socket connection", ie);
            } finally {
                try {
                    closeConnection();
                } catch (IOException ignored) {

                }
            }
        }
    }
}

package Console;

import java.io.IOException;

/**
 * Created by RobbinNi on 7/7/16.
 */
public interface ConsoleFront {

    public void close();

    public void shutdown();

    public String readLine() throws IOException;

    public void writeLine(String s);

    public void writeHelpMessage();

    public void startSocketServer(ConsoleBack back);
}

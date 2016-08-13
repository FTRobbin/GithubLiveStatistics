package Console;

/**
 * Created by RobbinNi on 7/7/16.
 */
public class SocketConsole implements Runnable {

    private final ConsoleFront front;
    private final ConsoleBack back;

    public SocketConsole(ConsoleFront front, ConsoleBack back) {
        this.front = front;
        this.back = back;
    }

    @Override
    public void run() {
        front.startSocketServer(back);
    }
}

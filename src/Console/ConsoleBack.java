package Console;

import GLSUtility.LogService;
import Main.GithubLiveStatistics;

import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/7/16.
 */
public class ConsoleBack {

    private static final String ENTITY = "Console.ConsoleBack";

    private final GithubLiveStatistics main;

    public ConsoleBack(GithubLiveStatistics main) {
        this.main = main;
    }

    public void runCommand(String command, ConsoleFront front) {
        main.logger.log(Level.FINER, ENTITY, "Received console command from front end : \'" + command + "\'");
        String[] tokens = command.split(" ");
        if (tokens.length > 0) {
            String head = tokens[0];
            if (head.equalsIgnoreCase("shutdown")) {
                front.writeLine("Bye");
                front.shutdown();
                main.envokeShutdown();
            } else if (head.equalsIgnoreCase("stat")) {
                front.writeLine(main.basicStat.getReport());
            } else if (head.equalsIgnoreCase("exit")) {
                front.writeLine("Bye");
                front.close();
            } else if (command.equalsIgnoreCase("help")) {
                front.writeHelpMessage();
            } else {
                front.writeHelpMessage();
            }
        }
    }
}

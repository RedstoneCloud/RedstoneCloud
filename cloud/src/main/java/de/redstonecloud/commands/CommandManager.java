package de.redstonecloud.commands;

import de.redstonecloud.commands.defaults.*;
import de.redstonecloud.logger.Logger;
import lombok.Getter;

import java.util.*;

public class CommandManager {

    @Getter
    private Map<String,Command> commandMap = new HashMap<>();
    private final Set<Command> commands;
    private String message;

    public CommandManager() {
        this.commands = new HashSet<Command>();
    }
    private final Logger logger = Logger.getInstance();

    private void addCommand(Command cmd) {
        commands.add(cmd);
        commandMap.put(cmd.getCommand(), cmd);
    }

    public void loadCommands() {
        addCommand(new ConsoleCommand("console"));
        addCommand(new EndCommand("end"));
        addCommand(new InfoCommand("info"));
        addCommand(new StartCommand("start"));
        addCommand(new StopCommand("stop"));
    }

    public void executeCommand(String command, String[] args) {
        Command cmd = getCommand(command);

        if (cmd != null) {
            try {
                cmd.onCommand(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if(!command.equals("")) {
                logger.info("This command does not exist!");
            }
        }
    }

    public Command getCommand(String name) {
        for (Command command : this.commands) {
            if (command.getCommand().equalsIgnoreCase(name)) {
                return command;
            }
        }
        return null;
    }

    public Command getCommand(Class<? extends Command> name) {
        for (Command command : this.commands) {
            if (command.getClass().equals(name)) {
                return command;
            }
        }
        return null;
    }
}
package de.redstonecloud.console;

import de.redstonecloud.RedstoneCloud;
import de.redstonecloud.commands.Command;
import lombok.RequiredArgsConstructor;
import org.jline.reader.*;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ConsoleCompleter implements Completer {
    private final RedstoneCloud server;

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> candidates) {
        if(server.getCurrentLogServer() == null) {
            if (parsedLine.wordIndex() == 0) {
                if (parsedLine.word().isEmpty()) {
                    addCandidates(s -> candidates.add(new Candidate(s)));
                    return;
                }
                SortedSet<String> names = new TreeSet<>();
                addCandidates(names::add);
                for (String match : names) {
                    if (!match.toLowerCase().startsWith(parsedLine.word().toLowerCase())) {
                        continue;
                    }

                    candidates.add(new Candidate(match));
                }
            } else if (parsedLine.wordIndex() > 0 && !parsedLine.word().isEmpty()) {
                String command = parsedLine.words().get(0);
                Command cmd;
                if ((cmd = server.getCommandManager().getCommand(command)) != null) {
                    if(parsedLine.words().size() - 2 > cmd.argCount) {
                        return;
                    }

                    for(String arg : cmd.getArgs()) {
                        if (!arg.toLowerCase().startsWith(parsedLine.word().toLowerCase())) {
                            continue;
                        }
                        candidates.add(new Candidate(arg));
                    }
                }
            } else {
                String command = parsedLine.words().get(0);
                Command cmd;
                if ((cmd = server.getCommandManager().getCommand(command)) != null) {
                    //check if argCount is reached
                    if (parsedLine.words().size() - 2 > cmd.argCount) {
                        return;
                    }

                    for(String arg : cmd.getArgs()) {
                        candidates.add(new Candidate(arg));
                    }
                }
            }
        } else {
            if (parsedLine.wordIndex() == 0) {
                if (parsedLine.word().isEmpty()) {
                    candidates.add(new Candidate("_exit"));
                }
            }
        }
    }

    private void addCandidates(Consumer<String> commandConsumer) {
        for (String command : server.getCommandManager().getCommandMap().keySet()) {
            if (!command.contains(":")) {
                commandConsumer.accept(command);
            }
        }
    }
}
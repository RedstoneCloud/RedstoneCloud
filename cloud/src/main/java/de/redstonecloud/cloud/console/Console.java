package de.redstonecloud.cloud.console;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.logger.Logger;
import de.redstonecloud.cloud.utils.Utils;
import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

@RequiredArgsConstructor
public class Console extends SimpleTerminalConsole {
    private final RedstoneCloud server;

    @Override
    protected boolean isRunning() {
        return RedstoneCloud.isRunning();
    }

    @Override
    protected void runCommand(String command) {
        boolean hasLogServer = server.getCurrentLogServer() != null;
        if (!hasLogServer) {
            String cmd = command.split(" ")[0];
            String[] args = Utils.dropFirstString(command.split(" "));
            server.getCommandManager().executeCommand(cmd, args);
        } else {
            if (command.equalsIgnoreCase("_exit")) {
                server.getCurrentLogServer().disableConsoleLogging();
                server.setCurrentLogServer(null);
                Logger.getInstance().info("Exited console");
            } else {
                server.getCurrentLogServer().getServer().writeConsole(command);
            }
        }
    }

    @Override
    protected void shutdown() {
        server.stop();
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        builder.completer(new ConsoleCompleter(server));
        builder.appName("RedstoneCloud");
        builder.option(LineReader.Option.HISTORY_BEEP, false);
        builder.option(LineReader.Option.HISTORY_IGNORE_DUPS, true);
        builder.option(LineReader.Option.HISTORY_IGNORE_SPACE, true);
        return super.buildReader(builder);
    }
}
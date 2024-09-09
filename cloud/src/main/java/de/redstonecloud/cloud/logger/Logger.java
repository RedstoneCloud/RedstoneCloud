package de.redstonecloud.cloud.logger;

import de.redstonecloud.cloud.RedstoneCloud;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Log4j2
public class Logger {
    @Getter public static Logger instance = new Logger();

    public void server(String name, String message) {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);
        System.out.println(Colors.toColor(formattedDate + " §7| §rLOG   §r: §b" + name + " §r" + message + "§r"));
        log.log(Level.ALL, formattedDate + " | §7LOG   : " + name + " " + message);
    }

    private void writeToLog(String message) {
        try {
            RedstoneCloud.getInstance().getLogFile().write(message + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //public static String name() {
    //    return "RedstoneCloud";
    //}

    public void error(String error) {
        error("RedstoneCloud", error);
    }

    public void debug(String debug) {
        debug("RedstoneCloud", debug);
    }

    public void warning(String warning) {
        warning("RedstoneCloud", warning);
    }

    public void info(String info) {
        info("RedstoneCloud", info);
    }

    protected String getDate() {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dateTime.format(formatter);
    }

    public void error(String name, String error) {
        String date = this.getDate();
        writeToLog(date + " | §cERROR : " + name + " " + error);
        if (RedstoneCloud.getInstance().getCurrentLogServer() == null)
            System.out.println(Colors.toColor(date + " §7| §cERROR §r: §b" + name + " §r" + error + "§r"));
        log.log(Level.ALL, date + " | §cERROR : " + name + " " + error);
    }

    public void debug(String name, String debug) {
        String date = this.getDate();
        writeToLog(date + " | §bDEBUG : " + name + " " + debug);
        if (RedstoneCloud.getInstance().getCurrentLogServer() == null)
            System.out.println(Colors.toColor(date + " §7| §bDEBUG §r: §b" + name + " §r" + debug + "§r"));
        log.log(Level.ALL, date + " | §bDEBUG : " + name + " " + debug);
    }

    public void warning(String name, String warning) {
        String date = this.getDate();
        writeToLog(date + " | §eWARN  : " + name + " " + warning);
        if (RedstoneCloud.getInstance().getCurrentLogServer() == null)
            System.out.println(Colors.toColor(date + " §7| §eWARN  §r: §b" + name + " §r" + warning + "§r"));
        log.log(Level.ALL, date + " | §eWARN  : " + name + " " + warning);
    }

    public void info(String name, String info) {
        String date = this.getDate();
        writeToLog(date + " | §aINFO  : " + name + " " + info);
        if (RedstoneCloud.getInstance().getCurrentLogServer() == null)
            System.out.println(Colors.toColor(date + " §7| §aINFO  §r: §b" + name + " §r" + info + "§r"));
        log.log(Level.ALL, date + " | §aINFO  : " + name + " " + info);
    }
}
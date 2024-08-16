package de.redstonecloud.logger;

import de.redstonecloud.RedstoneCloud;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Log4j2
public class Logger {
    public static Logger instance = new Logger();

    public static Logger getInstance() {
        return instance;
    }

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

    public static String name() {
        return "RedstoneCloud";
    }

    public void error(String error) {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);
        writeToLog(formattedDate + " | §cERROR : " + name() + " " + error);
        if(RedstoneCloud.getInstance().getCurrentLogServer() == null) System.out.println(Colors.toColor(formattedDate + " §7| §cERROR §r: §b" + name() + " §r" + error + "§r"));
        log.log(Level.ALL, formattedDate + " | §cERROR : " + name() + " " + error);
    }

    public void debug(String debug) {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);
        writeToLog(formattedDate + " | §bDEBUG : " + name() + " " + debug);
        if(RedstoneCloud.getInstance().getCurrentLogServer() == null) System.out.println(Colors.toColor(formattedDate + " §7| §bDEBUG §r: §b" + name() + " §r" + debug + "§r"));
        log.log(Level.ALL, formattedDate + " | §bDEBUG : " + name() + " " + debug);
    }

    public void warning(String warning) {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);
        writeToLog(formattedDate + " | §eWARN  : " + name() + " " + warning);
        if(RedstoneCloud.getInstance().getCurrentLogServer() == null) System.out.println(Colors.toColor(formattedDate + " §7| §eWARN  §r: §b" + name() + " §r" + warning + "§r"));
        log.log(Level.ALL, formattedDate + " | §eWARN  : " + name() + " " + warning);
    }

    public void info(String info) {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);
        writeToLog(formattedDate + " | §aINFO  : " + name() + " " + info);
        if(RedstoneCloud.getInstance().getCurrentLogServer() == null) System.out.println(Colors.toColor(formattedDate + " §7| §aINFO  §r: §b" + name() + " §r" + info + "§r"));
        log.log(Level.ALL, formattedDate + " | §aINFO  : " + name() + " " + info);
    }
}
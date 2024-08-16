package de.redstonecloud.server;

import lombok.Builder;
import lombok.Getter;

import java.io.*;

@Builder
public class DummyErrorReader extends Thread {
    @Getter
    private Server server;
    @lombok.Builder.Default
    private boolean running = true;

    public void run() {
        while(running && server.getProcess() != null && server.getProcess().getErrorStream() != null) {
            BufferedReader out = new BufferedReader(new InputStreamReader(server.getProcess().getErrorStream()));
            String line = "";
            try {
                while(running && (line = out.readLine()) != null){

                }
            } catch (IOException e) {
            }
        }
    }

    public void cancel() {
        running = false;
        this.interrupt();
    }
}
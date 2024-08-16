package de.redstonecloud.scheduler.defaults;

import de.redstonecloud.scheduler.task.Task;
import de.redstonecloud.server.ServerManager;
import de.redstonecloud.server.Template;

public class CheckTemplateTask extends Task {
    @Override
    protected void onRun(long currentMillis) {
        for(Template template : ServerManager.getInstance().getTemplates().values())
            template.checkServers();
    }
}

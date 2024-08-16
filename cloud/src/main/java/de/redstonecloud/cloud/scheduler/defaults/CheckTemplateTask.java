package de.redstonecloud.cloud.scheduler.defaults;

import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.cloud.server.Template;
import de.redstonecloud.cloud.scheduler.task.Task;

public class CheckTemplateTask extends Task {
    @Override
    protected void onRun(long currentMillis) {
        for(Template template : ServerManager.getInstance().getTemplates().values())
            template.checkServers();
    }
}

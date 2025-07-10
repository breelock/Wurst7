package net.wurstclient.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.Iterator;
import java.util.LinkedList;

public class TickScheduler
{
    private static final LinkedList<ScheduledTask> tasks = new LinkedList<>();

    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Iterator<ScheduledTask> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                ScheduledTask task = iterator.next();
                task.ticksLeft--;

                if (task.ticksLeft <= 0) {
                    task.action.run();
                    iterator.remove();
                }
            }
        });
    }

    public static void schedule(int ticks, Runnable action)
    {
        if (ticks <= 0) {
            action.run();
            return;
        }
        tasks.add(new ScheduledTask(ticks, action));
    }

    private static class ScheduledTask
    {
        int ticksLeft;
        Runnable action;

        public ScheduledTask(int ticks, Runnable action)
        {
            this.ticksLeft = ticks;
            this.action = action;
        }
    }
}

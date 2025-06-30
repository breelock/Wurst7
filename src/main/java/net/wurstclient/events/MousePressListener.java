package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface MousePressListener extends Listener
{
    void onMousePress(MousePressEvent event);

    class MousePressEvent extends Event<MousePressListener>
    {
        private final int button;
        private final int action;
        private final int modifiers;

        public MousePressEvent(int button, int action, int modifiers)
        {
            this.button = button;
            this.action = action;
            this.modifiers = modifiers;
        }

        @Override
        public void fire(ArrayList<MousePressListener> listeners)
        {
            for (MousePressListener listener : listeners)
                listener.onMousePress(this);
        }

        @Override
        public Class<MousePressListener> getListenerType()
        {
            return MousePressListener.class;
        }

        public int getButton()
        {
            return button;
        }

        public int getAction()
        {
            return action;
        }

        public int getModifiers()
        {
            return modifiers;
        }
    }
}

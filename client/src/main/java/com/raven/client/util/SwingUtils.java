package com.raven.client.util;

import javax.swing.*;

public final class SwingUtils {
    
    private SwingUtils() {
    }

    /**
     * Runs the specified action on the Event Dispatch Thread (EDT).
     * If the current thread is already the EDT, runs it immediately.
     */
    public static void runOnEDT(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}

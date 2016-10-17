package ch.lernstick.vanishedroothandler;

import ch.fhnw.util.ProcessExecutor;
import ch.fhnw.util.StorageDevice;
import ch.fhnw.util.StorageTools;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * A program that handles the situation when the root file system of a live
 * system vanishes, e.g. because the connection between a USB stick and the
 * computer got lost.
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class VanishedRootHandler implements PropertyChangeListener {

    private static final Logger LOGGER
            = Logger.getLogger(VanishedRootHandler.class.getName());

    private static final Pattern REMOVED_PATTERN = Pattern.compile(
            ".*: Removed (/org/freedesktop/UDisks2/block_devices/.*)");

    private StorageDevice systemStorageDevice;
    private boolean rootVanished;

    /**
     * creates a new VanishedRootHandler
     */
    public VanishedRootHandler() {
        // log to console
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        consoleHandler.setLevel(Level.ALL);
        LOGGER.addHandler(consoleHandler);
        // also log into a rotating temporaty file of max 5 MB
        try {
            FileHandler fileHandler = new FileHandler(
                    "%t/VanishedRootHandler", 5000000, 2, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            LOGGER.addHandler(fileHandler);

        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, "can not create log file", ex);
        }

        try {
            systemStorageDevice = StorageTools.getSystemStorageDevice();
            LOGGER.log(Level.INFO, "system storage device: {0}",
                    systemStorageDevice == null
                            ? null
                            : systemStorageDevice.getDevice());
        } catch (IOException | DBusException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
        }
    }

    /**
     * enforces an instant system reboot
     */
    public static void reboot() {
        LOGGER.log(Level.INFO, "trying to reboot the system");
        new ProcessExecutor().executeProcess(true, true,
                "pkexec", "vanished_root_handler_reboot");
    }

    /**
     * enforces an instant system poweroff
     */
    public static void powerOff() {
        LOGGER.log(Level.INFO, "trying to power off the system");
        new ProcessExecutor().executeProcess(true, true,
                "pkexec", "vanished_root_handler_poweroff");
    }

    /**
     * starts monitoring for device removals
     */
    public void start() {
        UdisksMonitorThread thread = new UdisksMonitorThread();
        thread.start();
    }

    /**
     * blocks until the root file system vanished
     */
    public synchronized void waitForVanishedRoot() {
        LOGGER.info("waiting for the root file system to vanish");
        while (!rootVanished) {
            try {
                wait();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "", ex);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Take great care when calling UI functions, because here we are on the
        // UdisksMonitorThread!

        // only handle line changes
        if (!ProcessExecutor.LINE.equals(evt.getPropertyName())) {
            return;
        }

        // only handle device removals
        String line = (String) evt.getNewValue();
        Matcher matcher = REMOVED_PATTERN.matcher(line);
        if (matcher.matches()) {
            String[] tokens = line.split("/");
            final String device = tokens[tokens.length - 1];
            LOGGER.log(Level.INFO, "removed device: {0}", device);
            if ((systemStorageDevice != null)
                    && device.equals(systemStorageDevice.getDevice())) {
                setRootVanished();
            }
        }
    }

    private synchronized void setRootVanished() {
        LOGGER.info("the root file system vanished");
        rootVanished = true;
        notifyAll();
    }

    private class UdisksMonitorThread extends Thread {

        // use local ProcessExecutor because the udisks process is blocking and
        // long-running
        ProcessExecutor executor = new ProcessExecutor();

        @Override
        public void run() {
            Map<String, String> environment = new HashMap<>();
            environment.put("LC_ALL", "C");
            executor.setEnvironment(environment);
            executor.addPropertyChangeListener(VanishedRootHandler.this);
            executor.executeProcess("udisksctl", "monitor");
        }

        public void stopMonitoring() {
            executor.destroy();
        }
    }
}

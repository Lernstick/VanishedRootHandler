package ch.lernstick.vanishedroothandler;

import ch.fhnw.util.ProcessExecutor;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * the currently running Linux desktop environment
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public enum CurrentLinuxDesktop {

    /**
     * The GNOME desktop. We need to show the test stage. The autologin
     * processes are started quite late so that the desktop seems to be already
     * completely initialized. Therefore we can use a very short timeout.
     */
    GNOME(5),
    /**
     * The KDE Plasma desktop. We don't need to show the test stage.
     */
    KDE,
    /**
     * The Cinnamon desktop. We need to show the test stage. A timeout of 5
     * seconds was too short for Cinnamon. The test stage was not shown and
     * Cinnamon just crashed after the root vanished instead of showing our
     * error stage. 10 seconds was just good enough on a really fast machine. So
     * to be better save than sorry we use a timeout of 15 seconds.
     */
    CINNAMON(15),
    /**
     * The MATE desktop. We need to show the test stage. The autologin processes
     * are started quite late so that the desktop seems to be already completely
     * initialized. Therefore we can use a very short timeout.
     */
    MATE(5),
    /**
     * The Xfce desktop. We don't need to show the test stage.
     */
    XFCE,
    /**
     * The LXDE desktop. We don't need to show the test stage.
     */
    LXDE,
    /**
     * The Enlightenment desktop. We don't need to show the test stage.
     */
    ENLIGHTENMENT,
    /**
     * An unknown Linux desktop. To be save, we better show the test stage with
     * a long timeout.
     */
    UNKNOWN(15);

    // If we don't show the test stage some desktops (GNOME, Cinnamon) just
    // crash when the root file system vanishes instead of showing our
    // interactive main scene. Maybe there is more stuff that needs to be
    // initialized before we can show our main scene on these desktops than just
    // some memlocked files...
    private final boolean needsTestStage;
    private final int autostartTimeout;
    private static CurrentLinuxDesktop instance;

    private CurrentLinuxDesktop() {
        this.needsTestStage = false;
        this.autostartTimeout = 0;
    }

    private CurrentLinuxDesktop(int autostartTimeout) {
        this.needsTestStage = true;
        this.autostartTimeout = autostartTimeout;
    }

    /**
     * returns <code>true</code> if it is necessary to show the test stage on
     * this desktop environment, <code>false</code> otherwise
     *
     * @return <code>true</code> if it is necessary to show the test stage on
     * this desktop environment, <code>false</code> otherwise
     */
    public boolean needsTestStage() {
        return needsTestStage;
    }

    /**
     * returns the timeout (in seconds) we wait after our handler got startet
     * until we show our test stage
     *
     * @return the timeout (in seconds) we wait after our handler got startet
     * until we show our test stage
     */
    public int getAutostartTimeout() {
        return autostartTimeout;
    }

    /**
     * returns the currently running Linux desktop
     *
     * @return the currently running Linux desktop
     */
    public synchronized static CurrentLinuxDesktop getInstance() {

        // lazy desktop detection
        if (instance == null) {

            // XDG compliant desktop environments are easy to detect by checking
            // the XDG_CURRENT_DESKTOP environment variable
            String xdgDesktop = System.getenv("XDG_CURRENT_DESKTOP");
            if (xdgDesktop != null) {
                switch (xdgDesktop) {
                    case "GNOME":
                        instance = GNOME;
                        break;
                    case "KDE":
                        instance = KDE;
                        break;
                    case "X-Cinnamon":
                        instance = CINNAMON;
                        break;
                    case "MATE":
                        instance = MATE;
                        break;
                    case "LXDE":
                        instance = LXDE;
                }
            }

            // for non XDG compliant desktop environments we can use the wmctrl
            // command to get some information about the currently running
            // window manager
            if (instance == null) {
                ProcessExecutor executor = new ProcessExecutor();
                executor.executeProcess(true, true, "wmctrl", "-m");
                List<String> output = executor.getStdOutList();
                Pattern namePattern = Pattern.compile("Name: (.*)");
                for (String line : output) {
                    Matcher matcher = namePattern.matcher(line);
                    if (matcher.matches()) {
                        switch (matcher.group(1)) {
                            case "Xfwm4":
                                instance = XFCE;
                                break;
                            case "Enlightenment":
                                instance = ENLIGHTENMENT;
                        }
                        break;
                    }
                }
            }

            // ok, we have no idea what desktop we are running on...
            if (instance == null) {
                instance = UNKNOWN;
            }
        }

        return instance;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + (needsTestStage
                ? "teststage needs a timeout of " + autostartTimeout
                + " seconds"
                : "no teststage needed");
    }
}

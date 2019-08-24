package ch.lernstick.vanishedroothandler.view.javafx;

import ch.fhnw.util.ProcessExecutor;
import ch.lernstick.vanishedroothandler.CurrentLinuxDesktop;
import ch.lernstick.vanishedroothandler.VanishedRootHandler;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

/**
 * the FX view for the VanishedRootHandler
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class VanishedRootHandlerFX extends Application {

    private static final Logger LOGGER
            = Logger.getLogger(VanishedRootHandlerFX.class.getName());
    private static final ResourceBundle STRINGS = ResourceBundle.getBundle(
            "ch/lernstick/vanishedroothandler/Strings");
    private static boolean test;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // We probably still miss something in our memlockd config as without
        // this call to "grease" pkexec it doesn't work when pressing the reboot
        // or poweroff buttons but produces a bus error instead.
        new ProcessExecutor().executeProcess(true, true,
                "pkexec", "vanished_root_handler_dummy");

        for (String arg : args) {
            if (arg.equals("test")) {
                test = true;
                break;
            }
        }

        System.setProperty("prism.lcdtext", "false");
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        // load and prepare our main stage and scene
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("MainScene.fxml"), STRINGS);
        Parent root = loader.load();
        MainSceneController controller = loader.getController();
        stage.setScene(new Scene(root));
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreen(true);
        stage.setOnShowing((event) -> {
            controller.countDown();
        });

        if (test) {
            stage.show();
        } else {
            CurrentLinuxDesktop currentLinuxDesktop
                    = CurrentLinuxDesktop.getInstance();
            LOGGER.log(Level.INFO, "current destkop: {0}", currentLinuxDesktop);
            if (currentLinuxDesktop.needsTestStage()) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            TimeUnit.SECONDS.sleep(
                                    currentLinuxDesktop.getAutostartTimeout());
                            Platform.runLater(() -> {
                                try {
                                    showTestStage();
                                } catch (IOException ex) {
                                    LOGGER.log(Level.SEVERE, null, ex);
                                }
                            });
                        } catch (InterruptedException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                        }
                    }
                }.start();
            }
            new Thread() {
                @Override
                public void run() {
                    VanishedRootHandler handler = new VanishedRootHandler();
                    handler.start();
                    handler.waitForVanishedRoot();
                    Platform.runLater(() -> {
                        stage.show();
                    });
                }
            }.start();
        }
    }

    /**
     * If we don't show the test stage some desktops (GNOME, Cinnamon) just
     * crash when the root file system vanishes instead of showing our
     * interactive main scene. Maybe there is more stuff that needs to be
     * initialized before we can show our main scene on these desktops than just
     * some memlocked files...
     */
    private void showTestStage() throws IOException {
        // To be able to init all necessary things (whatever they are) by
        // shortly displaying a test stage we have to disable the implicit exit
        // attribute of the JavaFX platform. Otherwise the JavaFX runtime just
        // exits after we close our teststage.
        Platform.setImplicitExit(false);

        // load and show the test stage
        Parent root = FXMLLoader.load(
                getClass().getResource("TestScene.fxml"), STRINGS);
        Stage testStage = new Stage();
        testStage.setScene(new Scene(root));
        testStage.show();

        // hide the test stage after a short while
        new Thread() {
            @Override
            public void run() {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
                Platform.runLater(() -> {
                    testStage.hide();
                });
            }
        }.start();
    }
}

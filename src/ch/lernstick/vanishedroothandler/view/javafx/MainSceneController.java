package ch.lernstick.vanishedroothandler.view.javafx;

import ch.lernstick.vanishedroothandler.VanishedRootHandler;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * the controller for the main scene
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class MainSceneController implements Initializable {

    private static final ResourceBundle STRINGS = ResourceBundle.getBundle(
            "ch/lernstick/vanishedroothandler/Strings");
    private static final int TIMEOUT = 30;

    @FXML
    private Button rebootButton;

    @FXML
    private Button powerOffButton;

    @FXML
    private Label timeoutLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rebootButton.setOnAction(a -> VanishedRootHandler.reboot());
        powerOffButton.setOnAction(a -> VanishedRootHandler.powerOff());

        Task<Integer> timeoutTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                for (int i = 0; i < TIMEOUT; i++) {
                    updateProgress(i, TIMEOUT);
                    TimeUnit.SECONDS.sleep(1);
                }
                updateProgress(TIMEOUT, TIMEOUT);
                VanishedRootHandler.reboot();
                return null;
            }
        };

        timeoutLabel.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    int remainingSeconds = TIMEOUT
                            - (int) (TIMEOUT * timeoutTask.getProgress());
                    String timeString;
                    switch (remainingSeconds) {
                        case 0:
                            timeString = STRINGS.getString("Now");
                            break;
                        case 1:
                            timeString = STRINGS.getString("Second");
                            break;
                        default:
                            timeString = MessageFormat.format(STRINGS.getString(
                                    "Seconds"), remainingSeconds);
                    }
                    return MessageFormat.format(STRINGS.getString(
                            "Auto_Reboot_Label"), timeString);
                }, timeoutTask.progressProperty())
        );

        new Thread(timeoutTask).start();
    }
}

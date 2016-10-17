package ch.lernstick.vanishedroothandler.view.javafx;

import ch.lernstick.vanishedroothandler.VanishedRootHandler;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

/**
 * the controller for the main scene
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class MainSceneController implements Initializable {

    @FXML
    private Button rebootButton;

    @FXML
    private Button powerOffButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rebootButton.setOnAction(a -> VanishedRootHandler.reboot());
        powerOffButton.setOnAction(a -> VanishedRootHandler.powerOff());
    }
}

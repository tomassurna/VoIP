package com.rossisurna.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;


/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Controller for Client's UI
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Mar 18 2019
 */


public class ClientController {
    @FXML
    private Button connectButton; //connect button
    @FXML
    private TextField createTableInput; //create group input
    @FXML
    private TextField joinTableInput; //join group input
    @FXML
    private TextField displayNameInput; //username input
    @FXML
    private Circle connectionStatus; //connection status circle
    @FXML
    private TextField ipAddressTextField; //ip address for server input
    @FXML
    private ProgressBar microphoneProgressBar; //mic volume indicator
    @FXML
    private Slider microphoneSlider; //amp slider
    @FXML
    private Label micAmpNumberLabel; //mic amp value
    @FXML
    private Label micLevelNumberLabel; //mic volume value

    //Util
    private Stage stage; //Entire stage of the UI
    private Scene scene; //Client menu scene
    private boolean isStageShown = false; //if the stage has launched

    /**
     * Constructor
     */
    public ClientController() {
    }

    //GETTERS

    /**
     * Get connect button
     *
     * @return the connect button
     */
    public Button getConnectButton() {
        return connectButton;
    }

    /**
     * Get create table text field
     *
     * @return create table textField
     */
    public TextField getCreateTableInput() {
        return createTableInput;
    }

    /**
     * Get join table text field
     *
     * @return join table textField
     */
    public TextField getJoinTableInput() {
        return joinTableInput;
    }

    /**
     * Get username text field
     *
     * @return return displayname textField
     */
    public TextField getDisplayNameInput() {
        return displayNameInput;
    }

    /**
     * Get stage
     *
     * @return stage of client's UI
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Set stage variable
     *
     * @param stage the current stage of client's UI
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Get Scene
     *
     * @return current scene of the client's UI
     */
    public Scene getScene() {
        return scene;
    }

    /**
     * Set scene variable
     *
     * @param scene the current scene of main menu
     */
    public void setScene(Scene scene) {
        this.scene = scene;
    }

    /**
     * Get ip address text field
     *
     * @return the ip address textField
     */
    public TextField getIpAddressTextField() {
        return ipAddressTextField;
    }

    /**
     * Get mic volume progress bar
     *
     * @return the volume progress bar indicator
     */
    public ProgressBar getMicrophoneProgressBar() {
        return microphoneProgressBar;
    }

    /**
     * Get mic amp sldier
     *
     * @return the mic slider from main menu UI
     */
    public Slider getMicrophoneSlider() {
        return microphoneSlider;
    }

    /**
     * Get mic amp value label
     *
     * @return microphone amp label
     */
    public Label getMicAmpNumberLabel() {
        return micAmpNumberLabel;
    }

    //SETTERS

    /**
     * Get mic volume value label
     *
     * @return microphone volume value label
     */
    public Label getMicLevelNumberLabel() {
        return micLevelNumberLabel;
    }

    /**
     * Get boolean of is stage shown
     *
     * @return boolean if stage is shown of client's UI
     */
    public boolean isStageShown() {
        return isStageShown;
    }

    /**
     * Set if stage is shown
     *
     * @param stageShown whether the stage is shown
     */
    public void setStageShown(boolean stageShown) {
        isStageShown = stageShown;
    }

    //MUTATORS

    /**
     * Takes in a state and changes the connection status of the circle to reflect the color
     *
     * @param state the state of the connection status
     */
    public void setStateOfConnectionStatus(int state) {
        switch (state) {
            case 1: {
                //RED FAILEd
                connectionStatus.setFill(Color.RED);
                break;
            }
            case 2: {
                //ORANGE CONNECTING
                connectionStatus.setFill(Color.ORANGE);
                break;
            }
            case 3: {
                //GREEN CONNECTED
                connectionStatus.setFill(Color.GREEN);
                break;
            }
        }

    }

    /**
     * Changes the scene to the main menu scene and clears the main menu of any info
     */
    public void changeScene() {
        Platform.runLater(() -> {
            stage.setScene(scene);
            stage.setTitle("Voice Chat");
            stage.show();
        });
        getJoinTableInput().setText("");
        getCreateTableInput().setText("");
    }
}

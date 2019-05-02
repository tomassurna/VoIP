package com.rossisurna.client.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Controller for the group UI
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Apr 05 2019
 */

public class ClientGroupController {

    private final ConcurrentHashMap<Integer, Label> labelsOfUsersInGroupHashMap = new ConcurrentHashMap<>(); //hashmap of all the user labels in the group
    private final ConcurrentHashMap<Integer, Circle> circleOfUsersInGroupHashMap = new ConcurrentHashMap<>(); //hashmap of all the user circles in the group
    //FXML Items
    @FXML
    private Label groupNameLabel; //group name label
    @FXML
    private Label clientLabel; //default client label that display username
    @FXML
    private TextArea chatTextArea; //chat area
    @FXML
    private TextField chatInput; //chat input field
    @FXML
    private Button chatButton; //chat send button
    @FXML
    private AnchorPane groupDetailsAnchorPane; //anchor pane of where all the usernames are displayed
    @FXML
    private TextField pushToTalkHotKeyTextField; //push to talk hot key text field
    @FXML
    private Label groupUrlLabel; //group url display label
    @FXML
    private Circle clientCircle; //default green circle that displays if you're talking
    @FXML
    private Button quitButton; //quit button
    //Client variables
    private Stage stage; //stage of the UI
    private Scene scene; //Group scene
    private boolean isStageShown = false; //if the stage has been shown
    private double amplification = 1.0; //default amplification for the mic

    /**
     * Constructor
     */
    public ClientGroupController() {

    }

    //GETTERS

    /**
     * get chat text area
     *
     * @return the chat textArea
     */
    public TextArea getChatTextArea() {
        return chatTextArea;
    }

    /**
     * Get chat input text field
     *
     * @return the chat input textField
     */
    public TextField getChatInput() {
        return chatInput;
    }

    /**
     * Get chat send button
     *
     * @return the chat send button
     */
    public Button getChatButton() {
        return chatButton;
    }

    /**
     * Get boolean of if stage is shown
     *
     * @return whether stage of client's UI is shown
     */
    public boolean isStageShown() {
        return isStageShown;
    }

    /**
     * Sets if the stage is shown
     *
     * @param stageShown whether the stage is shown of client's UI
     */
    public void setStageShown(boolean stageShown) {
        isStageShown = stageShown;
    }

    /**
     * Get default client label
     *
     * @return the default client label
     */
    public Label getClientLabel() {
        return clientLabel;
    }

    /**
     * Get anchor pane that houses all the username labels
     *
     * @return the anchor pane where all user labels exist
     */
    public AnchorPane getGroupDetailsAnchorPane() {
        return groupDetailsAnchorPane;
    }

    /**
     * get push to talk textfield input
     *
     * @return the push to talk textField
     */
    public TextField getPushToTalkHotKeyTextField() {
        return pushToTalkHotKeyTextField;
    }

    /**
     * Get size of the hashmap that holds all the labels of the users in the group
     *
     * @return the number of labels in the hashmap
     */
    public int getSizeOfLabelsOfUsersInGroupHashMap() {
        return labelsOfUsersInGroupHashMap.size();
    }

    /**
     * Get the quit button
     *
     * @return the quit button
     */
    public Button getQuitButton() {
        return quitButton;
    }

    /**
     * Get amplification value
     *
     * @return the amplification value of microphone
     */
    public double getAmplification() {
        return amplification;
    }

    /**
     * Sets the amplification value
     *
     * @param amplification the amplification value for the microphone
     */
    public void setAmplification(double amplification) {
        this.amplification = amplification;
    }

    //SETTERS

    /**
     * get default circle that shows if you're talking or not
     *
     * @return the default circle that indicates users speaking
     */
    public Circle getClientCircle() {
        return clientCircle;
    }

    /**
     * Get the scene of the group UI
     *
     * @return the scene of client's UI
     */
    public Scene getScene() {
        return scene;
    }

    /**
     * Sets the scene variable
     *
     * @param scene the scene of client's UI
     */
    public void setScene(Scene scene) {
        this.scene = scene;
    }

    /**
     * Sets the stage variable
     *
     * @param stage the stage of client's UI
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Add label to the labels hashmap that represent all the users in the group
     *
     * @param key   the client ID
     * @param label the client ID's label
     */
    public void addLabelOfUserInGroup(int key, Label label) {
        labelsOfUsersInGroupHashMap.put(key, label);
    }

    /**
     * Add circle to the circle hashmap that represents all the users in the group
     *
     * @param key    the client ID
     * @param circle the client ID"s circle
     */
    public void addCircleOfUserInGroup(int key, Circle circle) {
        circleOfUsersInGroupHashMap.put(key, circle);
    }

    //MUTATOR

    /**
     * Cleans up the group UI by removing all of the values and labels of the user and clearing the text
     *
     * @param username client's username
     * @param group    group name client is in
     * @param groupUrl group url client is in
     * @param clientID client's ID
     */
    public void cleanUp(String username, String group, String groupUrl, int clientID) {
        //Plat form runlater since this is not a javafx thread
        Platform.runLater(() -> {
            //resets the default labels
            clientLabel.setText(username);
            groupNameLabel.setText(group);
            groupUrlLabel.setText(groupUrl);

            //resets text area
            chatTextArea.setText("Welcome to " + group);

            //resets text input and hot key value
            chatInput.setText("");
            pushToTalkHotKeyTextField.setText("K");

            //loops through all labels and removes them and clears hashmap
            for (Map.Entry<Integer, Label> entry : labelsOfUsersInGroupHashMap.entrySet()) {
                groupDetailsAnchorPane.getChildren().remove(entry.getValue());
            }

            labelsOfUsersInGroupHashMap.clear();

            //default circle to hold the default circle in the hashmap
            Circle defaultCircle = null;

            //loop through hashmap and grab default circle and remove rest then clear
            for (Map.Entry<Integer, Circle> entry : circleOfUsersInGroupHashMap.entrySet()) {
                if (entry.getKey() != clientID) {
                    groupDetailsAnchorPane.getChildren().remove(entry.getValue());
                } else {
                    defaultCircle = entry.getValue();
                }
            }

            circleOfUsersInGroupHashMap.clear();

            //puts default circle back into hashmap
            if (defaultCircle != null) {
                circleOfUsersInGroupHashMap.put(clientID, defaultCircle);
            }

            //sets the scene and updates title and shows
            stage.setScene(scene);
            stage.setTitle("Group Voice Chat");
            stage.show();
        });
    }

    /**
     * Removes specific label from the UI based on the clientID of the user then updates all the remaining user's labels positions on the UI
     *
     * @param key user ID to remove
     */
    public void removeLabelAndCircleOfUserInGroup(int key) {
        //Platform runlater since no java fx thread
        Platform.runLater(() -> {
            //remove both label and circle of the clientID key from hashmap and UI
            groupDetailsAnchorPane.getChildren().remove(labelsOfUsersInGroupHashMap.get(key));
            groupDetailsAnchorPane.getChildren().remove(circleOfUsersInGroupHashMap.get(key));

            labelsOfUsersInGroupHashMap.remove(key);
            circleOfUsersInGroupHashMap.remove(key);

            //update positioning of all labels
            int index = 1;
            for (Map.Entry<Integer, Label> entry : labelsOfUsersInGroupHashMap.entrySet()) {
                entry.getValue().setLayoutY(clientLabel.getLayoutY() + ((clientLabel.getHeight() + 5) * index));
                circleOfUsersInGroupHashMap.get(entry.getKey()).setLayoutY(entry.getValue().getLayoutY() + (clientCircle.getLayoutY() - clientLabel.getLayoutY()));
                index++;
            }
        });
    }

    /**
     * Changes the circle color that shows if you're talking to the one specified by the state
     *
     * @param key    user ID
     * @param status status of microphone
     */
    public void changeCircleColorDependingOnStatus(int key, int status) {
        if (status == 1) {
            circleOfUsersInGroupHashMap.get(key).setFill(Color.GREEN);
        } else {
            circleOfUsersInGroupHashMap.get(key).setFill(Color.web("#38383a"));
        }
    }
}

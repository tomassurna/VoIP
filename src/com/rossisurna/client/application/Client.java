package com.rossisurna.client.application;

import com.rossisurna.client.threads.AudioToSpeaker;
import com.rossisurna.client.threads.MicToServer;
import com.rossisurna.client.threads.MicToUI;
import com.rossisurna.client.ui.ClientController;
import com.rossisurna.client.ui.ClientGroupController;
import com.rossisurna.util.Packet;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Client Client method that controls and processes anything related to the client side of the application.
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Mar 18 2019
 */


public class Client implements Runnable {
    
    //Protocol CODES for sending messages to server, most aren't required to be global but this allows for easy access
    // and understanding of the protocols
    private final String IMSG = "ISMG"; //Initial message tp send clientID
    private final String MSG = "MSG"; //Message
    private final String JG = "JG"; //Join group
    private final String CG = "CG"; //Create group
    private final String SU = "SU"; //Send username
    private final String CS = "CS"; //Change Scene
    private final String UJ = "UJ"; //User joined chat for labels and chat
    private final String AP = "AP"; //Audio packet
    private final String LG = "LG"; //leave group
    private final String UL = "UL"; //User Left
    private final String DS = "DS"; //client disconnected
    //out for the socket to server
    private ObjectOutputStream outO; //socket out stream
    private ObjectInputStream inO; //Socket in stream
    private Socket socket = null; //Socket
    //Client identifiers
    private int clientID; //The client ID that is given by server
    private String username; //The user inputed username
    private String groupURL; //The group url they are in
    private String groupName; //The group name they are in
    private boolean isInGroup = false; // If they are in a group
    private boolean tryingToEstablishConnection = false; //if the client is trying to connect to the server
    private boolean close = false; //If the client is in the process of shutting down
    private boolean isGroupSceneMade = false; //Tells the UI whether the group scene has been made already from previous group
    //JavaFX
    private ClientController clientController; //main menu controller for UI
    private ClientGroupController clientGroupController; //group UI controller
    
    //Audio
    private ArrayList<AudioToSpeaker> audioToSpeakerArrayList = new ArrayList<>(); //All of the audio to speaker threads associated for this client, one per user in group
    private MicToServer micToServer; //Mic to server thread
    private MicToUI micToUI; //Mic to UI thread
    
    /**
     * Constructor
     */
    public Client() {
    }
    
    /**
     * Client run method that is in a infinite while true loop checking for incoming packets from the server
     * The run method also processes and launches user related tasks.
     */
    public void run() {
        //launch UI
        clientController = new ClientController();
    
        //Launches UI through Platform.runlater
        new Thread() {
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //FXML loader
                            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("../ui/ClientUI.fxml"));
                            loader.setController(clientController);
    
                            Parent root = loader.load();
                            Scene scene = new Scene(root);
    
                            Stage stage = new Stage();
                            stage.setScene(scene);
                            stage.setTitle("Voice Chat");
                            stage.show();
    
                            //Tells controller stage is shown
                            clientController.setStageShown(true);
                            //Gives controller stage and scene for future purposes
                            clientController.setStage(stage);
                            clientController.setScene(scene);
    
                            //Closes thread
                            Thread.currentThread().interrupt();
                        } catch (IOException e) {
                            //if the UI does not launch then close everything
                            close();
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.start();
    
        //Holds client until UI is launched
        while (!clientController.isStageShown()) {
            try {
//                System.out.printf("%nClient UI waiting to launch...");
                Thread.sleep(100);
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }
        }
    
        //Launches mic to ui
        launchMicrophoneForUI();
    
        //Sets up event handlers for the UI
        setUpEventHandlersForUI();
    
        //Establishes connection to server
        establishConnection();
    
        //waits for incoming messages
        while (!close) {
            try {
                //if the socket inputstream has something avaliable, and while tryingtoconnect is false
                if ((socket.getInputStream().available() > 0) && (!tryingToEstablishConnection)) {
                    //Grab packet and turn it into an object and grab all the relevant data
                    Packet packet = (Packet) inO.readObject();
                    String code = packet.getCode();
                    Object subPacket = packet.getSubPacket();
                    int senderID = packet.getSenderID();
    
                    //takes the incoming code and determines the task for it
                    switch (code) {
                        case IMSG: {
                            //Initial message that sends clientID to client from server
                            clientID = Integer.valueOf((String) subPacket);
                            break;
                        }
                        case MSG: {
                            //Chat message which needs to be added onto chatBox
                            System.out.printf("%nMessage Received: %s", (String) subPacket);
                            addMSGToTextArea((String) subPacket);
                            break;
                        }
                        case CS: {
                            //Change scene protocol that switches from main menu to group when the server tells it to
    
                            //Closes mic to UI
                            closeMicrophoneForUI();
    
                            //Group info
                            String[] details = (String[]) subPacket;
                            groupName = details[1];
                            groupURL = details[0];
    
                            //Sets if in group to true
                            isInGroup = true;
                            if (isGroupSceneMade) {
                                //if true then scene is already made and simply needs to be cleanedup
                                clientGroupController.cleanUp(username, groupName, groupURL, clientID);
                                setUPEventHandlersForGroupUI();
                            } else {
                                //else scene made already and simply needs to be updated for current group info
                                //When client receives this message from server it should change scene into group
//                                System.out.printf("%nMessage Received to Change Scene and URL: %s", groupURL);
    
                                //Change Scene
                                clientGroupController = new ClientGroupController();
    
                                //launch platform runlater to launch new scene
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("../ui/ClientGroupUI.fxml"));
                                            loader.setController(clientGroupController);
    
                                            Parent root = loader.load();
                                            Scene scene = new Scene(root);
    
                                            Stage stage = clientController.getStage();
                                            stage.setScene(scene);
                                            stage.setTitle("Group Voice Chat");
                                            stage.show();
    
                                            clientGroupController.setStageShown(true);
                                            clientGroupController.setStage(stage);
//                                                clientGroupController.getClientLabel().setText(username);
                                            clientGroupController.cleanUp(username, groupName, groupURL, clientID);
                                            clientGroupController.setScene(scene);
                                            clientGroupController.addCircleOfUserInGroup(clientID, clientGroupController.getClientCircle());
    
                                            Thread.currentThread().interrupt();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
    
                                //Waits for the group UI to launch
                                while (!clientGroupController.isStageShown()) {
                                    try {
//                                        System.out.printf("%nClient Group UI waiting to launch...");
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
                                    }
                                }
    
                                //Sets up event handlers for the group UI
                                setUPEventHandlersForGroupUI();
    
                                //Sends test message to the server to clear the socket from userWNG so that userWG socket works
                                sendMessageToServer(MSG, "");

//                                System.out.printf("%nClient %d Group UI Launched", clientID);
    
                                //Launch microphone thread to start being able to capture data
                                try {
                                    clientGroupController.setAmplification(clientController.getMicrophoneSlider().getValue());
                                    micToServer = new MicToServer(outO, clientGroupController, clientID);
                                    new Thread(micToServer).start();
                                } catch (Exception e) {
                                    //Do nothing, only reason this try catch is here is because if two clients launch on same computer
                                    //Then the mic to server will throw thread because the first instance already has access to mic and second
                                    //Instance cannot have access
                                }
                            }
    
                            //copy group URL to clipboard
                            StringSelection stringSelectionGroupURL = new StringSelection(groupURL);
                            Clipboard groupUrlClipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            groupUrlClipBoard.setContents(stringSelectionGroupURL, null);
                            break;
                        }
                        case UJ: {
                            //User joined into the group
//                            System.out.printf("%nAdding New Label (Client.java %d)", clientID);
    
                            //User joined, update labels
                            Label newLabel = new Label();
                            Label defaultLabel = clientGroupController.getClientLabel();
    
                            //Add label to master list
                            clientGroupController.addLabelOfUserInGroup(senderID, newLabel);
    
                            //Set up new label
                            newLabel.setLayoutX(defaultLabel.getLayoutX());
                            newLabel.setLayoutY(defaultLabel.getLayoutY() + ((defaultLabel.getHeight() + 5) * (clientGroupController.getSizeOfLabelsOfUsersInGroupHashMap())));
                            newLabel.setMinWidth(defaultLabel.getWidth());
                            newLabel.setMinHeight(defaultLabel.getHeight());
                            newLabel.setText((String) subPacket);
                            newLabel.setTextFill(Color.BLACK);
                            newLabel.setStyle("-fx-font: 14px;");
                            newLabel.setFont(defaultLabel.getFont());
                            newLabel.setEllipsisString("...");
    
                            //Set up new circle that shows if someone is talking
                            Circle newCircle = new Circle();
                            Circle defaultCircle = clientGroupController.getClientCircle();
    
                            clientGroupController.addCircleOfUserInGroup(senderID, newCircle);
    
                            newCircle.setLayoutX(defaultCircle.getLayoutX());
                            newCircle.setLayoutY(newLabel.getLayoutY() + (defaultCircle.getLayoutY() - defaultLabel.getLayoutY()));
                            newCircle.setRadius(defaultCircle.getRadius());
                            newCircle.setFill(Color.web("#38383A"));
    
                            //Add label using Platform run later since javafx only allows javafx threads to touch UI
                            Platform.runLater(() -> {
                                clientGroupController.getGroupDetailsAnchorPane().getChildren().add(newLabel);
                                clientGroupController.getGroupDetailsAnchorPane().getChildren().add(newCircle);
                                System.out.printf("%nPlatform Later Label added");
                                Thread.currentThread().interrupt();
                            });
                            break;
                        }
                        case AP: {
                            //Audio packet received
//                            System.out.printf("%nAudio Packet Received");
    
                            //Grabs the correct Audio to speaker channel to broadcast data to send packet to while also checking if any need to be closed
                            AudioToSpeaker audioToSpeaker = null;
                            for (AudioToSpeaker audioToSpeakerFL : audioToSpeakerArrayList) {
                                if (audioToSpeakerFL.getUserInGroupID() == packet.getSenderID()) {
                                    audioToSpeaker = audioToSpeakerFL;
                                } else {
                                    if (audioToSpeakerFL.shouldClose()) {
                                        audioToSpeakerFL.close();
                                    }
                                }
                            }
    
                            //If there is no audio to speaker thread for that specific senderID then create a new one
                            if (audioToSpeaker != null) {
                                audioToSpeaker.addToAudioQueue(packet);
                            } else {
                                //else add the new packet to the thread's queue
                                audioToSpeaker = new AudioToSpeaker(packet.getSenderID(), clientGroupController);
                                new Thread(audioToSpeaker).start();
                                audioToSpeaker.addToAudioQueue(packet);
                                audioToSpeakerArrayList.add(audioToSpeaker);
                            }
                            break;
                        }
                        case LG: {
                            //Server told client to leave group
                            //Change scene to main menu
                            //This acts as servers confirmation to client that they left
                            if (!close) {
                                processLeaveGroupProtocol();
                            }
    
                            sendMessageToServer("GB"); //send fake message to clear socket
                            break;
                        }
                        case UL: {
                            //User left group
                            addMSGToTextArea(String.format("User: %s left the group.", (String) subPacket));
                            clientGroupController.removeLabelAndCircleOfUserInGroup(senderID);
    
                            for (AudioToSpeaker audioToSpeakerFL : audioToSpeakerArrayList) {
                                if (audioToSpeakerFL.getUserInGroupID() == packet.getSenderID()) {
                                    audioToSpeakerFL.clearSequence();
                                }
                            }
                            break;
                        }
                        default: {
                            System.out.printf("%nInvalid Code: %s (Client.java %d)", code, clientID);
                        }
                    }
                    //In case clientID hasn't been initialized yet we will have try catch
                    //REMOVED to minimize console spam
//                    try {
//                        System.out.printf("%nClient %d received message: %s ### %s ### %d", clientID, code, subPacket, senderID);
//                    } catch (Exception e) {
//                        System.out.printf("%nClient received message: %s ### %s ### %d", code, subPacket, senderID);
//                    }
                } else {
                    Thread.sleep(100);
                }
            } catch (IOException | NullPointerException e) {
                //if Socket looses connection or becomes null
                if (!tryingToEstablishConnection) {
                    socket = null;
//                    System.out.printf("%nSocket Failed");
                    processLeaveGroupProtocol();
                    establishConnection();
                }
//                    e.printStackTrace();
            } catch (ClassNotFoundException e) {
                //Do nothing, wrong object was sent over stream
            } catch (InterruptedException e) {
                //Do nothing, this is when sleep is interrupted
            }
        }
    }
    
    /**
     * Sends a message to the server from the client
     *
     * @param code    protocol code
     * @param message string message to be sent to server
     */
    private void sendMessageToServer(String code, String message) {
        try {
            outO.writeObject(new Packet(code, clientID, message));
        } catch (IOException e) {
            if (!tryingToEstablishConnection) {
                socket = null;
                System.out.printf("%nSocket Failed");
                processLeaveGroupProtocol();
                establishConnection();
            }
//            e.printStackTrace();
        }
    }
    
    /**
     * Sends a message to the server from the client
     *
     * @param code protocol code
     */
    private void sendMessageToServer(String code) {
        try {
            outO.writeObject(new Packet(code, clientID, null));
        } catch (IOException e) {
            if (!tryingToEstablishConnection) {
                socket = null;
                System.out.printf("%nSocket Failed");
                processLeaveGroupProtocol();
                establishConnection();
            }
//            e.printStackTrace();
        }
    }
    
    /**
     * Tells server to create a group using groupName
     *
     * @param groupName name of the group user wants to create
     */
    private void createGroup(String groupName) {
        sendMessageToServer(CG, groupName);
    }
    
    /**
     * Tells server to join specific group using URL
     *
     * @param url url of the group user wants to join
     */
    private void joinGroup(String url) {
        sendMessageToServer(JG, url);
    }
    
    /**
     * Sets username for the client
     *
     * @param username user's username
     */
    private void setUsername(String username) {
        if (!username.equals(this.username)) {
            this.username = username;
            sendMessageToServer(SU, username);
        }
    }
    
    
    /**
     * Closes the entire application thread and shuts down any child threads
     */
    private void close() {
        processLeaveGroupProtocol();
    
        //If there is a connetion say goodbye to server
        if (!tryingToEstablishConnection) {
            sendMessageToServer(LG);
            sendMessageToServer(DS);
        }
    
        //Try to close mic threads
        try {
            closeMicrophoneForUI();
            micToServer.close();
        } catch (Exception e) {
            //in case one of them is not initalized
        }
        close = true;
        Thread.currentThread().interrupt();
    }
    
    /**
     * Method that launches thread to try to establish connection between client and server, halts any actions till
     * connection is made
     */
    private void establishConnection() {
        //if there isn't already another instance of this method running
        if (!tryingToEstablishConnection) {
            tryingToEstablishConnection = true;
            TextField ipAddressTextField = clientController.getIpAddressTextField();
    
            //Change connection color to orange
            clientController.setStateOfConnectionStatus(2);
    
            //Launched into a new thread to allow for the UI to keep updating and not be lagged while waiting for connection
            new Thread(() -> {
                //While socket is null
                while (socket == null && !close) {
                    //try to connect otherwise sleep 10sec
                    try {
                        socket = new Socket(ipAddressTextField.getText(), 80);
    
                        try {
                            outO.close();
                            inO.close();
                        } catch (Exception e) {
                            //Do nothing, simply means they are null
                        }
    
                        //in and out of the socket
                        outO = new ObjectOutputStream(socket.getOutputStream());
                        inO = new ObjectInputStream(socket.getInputStream());
    
                        tryingToEstablishConnection = false;
    
                        //set color of connection to green
                        clientController.setStateOfConnectionStatus(3);
                    } catch (IOException io) {
                        try {
                            System.out.printf("%nSocket Failed.. retry in 10 sec");
                            //retry in 10 secs
                            Thread.sleep(10000);
                        } catch (Exception ex) {
//                        ex.printStackTrace();
                        }
                    }
                }
    
                Thread.currentThread().interrupt();
            }).start();
        }
    }
    
    /**
     * Protocol to leave the group, this is called when server tells user to leave or when client tells user to leave
     * Also a part of the closing client protocol
     */
    private void processLeaveGroupProtocol() {
        //If the user is in a group then close everything related to the group
        if (isInGroup) {
            //change scene to main menu
            clientController.changeScene();
            isInGroup = false;
            //try to close the mic to server thread and open mic to UI, sleep for a second to allow for the microphone to close
            try {
                micToServer.close();
                if (!close) {
                    launchMicrophoneForUI();
                }
            } catch (Exception e) {
//            e.printStackTrace();
            }
    
            //Close all the audio to speaker's for this client
            for (AudioToSpeaker audioToSpeaker : audioToSpeakerArrayList) {
                audioToSpeaker.close();
            }
    
            //clear list
            audioToSpeakerArrayList.clear();
        }
    }
    
    /**
     * Launches mic to UI thread
     */
    private void launchMicrophoneForUI() {
        micToUI = new MicToUI(clientController);
        new Thread(micToUI).start();
    }
    
    /**
     * Closes mic to UI thread
     */
    private void closeMicrophoneForUI() {
        micToUI.close();
    }
    
    /**
     * Sets up Event Handlers for the base UI menu screen
     */
    private void setUpEventHandlersForUI() {
        //UI CONTROLS
        Button connectButton = clientController.getConnectButton();
    
        //UI TextFields
        TextField createTableInput = clientController.getCreateTableInput();
        TextField joinTableInput = clientController.getJoinTableInput();
        TextField displayNameInput = clientController.getDisplayNameInput();
    
        //Stage
        Stage stage = clientController.getStage();
    
        try {
            connectButton.setOnAction(e -> processConnectAction());
        
            //if When in textfield the user presses enter it processes the connect action
            createTableInput.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                    processConnectAction();
                }
            });
        
            joinTableInput.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                    processConnectAction();
                }
            });
        
            displayNameInput.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                    processConnectAction();
                }
            });
        
            //When the UI closes
            stage.setOnCloseRequest(e -> {
                //If there is a connetion say goodbye to server
                if (!tryingToEstablishConnection) {
                    if (isInGroup) {
                        sendMessageToServer(LG);
                    }
                    sendMessageToServer(DS);
                }
    
                //Try to close mic threads
                try {
                    closeMicrophoneForUI();
                    micToServer.close();
                } catch (Exception e2) {
                    //in case one of them is not initalized
                }
                close = true;
                clientController.getStage().close();
                Thread.currentThread().interrupt();
            });
        } catch (Exception e) {
            //Socket might be null therefore throws exception and renders UI useless
        }
    }
    
    /**
     * The protocol to try to join or create a group, runs only when socket is active
     */
    private void processConnectAction() {
        //Make sure socket is actually active, otherwise throw request
        if (!tryingToEstablishConnection) {
            //UI TextFields
            TextField createTableInput = clientController.getCreateTableInput();
            TextField joinTableInput = clientController.getJoinTableInput();
            TextField displayNameInput = clientController.getDisplayNameInput();
    
            //is the username is filled in with at least 1 character
            if (displayNameInput.getText().length() != 0) {
                setUsername(displayNameInput.getText());
            }
    
            //checks to see which of the two fields is not empty, either join or create
            if (joinTableInput.getText().length() != 0) {
                joinGroup(joinTableInput.getText());
            } else if (createTableInput.getText().length() != 0) {
                createGroup(createTableInput.getText());
            }
        }
    }
    
    /**
     * Sets up Event Handlers for the group UI screen
     */
    private void setUPEventHandlersForGroupUI() {
        //UI Controls
        Button chatButton = clientGroupController.getChatButton();
        Button quitButton = clientGroupController.getQuitButton();
    
        //UI TextFields
        TextField chatInput = clientGroupController.getChatInput();
    
        chatButton.setOnAction(e -> {
            //Send Info
            processChatMessage();
        });
    
        chatInput.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                processChatMessage();
            }
        });
    
        quitButton.setOnAction(e -> {
            sendMessageToServer(LG);
            if (tryingToEstablishConnection) {
                processLeaveGroupProtocol();
            }
        });
    
    }
    
    /**
     * Processes the chat message that was inputed by the user and told to be sent off
     */
    private void processChatMessage() {
        //UI TextFields
        TextField chatInput = clientGroupController.getChatInput();
        TextArea chatTextArea = clientGroupController.getChatTextArea();
    
        String message = chatInput.getText();
        if (message.length() != 0) {
            chatTextArea.setText(chatTextArea.getText() + "\n" + message);
            message = "> " + username + ": " + message;
            sendMessageToServer(MSG, message);
            chatInput.setText("");
        }
    }
    
    /**
     * Adds message to text area of the scene
     *
     * @param message chat message to add to chat area
     */
    private void addMSGToTextArea(String message) {
        TextArea chatTextArea = clientGroupController.getChatTextArea();
        if (chatTextArea != null && message.length() > 0) {
            chatTextArea.setText(chatTextArea.getText() + "\n" + message);
        }
    }
}

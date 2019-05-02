package com.rossisurna.server.threads;

import com.rossisurna.util.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * User with group object that holds all the information about a user with a group
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Mar 18 2019
 */

public class UserWG implements Runnable {

    private final ObjectInputStream inO; //input stream for the socket
    private final ObjectOutputStream outO; //output stream for the socket
    private final int clientID; //Client's specific ID used to identify them within the master HashMap
    //Protocol CODES
    private final String MSG = "MSG"; //Message
    private final String CS = "CS"; //Change scene
    private final String UJ = "UJ"; //user joined group
    private final String AP = "AP"; //audio packet
    private final String LG = "LG"; //leave group
    private final String DS = "DS"; //client disconnected
    //Broadcast
    private final ConcurrentLinkedQueue<Packet> packetQueue; //queue to send packets from client to group then to other clients
    private final ArrayList<Packet> requiredPacketArrayList; //required queue of items to send to client on connection
    //Instruction Queue
    private final ConcurrentLinkedQueue<Packet> instructionQueue;
    //Socket handling
    private Socket socket; //socket
    //User with group data
    private String username; //username of the client
    private String groupURL; //groupURL they are in
    private String groupName; //Group name they are in
    //Util
    private boolean close = false; //tells object when to close
    
    /**
     * Constructor that takes in all the socket information and then all the group information
     *
     * @param socket client's socket
     * @param inO client's objectInputStream
     * @param outO client's objectOutputStream
     * @param clientID client's ID
     * @param username client's username
     * @param groupURL client's URL
     * @param groupName client's group name
     * @param packetQueue client's broadcast queue
     * @param requiredPacketArrayList client's requiredPackets
     * @param instructionQueue instruction queue to server
     */
    public UserWG(Socket socket, ObjectInputStream inO, ObjectOutputStream outO, int clientID, String username, String groupURL, String groupName, ConcurrentLinkedQueue<Packet> packetQueue, ArrayList<Packet> requiredPacketArrayList, ConcurrentLinkedQueue<Packet> instructionQueue) {
        this.inO = inO;
        this.outO = outO;
        this.socket = socket;
        this.clientID = clientID;
        this.username = username;
        this.groupURL = groupURL;
        this.groupName = groupName;
        this.packetQueue = packetQueue;
        this.requiredPacketArrayList = requiredPacketArrayList;
        this.instructionQueue = instructionQueue;

        System.out.printf("%nUser %d Created", clientID);

        //automatically tell client to change scene and give it the URL
        sendMessageToClient(new Packet(CS, clientID, new String[]{groupURL, groupName}));

        //Send all required data to client
        sendRequiredBroadcastDataToClient();
    }

    /**
     * Client run method
     */
    public void run() {
        //Read incoming messages
        //Infinite loop for the user with group while not !close
        while (!close) {
            try {
                //If the socket has received a queue
                while (socket.getInputStream().available() > 0 && !close) {
                    //transfer from input stream to packet object
                    Packet packet = (Packet) inO.readObject();
                    String code = packet.getCode();
                    Object subPacket = packet.getSubPacket();
                    int senderID = packet.getSenderID();

                    //takes the incoming code and determines the task for it
                    switch (code) {
                        case MSG: {
                            //Chat Client message which needs to be added to broadcastqueue
//                                Packet packet = new Packet(MSG, senderID, subPacket);
                            if (((String) subPacket).length() != 0) {
                                packetQueue.add(packet);
                                requiredPacketArrayList.add(packet);
                            }
                            break;
                        }
                        case AP: {
                            //Audio packet received, transfer to group
                            packetQueue.add(packet);
                            break;
                        }
                        case LG: {
                            //instruction transfers back to the group for processing
                            instructionQueue.add(packet);
                            break;
                        }
                        case DS: {
                            //client disconnected, instruction given to group
                            instructionQueue.add(packet);
                            break;
                        }
                        default: {
                            System.out.printf("%nInvalid Code: %s (UserWG.java)", code);
                        }
                    }

                    //In case clientID hasn't been initialized yet we will have try catch
                    System.out.printf("%nUserWG 0 received message: %s ### %s ### %s", code, subPacket, senderID);
                }

                //if input stream is empty then sleep
                Thread.sleep(100);

            } catch (ClassNotFoundException | InterruptedException | IOException e) {
                //if some sort of error with socket then discard information and move on.
//                    e.printStackTrace();
            }
        }
    }

    //GETTERS

    /**
     * Returns the socket of the client
     *
     * @return client's socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Return object output stream of the client
     *
     * @return client's objectOutputStream
     */
    public ObjectOutputStream getOutO() {
        return outO;
    }

    /**
     * Return object input stream of the client
     *
     * @return client's objectInputStream
     */
    public ObjectInputStream getInO() {
        return inO;
    }

    /**
     * Return username of the client
     *
     * @return client's username
     */
    public String getUsername() {
        return username;
    }


    //SETTERS


    //MUTATORS

    /**
     * Sends message over socket to client
     *
     * @param packet packet to send to client
     */
    private void sendMessageToClient(Packet packet) {
        try {
            outO.writeObject(packet);
        } catch (IOException e) {
            //if some sort of error then close the user
            if (!close) {
                closeSocket();
            }
//            e.printStackTrace();
        }
    }

    /**
     * Broadcast any data in the broadcast queue to all the clients expect the one that was send
     *
     * @param packet packet to send to client
     */
    public void sendBroadcastDataToClient(Packet packet) {
        if (packet.getSenderID() != clientID) {
//            switch (packet.getCode()) {
//                case MSG: {
//                    //Sends message from broadcast packet to client
//                    String message = (String) packet.getPacketDetails();
//                    if (message != null) {
//                        sendMessageToClient(MSG, message);
//                    }
//                    break;
//                }
//                case UJ: {
//                    //Sends username of the user that joined for the other clients to know
//                    String username = (String) packet.getPacketDetails();
//
//                    if (username != null) {
//                        sendMessageToClient(UJ, username);
//                    }
//                    break;
//                }
//                default:
//                    //Do nothing for now
//                    break;
//            }
            sendMessageToClient(packet);
        }
    }

    /**
     * Broadcast the required data to all of the clients because the data is required on launch rather then simply being there
     */
    private void sendRequiredBroadcastDataToClient() {
        for (Packet packet : requiredPacketArrayList) {
            sendBroadcastDataToClient(packet);
        }
    }


    /**
     * Tells the user to close this object
     */
    public void close() {
        System.out.printf("%nClosing UserWG: %d", clientID);
        close = true;
        Thread.currentThread().interrupt();
    }

    /**
     * Tells the object to close socket and say goodbye to the client, then shutdown
     */
    public void closeSocket() {
        //Good bye to the client
        close = true;
        sendMessageToClient(new Packet(LG, clientID, null));

        //try to close socket
        try {
            socket.close();
        } catch (IOException e) {
//            e.printStackTrace();
        }

        //close
        close();
    }
}

package com.rossisurna.server.threads;

import com.rossisurna.util.Packet;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Group thread that manages all users under it's control and all the data incoming to it
 * it will also send out all the data to the clients that it has received from the clients
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Mar 18 2019
 */

public class Group implements Runnable {
    
    //Group Data
    private final String groupUrl; //Identifier of group used to join and used by server home
    //Lists
    private final ConcurrentHashMap<Integer, UserWG> userWGConcurrentHashMap = new ConcurrentHashMap<>(); //Stores all users within the group, key is clientID
    private final ConcurrentLinkedQueue<Packet> packetQueue = new ConcurrentLinkedQueue<>(); //Queue that holds all the info that need to be send out to the clients
    private final ArrayList<Packet> requiredPacketArrayList = new ArrayList<>(); //A list that will hold all the information a user will need the moment they join the group.
    private final ConcurrentLinkedQueue<Packet> instructionQueue = new ConcurrentLinkedQueue<>(); //Instruction queue that tells the the group what to do from the client's perspective
    private final ConcurrentLinkedQueue<Packet> serverInstructionQueue; //Instruction queue for the server to do tasks
    //Protocol Codes
    private final String UJ = "UJ"; // User joined
    private final String LG = "LG"; //leave group
    private final String UL = "UL"; //User left
    private final String MSG = "MSG"; //message
    private final String DS = "DS"; //client disconnected
    private String groupName; //name of the group
    //Util
    private boolean close = false; //tells group to shutdown
    //Broadcaster
    private Broadcaster broadcaster;
    
    /**
     * Constructor that takes in group url, name, and queue to add packets to for the server
     *
     * @param groupUrl  group url
     * @param groupName group name
     * @param serverInstructionQueue instruction queue for server
     */
    public Group(String groupUrl, String groupName, ConcurrentLinkedQueue<Packet> serverInstructionQueue) {
        this.groupUrl = groupUrl;
        this.groupName = groupName;
        this.serverInstructionQueue = serverInstructionQueue;
    
        //launches broadcaster
        broadcaster = new Broadcaster(packetQueue, userWGConcurrentHashMap);
        new Thread(broadcaster).start();
    
    }
    
    /**
     * Main run method that is in a constant loop waiting for instructions to come in from the clients
     */
    public void run() {
        while (!close) {
            //Launch thread which will wait for a broadcast packet to then broadcast out to every client
            while (!instructionQueue.isEmpty()) {
                Packet instruction = instructionQueue.remove();
    
                //sends code to the proper protocol
                switch (instruction.getCode()) {
                    case LG: {
                        //user wants to leave group therefore make new WNG and delete this
                        int clientID = instruction.getSenderID();
    
                        //add to the server instruction queue and then to broadcast queue
                        serverInstructionQueue.add(new Packet(LG, clientID, userWGConcurrentHashMap.get(clientID)));
                        packetQueue.add(new Packet(UL, clientID, userWGConcurrentHashMap.get(clientID).getUsername()));
    
                        //remove user
                        userWGConcurrentHashMap.remove(clientID);
    
                        //remove the packet from the requirepacketlist that says he ever joined
                        int index = 0;
                        int removeIndex = -1;
                        for (Packet packet : requiredPacketArrayList) {
                            if ((packet.getSenderID() == clientID) && (packet.getCode().equals(UJ))) {
                                removeIndex = index;
                                break;
                            }
                            index++;
                        }
    
                        //remove packet from list
                        if (removeIndex != -1) {
                            System.out.printf("%n-----Sender ID: %d, Client ID: %d Code: %s, Index: %d, IndexRemove: %d", requiredPacketArrayList.get(removeIndex).getSenderID(), clientID, requiredPacketArrayList.get(removeIndex).getCode(), index, removeIndex);
                            requiredPacketArrayList.remove(removeIndex);
                        }
    
                        //check if group is empty, if so then close it self
                        checkIfGroupIsEmpty();
                        break;
                    }
                    case DS: {
                        //User wants to disconnect, close his objects and remove them
                        userWGConcurrentHashMap.get(instruction.getSenderID()).closeSocket();
                        userWGConcurrentHashMap.remove(instruction.getSenderID());
    
                        //check if group is empty
                        checkIfGroupIsEmpty();
                        break;
                    }
                    default: {
                        System.out.printf("%nInvalid Code: %s (Group.java)", instruction.getCode());
                    }
                }
            }
    
            //Sleep if the queue is empty
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }
    
        }
    }
    
    /**
     * Creates and adds user to the master hashmap for this group
     *
     * @param socket   client's socket
     * @param inO      objectInputStream of client's socket
     * @param outO     objectOutputStream of client's socket
     * @param clientID client's ID
     * @param username client's username
     */
    public void addUserToGroup(Socket socket, ObjectInputStream inO, ObjectOutputStream outO, int clientID, String username) {
        //creates new user with group
        UserWG userWG = new UserWG(socket, inO, outO, clientID, username, groupUrl, groupName, packetQueue, requiredPacketArrayList, instructionQueue);
        new Thread(userWG).start();
    
        //Add to broadcast queue and required queue that user joined
        packetQueue.add(new Packet(UJ, clientID, username));
        packetQueue.add(new Packet(MSG, clientID, String.format("%s has joined the group.", username)));
        requiredPacketArrayList.add(new Packet(UJ, clientID, username));
    
        //adds user into this group
        userWGConcurrentHashMap.put(clientID, userWG);
    
        System.out.printf("%nClient %d (%s) had been added to group %s through url: %s", clientID, username, groupName, groupUrl);
    }
    
    /**
     * Closes the group thread
     */
    public void close() {
        packetQueue.add(new Packet(LG, 0, null));
    
        for (Map.Entry<Integer, UserWG> entry : userWGConcurrentHashMap.entrySet()) {
            entry.getValue().closeSocket();
        }
    
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
//            e.printStackTrace();
        }
        broadcaster.close();
        close = true;
        Thread.currentThread().interrupt();
    }
    
    /**
     * Checks if the group is empty and if so then close itself
     */
    private void checkIfGroupIsEmpty() {
        if (userWGConcurrentHashMap.size() == 0) {
            System.out.printf("%nClosing Group Url: %s, Name %s", groupUrl, groupName);
            close();
        }
    }
}

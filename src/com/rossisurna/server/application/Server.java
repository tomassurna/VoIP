package com.rossisurna.server.application;

import com.rossisurna.server.threads.Group;
import com.rossisurna.server.threads.SocketServer;
import com.rossisurna.server.threads.UserWG;
import com.rossisurna.server.threads.UserWNG;
import com.rossisurna.util.Packet;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Server object that manages all communication between client and server
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Mar 18 2019
 */

public class Server implements Runnable {

    //Data Structures
    private final ConcurrentHashMap<Integer, UserWNG> userWNGConcurrentHashMap = new ConcurrentHashMap<>(); //Hashmap that holds all the users with no group, key is the clientID
    private final ConcurrentHashMap<String, Group> groupConcurrentHashMap = new ConcurrentHashMap<>(); //Hashmap that holds all the groups, ket is the URL
    private final ConcurrentLinkedQueue<Packet> instructionQueue = new ConcurrentLinkedQueue<>(); //Queue to hold all the instructions for the sever to rn
    private final ArrayList<String> usedURLS = new ArrayList<>(); //ArrayList that holds all the URLS actively used for the groups
    //Protocols
    private final String LG = "LG"; //tells user to leave group
    private final String DS = "DS"; //disconnect user
    private final String JG = "JG"; //move user into a pre built group
    private final String CG = "CG"; //create group object
    //Util
    private boolean close = false; //Tells the server when to shut down
    //Sockets
    private SocketServer socketServer; //socket subserver that takes in all incoming sockets
    
    
    /**
     * Constructor
     */
    public Server() {
        System.out.printf("%nServer Home Launched.");
    }

    /**
     * Main run method for the server that waits for instructions to come into it's queue for it to process
     */
    @Override
    public void run() {
        //Launch Socket Server
        socketServer = new SocketServer(userWNGConcurrentHashMap, instructionQueue);
        new Thread(socketServer).start();

        //Server is in a constant loop checking to see if the queue is empty, if so then sleep, loop while close is false
        while (!close) {
            //If there are instructions to run
            while (!instructionQueue.isEmpty()) {
                //Handle instruction (Instruction is a rename of the Packet object
                System.out.printf("%nServer Instruction received, Size: %d", instructionQueue.size());
                Packet instruction = instructionQueue.remove();

                //Switch statement to check which CODE the instruction is
                switch (instruction.getCode()) {
                    case JG: {
                        //Join Group instruction
                        int clientID = instruction.getSenderID();
                        String groupURL = (String) instruction.getSubPacket();

                        //Get user and group
                        UserWNG userWNG = userWNGConcurrentHashMap.get(clientID);
                        Group group = groupConcurrentHashMap.get(groupURL);
                        new Thread(group).start();

                        //If both aren't null then...
                        if (group != null && userWNG != null) {
                            //Move user into the group
                            group.addUserToGroup(userWNG.getSocket(), userWNG.getInO(), userWNG.getOutO(), userWNG.getClientID(), userWNG.getUsername());
                            userWNGConcurrentHashMap.remove(clientID);
                            userWNG.close(); //End userWNG thread
                            System.out.printf("%nMoved Client %d to Group with URL %s", clientID, groupURL);
                        } else {
                            System.out.printf("%nNo such group error: %s", groupURL);
                        }
                        break;
                    }
                    case CG: {
                        //Create Group instruction
                        int clientID = instruction.getSenderID();
                        String groupName = (String) instruction.getSubPacket();

                        //Get user from hashmap
                        UserWNG userWNG = userWNGConcurrentHashMap.get(clientID);

                        //If user is not null
                        if (userWNG != null) {
                            //Make URL for group and make group
                            String groupURL = groupURLGenerator();
                            Group group = new Group(groupURL, groupName, instructionQueue);
                            new Thread(group).start();

                            //Put group into master hashmap
                            groupConcurrentHashMap.put(groupURL, group);

                            //Add user to the group
                            group.addUserToGroup(userWNG.getSocket(), userWNG.getInO(), userWNG.getOutO(), userWNG.getClientID(), userWNG.getUsername());
                            userWNGConcurrentHashMap.remove(clientID);
                            userWNG.close();

                            System.out.printf("%nCreated '%s' Group with URL '%s', Moved Client %d to Group ", groupName, groupURL, clientID);
                        } else {
                            //No such group
                            System.out.printf("%nUser is null error: id %d", clientID);
                        }
                        break;
                    }
                    case LG: {
                        //moves client from UserWG to UserWNG
                        UserWG userWG = (UserWG) instruction.getSubPacket();

                        //new userWNG object
                        UserWNG userWNG = new UserWNG(userWG.getSocket(), userWG.getInO(), userWG.getOutO(), instruction.getSenderID(), userWG.getUsername(), instructionQueue);
                        new Thread(userWNG).start();

                        //add the new object to hashmap
                        userWNGConcurrentHashMap.put(instruction.getSenderID(), userWNG);

                        //close old object
                        userWG.close();

                        System.out.printf("%nUserWNG Size: %d", userWNGConcurrentHashMap.size());
                        break;
                    }
                    case DS: {
                        //Disconnect user from server by removing their object and closing it
                        userWNGConcurrentHashMap.get(instruction.getSenderID()).closeSocket();
                        userWNGConcurrentHashMap.remove(instruction.getSenderID());
                        break;
                    }
                    default: {
                        System.out.printf("%nInvalid Code: %s (Server.java)", instruction.getCode());
                    }
                }
            }
            try {
                //if queue is empty then sleep
                Thread.sleep(100);
            } catch (InterruptedException e) {
//                    e.printStackTrace();

            }
        }
    }

    /**
     * Creates a randomly generated group URL length: 6, using alphabet
     *
     * @return randomly generate group URL
     */
    private String groupURLGenerator() {
        // Builds random url for group
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder url = new StringBuilder();
        Random randomGen = new Random();

        //Randomly choose letters from the alphabet
        while (url.length() < 6) {
            url.append(alphabet.charAt((int) (randomGen.nextFloat() * alphabet.length())));
        }

        //Checks if the url has been used before
        boolean used = false;

        for (String urlS : usedURLS) {
            if (urlS.equals(url.toString())) {
                used = true;
            }
        }

        //If used then return a newly gen'd url through recursion
        if (used) {
            return groupURLGenerator();
        } else {
            usedURLS.add(url.toString());
            return url.toString();
        }

        //in theory this could use all URLS ever but its VERY unlikely in a final project
    }


    /**
     * Tells server to shut down making it close all of it's sub threads and tell all users to disconnect.
     */
    public void close() {
        close = true;

        //close socket server
        socketServer.close();

        //kick all groups
        for (Map.Entry<String, Group> entry : groupConcurrentHashMap.entrySet()) {
            entry.getValue().close();
        }

        //kick all users
        for (Map.Entry<Integer, UserWNG> entry : userWNGConcurrentHashMap.entrySet()) {
            entry.getValue().closeSocket();
        }

        System.out.printf("%nServer Stopped.");
        Thread.currentThread().interrupt();
    }


}


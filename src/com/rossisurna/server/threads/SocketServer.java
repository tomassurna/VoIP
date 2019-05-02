package com.rossisurna.server.threads;

import com.rossisurna.util.Packet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * This class is used to accept all incoming sockets from there adds them into
 * the master hashmap that contains alls the user with no group
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Mar 23 2019
 */

public class SocketServer implements Runnable {

    private static int userWNGKeyCount = 1; //Current key count for usersWithNoGroup
    private final ConcurrentLinkedQueue<Packet> instructionQueue; //Queue to add instructions for server home to run
    //Lists
    private ConcurrentHashMap<Integer, UserWNG> userWNGConcurrentHashMap; //Hashmap that holds all users with no group, data is taken in from server home
    //Sockets
    private ServerSocket serverSocket; //socket server itself

    //Util
    private boolean close = false; //tells server when to close

    /**
     * Constructor that takes in master user hashmap and instruction queue for the server
     *
     * @param userWNGConcurrentHashMap user WNG hashmap list
     * @param instructionQueue         instruction Queue
     */
    public SocketServer(ConcurrentHashMap<Integer, UserWNG> userWNGConcurrentHashMap, ConcurrentLinkedQueue<Packet> instructionQueue) {
        this.userWNGConcurrentHashMap = userWNGConcurrentHashMap;
        this.instructionQueue = instructionQueue;
    }

    /**
     * Main run method that launches server and then sits and waits for incoming sockets until it's closed
     */
    public void run() {
        /*
            This thread runs the server socket that will accept all incoming sockets and then creates
            the user object and throws them into a hashmap
         */
        try {
            //launches server socket
            serverSocket = new ServerSocket(80); //Add port and IP address
            System.out.printf("%nSocket Server Launched, Address: %s, Chat Server Port: %d, (SocketServer.java)", serverSocket.getInetAddress(), serverSocket.getLocalPort());

            //while the server is running
            while (!close) {
                //launch into a new thread that manages the specific socket
                Socket s = serverSocket.accept();

                //create new user and add them to hashmap and launch their thread
                UserWNG userWNG = new UserWNG(s, userWNGKeyCount, instructionQueue);
                new Thread(userWNG).start();
                userWNGConcurrentHashMap.put(userWNGKeyCount++, userWNG);

                System.out.printf("%nSocket received, HashMap Size: %d (ServerHome (Client.java))", userWNGConcurrentHashMap.size());
            }
        } catch (IOException e) {
            //error with socket, discard
//            e.printStackTrace();
        }
    }

    /**
     * Try to close server and close thread
     */
    public void close() {
        try {
            System.out.printf("%nSocket Server Closed");
            close = true;
            serverSocket.close();
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            Thread.currentThread().interrupt();
//            e.printStackTrace();
        }
    }
}

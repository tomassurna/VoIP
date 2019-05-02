package com.rossisurna.server.threads;

import com.rossisurna.util.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * User with no group thread that a client runs on when they have no group to be apart of
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Mar 18 2019
 */

public class UserWNG implements Runnable {

    //Queues
    private final ConcurrentLinkedQueue<Packet> instructionQueue; //instruction queue that the server is waiting for instructions
    private final int clientID; //Client's specific ID used to identify them within the master HashMap
    //Protocol CODES
    private final String IMSG = "ISMG"; //Initial message tp send clientID
    private final String MSG = "MSG"; //Message
    private final String JG = "JG"; //Join group
    private final String CG = "CG"; //Create group
    private final String SU = "SU"; //Set Username
    private final String LG = "LG"; //left group
    private final String DS = "DS"; //client disconnected
    //Socket handling
    private ObjectOutputStream outO; //output stream of the socket
    private ObjectInputStream inO; //input stream of the socket
    private Socket socket; //the socket connection to the client
    //User with no group data
    private String username; //the client username
    //Util
    private boolean close = false; //tells this object to quit and leave


    /**
     * Constructor that takes in the physical socket, clientid, and then the queue and creates all
     * the remaining output and input streams
     *
     * @param socket           client's socket
     * @param clientID         client's ID
     * @param instructionQueue instruction queue for server
     */
    public UserWNG(Socket socket, int clientID, ConcurrentLinkedQueue<Packet> instructionQueue) {
        //try to make the in and out for the socket
        try {
            this.socket = socket;
            outO = new ObjectOutputStream(socket.getOutputStream());
            inO = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            closeSocket();
        }

        this.instructionQueue = instructionQueue;
        this.clientID = clientID;

        System.out.printf("%nUser With Group, ClientID: %dCreated", clientID);
    }

    /**
     * Constructor that takes in the socket, in, out, and all other information rather then making itself. This is used when userWG goes to userWNG, rather then on initial connection.
     *
     * @param socket           client's socket
     * @param inO              client's objectInputStream from socket
     * @param outO             client's objectOutputStream from socket
     * @param clientID         client's ID
     * @param username         client's username
     * @param instructionQueue instruction queue for server
     */
    public UserWNG(Socket socket, ObjectInputStream inO, ObjectOutputStream outO, int clientID, String username, ConcurrentLinkedQueue<Packet> instructionQueue) {
        this.socket = socket;
        this.inO = inO;
        this.outO = outO;
        this.clientID = clientID;
        this.username = username;
        this.instructionQueue = instructionQueue;

        //send confirmation to client that they left group
        sendMessageToClient(LG);
    }

    /**
     * Client run method
     */
    public void run() {
        //send initial message to client giving them their clientID
        sendMessageToClient(IMSG, String.valueOf(clientID));

        //Read incoming messages, acts as if while(true) for 99% of the time
        while (!close) {
            try {
                //If input stream received object
                while (socket.getInputStream().available() > 0 && !close) {
                    Packet packet = (Packet) inO.readObject();
                    String code = packet.getCode();
                    Object subPacket = packet.getSubPacket();
                    int senderID = packet.getSenderID();

                    //takes the incoming code and determines the task for it
                    switch (code) {
                        case SU: {
                            //set username that is received from socket
                            username = (String) subPacket;
                            break;
                        }
                        case JG: {
                            //Join group case
                            //When you join a group the sever receives the instruction and checks if group
                            //actually exists, if so then it moves the user, if not then nothing. The user knows they are being called
                            //when the specific method is called below.

                            //, tells server by giving it an instruction
                            instructionQueue.add(new Packet(JG, clientID, subPacket));
                            break;
                        }
                        case CG: {
                            //create group case, tells server by giving it an instruction
                            instructionQueue.add(new Packet(CG, clientID, subPacket));
                            break;
                        }
                        case DS: {
                            //Tells server that user disconnected.
                            instructionQueue.add(packet);
                            break;
                        }
                        default: {
                            System.out.printf("%nInvalid Code:%s (UserWNG.java)", code);
                        }
                    }

                    //In case clientID hasn't been initialized yet we will have try catch
                    System.out.printf("%nServer 0 received message: %s ### %s ### %d", code, subPacket, senderID);
                }

                //sleep if the socket input is empty
                Thread.sleep(100);

            } catch (ClassNotFoundException | IOException | InterruptedException e) {
                //Do nothing but discard all information
//                e.printStackTrace();
            }
        }
    }

    //GETTERS

    /**
     * get username
     *
     * @return client's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * get clientID
     *
     * @return client's ID
     */
    public int getClientID() {
        return clientID;
    }

    /**
     * get in of the socket
     *
     * @return client's objectInputStream
     */
    public ObjectInputStream getInO() {
        return inO;
    }

    /**
     * get out of the socket
     *
     * @return client's objectOutputStream
     */
    public ObjectOutputStream getOutO() {
        return outO;
    }

    /**
     * Get the socket of the client
     *
     * @return client's socket
     */
    public Socket getSocket() {
        return socket;
    }

    //SETTERS

    //MUTATORS

    /**
     * Send message to client using message format
     *
     * @param code    packet's protocol code
     * @param message packet's message in string form
     */
    private void sendMessageToClient(String code, String message) {
        try {
            outO.writeObject(new Packet(code, 0, message));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        out.println(code + "###" + message + "###0");
    }

    /**
     * Send message to client using code only, empty packet
     *
     * @param code packet's protocol code
     */
    private void sendMessageToClient(String code) {
        try {
            outO.writeObject(new Packet(code, 0, null));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        out.println(code + "###" + message + "###0");
    }

    /**
     * Close the user thread
     */
    public void close() {
        System.out.printf("%nClosing UserWNG %d Thread", clientID);
        close = true;
        Thread.currentThread().interrupt();
    }

    /**
     * Close the socket and tell user bye.
     */
    public void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        close();
    }
}

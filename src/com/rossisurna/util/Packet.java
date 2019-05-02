package com.rossisurna.util;

import java.io.Serializable;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Object that is sent between client and server to transfer information
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Apr 01 2019
 */

public class Packet implements Serializable {

    //Packet info
    private final String code; //the protocol code of the packet
    private final int senderID; //the sender id of the packet
    private final Object subPacket; //the subpacket of the pakcet, which could be any object

    /*
        Possible Codes:
        MSG - Chat Message
        UJ - User Joined from server to client to update UI
        CS - Change Scene joined group to update UI
        IMSG - Initial message to send clientID from server to client
        JG - Coin group x
        CG - Create group x
        SU - Send Username from client to server
        AP - Audio Packet w/ data
        LG - Leave group
     */

    /**
     * Constructor that takes in protocol code, senderID, and the subpacket object
     *
     * @param code      protocol code
     * @param senderID  sender ID of packet
     * @param subPacket subpacket of object you want to send
     */
    public Packet(String code, int senderID, Object subPacket) {
        this.code = code;
        this.senderID = senderID;
        this.subPacket = subPacket;
    }

    /**
     * Returns the code specific to the packet
     *
     * @return protocol code of packet
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the origin client ID of the packet
     *
     * @return origin ID of packet
     */
    public int getSenderID() {
        return senderID;
    }

    /**
     * Returns the subpacket object that holds all the data
     *
     * @return object sent within packet
     */
    public Object getSubPacket() {
        return subPacket;
    }
}

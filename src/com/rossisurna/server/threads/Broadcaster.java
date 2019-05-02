package com.rossisurna.server.threads;

import com.rossisurna.util.Packet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Broadcaster thread waits for items in the broadcast queue to show up and once the packets are there
 * the broadcaster loops through all the clients for the group and sends the packet to them except to the origin sender
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Apr 02 2019
 */

public class Broadcaster implements Runnable {

    //Broadcast queue
    private final ConcurrentLinkedQueue<Packet> packetQueue;

    //List of users
    private final ConcurrentHashMap<Integer, UserWG> userWGConcurrentHashMap;

    //Util
    private boolean close = false; //tell broadcaster to shut down

    /**
     * Constructor, takes in broadcast queue and list of users to broadcast to
     *
     * @param packetQueue             broadcast queue
     * @param userWGConcurrentHashMap userWG hashmap list
     */
    Broadcaster(ConcurrentLinkedQueue<Packet> packetQueue, ConcurrentHashMap<Integer, UserWG> userWGConcurrentHashMap) {
        this.packetQueue = packetQueue;
        this.userWGConcurrentHashMap = userWGConcurrentHashMap;
        System.out.printf("%nBroadcaster Up and Running");
    }

    /**
     * Run Method that sits and waits for items to be put into broadcast queue to be sent out
     */
    public void run() {
        while (!close) {
            //while queue is not empty send out packets
            while (!packetQueue.isEmpty()) {
                //send to all users but orgin user
                Packet packet = packetQueue.remove();
                for (Map.Entry<Integer, UserWG> userWG : userWGConcurrentHashMap.entrySet()) {
                    userWG.getValue().sendBroadcastDataToClient(packet);
                }
            }

            //sleep if queue is empty
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }

        }
    }

    /**
     * Close broadcaster
     */
    public void close() {
        close = true;
        Thread.currentThread().interrupt();
    }
}

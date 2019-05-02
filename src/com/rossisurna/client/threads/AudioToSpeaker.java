package com.rossisurna.client.threads;

import com.rossisurna.client.ui.ClientGroupController;
import com.rossisurna.util.AudioPacket;
import com.rossisurna.util.Packet;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.GZIPInputStream;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Audio to speaker thread takes in audio packets and plays the sounds to the speakers
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Apr 02 2019
 */

public class AudioToSpeaker implements Runnable {
    
    //User info
    private int userInGroupID; //user id of the client who's thread this is
    
    //Packet queue
    private Queue<Packet> packetQueue = new LinkedList<>(); //queue of all the packets waiting to be processed
    
    //Client UI
    private ClientGroupController clientGroupController;
    private boolean isCircleGreen = false; //true when the circle indicator is green
    private long lastGreenCircleTime = System.currentTimeMillis(); //The last time the circle was green
    
    //Audio
    private SourceDataLine sourceDataLine; //speaker
    private int sequence = 0;
    
    //Util
    private long lastTimeAPacketArrived = System.currentTimeMillis(); //The last time a packet arrived to this thread
    private boolean close = false; //Tells the thread to close everything when true
    
    /**
     * Constructor that takes in userID of the packets that go this thread and the UI controller. The constructor also
     * tries to make a connection to the speaker and open it.
     *
     * @param userInGroupID         the client ID of the user of which this object plays audio specifically from
     * @param clientGroupController the client's groupController
     */
    public AudioToSpeaker(int userInGroupID, ClientGroupController clientGroupController) {
        this.userInGroupID = userInGroupID;
        this.clientGroupController = clientGroupController;
        System.out.printf("%nNew AudioToSpeaker created for user: %d", userInGroupID);
    
        //Set up audio format and grab speaker data line
        try {
            //SourceDataLine = speaker
            final AudioFormat audioFormat = AudioPacket.audioFormat;
            final DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
    
            //Open speaker
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();
        } catch (LineUnavailableException e) {
            //if some sort of error, close thread
//            e.printStackTrace();
            close();
        }
    }
    
    /**
     * Client run method that is in a while loop until close, that checks for new packets and then procsses them to the speaker
     */
    public void run() {
        //keeps track of the sequence of pakets
    
        while (!close) {
            //If packet Queue is not empty
            while (!packetQueue.isEmpty()) {
                //Updates the last time a packet arrived
                lastTimeAPacketArrived = System.currentTimeMillis();
    
                //if the circle is not green then make it green and update time of last green circle
                if (!isCircleGreen) {
                    clientGroupController.changeCircleColorDependingOnStatus(userInGroupID, 1);
                    isCircleGreen = true;
                    lastGreenCircleTime = System.currentTimeMillis();
                }
    
                //Grab packets
                Packet packet = packetQueue.remove();
    
                //Grab audio packet
                AudioPacket audioPacket = (AudioPacket) packet.getSubPacket();
    
                if (sequence < audioPacket.getSequenceNumber()) {
                    sequence = audioPacket.getSequenceNumber();
                }
    
                //IF Audio packet is not null
                if (sequence == audioPacket.getSequenceNumber()) {
//                    System.out.printf("%nAudio Packet not null, Sequence number: %d", audioPacket.getSequenceNumber());
    
                    try {
                        //Decompress the data
                        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(audioPacket.getAudioDataInByteForm()));
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        
                        //Write the data to the byte stream
                        while (true) {
                            int intByte = gzipInputStream.read();
                            if (intByte == -1) {
                                //End byte
                                break;
                            } else {
                                byteArrayOutputStream.write((byte) intByte);
                            }
                        }
        
                        //Writes bytes to the speaker
                        byte[] audioToSpeaker = byteArrayOutputStream.toByteArray();
                        sourceDataLine.write(audioToSpeaker, 0, audioToSpeaker.length);
                    } catch (Exception e) {
                        //if some issue then discard packet
//                        e.printStackTrace();
                    }
                } else {
                    System.out.printf("%nAudio Packet null or wrong sequence number");
                }
            }
        
            //if circle is green is true and its been 1 second since last time a packet arrived, then change back to grey
            if (isCircleGreen && ((System.currentTimeMillis() - lastGreenCircleTime) > 1000)) {
                clientGroupController.changeCircleColorDependingOnStatus(userInGroupID, 0);
                isCircleGreen = false;
                sourceDataLine.write(new byte[]{}, 0, 0);
            }
        
            //Sleep if nothing to do
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }
        }
    }
    
    /**
     * Closes the thread
     */
    public void close() {
        close = true;
        sourceDataLine.close();
//        System.out.printf("%nAudioToSpeaker closing %d", userInGroupID);
        Thread.currentThread().interrupt();
    }
    
    /**
     * Add packet to the queue
     *
     * @param packet audio packet to play
     */
    public void addToAudioQueue(Packet packet) {
        packetQueue.add(packet);
    }
    
    /**
     * Grab the client ID associated with this thread
     *
     * @return return the client ID of the user of which this object plays audio specifically from
     */
    public int getUserInGroupID() {
        return userInGroupID;
    }
    
    /**
     * Tells the client whether this thread hasn't received any packets in a while and should be closed
     *
     * @return return whether this thread has been inactive for 5 or more minutes
     */
    public boolean shouldClose() {
        //should be closed if been 5 min since last packet arrived.
//        System.out.printf("%nAudioToSpeaker checking if should close %d", userInGroupID);
        return (System.currentTimeMillis() - lastGreenCircleTime) > 300000;
    }
    
    /**
     * Reset the sequence of this object incase the user rejoins the group
     */
    public void clearSequence() {
        sequence = 0;
    }
}

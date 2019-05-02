package com.rossisurna.client.threads;

import com.rossisurna.client.ui.ClientGroupController;
import com.rossisurna.util.AudioPacket;
import com.rossisurna.util.Packet;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPOutputStream;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * MicToServer threads connects to the default microphone on the system and captures the audio when the hot key is pressed
 * then formats the data in bytes and creates an Audio packet and then a Packet object, sending the data to the server
 * which processes it and places it into the broadcastQueue for the remaining clients to receive.
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Apr 02 2019
 */

public class MicToServer implements Runnable {

    //Server connection
    private final ObjectOutputStream outO; //output stream
    //JavaFX
    private final ClientGroupController clientGroupController; //UI Controller
    //Client info
    private final int clientID; //the user's client ID
    //Microphone
    private TargetDataLine microphone; //microphone dataline
    private char pushToTalkHotKey; //hot key for push to talk
    private boolean pushToTalkHotKeyActive; //if hot key is being pressed
    private long lastTimeMicWasActive = System.currentTimeMillis(); //the last time the mic was active
    private int timeMicIsActivePastHotkeyPress = 50; //how long to wait after hot key is no longer being pressed to stop mic (in mili sec)
    private boolean isCircleGreen = false; //is the user's circle green
    private boolean close = false; //whether the thread needs to be closed


    /**
     * Constructor that takes in the output stream, controller, clientID and then sets up the handlers and
     * initializes the microphone
     *
     * @param outO                  the objectOutputStream of the client's socket
     * @param clientGroupController the client's group controller
     * @param clientID              the client's ID
     */
    public MicToServer(ObjectOutputStream outO, ClientGroupController clientGroupController, int clientID) {
        this.outO = outO;
        this.clientGroupController = clientGroupController;
        this.pushToTalkHotKey = 'K';
        this.pushToTalkHotKeyActive = false;
        this.clientID = clientID;
        //Launches event handlers for the hot key and for the hot key setting
        setUpPushToTalkEventHandlers();

        //Get Microphone
        AudioFormat audioFormat = AudioPacket.audioFormat;
        try {
            //Gets default microphone in system
            microphone = AudioSystem.getTargetDataLine(audioFormat);
            microphone.open(audioFormat);
            microphone.start();
        } catch (LineUnavailableException e) {
            close = true;
            e.printStackTrace();
            //No mic so close thread
            Thread.currentThread().interrupt();
        }

        System.out.printf("%nMice Thread Launched for: %d", clientID);
    }

    /**
     * Run method for the thread which grabs microphone data and sends it to the server after processing it.
     */
    public void run() {

        //shifts the lastime the mic was active.
        lastTimeMicWasActive -= timeMicIsActivePastHotkeyPress;

        //queue for the packets to be processed from mic to the server
        Queue<byte[]> bytes = new ConcurrentLinkedQueue<>();

        //the amplification for the mic defined by user
        double amplification = clientGroupController.getAmplification();

        //New thread that takes in from the queue and processes the packets
        new Thread(() -> {
            //the sequence count
            int sequence = 0;

            //while the thread is running
            while (!close) {
                //while the queue is not empty
                while (!bytes.isEmpty()) {
                    //grab the segment
                    byte[] audioPacketSegment = bytes.remove();

                    //Calculates the average volume of the packet
                    long volume = 0;
                    for (int i = 0; i < audioPacketSegment.length; i++) {
                        audioPacketSegment[i] *= amplification;
                        volume += Math.abs(audioPacketSegment[i]);
                    }
                    volume *= 2.5;
                    volume /= audioPacketSegment.length;

                    //turns the volume into an average
                    double averageVolume = volume / 100.0;

                    //Create audio packet and send to server if the audi packet is a min of 50% volume, else discard
                    if (averageVolume >= .5) {
                        System.out.printf("%nSize: %d, Queue Size: %d, Total: %.2f", audioPacketSegment.length, bytes.size(), averageVolume);
                        try {
                            //Creates packet and compresses it
                            Packet packet;

                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
                            gzipOutputStream.write(audioPacketSegment);
                            gzipOutputStream.flush();
                            gzipOutputStream.close();
                            byteArrayOutputStream.flush();
                            byteArrayOutputStream.close();

                            //finishes creating packet and sends it off to server
                            packet = new Packet("AP", clientID, new AudioPacket(byteArrayOutputStream.toByteArray(), sequence++));
                            outO.writeObject(packet);
                        } catch (IOException e) {
                            //if error, discard packet
//                                e.printStackTrace();
                        }
                    }
                }
                //sleep for 10 milisec then check if queue is not empty
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
//                        e.printStackTrace();
                }

            }
            Thread.currentThread().interrupt();
        }).start();

        //While loop for the mic taking in data, while not close
        while (!close) {
            //hot key is not active and past the timeMicIsActivePastHotKeyPress threshold, then flush data and remove green circle
            if (!pushToTalkHotKeyActive && (System.currentTimeMillis() - lastTimeMicWasActive) > timeMicIsActivePastHotkeyPress) {
                microphone.flush();
                if (isCircleGreen) {
                    clientGroupController.changeCircleColorDependingOnStatus(clientID, 0);
                    isCircleGreen = false;
                }
            }

            //While the hotkey is active or while within thresh hold of last hot key press
            while (pushToTalkHotKeyActive || (System.currentTimeMillis() - lastTimeMicWasActive) < timeMicIsActivePastHotkeyPress) {
                //Microphone is available

                //updates circle to be green
                if (!isCircleGreen) {
                    clientGroupController.changeCircleColorDependingOnStatus(clientID, 1);
                    isCircleGreen = true;
                }

                //updates last time mic was active
                if (pushToTalkHotKeyActive) {
                    lastTimeMicWasActive = System.currentTimeMillis();
                }

                //if there is data available in the microphone
                if (microphone.available() > 1) {
                    //Byte segment in sample format
                    byte[] audioPacketSegment = new byte[AudioPacket.audioByteLength]; // x samples

                    //reads microphone data into the byte array
                    microphone.read(audioPacketSegment, 0, audioPacketSegment.length);

                    //adds byte array to the queue
                    bytes.add(audioPacketSegment);
                } else {
                    //if nothing is available wait 10 mili sec
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
//                        e.printStackTrace();
                    }
                }
            }

            //if hot key is not being pressed then wait 10 mili sec
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }
        }
    }

    /**
     * Launches event handlers for the Javafx to grab hot key information
     */
    private void setUpPushToTalkEventHandlers() {
        Scene scene = clientGroupController.getScene();

        //When a key is pressed, checks if key is hot key
        scene.setOnKeyPressed(keyEvent -> {
            KeyCode hotKey = KeyCode.getKeyCode(String.valueOf(pushToTalkHotKey));

            //if the key being pressed is either the hotkey code or the hotkey character
            if (keyEvent.getCode() == hotKey) {
                pushToTalkHotKeyActive = true;
            } else if (keyEvent.getCharacter().equals(String.valueOf(pushToTalkHotKey))) {
                pushToTalkHotKeyActive = true;
            }
        });

        //When a key is released, checks if key is hot key
        scene.setOnKeyReleased(keyEvent -> {
            KeyCode hotKey = KeyCode.getKeyCode(String.valueOf(pushToTalkHotKey));
            //if the key being released is either the hotkey code or the hotkey character
            if (keyEvent.getCode() == hotKey) {
                pushToTalkHotKeyActive = false;
            } else if (keyEvent.getCharacter().equals(String.valueOf(pushToTalkHotKey))) {
                pushToTalkHotKeyActive = false;
            }
        });


        TextField pushToTalkHotKeyTextField = clientGroupController.getPushToTalkHotKeyTextField();
        //Default hotkey
        pushToTalkHotKeyTextField.setText("K");

        //Adds listener to the text field to see if hotkey is changed
        pushToTalkHotKeyTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            String text = pushToTalkHotKeyTextField.getText();
            if (text.length() > 0) {
                pushToTalkHotKey = text.charAt(0);
                pushToTalkHotKeyTextField.setText(String.valueOf(pushToTalkHotKey));
            }
        });
    }

    /**
     * Methods tells the thread to close
     */
    public void close() {
        System.out.printf("%nMic To Server %d Closing", clientID);
        microphone.close();
        close = true;
        Thread.currentThread().interrupt();
    }

}

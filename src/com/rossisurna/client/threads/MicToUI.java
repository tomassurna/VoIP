package com.rossisurna.client.threads;

import com.rossisurna.client.ui.ClientController;
import com.rossisurna.util.AudioPacket;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Thread that takes in microphone data and shows the volume on the main menu screen
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Apr 08 2019
 */

public class MicToUI implements Runnable {

    //UI
    private ClientController clientController;

    //Util
    private boolean close = false;

    //Microphone
    private TargetDataLine microphone;

    /**
     * Constructor to take in controller and initialize the microphone
     *
     * @param clientController the client's main menu controller
     */
    public MicToUI(ClientController clientController) {
        this.clientController = clientController;

        AudioFormat audioFormat = AudioPacket.audioFormat;
        try {
            //Gets default microphone in system
            microphone = AudioSystem.getTargetDataLine(audioFormat);
            microphone.open(audioFormat);
            microphone.start();
        } catch (LineUnavailableException e) {
//            e.printStackTrace();
            //No mic so close thread
            close();
        }
    }

    /**
     * Run main method which takes in microphone data and updates the UI related nodes
     */
    @Override
    public void run() {
        //Java fx nodes from controller
        ProgressBar microphoneVolumeBar = clientController.getMicrophoneProgressBar();
        Slider amplificationSlider = clientController.getMicrophoneSlider();
        Label micAmpNumberLabel = clientController.getMicAmpNumberLabel();
        Label micLevelNumberLabel = clientController.getMicLevelNumberLabel();

        //While not close
        while (!close) {
            //if there is data in the microphone available
            if (microphone.available() > 1) {
                byte[] audioPacketSegment = new byte[2000];

//                while (microphone.available() < audioPacketSegment.length && !close) {
//                    //while there is not enough data for the segment
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
////                        e.printStackTrace();
//                    }
//                }

                //read in data and flush old data
                microphone.read(audioPacketSegment, 0, audioPacketSegment.length);
                microphone.flush();

                //update the amplification slider to only show 2 digits
                amplificationSlider.setValue(Double.valueOf(String.format("%.2f", amplificationSlider.getValue())));

                //calculate the volume of the data packet
                long volume = 0;
                for (int i = 0; i < audioPacketSegment.length; i++) {
                    audioPacketSegment[i] *= amplificationSlider.getValue();
                    volume += Math.abs(audioPacketSegment[i]);
                }
                volume *= 2.5;
                volume /= audioPacketSegment.length;

                //average volume
                double averageVolume = volume / 100.0;

                //Send off platform runlater to update the UI nodes with the microphone data
                Platform.runLater(() -> {
                    micAmpNumberLabel.setText(String.format("%.2f", amplificationSlider.getValue()));
                    micLevelNumberLabel.setText(String.format("%.2f", averageVolume));
                    microphoneVolumeBar.setProgress(averageVolume);
                });
            }
        }
    }

    /**
     * Method to close thread
     */
    public void close() {
        close = true;
        microphone.close();
        Thread.currentThread().interrupt();
    }
}

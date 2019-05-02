package com.rossisurna.util;

import javax.sound.sampled.AudioFormat;
import java.io.Serializable;

/**
 * Network Programming Spring 2019 Final Project
 * <p>
 * Audio Packet object that holds the audio format, and the physical data and length of the specific segment of audio
 *
 * @author Stephen R
 * @author Tomas S
 * @version 1.0
 * @since Apr 02 2019
 */

public class AudioPacket implements Serializable {
    /*
        INFO:
        Sample rate: the rate at which the snapshots of the sound pressure are taken per second per channel
        Sample size: how many bits are used to store each snapshot, 8 and 16 are typical
        Channels: 1 for mono, 2 for stereo
        Signed: whether the data is signed or unsigned
        Big Endian: whether the data is stored in big endian order or little endian order
    */
    
    //Audio static info
    public static AudioFormat audioFormat = new AudioFormat(16000.0f, 16, 1, true, false); //Audio Format
    public static int audioByteLength = 10000; //Length of the segment
    
    //Audio instance info
    private byte[] audioDataInByteForm; //Data of the audio in byte form
    private int sequenceNumber; //sequence value of this specific packet
    
    /**
     * Constructor that takes in the audio in byte form and the sequence number
     *
     * @param audioDataInByteForm byte array of audio
     * @param sequenceNumber sequence number of packet
     */
    public AudioPacket(byte[] audioDataInByteForm, int sequenceNumber) {
        this.audioDataInByteForm = audioDataInByteForm;
        this.sequenceNumber = sequenceNumber;
    }
    
    //GETTERS
    
    /**
     * Returns the sequence number of this packet
     *
     * @return sequence number of the audio packet
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    /**
     * Get audio data
     *
     * @return byte array audio of packet
     */
    public byte[] getAudioDataInByteForm() {
        return audioDataInByteForm;
    }
}

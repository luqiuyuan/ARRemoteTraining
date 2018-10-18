/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import java.awt.image.BufferedImage;

/**
 *
 * @author lu
 */
public interface VideoSender{

        /**     
         * send this image to the client
         * @param image the image to send
         */
        void send(BufferedImage image);
        
        /**
         * Stop recording temporarily.  The recording can be started again
         * with start()
         */
        void pause();
        
        /**
         * Start the recording.
         */
        void start();
        
        /**
         * Closes the video file, writing appropriate headers, trailers, etc.
         * After this is called, no more recording can be done.
         */
        void finish();  
}

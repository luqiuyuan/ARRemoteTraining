/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.profile.AppProfiler;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

/**
 *
 * @author lu
 */
public class JPGVideoSender extends AbstractVideoSender{
    int current;
    
    // total bytes
    int num_frames_sent = 0;
    int total_bytes = 0;
      
    public JPGVideoSender(Main app, Client client, OutputStream output) throws IOException {
        super(app, client, output);
    }

    public void send(BufferedImage rawFrame) {
        try {
            if (Config.SAVE_IMAGES_TO_DISK) {
                File file = new File("image" + num_frames_sent + ".jpg");
                ImageIO.write(rawFrame, "jpg", file);
            }
            
            num_frames_sent++;
            
            ByteArrayOutputStream imageByteOutputStream = new ByteArrayOutputStream();
            ImageIO.write(rawFrame, "png", imageByteOutputStream);
            byte[] commandBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(Commands.IMAGE).array();
            byte[] imageBytes = imageByteOutputStream.toByteArray();
            byte[] lengthBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(imageBytes.length).array();
            output.write(commandBytes);
            output.write(lengthBytes);
            output.write(imageBytes);
        }
        catch (IOException e) {e.printStackTrace();}
    }

    public void finish() {}
    
    public void setProfiler(AppProfiler profiler) {}
}

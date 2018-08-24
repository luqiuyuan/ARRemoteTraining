/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.state.AbstractAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author lu
 */
public class Client extends AbstractAppState {
    
    int id;
    Main app;
    InputStream input;
    OutputStream output;
    
    public Client(Main app, InputStream input, OutputStream output, int id) {
        this.app = app;
        this.input = input;
        this.output = output;
        this.id = id;
    }
    
    @Override
    public void update(float tpf) {
        // interaction
        receiveInteraction();
    }
    
    private void receiveInteraction() {
        int command = readInt();
        switch(command) {
            case Commands.TARGET_POSE:
                String name = readString();
                float[] nums = readFloatArray(16);
                System.out.println("Receive Target Pose: " + name + ", " + nums);
                break;
        }
    }
    
    float[] readFloatArray(int length) {
        float[] arr = new float[length];
        
        for (int i = 0; i < length; i++) {
            arr[0] = readFloat();
        }
        
        return arr;
    }
    
    int readInt(){
        int intNum = -1;
        try {
            byte bytes[] = new byte[4];
            int index = 0;
            while (index < 4)
            {
                int bytesRead = input.read(bytes, index, 4 - index);
                index += bytesRead;
            }
            intNum = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
        } catch (IOException e) {
            System.err.println("Reading operation failed.");
            System.exit(1);
        }
        return intNum;
    }
    
    String readString() {
        int length = readInt();
        byte[] data = new byte[length];
        try {
            int index = 0;
            while (index < length)
            {
                int bytesRead = input.read(data, index, length - index);
                index += bytesRead;
            }
        } catch (IOException e) {
            System.err.println("Reading operation failed.");
            System.exit(1);
        }
        return new String(data);
    }
    
    float readFloat() {
        float delta = 0;
        try {
            byte[] data = new byte[4];
            int index = 0;
            while (index < 4)
            {
                int bytesRead = input.read(data, index, 4 - index);
                index += bytesRead;
            }
            delta = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getFloat();
        } catch (IOException e) {
            System.err.println("Reading float failed.");
            System.exit(1);
        }
        return delta;
    }
    
}

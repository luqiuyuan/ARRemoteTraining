package mygame;

import com.jme3.math.Vector3f;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lu on 2019-03-04.
 */

public class Network {
    
    //================================
    // Advanced readers and senders
    //================================

    public static float[] readFloatArray(int length, InputStream input) {
        float[] arr = new float[length];
        
        for (int i = 0; i < length; i++) {
            arr[i] = readFloat(input);
        }
        
        return arr;
    }
    public static void sendFloatArray(float[] nums, OutputStream output) {
        for (int i = 0; i < nums.length; i++) {
            sendFloat(nums[i], output);
        }
    }

    public static void sendVector(Vector3f vector, OutputStream output) {
        sendFloat(vector.x, output);
        sendFloat(vector.y, output);
        sendFloat(vector.z, output);
    }

    //================================
    // Basic readers and senders
    //================================

    public static int readCommand(InputStream input){
        int command = Commands.NO_COMMAND;
        try {
            if (input.available() >= 4) {
                byte bytes[] = new byte[4];
                input.read(bytes);
                command = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
            }
        } catch (IOException e) {
            System.err.println("Reading operation failed.");
            System.exit(1);
        }
        return command;
    }

    public static int readInt(InputStream input) {
        byte[] bytes = new byte[4];
        try {
            int index = 0;
            while (index < 4) {
                int bytesRead = input.read(bytes, index, 4 - index);
                index += bytesRead;
            }
        } catch (IOException e) {
            System.err.println("Reading operation failed.");
            System.exit(1);
        }
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }
    public static void sendInt(int num, OutputStream output) {
        byte[] command = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(num).array();
        try {
            if (output != null) {
                output.write(command);
            }
        } catch (IOException e) {
            System.err.println("Sending operation command failed.");
            System.exit(1);
        }
    }

    public static float readFloat(InputStream input) {
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
    public static void sendFloat(float num, OutputStream output) {
        try {
            byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(num).array();
            if (output != null) {
                output.write(bytes);
            }
        } catch (IOException e) {
            System.err.println("Sending operation failed.");
            System.exit(1);
        }
    }

    public static String readString(InputStream input) {
        int length = readInt(input);
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
    public static void sendString(String str, OutputStream output) {
        byte[] bytes = str.getBytes();
        try {
            sendInt(bytes.length, output);
            if (output != null) {
                output.write(bytes);
            }
        } catch (IOException e) {
            System.err.println("Sending operation command failed.");
            System.exit(1);
        }
    }

    public static byte[] readBytes(int size, InputStream input) {
        byte[] data = new byte[size];
        try {
            int index = 0;
            while (index < size)
            {
                int bytesRead = input.read(data, index, size - index);
                index += bytesRead;
            }
        } catch (Exception e) {
            System.err.println("Reading operation command failed.");
            System.exit(1);
        }
        return data;
    }
    public static void sendBytes(byte[] data, OutputStream output) {
        try {
            if (output != null) {
                output.write(data);
            }
        } catch (IOException e) {
            System.err.println("Sending operation command failed.");
            System.exit(1);
        }
    }

}

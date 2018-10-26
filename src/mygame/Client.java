/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.state.AbstractAppState;
import com.jme3.input.ChaseCamera;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Transform;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

/**
 *
 * @author lu
 */
public class Client extends AbstractAppState {
    
    // Trainers & Trainees
    public static ArrayList<Client> trainers = new ArrayList<>();
    public static ArrayList<Client> trainees = new ArrayList<>();
    
    int id;
    private String role;
    Main app;
    InputStream input;
    OutputStream output;
    
    // JPGVideoSender
    JPGVideoSender sender;
    
    // Targets
    Map<String, Matrix4f> transformations;
    Map<String, Matrix4f> transformations_relative;
    
    // Reference object
    String referenceName = "hexagon";
    
    // Camera
    Camera cam;
    
    // Render maps
    ArrayList<ArrayList<String>> render_maps;
    
    // Dimensions
    int width, height;
    float aspect_ratio;
    
    int num_of_frame = 0;
    Matrix4f mat1, mat2, mat3, mat4;
    
    public Client(Main app, InputStream input, OutputStream output, int id) {
        this.app = app;
        this.input = input;
        this.output = output;
        this.id = id;
        
        // Initialize transformations_relative
        transformations = new HashMap<>();
        transformations_relative = new HashMap<>();
        
        System.out.println("create client: " + this.id);
    }
    
    public void initializeCamera() {
        // setup camera for client
        cam = app.getCamera().clone();
    }
    
    @Override
    public void update(float tpf) {
        // interaction
        receiveInteraction();

        if (this.role.equals(Constants.NAME_TRAINEE)) {
            updateRenderMap();
        }
    }
    
    private void receiveInteraction() {
        int command = readCommand();
        while (command != Commands.NO_COMMAND) {
            switch(command) {
                case Commands.SET_ROLE:
                    String role_in = readString();
                    this.setRole(role_in);
                    // Add the client to trainers or trainees
                    if (this.role.equals(Constants.NAME_TRAINER)) {
                        Client.trainers.add(this);
                    } else {
                        Client.trainees.add(this);
                    }
                    if (Config.DEUBG_MODE) {
                        System.out.println("Client #" + this.id + ": " + "set role as " + this.role);
                    }
                    break;
                case Commands.SET_TRANSFORMATION:
                    String name = readString();
                    float[] nums_vector = readFloatArray(3);
                    float[] nums_vector_x = readFloatArray(3);
                    float[] nums_translation = readFloatArray(3);
                    Spatial model = app.models.get(name);
                    if (model != null) {
                        Vector3f vector = new Vector3f(nums_vector[0], nums_vector[1], nums_vector[2]);
                        Vector3f vector_x = new Vector3f(nums_vector_x[0], nums_vector_x[1], nums_vector_x[2]);
                        Vector3f axis = new Vector3f(0.0f, 1.0f, 0.0f).cross(vector);
                        Vector3f axis_x = new Vector3f(1.0f, 0.0f, 0.0f).cross(vector_x);
                        double angle = Math.asin(axis.length() / vector.length());
                        double angle_x = Math.asin(axis_x.length() / vector_x.length());
                        Quaternion rotation = new Quaternion();
                        Quaternion rotation_x = new Quaternion();
                        rotation.fromAngleAxis((float) angle, axis.normalize());
                        rotation_x.fromAngleAxis((float) -angle_x, axis_x.normalize());
                        rotation = rotation_x.mult(rotation);
                        model.setLocalTranslation(nums_translation[0], nums_translation[1], nums_translation[2]);
                        model.setLocalRotation(rotation);
                    }
                    this.num_of_frame++;
                    break;
                case Commands.SET_TRANSFORMATION_RELATIVE:
                    name = readString();
                    float[] nums = readFloatArray(16);
                    Matrix4f mat = new Matrix4f(nums);
                    transformations_relative.put(name, mat);
                    break;
                case Commands.RESOLUTION:
                    this.height = readInt();
                    this.width = readInt();
                    if (Config.DEUBG_MODE) {
                        System.out.println("Client #" + this.id + ": set resolution as " + this.width + " (width)" + " : " + this.height + " (height)");
                    }
                    aspect_ratio = (float) width / (float) height;
                    cam.setFrustumPerspective(80.0f, 1, aspect_ratio, 10000);
                    break;
                case Commands.SET_CAMERA_PROJECTION_MATRIX:
                    nums = readFloatArray(16);
                    mat = new Matrix4f(nums);
//                    cam.setProjectionMatrix(mat);
                    break;
            }
            
            command = readCommand();
        }
    }
    
    public int readCommand(){
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
    
    float[] readFloatArray(int length) {
        float[] arr = new float[length];
        
        for (int i = 0; i < length; i++) {
            arr[i] = readFloat();
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
    
    void setJPGVideoSender(JPGVideoSender sender) {
        this.sender = sender;
    }
    
    String getRole() { return this.role; }
    void setRole(String role) { this.role = role; }
    
    void updateRenderMap() {
        ArrayList<Client> opposites;
        if (this.role.equals(Constants.NAME_TRAINER)) {
            opposites = Client.trainees;
        } else {
            opposites = Client.trainers;
        }
        
        render_maps = new ArrayList<>();
        for (int i = 0; i < opposites.size(); i++) {
            ArrayList<String> render_map = new ArrayList<>();
            render_maps.add(render_map);
            Client opposite = opposites.get(i);
            for (Map.Entry<String, Matrix4f> entry : opposite.transformations_relative.entrySet()) {
                if (!entry.getKey().equals(Constants.NAME_PRIME_OBJECT) && this.transformations_relative.get(entry.getKey()) != null) {
                    if (!Helper.areTwoTransformationSimilar(this.transformations_relative.get(entry.getKey()), entry.getValue())) {
                        render_map.add(entry.getKey());
                    }
                }
            }
        }
    }
    
}

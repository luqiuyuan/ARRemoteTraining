/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.state.AbstractAppState;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
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
import com.jme3.scene.shape.Box;

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
    private Map<String, Matrix4f> transformations;
    private Map<String, Matrix4f> transformations_relative;
    private Map<String, Matrix4f> transformations_relative_start;
    
    // Reference object
    String referenceName = "hexagon";
    
    // Camera
    Camera cam;
    
    // Render maps
    ArrayList<ArrayList<String>> render_maps;
    
    // Dimensions
    int width, height;
    float aspect_ratio;
    
    Map<String, Spatial> models;
    
    public Client(Main app, InputStream input, OutputStream output, int id) {
        this.app = app;
        this.input = input;
        this.output = output;
        this.id = id;
        
        // Initialize transformations_relative
        transformations = new HashMap<>();
        transformations_relative = new HashMap<>();
        transformations_relative_start = new HashMap<>();
        
        // Initialize models
        this.models = this.app.cloneModels();
        this.attachModels();
        
        System.out.println("Created client: " + this.id);
    }
    
    public void initializeCamera() {
        // setup camera for client
        cam = app.getCamera().clone();
    }
    
    @Override
    public void update(float tpf) {
        // interaction
        receiveInteraction();

        if (this.role != null) {
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
                        this.app.models_trainers.add(this.models);
                    } else {
                        Client.trainees.add(this);
                        this.app.models_trainees.add(this.models);
                    }
                    if (Config.DEUBG_MODE) {
                        System.out.println("Client #" + this.id + ": " + "set role as " + this.role);
                    }
                    break;
                case Commands.SET_TRANSFORMATION:
                    String name_target = readString();
                    Target target = Target.getTargets().get(name_target);
                    String name_component = target.getNameComponent();
                    float[] nums_vector = readFloatArray(3);
                    float[] nums_vector_x = readFloatArray(3);
                    float[] nums_translation = readFloatArray(3);
                    Spatial model = this.models.get(name_component);
                    if (model != null) {
                        Vector3f vector_y = new Vector3f(nums_vector[0], nums_vector[1], nums_vector[2]);
                        Vector3f axis_y = new Vector3f(0.0f, 1.0f, 0.0f);
                        Vector3f rotation_axis_y = axis_y.cross(vector_y);
                        double angle = Math.acos(axis_y.dot(vector_y) / (axis_y.length() * vector_y.length()));
                        Quaternion rotation = new Quaternion();
                        rotation.fromAngleAxis((float) angle, rotation_axis_y.normalize());
                        
                        Vector3f vector_x = new Vector3f(nums_vector_x[0], nums_vector_x[1], nums_vector_x[2]);
                        Vector3f axis_x = new Vector3f(1.0f, 0.0f, 0.0f);
                        Vector3f axis_x_rotated = rotation.mult(axis_x);
                        Vector3f rotation_axis_x = axis_x_rotated.cross(vector_x);
                        double angle_x = Math.acos(axis_x_rotated.dot(vector_x) / (axis_x_rotated.length() * vector_x.length()));
                        Quaternion rotation_x = new Quaternion();
                        if (rotation_axis_x.dot(vector_y) > 0) {
                            rotation_x.fromAngleAxis((float) angle_x, axis_y);
                        } else {
                            rotation_x.fromAngleAxis((float) angle_x, axis_y.mult(-1));
                        }
                        
                        model.setLocalTranslation(nums_translation[0], nums_translation[1], nums_translation[2]);
                        model.setLocalRotation(Matrix3f.IDENTITY);
                        model.getLocalTransform().setScale(1.0f);
                        model.rotate(rotation);
                        model.rotate(rotation_x);

                        Matrix4f transformation_origin = model.getLocalTransform().toTransformMatrix();
                        Matrix4f transformation_delta = target.getTransformation();
                        Matrix4f transformation_new = transformation_origin.mult(transformation_delta.invert());
                        Transform transform_new = new Transform();
                        transform_new.fromTransformMatrix(transformation_new);
                        model.setLocalTransform(transform_new);
                        
                        // Update the absolute transformation
                        this.transformations.put(name_component, model.getLocalTransform().toTransformMatrix());
                        
                        // Update the relative transformation
                        if (name_component.equals(Constants.NAME_PRIME_OBJECT)) {
                            this.updateRelativeTransformationsForAll();
                        } else {
                            this.updateRelativeTransformationsForName(name_component);
                        }
                        
                        // Reset model transformation
                        if (this.transformations_relative.get(name_component) != null) {
                            Transform transform = new Transform();
                            transform.fromTransformMatrix(this.transformations_relative.get(name_component));
                            model.setLocalTranslation(Vector3f.ZERO);
                            model.setLocalRotation(Matrix3f.IDENTITY);
                            model.setLocalTransform(transform);
                        }
                        
                        // Update camera position
                        if (name_component.equals(Constants.NAME_PRIME_OBJECT)) {
                            Matrix4f transformation = this.transformations.get(Constants.NAME_PRIME_OBJECT);
                            Matrix4f transformation_inverse = transformation.invert();
                            Transform transform_camera = new Transform();
                            transform_camera.fromTransformMatrix(transformation_inverse);
                            this.cam.setLocation(Vector3f.ZERO);
                            this.cam.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
                            this.cam.setLocation(transform_camera.getTranslation());
                            this.cam.setRotation(transform_camera.getRotation());
                            this.cam.lookAtDirection(this.cam.getDirection().mult(-1), this.cam.getUp());
                        }
                    }
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
                    float[] nums = readFloatArray(16);
                    Matrix4f mat = new Matrix4f(nums);
                    mat.set(0, 0, -mat.get(0, 1));
                    mat.set(1, 1, -mat.get(1, 0));
                    mat.set(1, 0, 0);
                    mat.set(0, 1, 0);
                    mat.set(2, 1, 0);
                    mat.set(2, 2, -mat.get(2, 2));
                    mat.set(2, 3, mat.get(3, 2));
                    mat.set(3, 2, -1);
                    cam.setProjectionMatrix(mat);
                    System.out.println(mat);
                    break;
                case Commands.TARGET_FOUND:
                    name_target = readString();
//                    model = app.models.get(name);
//                    model.setCullHint(Spatial.CullHint.Never);
                    break;
                case Commands.TARGET_LOST:
                    name_target = readString();
//                    model = app.models.get(name);
//                    model.setCullHint(Spatial.CullHint.Always);
                    break;
                case Commands.REQUEST_FRAME:
                    this.sender.start();
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
                    // Do NOT render a model if it is NOT away from its starting position
                    if (Helper.areTwoTransformationSimilar(opposite.transformations_relative_start.get(entry.getKey()), entry.getValue()))
                        continue;
                    
                    if (!Helper.areTwoTransformationSimilar(this.transformations_relative.get(entry.getKey()), entry.getValue())) {
                        render_map.add(entry.getKey());
                    }
                }
            }
        }
    }
    
    private void updateRelativeTransformationsForAll() {
        for (Map.Entry<String, Matrix4f> entry : this.transformations.entrySet()) {
            this.updateRelativeTransformationsForName(entry.getKey());
        }
    }

    private void updateRelativeTransformationsForName(String name) {
        Matrix4f base = this.transformations.get(Constants.NAME_PRIME_OBJECT);
        if (base != null) {
            Matrix4f relative = Helper.getRelativeTransformation(base, transformations.get(name));
            this.transformations_relative.put(name, relative);
            
            // If the start relative transformation has not been initialized, initialize it
            if (this.transformations_relative_start.get(name) == null) {
                this.transformations_relative_start.put(name, relative);
            }
        }
    }
    
    public Matrix4f getTransformation(String name) {
        return this.transformations.get(name);
    }
    
    public Matrix4f getTransformationRelative(String name) {
        return this.transformations_relative.get(name);
    }
    
    private void attachModels() {
        for (Map.Entry<String, Spatial> entry : this.models.entrySet()) {
            entry.getValue().setCullHint(Spatial.CullHint.Always);
            this.app.getRootNode().attachChild(entry.getValue());
        }
    }
}

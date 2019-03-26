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
import java.util.Date;
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
    
    Map<String, Boolean> founds;
    Map<String, Boolean> founds_model;
    
    ComponentConfiguration component_configuration;
    
    // Arrows
    Map<String, Arrow> arrows;
    
    // Heartbeat
    private static final int HEARTBEAT_INTERVAL_IN_MILLISECONDS = 500;
    private long heartbeat_timestamp_last;
    
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
        
        this.founds = new HashMap<>();
        this.founds_model = new HashMap<>();
        
        System.out.println("Created client: " + this.id);
        
        // Initialize the last timestamp of heartbeat to the time of creating the client instance
        heartbeat_timestamp_last = new Date().getTime();
        
        // Initialize the arrows
        this.arrows = new HashMap<>();
    }
    
    public void initializeCamera() {
        // setup camera for client
        cam = app.getCamera().clone();
    }
    
    @Override
    public void update(float tpf) {
        // Initialization for update
        initializationForUpdate();
        
        // other operations
        otherOperations();
        
        // interaction
        receiveInteraction();

        if (this.role != null) {
            updateRenderMap();
        }
    }
    
    private void receiveInteraction() {
        int command = Network.readCommand(input);
        while (command != Commands.NO_COMMAND) {
            switch(command) {
                case Commands.SET_ROLE:
                    String role_in = Network.readString(input);
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
                    String name_target = Network.readString(input);
                    float[] nums_vector = Network.readFloatArray(3, input);
                    float[] nums_vector_x = Network.readFloatArray(3, input);
                    float[] nums_translation = Network.readFloatArray(3, input);
                    // After reading all inputs, check if target configuration has been set
                    if (this.component_configuration == null) break;
                    Target target = Target.getTargets().get(name_target);
                    String name_component = target.getNameComponent();
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
                    this.height = Network.readInt(input);
                    this.width = Network.readInt(input);
                    if (Config.DEUBG_MODE) {
                        System.out.println("Client #" + this.id + ": set resolution as " + this.width + " (width)" + " : " + this.height + " (height)");
                    }
                    aspect_ratio = (float) width / (float) height;
                    cam.setFrustumPerspective(80.0f, 1, aspect_ratio, 10000);
                    break;
                case Commands.SET_CAMERA_PROJECTION_MATRIX:
                    float[] nums = Network.readFloatArray(16, input);
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
                    name_target = Network.readString(input);
                    target = Target.getTargets().get(name_target);
                    name_component = target.getNameComponent();
                    founds.put(name_target, true);
                    founds_model.put(name_component, true);
                    break;
                case Commands.TARGET_LOST:
                    name_target = Network.readString(input);
                    target = Target.getTargets().get(name_target);
                    name_component = target.getNameComponent();
                    founds.put(name_target, false);
                    founds_model.put(name_component, false);
                    break;
                case Commands.REQUEST_FRAME:
                    this.sender.start();
                    break;
                case Commands.SET_TARGET_CONFIGURATION:
                    String target_configuration_str = Network.readString(input);
                    if (Config.DEUBG_MODE) {
                        System.out.println("Client #" + this.id + ": sent target configuration: " + target_configuration_str);
                    }
                    this.component_configuration = new ComponentConfiguration(target_configuration_str);
                    break;
            }
            
            command = Network.readCommand(input);
        }
    }
    
    private void initializationForUpdate() {
        // Detach all arrows at the beginning of every frame
        for (Map.Entry<String, Arrow> entry : this.arrows.entrySet()) {
            entry.getValue().detachFrom(this.app.getRootNode());
        }
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
                        // Show an arrow pointing from current location to destinational location.
                        updateArrow(entry.getKey(), entry.getValue());
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
    
    private void otherOperations() {
        // Send heartbeat
        sendHeartbeat();
    }
    
    private void sendHeartbeat() {
        long heartbeat_timestamp = new Date().getTime();
        if (heartbeat_timestamp - heartbeat_timestamp_last >= HEARTBEAT_INTERVAL_IN_MILLISECONDS) {
            Network.sendInt(Commands.HEARTBEAT, output);
            heartbeat_timestamp_last = heartbeat_timestamp;
        }
    }
    
    private void updateArrow(String name_component, Matrix4f transformation_end) {
        // Only show it for trainees and when current location is known.
        if (this.role.equals(Constants.NAME_TRAINEE) && this.founds_model.get(name_component) != null && this.founds_model.get(name_component) == true) {
            Arrow arrow;
            if (this.arrows.get(name_component) == null) { // If the arrow has not been created, create it.
                arrow = new Arrow(Vector3f.ZERO, Vector3f.ZERO, new ColorRGBA(251f / 255f, 130f / 255f, 0f, 0.5f), this.app.getAssetManager());
                arrow.hide(); // The arrow is created in hidden state
                this.arrows.put(name_component, arrow);
            }
            arrow = this.arrows.get(name_component);
            Matrix4f transformation_start = this.transformations_relative.get(name_component);
            Vector3f location_start = new Vector3f(transformation_start.m03, transformation_start.m13, transformation_start.m23);
            Vector3f location_end = new Vector3f(transformation_end.m03, transformation_end.m13, transformation_end.m23);
            arrow.update(location_start, location_end);
            arrow.attachTo(this.app.getRootNode());
        }
    }
}

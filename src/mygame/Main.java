package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.TangentBinormalGenerator;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {
    
    private static Main app;
    
    // Socket constants
    private static final int MAX_NUM_THREADS = 10;
    private static final int PORT_NUMBER = 23456;
    
    // Socket variables
    private ScheduledThreadPoolExecutor executor;
    private Callable<Void> socket_waiter;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    
    // Used for assigning the id of clients
    int clientCount = -1;
    
    // Models
    Map<String, Spatial> models;
    ArrayList<Map<String, Spatial>> models_trainers;
    ArrayList<Map<String, Spatial>> models_trainees;

    public static void main(String[] args) {
        app = new Main();
        
        app.setDisplayStatView(false);
        
        app.start();
    }

    @Override
    public void simpleInitApp() {
        initScene();
        initSocket();
        
        models_trainers = new ArrayList<>();
        models_trainees = new ArrayList<>();
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    void initScene() {
        // Load model names
        assetManager.registerLoader(JSONLoader.class, "json");
        ArrayList<String> model_names = (ArrayList<String>) assetManager.loadAsset("Configs/model_names.json");

        // Read models
        models = new HashMap<>();
        for (String name : model_names) {
            Spatial model;
            try {
                model = assetManager.loadModel("Models/" + name + ".obj");
            } catch (AssetNotFoundException e) {
                Box box = new Box(new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(2.0f, 2.0f, 2.0f));
                Geometry geo = new Geometry(name, box);
                Material mat = new Material(assetManager,
                  "Common/MatDefs/Light/Lighting.j3md");
                mat.setColor("Diffuse",ColorRGBA.Orange);
                geo.setMaterial(mat);
                model = (Spatial) geo;
            }
            models.put(name, model);
        }
        
        // Add ambient light
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(al);
        
        // Add directional light
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-.5f,.5f,-.5f).normalizeLocal());
        rootNode.addLight(sun);
        
        cam.setLocation(Vector3f.ZERO);
        cam.lookAtDirection(new Vector3f(0.0f, 0.0f, -1.0f), Vector3f.UNIT_Y);
    }
    
    public void initSocket() {
        // remove the default rendering view port
        renderManager.removeMainView("Default");

        executor = new ScheduledThreadPoolExecutor(MAX_NUM_THREADS);
        socket_waiter = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    serverSocket = new ServerSocket(PORT_NUMBER);
                    while (true) {
                        System.out.println("Waiting for clients connecting...");
                        clientSocket = serverSocket.accept();
                        final InputStream input = clientSocket.getInputStream();
                        final OutputStream output = clientSocket.getOutputStream();
                        app.enqueue(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                System.out.println("Client connected.");
                                Client client = createClient(input, output);
                                client.initializeCamera();
                                AbstractVideoSender sender = attachRenderer(client, output, client.cam);
                                return null;
                            }
                        });
                    }
                } catch(IOException e) {
                    System.out.println("Exception caught when trying to listen on port" + PORT_NUMBER + "or listening for a connection");
                    System.out.println(e.getMessage());
                }
                return null;
            }
        };
        executor.submit(socket_waiter);
    }
    
    public Client createClient(InputStream input, OutputStream output) {
        Client client = new Client(app, input, output, ++clientCount);
        stateManager.attach(client);
        return client;
    }
    
    private AbstractVideoSender attachRenderer(Client client, OutputStream output, Camera camera) throws IOException{
        AbstractVideoSender videoSender = new JPGVideoSender(app, client, output);
        client.setJPGVideoSender((JPGVideoSender)videoSender);
        ViewPort view_port;
        view_port = renderManager.createPostView("Remote Rendering", camera);
        view_port.setClearFlags(true, true, true);
        // get GUI node stuff
        for (Spatial s : guiViewPort.getScenes()){
            view_port.attachScene(s);
        }
        for (Spatial s : viewPort.getScenes()) {
            view_port.attachScene(s);
        }
        stateManager.attach(videoSender);
        view_port.addProcessor(videoSender);
        videoSender.setViewPort(view_port);
        return videoSender;
    }
    
    public Map<String, Spatial> cloneModels() {
        Map<String, Spatial> models_clone = new HashMap<>();
        
        for (Map.Entry<String, Spatial> entry : this.models.entrySet()) {
            models_clone.put(entry.getKey(), entry.getValue().clone());
        }
        
        return models_clone;
    }
}

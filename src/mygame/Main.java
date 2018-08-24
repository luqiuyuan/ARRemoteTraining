package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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
    private static final int MAX_NUM_THREADS = 1;
    private static final int PORT_NUMBER = 23456;
    
    // Socket variables
    private ScheduledThreadPoolExecutor executor;
    private Callable<Void> socket_waiter;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    
    // used for assigning the id of clients
    int clientCount = -1;

    public static void main(String[] args) {
        app = new Main();
        
        app.setDisplayStatView(false);
        
        app.start();
    }

    @Override
    public void simpleInitApp() {
        initSocket();
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
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
//                                client.initializeCamera();
//                                AbstractVideoSender sender1 = attachRenderer(client, null, false, client.cam);
//                                AbstractVideoSender sender2 = attachRenderer(client, output, true, client.cam);
//                                client.setVideoSender(sender1, sender2);
//                                clients.add(client); // add to the client list
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
}

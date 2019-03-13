/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author lu
 */
public abstract class AbstractVideoSender
        implements SceneProcessor, VideoSender, AppState{

    final OutputStream output;
    int width;
    int height;
    String targetFileName;
    FrameBuffer frameBuffer;
    Double fps = null;
    RenderManager renderManager;
    ByteBuffer byteBuffer;
    BufferedImage rawFrame;
    boolean isInitilized = false;
    boolean paused = false;
    Client client;
    ViewPort viewPort;
    boolean is_pass2;
    Main app;

    // view_port
    ViewPort view_port;

    Map<String, Spatial> models;

    // bounding box
    int bounding_box_pos[];
    int bounding_box_dim[];
    
    // Frame count
    private int frame_no = 0;

    public AbstractVideoSender(Main app, Client client, OutputStream output) throws IOException {
        this.app = app;
        this.client = client;
        this.output = output;
        models = app.models;
    }

    public double getFps() {
        return this.fps;
    }

    public AbstractVideoSender setFps(double fps) {
        this.fps = fps;
        return this;
    }

    public void initialize(RenderManager rm, ViewPort viewPort) {
        Camera camera = viewPort.getCamera();
        this.width = camera.getWidth();
        this.height = camera.getHeight();
        rawFrame = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);
        byteBuffer = BufferUtils.createByteBuffer(width * height * 4);
        this.renderManager = rm;
        this.isInitilized = true;
        this.viewPort = viewPort;
    }

    public void reshape(ViewPort vp, int w, int h) {
    }

    public boolean isInitialized() {
        return this.isInitilized;
    }

    public void preFrame(float tpf) {
        if (null == this.fps) {
            this.setFps(1.0 / tpf);
        }

        if (this.client.render_maps != null) {
            for (int i = 0; i < this.client.render_maps.size(); i++) {
                for (int j = 0; j < this.client.render_maps.get(i).size(); j++) {
                    String name = this.client.render_maps.get(i).get(j);
                    if (this.client.getRole().equals(Constants.NAME_TRAINER)) {
                        this.app.models_trainees.get(i).get(name).setCullHint(Spatial.CullHint.Never);
                    } else {
                        this.app.models_trainers.get(i).get(name).setCullHint(Spatial.CullHint.Never);
                    }
                }
            }
        }
        
        if (Config.SHOW_OWN_MODELS) {
            Map<String, Boolean> founds_model = new HashMap<>();
            for (Map.Entry<String, Boolean> entry : this.client.founds.entrySet()) {
                if (entry.getValue() != null && entry.getValue() == true) {
                    Target target = Target.getTargets().get(entry.getKey());
                    String name_component = target.getNameComponent();
                    founds_model.put(name_component, true);
                }
            }
            for (Map.Entry<String, Spatial> entry : this.client.models.entrySet()) {
                if (founds_model.get(entry.getKey()) != null) {
                    entry.getValue().setCullHint(Spatial.CullHint.Never);
                }
            }
        }
    }

    public void postQueue(RenderQueue rq) {
    }

    public void postFrame(FrameBuffer out) {
        if (!this.paused && output != null && client.aspect_ratio > 0) {
            byteBuffer.clear();
            renderManager.getRenderer().readFrameBufferWithFormat(out, byteBuffer, Image.Format.BGRA8);
            Screenshots.convertScreenShot(byteBuffer, rawFrame);
            send(rawFrame);
            
            // Send one frame and pause
            this.pause();
        }
        
        if (this.client.render_maps != null) {
            for (int i = 0; i < this.client.render_maps.size(); i++) {
                for (int j = 0; j < this.client.render_maps.get(i).size(); j++) {
                    String name = this.client.render_maps.get(i).get(j);
                    if (this.client.getRole().equals(Constants.NAME_TRAINER)) {
                        this.app.models_trainees.get(i).get(name).setCullHint(Spatial.CullHint.Always);
                    } else {
                        this.app.models_trainers.get(i).get(name).setCullHint(Spatial.CullHint.Always);
                    }
                }
            }
        }
        
        if (Config.SHOW_OWN_MODELS) {
            for (Map.Entry<String, Spatial> entry : this.client.models.entrySet()) {
                entry.getValue().setCullHint(Spatial.CullHint.Always);
            }
        }
        
        frame_no++;
    }

    public void cleanup() {
        this.pause();
        this.finish();
    }
        
    public void pause() {
        this.paused = true;
        if (view_port != null) {
            view_port.setEnabled(false);
        }

    }

    public void start() {
        this.paused = false;
        if (view_port != null) {
            view_port.setEnabled(true);
        }

    }

    // methods from AppState
    public void initialize(AppStateManager stateManager, Application app) {
    }

    public void setEnabled(boolean active) {
        if (active) {
            this.start();
        } else {
            this.pause();
        }
    }

    public boolean isEnabled() {
        return this.paused;
    }

    public void stateAttached(AppStateManager stateManager) {
    }

    public void stateDetached(AppStateManager stateManager) {
        this.pause();
        this.finish();
    }

    public void update(float tpf) {
    }

    public void render(RenderManager rm) {
    }

    public void postRender() {
    }
        
    void setViewPort(ViewPort view_port) {
        this.view_port = view_port;
    }
}

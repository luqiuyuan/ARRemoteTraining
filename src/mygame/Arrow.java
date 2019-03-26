/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;

/**
 *
 * @author lu
 */
public class Arrow extends Geometry {
    
    private final AssetManager assetManager;
    
    private Spatial stick;
    private Spatial cone;
    
    Arrow(Vector3f start, Vector3f end, ColorRGBA color, AssetManager assetManager) {
        this.assetManager = assetManager;
        
        initStick(start, end, color);
        initCone(start, end, color);
    }
    
    public void attachTo(Node node) {
        node.attachChild(stick);
        node.attachChild(cone);
    }
    
    public void detachFrom(Node node) {
        node.detachChild(stick);
        node.detachChild(cone);
    }
    
    public void update(Vector3f start, Vector3f end) {
        if (stick != null && cone != null) {
            transformStick(start, end);
            transformCone(start, end);
        }
    }
    
    private void initStick(Vector3f start, Vector3f end, ColorRGBA color) {
        float length = end.subtract(start).length() - 0.4f;
        Cylinder c = new Cylinder(2, 100, 0.1f, 1, true);
        Geometry geom = new Geometry("Cylinder", c);
        geom.scale(1f, 1f, length);
        stick = geom;
        
        // Set the material
        stick.setMaterial(generateMaterial(color));
        stick.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        // Transform the stick
        transformStick(start, end);
    }
    
    private void initCone(Vector3f start, Vector3f end, ColorRGBA color) {
        // Create the cone
        cone = assetManager.loadModel("Models/head.obj");
        
        // Set the material
        cone.setMaterial(generateMaterial(color));
        cone.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        // Transform the stick
        transformCone(start, end);
    }
    
    private Material generateMaterial(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors",true);
        mat.setColor("Ambient", color);
        mat.setColor("Diffuse", color);
        mat.setColor("Specular", color);
        mat.setFloat("Shininess", 64f);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return mat;
    }
    
    private Quaternion calculateRotation(Vector3f start, Vector3f end, Vector3f direction_orig) {
        Vector3f direction_dest = end.subtract(start);
        Vector3f cross_product = direction_orig.cross(direction_dest);
        Vector3f axis = cross_product.normalize();
        double angle = Math.asin(cross_product.length()/direction_orig.length()/direction_dest.length());
        float dot_product = direction_orig.dot(direction_dest);
        if (dot_product < 0) angle = Math.PI - angle;
        Quaternion q = new Quaternion();
        q.fromAngleAxis((float) angle, axis);
        return q;
    }
    
    private void transformStick(Vector3f start, Vector3f end) {
        if (stick == null) return;
        
        // Reset the transformation as well as the scale
        Transform t = new Transform();
        t.fromTransformMatrix(Matrix4f.IDENTITY);
        stick.setLocalTransform(t);
        
        // Re-scale the stick
        float length = end.subtract(start).length() - 0.4f;
        stick.scale(1f, 1f, length);
        
        // Rotate the cylinder
        stick.rotate(calculateRotation(start, end, new Vector3f(0f, 0f, 1f)));
        
        // Translate the cylinder
        Vector3f offset = start.subtract(end).normalize().mult(0.2f);
        Vector3f middle = end.subtract(start).divide(2).add(offset);
        stick.move(start.add(middle));
    }
    
    private void transformCone(Vector3f start, Vector3f end) {
        if (cone == null) return;
        
        // Reset the transformation as well as the scale
        Transform t = new Transform();
        t.fromTransformMatrix(Matrix4f.IDENTITY);
        cone.setLocalTransform(t);
        
        // Rotate the cone
        cone.rotate(calculateRotation(start, end, new Vector3f(0f, 1f, 0f)));
        
        // Translate the cone
        Vector3f offset = start.subtract(end).normalize().mult(0.2f);
        cone.move(end.add(offset));
    }
    
}


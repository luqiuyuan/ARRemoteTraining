package mygame;

import com.jme3.math.Matrix4f;
import java.util.Map;
import java.util.HashMap;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author lu
 */
public class Target {
    
    private final String name, name_component;
    private final Matrix4f transformation;
    
    private static Map<String, Target> targets = new HashMap<>();
    
    public Target(String name, String name_component, Matrix4f transformation) {
        this.name = name;
        this.name_component = name_component;
        this.transformation = transformation;
        
        // Make translation 100 times larger due to unit difference
        this.transformation.m03 *= 100;
        this.transformation.m13 *= 100;
        this.transformation.m23 *= 100;
        
        targets.put(name, this);
    }
    
    @Override
    public String toString() { return "Target " + name; }
    
    // Getters and Setters
    public String getName() { return name; }
    public String getNameComponent() { return name_component; }
    public Matrix4f getTransformation() { return transformation; }
    public static Map<String, Target> getTargets() { return targets; }
    
}

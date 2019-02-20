package mygame;

import com.jme3.math.Matrix4f;

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
    
    public Target(String name, String name_component, Matrix4f transformation) {
        this.name = name;
        this.name_component = name_component;
        this.transformation = transformation;
    }
    
    @Override
    public String toString() { return "Target " + name; }
    
    // Getters and Setters
    public String getName() { return name; }
    public String getNameComponent() { return name_component; }
    public Matrix4f getTransformation() { return transformation; }
    
}

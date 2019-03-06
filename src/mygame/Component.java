package mygame;

import com.jme3.math.Matrix4f;
import java.util.ArrayList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author lu
 */
public class Component {
    
    private final String name;
    private final ArrayList<Target> targets;
    
    public Component(String name) {
        this.name = name;
        targets = new ArrayList<>();
    }
    
    public void addTarget(String name_target, Matrix4f transformation) {
        targets.add(new Target(name_target, name, transformation));
    }
    
    public String toString() { return "Component " + name + " (" + targets + ")"; }
    
    // Getters and Setters
    public String getName() { return name; }
    public ArrayList<Target> getTargets() { return targets; }
    
}

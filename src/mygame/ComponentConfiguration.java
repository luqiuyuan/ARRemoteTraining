/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.math.Matrix4f;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author lu
 */
public class ComponentConfiguration {
    
    private ArrayList<Component> components;
    
    public ComponentConfiguration(String text) {
        this.components = new ArrayList<>();
        this.parse(text);
    }
    
    public void addComponent(Component component) { this.components.add(component); }
    
    //================================
    // Getters and setters
    //================================
    
    public ArrayList<Component> getComponents() { return this.components; }
    public void setComponents(ArrayList<Component> components) { this.components = components; }
    
    //================================
    // Private helpers
    //================================
    
    public void parse(String text) {
        JSONObject root_json = new JSONObject(text);
        JSONArray components_json = root_json.getJSONArray("components");
        for (int i = 0; i < components_json.length(); i++) {
            JSONObject component_json = components_json.getJSONObject(i);
            String name_component = component_json.getString("name");
            // Create component
            Component component = new Component(name_component);
            this.addComponent(component);
            JSONArray targets_json = component_json.getJSONArray("targets");
            for (int j = 0; j < targets_json.length(); j++) {
                JSONObject target_json = targets_json.getJSONObject(j);
                String name_target = target_json.getString("name");
                JSONArray transformation_json = target_json.getJSONArray("transformation");
                float[] data = new float[16];
                for (int k = 0; k < 16; k++) {
                    double num = transformation_json.getDouble(k);
                    data[(k%4)*4+k/4] = (float) num;
                }
                Matrix4f transformation = new Matrix4f(data);
                // Create target
                component.addTarget(name_target, transformation);
            }
        }
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.math.Matrix4f;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 *
 * @author lu
 */
public class JSONLoader implements AssetLoader {
    
    public Object load(AssetInfo asset_info) throws IOException {
        InputStream is = asset_info.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line, text = "";
        while((line = reader.readLine()) != null) {
            text += line+"\n";
        }
        reader.close();
        
        JSONObject root_json = new JSONObject(text);
        JSONArray components_json = root_json.getJSONArray("components");
        for (int i = 0; i < components_json.length(); i++) {
            JSONObject component_json = components_json.getJSONObject(i);
            String name_component = component_json.getString("name");
            // Create component
            Component component = new Component(name_component);
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
        
        return Component.getComponents();
    }
    
}

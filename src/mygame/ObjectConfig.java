/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.math.Matrix4f;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author lu
 */
public class ObjectConfig {
    
    private static Map<String, Map<String, Matrix4f>> objects;
    private static Map<String, String> targets;
    
    public static void init() {
        // Initialize instance variables
        ObjectConfig.objects = new HashMap<>();
        ObjectConfig.targets = new HashMap<>();
        
        Path inputFilepath = Paths.get("assets/target_merge.json");
        Charset charset = Charset.forName("UTF-8");
        try {
            BufferedReader reader = Files.newBufferedReader(inputFilepath, charset);
            String json_str = "", line;
            while ((line = reader.readLine()) != null) {
              json_str += line;
            }
            reader.close();
            
            JSONObject json_object = new JSONObject(json_str);
            Iterator<String> keys = json_object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                ObjectConfig.objects.put(key, new HashMap<String, Matrix4f>());
                JSONArray json_array = json_object.getJSONArray(key);
                for (int i = 0; i < json_array.length(); i++) {
                    JSONObject json_object2 = json_array.getJSONObject(i);
                    String name = json_object2.getString("name");
                    float[] data = new float[16];
                    JSONArray json_array2 = json_object2.getJSONArray("data");
                    for (int j = 0; j < json_array2.length(); j++) {
                        data[j] = (float) json_array2.getDouble(j);
                    }
                    Matrix4f transformation = new Matrix4f();
                    transformation.set(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9], data[10], data[11], data[12], data[13], data[14], data[15]);
                    ObjectConfig.objects.get(key).put(name, transformation.invert());
                    ObjectConfig.targets.put(name, key);
                }
            }
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
    }
    
    public static String getObjectNameFromTargetName(String name_target) {
        return ObjectConfig.targets.get(name_target);
    }
    
    public static Matrix4f getDeltaMatrix(String name_object, String name_target) {
        return ObjectConfig.objects.get(name_object).get(name_target);
    }
    
}

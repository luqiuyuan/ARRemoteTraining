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
import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 *
 * @author lu
 */
public class JSONLoader implements AssetLoader {
    
    public Object load(AssetInfo asset_info) throws IOException {
        ArrayList<String> model_names = new ArrayList<>();
        
        InputStream is = asset_info.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line, text = "";
        while((line = reader.readLine()) != null) {
            text += line+"\n";
        }
        reader.close();
        
        JSONObject root_json = new JSONObject(text);
        JSONArray model_names_json = root_json.getJSONArray("models");
        for (int i = 0; i < model_names_json.length(); i++) {
            model_names.add(model_names_json.getString(i));
        }
        
        return model_names;
    }
    
}

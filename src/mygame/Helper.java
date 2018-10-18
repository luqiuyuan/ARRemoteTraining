/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 *
 * @author lu
 */
public class Helper {
    
    public static boolean areTwoTransformationSimilar(Matrix4f transformation1, Matrix4f transformation2) {
        // Calculate diff of translation
        Vector3f translation1 = transformation1.toTranslationVector();
        Vector3f translation2 = transformation2.toTranslationVector();
        Vector3f diff_translation = translation2.subtract(translation1);
        float diff_translation_length = diff_translation.length();
        
        // Calculate diff of rotation
        Matrix3f rotation1 = transformation1.toRotationMatrix();
        Matrix3f rotation2 = transformation2.toRotationMatrix();
        Matrix3f diff_rotation = rotation2.mult(rotation1.invert());
        Quaternion diff_quaternion = new Quaternion();
        diff_quaternion.fromRotationMatrix(diff_rotation);
        Vector3f axis = new Vector3f();
        float diff_rotation_angle = diff_quaternion.toAngleAxis(axis);
        
        if (Config.DEUBG_MODE) {
//            System.out.println(transformation1.toString() + " : " + transformation2.toString());
//            System.out.println(diff_translation_length + " : " + diff_rotation_angle);
        }
        
        if (diff_translation_length <= Config.TRANSLATION_THRESHOLD && diff_rotation_angle <= Config.ROTATION_THRESHOLD) {
            return true;
        } else {
            return false;
        }
    }
    
}

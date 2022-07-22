package com.mcmiddleearth.plotsquared.util;

import com.mcmiddleearth.plotsquared.review.ReviewPlot;

import java.io.*;

public class FlatFile {

    public static void writeObjectToFile(ReviewPlot obj, File file){
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(obj);
            oos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Deserialization
    // Get object from a file.
    public static ReviewPlot readObjectFromFile(File file){
        ReviewPlot result = null;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            result = (ReviewPlot) ois.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }


}
package de.omegazirkel.risingworld;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.omegazirkel.risingworld.tools.OZLogger;

public class Utils {

    public static OZLogger logger() {
        return OZLogger.getInstance("OZ.DiscordConnect.Utils");
    }

    public static File byteArrayToFile(byte[] imageBytes, String fileName) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            logger().error("Invalid image data.");
            return null;
        }

        File outputFile = new File(fileName);
        try (InputStream in = new ByteArrayInputStream(imageBytes);
                FileOutputStream out = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[1024]; // 1KB buffer
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return outputFile;
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
            logger().error("Error while writing file: " + e.getMessage());
            // remove any existing data on error
            if (outputFile.exists()) {
                if (!outputFile.delete()) {
                    logger().error("Error while deleting corrupted file.");
                }
            }
            throw e; // delegate exception to handle it in the parent method
        }
    }
}

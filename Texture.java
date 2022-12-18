package project.util;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import org.lwjgl.BufferUtils;
import javax.imageio.ImageIO;
import java.nio.ByteBuffer;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class Texture {

    public int textureId;
    public int width, height;

    private static Map<String, Texture> textureMap = new HashMap<>();

    // To prevent creating multiple instances of the same texture
    private Texture() {}

    public static Texture load(String fileLocation) {
        if (textureMap.containsKey(fileLocation))
            return textureMap.get(fileLocation);

        try {
            Texture texture = new Texture();
            File file = new File(fileLocation);
            if (!file.exists())
                throw new IllegalArgumentException("File does not exist!");

            BufferedImage temp = ImageIO.read(file);
            ByteBuffer buffer = BufferUtils.createByteBuffer((texture.width = temp.getWidth()) * (texture.height = temp.getHeight()) * 4);
            for (int y = 0; y < temp.getHeight(); y++) {
                for (int x = 0; x < temp.getWidth(); x++) {
                    int color = temp.getRGB(x, y);
                    buffer.put((byte) ((color >> 16) & 0xff));
                    buffer.put((byte) ((color >> 8) & 0xff));
                    buffer.put((byte) (color & 0xff));
                    buffer.put((byte) ((color >> 24) & 0xff));
                }
            }
            buffer.flip();
            texture.textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture.textureId);

            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, texture.width, texture.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
            glBindBuffer(GL_TEXTURE_2D, 0);
            return texture;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void delete() {
        glDeleteTextures(textureId);
    }
}


package project.ui;

import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

/**
 *  Font Renderer using LWJGL (uses depricated methods which work on opengl version < 4, don't hurt me)
 *  Made by Luca Warmenhoven
 */

public class FontRenderer {

    /**
     * A bunch of variables to store the recently made fonts, to prevent remaking it.
     */
    private static Map<String, FontRenderer> loadedFonts = new HashMap<>();
    public static Collection<FontRenderer> getLoadedFonts() { return loadedFonts.values(); }
    public static Map<String, FontRenderer> getFontMap() { return loadedFonts; }

    private BufferedImage fontImage;
    private FontMetrics fontMetrics;
    private int texBufferId;
    private String fontMapEntry;
    private int charIdxMin = 32, charIdxMax = 126;
    private CharData[] charData = new CharData[charIdxMax - charIdxMin + 1]; // char 32 till 126 should be drawn (inclusive)

    /**
     * The constructor for the custom font renderer
     * @param fontName  The name of the font. The font has to exist on your PC. Returns NullPointerException if not. Too lazy to check
     * @param fontSize  The size of the font we would like to create
     * @param style     The fontstyle we want. Example: Font.BOLD, Font.REGULAR, Font.ITALIC
     */
    public FontRenderer(String fontName, int fontSize, int style) {

        if (loadedFonts.containsKey(fontName.toLowerCase() + fontSize))
            throw new IllegalAccessError("Font with the given size already exists!");

        fontImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        this.fontMapEntry = fontName.toLowerCase() + fontSize;

        Graphics2D gfx = (Graphics2D)fontImage.getGraphics();
        Font font = Font.decode(fontName);
        font = font.deriveFont(style, fontSize);
        gfx.setFont(font);
        gfx.setColor(Color.WHITE);
        gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (font == null)
            throw new RuntimeException("No font found!");


        fontMetrics = gfx.getFontMetrics();

        int x = 0;
        int y = fontMetrics.getHeight();

        // Draws all the characters onto a bitmap image using java Graphics2D.
        for (int i = 0; i < charData.length; i++) {
            char current = (char)(i + charIdxMin);

            if (x + fontMetrics.charWidth(current) > fontImage.getWidth()) {
                x = 0;
                y += fontMetrics.getHeight() + 5;
            }
            gfx.drawString(String.valueOf(current), x, y);
            charData[i] = new CharData(current,
                    fontImage.getWidth(), fontImage.getHeight(),
                    x, y - fontMetrics.getHeight() + 5,
                    fontMetrics.stringWidth(String.valueOf(current)), fontMetrics.getHeight(),
                    fontMetrics.getAscent(), fontMetrics.getDescent());
            x += fontMetrics.stringWidth(String.valueOf(current));
        }

        System.out.println("Registered font '" + fontName + "' with size " + fontSize);
        loadedFonts.put(this.fontMapEntry, this);

    }

    /**
     * Loads the given image data into OpenGL.
     * This has to be done on a different thread due to threading issues with Graphics2D
     */
    public void load() {
        if (texBufferId != 0) {
            System.err.println("Error: Font already loaded!");
            return;
        }

        ByteBuffer buf = BufferUtils.createByteBuffer(fontImage.getWidth() * fontImage.getHeight() * 4);

        for (int y = 0; y < fontImage.getHeight(); y++) {
            for (int x = 0; x < fontImage.getWidth(); x++) {
                int color = fontImage.getRGB(x, y);
                buf.put((byte) ((color >> 16) & 0xff));
                buf.put((byte) ((color >> 8) & 0xff));
                buf.put((byte) (color & 0xff));
                buf.put((byte) ((color >> 24) & 0xff));
            }
        }
        buf.flip();
        texBufferId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texBufferId);

        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, fontImage.getWidth(), fontImage.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        glBindBuffer(GL_TEXTURE_2D, 0);
    }

    public void delete() {
        loadedFonts.remove(this.fontMapEntry);
        glDeleteTextures(texBufferId);
    }

    /**
     * Renders a single character on the screen based on the given parameters
     * @param character The character we would like to render
     * @param x         The x coordinate of the character we would like to render
     * @param y         The y coordinate of the character we would like to render
     */
    public void drawChar(char character, float x, float y) {

        if (character >= charIdxMin && character <= charIdxMax) { // checking if the given character is registered
            CharData data = charData[character - charIdxMin];

            glTexCoord2f(data.texX, data.texY); // top left
            glVertex2f(x, y);

            glTexCoord2f(data.texX + data.texW, data.texY); // top right
            glVertex2f(x + data.charWidth, y);

            glTexCoord2f(data.texX + data.texW, data.texY + data.texH); // bottom right
            glVertex2f(x + data.charWidth, y + data.charHeight);

            glTexCoord2f(data.texX, data.texY + data.texH); // bottom left
            glVertex2f(x, y + data.charHeight);
        }
    }

    public void drawStringf(String text, float x, float y, int color, Object... formatters) {
        drawString(String.format(text, formatters), x, y, color);
    }

    /**
     * Method of rendering text on the screen with given parameters.
     * @param text  The text we would like to render on our screen
     * @param x     The x coordinate of the renderable text
     * @param y     The y coordinate of the renderable text
     * @param color The color of the text we would like to render
     */
    public void drawString(String text, float x, float y, int color) {
        char[] characters = text.toCharArray();
        float temp_x = x + 5;
        float temp_y = y;
        glColor4f(
                ((color >> 16) & 0xff) / 255.0f,
                ((color >> 8) & 0xff) / 255.0f,
                (color & 0xff) / 255.0f,
                ((color >> 24) & 0xff) / 255.0f
        );

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texBufferId);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBegin(GL_QUADS);

        for (int i = 0; i < characters.length; i++) {
            if (characters[i] == '\n')
            {
                temp_x = x + 5;
                temp_y += fontMetrics.getHeight();
                continue;
            }

            if (characters[i] > charIdxMax || characters[i] < charIdxMin) // not loaded onto the bitmap
                continue;
            drawChar(characters[i], temp_x, temp_y);
            temp_x += charData[characters[i] - charIdxMin].charWidth;
        }
        glEnd();
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);

    }

    /**
     * @param text The input text we would like to retreive the width of
     * @return     The width of the input text in pixels.
     */
    public double stringWidth(String text) {
        double len = 0;
        char[] characters = text.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            if (characters[i] > charIdxMax || characters[i] < charIdxMin) // not loaded onto the bitmap
                continue;
            CharData data = charData[characters[i] - charIdxMin];
            len += data.charWidth;
        }
        return len;
    }

    /**
     * @return The height of the font instance
     */
    public float stringHeight() { return fontMetrics.getHeight(); }

    /**
     * @param fontName The font we would like to retreive from the fontmap
     * @param size     The size of the font we would like to retreive
     * @return         The instance of the font in the fontmap, if it exists.
     */
    public static FontRenderer getFont(String fontName, int size) {
        return loadedFonts.get(fontName.toLowerCase() + size);
    }

    public CharData getCharData(char input) {
        CharData value = null;
        for (int i = 0; i < charData.length; i++) {
            if (charData[i].character == input)
            {
                value = charData[i];
                break;
            }
        }
        return value;
    }

    /**
     * A class for storing character data for in the bitmap image.
     * Useful for retrieving texture coordinates later on.
     */
    public class CharData {

        int ascend, descend;
        char character;
        float texX, texY, texW, texH;
        int imageX, imageY, charWidth, charHeight;

        public CharData(char character, int imgW, int imgH, int x, int y, int charWidth, int charHeight, int ascend, int descend) {
            this.texX = (1.0f / imgW) * x;
            this.texY = (1.0f / imgH) * y;
            this.texW = (1.0f / imgW) * charWidth;
            this.texH = (1.0f / imgH) * charHeight;
            this.imageX = x;
            this.imageY = y;
            this.charWidth = charWidth;
            this.charHeight = charHeight;
            this.character = character;
            this.ascend = ascend;
            this.descend = descend;
        }

        @Override
        public String toString() {
            return "CharData{" +
                    "x=" + imageX +
                    ", y=" + imageY +
                    ", width=" + charWidth +
                    ", height=" + charHeight +
                    ", imgPosX: " + texX +
                    ", imgPosY: " + texY +
                    ", ascend=" + ascend +
                    ", descend=" + descend +
                    ", character=" + character +
                    '}';
        }
    }

}

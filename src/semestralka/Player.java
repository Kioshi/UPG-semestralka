package semestralka;

import javafx.geometry.Point3D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Štěpán Martínek on 08.05.2016.
 */
public class Player
{
    public Point3D player;
    public Point3D blast;
    public ArrayList<Point3D> trajectoryPoints = new ArrayList<>();
    public int attempts = 0;
    public boolean stillPlaying = true;
    public Color color = Color.YELLOW;
    public BufferedImage image;
    int money = 20000;

    public Player() throws IOException
    {
        try
        {
            image = ImageIO.read(new File("./images/Pacman.png"));
        }
        catch (IOException e)
        {
            throw new IOException("Program could not load player image.");
        }
    }

    void setColor(Color c)
    {
        color = c;
        image = colorImage(image);
    }

    private BufferedImage colorImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        WritableRaster raster = image.getRaster();

        for (int xx = 0; xx < width; xx++) {
            for (int yy = 0; yy < height; yy++) {
                int[] pixels = raster.getPixel(xx, yy, (int[]) null);
                pixels[0] = color.getRed();
                pixels[1] = color.getGreen();
                pixels[2] = color.getBlue();
                raster.setPixel(xx, yy, pixels);
            }
        }
        return image;
    }

    public void payForRocket(int cost)
    {
        money -= cost;
        if (money <= 0)
            stillPlaying = false;
    }
}

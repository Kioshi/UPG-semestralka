package semestralka;

import javafx.geometry.Point3D;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static semestralka.Main.*;

/**
 * Created by Štěpán Martínek on 13.03.2016.
 */
class MapPanel extends JPanel
{
    private static final int ICON_SIZE = 15;
    private static final int WIND_SCALE = 2;
    private static final int TRAJECTORY_STROKE = 2;
    private static final int ROCKET_ICON_SIZE = 15;
    final private int width;
    final private int height;

    private static final int CROSS_SIZE = 5;

    private BufferedImage target;
    private BufferedImage missile;

    MapPanel(int w, int h) throws IOException
    {
        width = w;
        height = h;
        Dimension dimension = new Dimension(w,h);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
        setVisible(true);

        try
        {
            target = ImageIO.read(new File("./images/Ghost.png"));
        }
        catch (IOException e)
        {
            throw new IOException("Program could not load target image.");
        }
        try
        {
            missile = ImageIO.read(new File("./images/Missile.png"));
        }
        catch (IOException e)
        {
            throw new IOException("Program could not load missile image.");
        }
    }

    @Override
    public void paint(Graphics g)
    {
        _paint((Graphics2D)g);
    }

    private void _paint(Graphics2D g)
    {
        super.paint(g);

        drawMap(g);

        if (playersNr == 1)
            drawTarget(g);

        for (Player p : players)
        {
            if (p.player == null)
                continue;
            drawTrajectory(g,p);
            drawPlayer(g,p);
            drawRocket(g,p);
            drawBlastRadius(g,p);
        }
        drawWindIndicator(g);
    }

    private void drawWindIndicator(Graphics2D g)
    {
        if (Main.wind == null)
            return;

        int startX = 5+Main.MAX_WIND*WIND_SCALE;
        int startY = height-Main.MAX_WIND*WIND_SCALE-5;


        ActuallyUsefulLine line = new ActuallyUsefulLine(new Point2D.Double(0,0),Main.wind);
        double x = line.length()/(double)Main.MAX_WIND;
        line.setLength(Main.MAX_WIND);
        g.setColor(new Color(Color.HSBtoRGB(0.3333f-(float)x/3.0f,1.0f,1.0f)));
        drawArrow(g,startX,startY,(int)(startX + line.p2.x*WIND_SCALE), (int)(startY + line.p2.y*WIND_SCALE),2);
        if (DEBUG)
            g.drawString(String.format("%.2f",x), startX, startY - Main.MAX_WIND * WIND_SCALE);
    }

    private void drawArrow(Graphics2D g2, double x1, double y1, double x2, double y2, double lineThickness)
    {

        Double sx, sy, dv, kx, ky;
        g2.setStroke(new BasicStroke((float)lineThickness));

        g2.draw(new Line2D.Double(x1, y1, x2, y2));

        sx = x2 - x1;
        sy = y2 - y1;

        dv = Math.sqrt(sx*sx + sy*sy);

        sx /= dv;
        sy /= dv;

        kx = -sy;
        ky = sx;

        kx *= lineThickness;
        ky *= lineThickness;
        sx *= lineThickness;
        sy *= lineThickness;

        g2.draw(new Line2D.Double(x2 - sx + kx, y2 - sy + ky, x2, y2));
        g2.draw(new Line2D.Double(x2 - sx - kx, y2 - sy - ky, x2, y2));
        g2.setStroke(new BasicStroke(1));

    }

    private void drawBlastRadius(Graphics2D g, Player p)
    {
        if (p == currentlyPlaying && rocketStep > -1)
            return;

        g.setColor(p.color);
        Point3D point = p.blast;
        if (point == null)
            return;

        g.fillOval((int)(point.getX() - Main.blastRadius), (int)(point.getY()  - Main.blastRadius), (int)Main.blastRadius*2, (int)Main.blastRadius*2);
    }

    private void drawTarget(Graphics2D g)
    {
        g.drawImage(target,(int)(Main.target.getX()-ICON_SIZE/2),(int)(Main.target.getY()-ICON_SIZE/2),ICON_SIZE,ICON_SIZE,null);
    }


    private void drawPlayer(Graphics2D g, Player p)
    {
        g.drawImage(p.image,(int)(p.player.getX()-ICON_SIZE/2),(int)(p.player.getY()-ICON_SIZE/2),ICON_SIZE,ICON_SIZE,null);
    }

    private void drawCross(Graphics2D g, Color color, Point2D.Double point)
    {
        g.setColor(color);
        g.setStroke(new BasicStroke(2));
        g.drawLine((int)(point.x - CROSS_SIZE),(int)point.y, (int)(point.x + CROSS_SIZE), (int)point.y);
        g.drawLine((int)point.x, (int)(point.y - CROSS_SIZE), (int)point.x, (int)(point.y + CROSS_SIZE));
        g.setStroke(new BasicStroke(1));
    }

    private void drawMap(Graphics2D g)
    {
        g.drawImage(mapImage,0,0,width,height,null);
    }

    private void drawTrajectory(Graphics2D g, Player p)
    {
        if (p.trajectoryPoints.isEmpty() || p != currentlyPlaying)
            return;

        Color color1 = new Color(255,239,124);
        Color color2 = new Color(137,22,22);

        for (int i=1; i < p.trajectoryPoints.size(); i++)
        {
            if (rocketStep != -1 &&  i >= rocketStep)
                break;
            Point3D p1 = p.trajectoryPoints.get(i-1);
            Point3D p2 = p.trajectoryPoints.get(i);

            try
            {
                double ratio = Math.abs((p1.getZ() - trajectoryMinZ) / (trajectoryMaxZ - trajectoryMinZ));
                int red = (int) (color2.getRed() * ratio + color1.getRed() * (1 - ratio));
                int green = (int) (color2.getGreen() * ratio + color1.getGreen() * (1 - ratio));
                int blue = (int) (color2.getBlue() * ratio + color1.getBlue() * (1 - ratio));
                g.setColor(new Color(red, green, blue));
            }
            catch (Exception e)
            {
                g.setColor(color2);
            }
            Stroke stroke = g.getStroke();
            g.setStroke(new BasicStroke(TRAJECTORY_STROKE));
            g.drawLine((int)p1.getX(),(int)p1.getY(),(int)p2.getX(),(int)p2.getY());
            g.setStroke(stroke);
        }
    }

    public void drawRocket(Graphics2D g, Player p)
    {
        if (rocketStep < 0 || rocketStep > p.trajectoryPoints.size() || p.trajectoryPoints.size() < 2 || p != currentlyPlaying)
            return;

        double x1;
        double y1;
        double x2;
        double y2;
        if (rocketStep == 0)
        {
            x1 = p.player.getX();
            y1 = p.player.getY();
            x2 = p.trajectoryPoints.get(0).getX();
            y2 = p.trajectoryPoints.get(0).getY();
        }
        else if (rocketStep == p.trajectoryPoints.size())
        {
            x1 = p.trajectoryPoints.get(p.trajectoryPoints.size()-2).getX();
            y1 = p.trajectoryPoints.get(p.trajectoryPoints.size()-2).getY();
            x2 = p.trajectoryPoints.get(p.trajectoryPoints.size()-1).getX();
            y2 = p.trajectoryPoints.get(p.trajectoryPoints.size()-1).getY();
        }
        else
        {
            x1 = p.trajectoryPoints.get(rocketStep-1).getX();
            y1 = p.trajectoryPoints.get(rocketStep-1).getY();
            x2 = p.trajectoryPoints.get(rocketStep).getX();
            y2 = p.trajectoryPoints.get(rocketStep).getY();
        }

        ActuallyUsefulLine line = new ActuallyUsefulLine(new Point2D.Double(x1,y1), new Point2D.Double(x2,y2));
        AffineTransform at = new AffineTransform();

        at.translate((int)x1 - ROCKET_ICON_SIZE /2, (int)y1 - ROCKET_ICON_SIZE/2);
        at.scale(ROCKET_ICON_SIZE/(double)missile.getWidth(), ROCKET_ICON_SIZE/(double)missile.getHeight());
        at.rotate(-line.angle(),missile.getWidth()/2.0, missile.getHeight()/2.0);

        g.drawImage(missile, at, null);
    }

}

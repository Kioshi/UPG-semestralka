package semestralka;

import javafx.geometry.Point3D;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by Štěpán Martínek on 13.03.2016.
 */
public class MapPanel extends JPanel
{

    private static final int ICON_SIZE = 15;
    private static final int WIND_SCALE = 5;
    private static final int TRAJECTORY_POINT_SIZE = 3;
    public int width;
    public int height;

    public static final int CROSS_SIZE = 5;

    BufferedImage player;
    BufferedImage target;

    MapPanel(int w, int h) throws IOException
    {
        width = w;
        height = h;
        Dimension dimension = new Dimension(w,h);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
        setVisible(true);

        player = ImageIO.read(new File("./images/Pacman.png"));
        target = ImageIO.read(new File("./images/Ghost.png"));
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
        drawPlayer(g);
        drawTarget(g);
        drawBlastRadius(g);
        drawWindIndicator(g);
        drawTrajectory(g);
    }

    private void drawWindIndicator(Graphics2D g)
    {
        if (Main.wind == null)
            return;

        int startX = 1+Main.MAX_WIND*WIND_SCALE;
        int startY = height-Main.MAX_WIND*WIND_SCALE-1;

        g.setColor(Color.PINK);
        //g.drawRect(0,height - Main.MAX_WIND*2,Main.MAX_WIND*2,height);
        g.setColor(Color.MAGENTA);
        drawArrow(g,startX,startY,(int)(startX + Main.wind.x*WIND_SCALE), (int)(startY - Main.wind.y*WIND_SCALE),2);
    }

    public void drawArrow(Graphics2D g2,double x1, double y1, double x2, double y2,double lineThickness)
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

    private void drawBlastRadius(Graphics2D g)
    {
        g.setColor(Color.ORANGE);
        Point2D.Double point = Main.blast;
        if (point == null)
            return;

        g.fillOval((int)(point.x - Main.BLAST_RADIUS), (int)(point.y  - Main.BLAST_RADIUS), Main.BLAST_RADIUS*2, Main.BLAST_RADIUS*2);
    }

    private void drawTarget(Graphics2D g)
    {
        //drawCross(g, Color.RED,Main.target);
        g.drawImage(target,(int)(Main.target.x-ICON_SIZE/2),(int)(Main.target.y-ICON_SIZE/2),ICON_SIZE,ICON_SIZE,null);
    }


    private void drawPlayer(Graphics2D g)
    {
        //drawCross(g, Color.BLUE,Main.player);
        g.drawImage(player,(int)(Main.player.x-ICON_SIZE/2),(int)(Main.player.y-ICON_SIZE/2),ICON_SIZE,ICON_SIZE,null);
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
        g.drawImage(Main.mapImage,0,0,width,height,null);
    }

    void drawTrajectory(Graphics2D g)
    {
        if (Main.trajectoryPoints.isEmpty())
            return;

        for (Point3D point : Main.trajectoryPoints)
        {
            g.setColor(Color.RED);
            g.fillOval((int)point.getX() - TRAJECTORY_POINT_SIZE, (int)point.getY() - TRAJECTORY_POINT_SIZE,  TRAJECTORY_POINT_SIZE*2, TRAJECTORY_POINT_SIZE*2);
            g.drawString(""+point.getZ(),(float)point.getX(),(float)point.getY());
        }
    }

}

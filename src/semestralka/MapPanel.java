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

import static semestralka.Main.trajectoryPoints;

/**
 * Created by Štěpán Martínek on 13.03.2016.
 */
class MapPanel extends JPanel
{

    private static final boolean DEBUG = Main.DEBUG;
    private static final int ICON_SIZE = 15;
    private static final int WIND_SCALE = 2;
    private static final int TRAJECTORY_STROKE = 2;
    private int width;
    private int height;

    private static final int CROSS_SIZE = 5;

    private BufferedImage player;
    private BufferedImage target;

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
        drawTrajectory(g);
        drawPlayer(g);
        drawTarget(g);
        drawBlastRadius(g);
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
        g.drawImage(target,(int)(Main.target.x-ICON_SIZE/2),(int)(Main.target.y-ICON_SIZE/2),ICON_SIZE,ICON_SIZE,null);
    }


    private void drawPlayer(Graphics2D g)
    {
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

    private void drawTrajectory(Graphics2D g)
    {
        if (trajectoryPoints.isEmpty())
            return;

        for (int i=1; i < trajectoryPoints.size(); i++)
        {
            Point3D p1 = trajectoryPoints.get(i-1);
            Point3D p2 = trajectoryPoints.get(i);
            g.setColor(Color.RED);
            Stroke stroke = g.getStroke();
            g.setStroke(new BasicStroke(TRAJECTORY_STROKE));
            g.drawLine((int)p1.getX(),(int)p1.getY(),(int)p2.getX(),(int)p2.getY());
            g.setStroke(stroke);
        }
    }

}

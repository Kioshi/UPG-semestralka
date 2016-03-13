package semestralka;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Created by Kexik on 13.03.2016.
 */
public class MapPanel extends JPanel
{

    public int width;
    public int height;

    public static final int CROSS_SIZE = 10;

    MapPanel(int w, int h)
    {
        width = w;
        height = h;
        Dimension dimension = new Dimension(w,h);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
        setVisible(true);
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
    }

    private void drawBlastRadius(Graphics2D g)
    {
        g.setColor(Color.RED);
        Point2D.Double point = Main.blast;
        if (point == null)
            return;

        g.fillOval((int)(point.x - Main.BLAST_RADIUS/2), (int)(point.y  - Main.BLAST_RADIUS/2), Main.BLAST_RADIUS, Main.BLAST_RADIUS);
    }

    private void drawTarget(Graphics2D g)
    {
        drawCross(g, Color.RED,Main.target);
    }


    private void drawPlayer(Graphics2D g)
    {
        drawCross(g, Color.BLUE,Main.player);
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
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        for (int i =0; i <width; i++)
            for (int j =0; j <height; j++)
            {
                int point = Main.map[i][j];
                int color = (int)(((point-Main.minH) / (double)(Main.maxH-Main.minH))*0xFF);
                if (Main.minH == Main.maxH)
                    color = 128;
                g.setColor(new Color(color,color,color,0xFF));
                g.fillRect(i,j,1,1);
            }
    }

}

package semestralka;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

public class Main
{
    public static final int BLAST_RADIUS = 10;
    public static int[][] map;
    public static int h;
    public static int w;
    public static Point2D.Double player;
    public static Point2D.Double target;
    public static Point2D.Double blast;
    public static int minH;
    public static int maxH;
    public static MapPanel mapPanel;

    public static void main(String[] args) throws IOException
    {
        loadMap();
        createWindow();
        play();

    }

    private static void play()
    {
        System.out.println("Player-Target air distance: " + player.distance(target));
        System.out.println("Player heigh: " + map[(int)player.x][(int)player.y]);
        System.out.println("Target heigh: " + map[(int)target.x][(int)target.y]);

        Scanner scanner = new Scanner(System.in);

        while(true)
        {
            System.out.print("Angle: ");
            double angle = scanner.nextDouble();
            System.out.print("Distance: ");
            double distance = scanner.nextDouble();

            calculateBlastLocation(angle,distance);

            if (target.distance(blast) <= BLAST_RADIUS)
                break;

            System.out.println("Nope. Try again!");
            mapPanel.repaint();

        }
        System.out.println("You won!");
        mapPanel.repaint();
    }

    private static void calculateBlastLocation(double angle, double distance)
    {
        ActuallyUsefulLine line = new ActuallyUsefulLine();

        line.setP1(player);
        line.setLength(distance);
        line.setAngle(angle);

        blast = line.p2;
    }

    private static void createWindow()
    {
        JFrame frame = new JFrame("The Game");
        mapPanel = new MapPanel(w, h);

        frame.setLayout(new GridBagLayout());
        frame.add(mapPanel);
        frame.setSize(w + 50, h + 50);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void loadMap() throws IOException
    {
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream("./terrens/terrain257x257_300_600.ter"));

        w = dataInputStream.readInt();
        h = dataInputStream.readInt();
        player = new Point2D.Double(dataInputStream.readInt(),dataInputStream.readInt());
        target = new Point2D.Double(dataInputStream.readInt(),dataInputStream.readInt());

        map = new int[w][h];

        for (int i =0; i <w; i++)
            for (int j =0; j <h; j++)
            {
                map[i][j] = dataInputStream.readInt();

                if (i ==0 && j == 0)
                    minH = maxH = map[i][j];

                minH = Math.min(minH,map[i][j]);
                maxH = Math.max(maxH,map[i][j]);
            }

    }
}

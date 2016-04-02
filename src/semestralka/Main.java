package semestralka;

import javafx.geometry.Point3D;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by Štěpán Martínek on 13.03.2016.
 */

public class Main
{
    public static final int BLAST_RADIUS = 3;
    public static final int MAX_WIND = 10;
    public static int[][] map;
    public static int h;
    public static int w;
    public static Point2D.Double player;
    public static Point2D.Double target;
    public static Point2D.Double blast;
    public static Point2D.Double wind;
    public static BufferedImage mapImage;
    public static ArrayList<Point3D> trajectoryPoints = new ArrayList<>();

    public static JFrame frame;

    public static Scanner scanner;

    public static void main(String[] args) throws IOException
    {
        scanner = new Scanner(System.in);

        loadMap(args);
        createWindow();
        do
        {
            play();

            System.out.println("Press Y to play again.");
        }
        while(scanner.next().toUpperCase().equals("Y"));
        frame.dispose();
    }


    private static void play()
    {
        System.out.println("Player-Target air distance: " + player.distance(target));
        System.out.println("Player heigh: " + map[(int)player.x][(int)player.y]);
        System.out.println("Target heigh: " + map[(int)target.x][(int)target.y]);

        generateWind();
        repaint();

        while(true)
        {
            System.out.print("Angle: ");
            double angle = scanner.nextDouble();
            /*System.out.print("Distance: ");
            double distance = scanner.nextDouble();
            calculateBlastLocation(angle,distance);
            */
            System.out.println("Elevation: ");
            double elevation = scanner.nextDouble();

            System.out.println("Rocket speed: ");
            double startSpeed = scanner.nextDouble();
            launchRocket(angle,elevation,startSpeed);

            if (blast == null)
            {
                System.out.println("Rocket out of playground!");
                repaint();
                continue;
            }

            if (target.distance(blast) <= BLAST_RADIUS)
            {
                System.out.println("You won!");
                break;
            }
            if (player.distance(blast) <= BLAST_RADIUS)
            {
                System.out.println("Game over!");
                break;
            }

            System.out.println("Nope. Try again!");
            repaint();

        }
        repaint();
    }

    private static void launchRocket(double angle, double elevation, double startSpeed)
    {
        trajectoryPoints.clear();
        Point3D rockPos = new Point3D(player.x, player.y, (double)map[(int)player.x][(int)player.y]);
        trajectoryPoints.add(rockPos);
        double deltaT = 0.01;
        Point3D rockSpd;
        double g = 10.0;
        double b = 0.05;
        Point3D windSpeed = new Point3D(wind.x, wind.y, 0);

        {
            ActuallyUsefulLine l1 = new ActuallyUsefulLine();
            l1.setAngle(elevation);
            l1.setLength(startSpeed);
            ActuallyUsefulLine l2 = new ActuallyUsefulLine();
            l2.setAngle(0);
            l2.setLength(l1.p2.x);
            l2.setAngle(angle);
            rockSpd = new Point3D(l2.p2.x, l2.p2.y, -l1.p2.y);
        }

        //System.out.println(rockPos.toString() + " " + rockSpd.toString());

        while(true)
        {
            //first we use old speed
            Point3D newRockPos = rockPos.add(rockSpd);

            //save new position
            trajectoryPoints.add(newRockPos);

            //recaluculate speed
            // y = vt + (0,0,-1)*g*deltaT
            Point3D newRockSpd = rockSpd.add(new Point3D(0,0,-1).multiply(g*deltaT));
            // y = vw - vt
            Point3D temp = windSpeed.add(rockSpd);
            // (x)*b*deltaT
            temp = temp.multiply(deltaT/b);
            // newSpeed = y + x
            newRockSpd = newRockSpd.add(temp);

            rockPos = newRockPos;
            rockSpd = newRockSpd;

            //System.out.println(rockPos.toString() + " " + rockSpd.toString());

            if (rockPos.getX() < 0 || rockPos.getY() < 0 || rockPos.getX() >= w || rockPos.getY() >= h)
            {
                blast = null;
                break;
            }

            if (rockPos.getZ() <= map[(int)rockPos.getX()][(int)rockPos.getY()])
            {
                blast = new Point2D.Double(rockPos.getX(),rockPos.getY());
                break;
            }
        }
    }

    private static void generateWind()
    {
        Random rand = new Random();
        int x = rand.nextInt((MAX_WIND + MAX_WIND) + 1) -MAX_WIND;
        int y = rand.nextInt((MAX_WIND + MAX_WIND) + 1) -MAX_WIND;
        wind = new Point2D.Double(x,y);
        //wind = new Point2D.Double(0,0);
    }

    private static void repaint()
    {
        frame.toFront();
        frame.repaint();
    }

    private static void calculateBlastLocation(double angle, double distance)
    {
        ActuallyUsefulLine line = new ActuallyUsefulLine();

        line.setP1(player);
        line.setLength(distance);
        line.setAngle(angle);

        blast = line.p2;
    }

    private static void createWindow() throws IOException
    {
        frame = new JFrame("The Game");
        frame.setLayout(new GridBagLayout());
        frame.add(new MapPanel(w, h));
        frame.setSize(w + 50, h + 50);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
    }

    public static void loadMap(String[] args) throws IOException
    {
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream("./terrens/" + (args.length > 0 ? args[0] : "terrain257x257_300_600")+".ter"));

        w = dataInputStream.readInt();
        h = dataInputStream.readInt();
        player = new Point2D.Double(dataInputStream.readInt(),dataInputStream.readInt());
        target = new Point2D.Double(dataInputStream.readInt(),dataInputStream.readInt());

        map = new int[w][h];
        int minH = Integer.MAX_VALUE;
        int maxH = Integer.MIN_VALUE;

        for (int j =0; j <h; j++)
            for (int i =0; i <w; i++)
            {
                map[i][j] = dataInputStream.readInt();

                if (i ==0 && j == 0)
                    minH = maxH = map[i][j];

                minH = Math.min(minH,map[i][j]);
                maxH = Math.max(maxH,map[i][j]);
            }

        mapImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] srcPixels = ((DataBufferInt) mapImage.getRaster().getDataBuffer()).getData();

        for (int j =0; j <h; j++)
            for (int i =0; i <w; i++)
            {
                int color = (int)(((map[i][j]-minH) / (double)(maxH-minH))*0xFF);
                if (minH == maxH)
                    color = 128;
                srcPixels[j * w + i] = new Color(color, color, color, 0xFF).getRGB();
            }
    }
}

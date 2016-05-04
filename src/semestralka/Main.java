package semestralka;

import javafx.geometry.Point3D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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
 *
 */


public class Main
{
    static final boolean DEBUG = false; // T-T zlatej #define DEBUG nebo aspon globalni konstanta
    static final int BLAST_RADIUS = 3;
    static final int MAX_WIND = 5;
    private static final int ATTEMPTS_PER_WIND = 5;
    private static final double PIXELS_PER_METER = 10.0;
    private static int[][] map;
    private static int h;
    private static int w;
    private static int minH;
    private static int maxH;
    static Point2D.Double player;
    static Point2D.Double target;
    static Point2D.Double blast;
    static Point2D.Double wind;
    static BufferedImage mapImage;
    static ArrayList<Point3D> trajectoryPoints = new ArrayList<>();
    private static ArrayList<Point3D> elevationPoints = new ArrayList<>();

    private static JFrame mainFrame;
    private static JFrame elevationFrame;
    private static JFrame terrainFrame;

    private static Scanner scanner;

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
        mainFrame.dispose();
        if (elevationFrame != null)
            elevationFrame.dispose();
        if (terrainFrame != null)
            terrainFrame.dispose();
    }


    private static void play()
    {
        System.out.println("Player-Target air distance: " + player.distance(target)*PIXELS_PER_METER);
        System.out.println("Player heigh: " + map[(int)player.x][(int)player.y]);
        System.out.println("Target heigh: " + map[(int)target.x][(int)target.y]);

        int attempts = 0;
        while(true)
        {
            System.out.print("Choose action (S - shoot, V - visualization): ");
            String action = scanner.next().toUpperCase();
            boolean isVisualisation = action.equals("V");

            if (!isVisualisation)
            {
                if (!action.equals("S"))
                {
                    System.out.println("Wrong action!");
                    continue;
                }
                if (attempts++ % ATTEMPTS_PER_WIND == 0)
                {
                    generateWind();
                    repaint();
                }
            }

            double angle = 0.0;
            if (!isVisualisation)
            {
                System.out.print("Angle: ");
                angle = scanner.nextDouble();
            }
            System.out.print("Elevation: ");
            double elevation = scanner.nextDouble();
            if (elevation < 0.0 || elevation > 90.0)
            {
                System.out.println("Wrong elevation! Elevation must be in 0-90 interval");
                attempts--;
                continue;
            }

            System.out.print("Rocket speed: ");
            double startSpeed = scanner.nextDouble();
            if (startSpeed < 0.0)
            {
                System.out.println("Wrong rocket speed! Rocket speed cant be negative.");
                attempts--;
                continue;
            }
            launchRocket(angle, elevation, startSpeed,isVisualisation);

            if (isVisualisation)
            {
                generateElevationData(startSpeed,elevation);
                updateVisualisation();
                continue;
            }

            if (blast == null)
            {
                System.out.println("Rocket out of playground!");
                System.out.println(ActuallyUsefulLine.missStrings[new Random().nextInt(ActuallyUsefulLine.MAX_MISS_STRINGS)]);
                repaint();
                continue;
            }

            if (target.distance(blast) <= BLAST_RADIUS)
            {
                System.out.println("You won! " + (attempts > 1 ? "If you really consider this win when it took you so many (" + attempts + ") attempts!" : ""));
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

    private static void launchRocket(double angle, double elevation, double startSpeed, boolean forVisualization)
    {
        trajectoryPoints.clear();
        Point3D rockPos = new Point3D(player.x, player.y, (double)map[(int)player.x][(int)player.y]);
        double deltaT = 0.01;
        Point3D rockSpd;
        double g = 10.0;
        double b = 0.05;
        Point3D windSpeed = forVisualization ? new Point3D(0,0,0) : new Point3D(wind.x, wind.y, 0);

        if (forVisualization)
        {
            ActuallyUsefulLine angleLine = new ActuallyUsefulLine(new Point2D.Double(player.x,player.y),new Point2D.Double(target.x,target.y));
            angle = angleLine.angle();
        }
        {
            ActuallyUsefulLine l1 = new ActuallyUsefulLine();
            l1.setAngle(elevation).setLength(startSpeed);
            ActuallyUsefulLine l2 = new ActuallyUsefulLine();
            l2.setAngle(0).setLength(l1.p2.x).setAngle(angle);
            rockSpd = new Point3D(l2.p2.x, l2.p2.y, -l1.p2.y);
        }

        if (DEBUG)
            System.out.println(rockPos.toString() + " " + rockSpd.toString());

        while(true)
        {
            //first we use old speed
            // rocket speed is in mps but position is in pixels so we need to divide speed by ppm ratio
            Point3D newRockPos = rockPos.add(rockSpd.multiply(deltaT/PIXELS_PER_METER));

            //save new position
            trajectoryPoints.add(newRockPos);

            //recaluculate speed
            // y = vt + (0,0,-1)*g*deltaT
            Point3D newRockSpd = rockSpd.add(new Point3D(0,0,-1).multiply(g*deltaT));
            // y = vw - vt
            Point3D temp = windSpeed.subtract(rockSpd);
            // (x)*b*deltaT
            temp = temp.multiply(deltaT*b);
            // newSpeed = y + x
            newRockSpd = newRockSpd.add(temp);

            rockPos = newRockPos;
            rockSpd = newRockSpd;

            if (DEBUG)
                System.out.println(rockPos.toString() + " " + map[Math.max(0,Math.min((int)rockPos.getX(),w-1))][Math.max(0,Math.min((int)rockPos.getY(),h-1))]+ " " + rockSpd.toString());

            if (rockPos.getX() < 0 || rockPos.getY() < 0 || rockPos.getX() >= w || rockPos.getY() >= h)
            {
                if (!forVisualization)
                    blast = null;
                break;
            }

            if (rockPos.getZ() <= map[(int)rockPos.getX()][(int)rockPos.getY()])
            {
                if (!forVisualization)
                    blast = new Point2D.Double(rockPos.getX(),rockPos.getY());
                break;
            }
        }
    }

    private static void generateWind()
    {
        Random rand = new Random();

        ActuallyUsefulLine line = new ActuallyUsefulLine();
        line.setAngle(360.0*rand.nextDouble());
        line.setLength(rand.nextDouble()*MAX_WIND);

        wind = new Point2D.Double(line.p2.x,line.p2.y);
    }

    private static void repaint()
    {
        mainFrame.toFront();
        mainFrame.repaint();
    }

    private static void createWindow() throws IOException
    {
        mainFrame = new JFrame("The Game");
        mainFrame.setLayout(new GridBagLayout());
        mainFrame.add(new MapPanel(w, h));
        mainFrame.setSize(w + 50, h + 50);
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setAlwaysOnTop(true);
        mainFrame.setVisible(true);
    }

    private static void generateElevationData(double rocketSpeed, double elevation)
    {
        elevationPoints.clear();
        Point3D rockPos = new Point3D(0,0,0);
        double deltaT = 0.01;
        Point3D rockSpd;
        double g = 10.0;
        double b = 0.05;
        Point3D windSpeed = new Point3D(0,0,0);

        {
            ActuallyUsefulLine angleLine = new ActuallyUsefulLine(new Point2D.Double(0,0),new Point2D.Double(w,h));
            ActuallyUsefulLine l1 = new ActuallyUsefulLine();
            l1.setAngle(elevation).setLength(rocketSpeed);
            ActuallyUsefulLine l2 = new ActuallyUsefulLine();
            l2.setAngle(0).setLength(l1.p2.x).setAngle(angleLine.angle());
            rockSpd = new Point3D(l2.p2.x, l2.p2.y, -l1.p2.y);
        }

        while(true)
        {
            //first we use old speed
            // rocket speed is in mps but position is in pixels so we need to divide speed by ppm ratio
            Point3D newRockPos = rockPos.add(rockSpd.multiply(deltaT/PIXELS_PER_METER));

            //save new position
            elevationPoints.add(newRockPos);

            //recaluculate speed
            // y = vt + (0,0,-1)*g*deltaT
            Point3D newRockSpd = rockSpd.add(new Point3D(0,0,-1).multiply(g*deltaT));
            // y = vw - vt
            Point3D temp = windSpeed.subtract(rockSpd);
            // (x)*b*deltaT
            temp = temp.multiply(deltaT*b);
            // newSpeed = y + x
            newRockSpd = newRockSpd.add(temp);

            rockPos = newRockPos;
            rockSpd = newRockSpd;

            if (rockPos.getX() < 0 || rockPos.getY() < 0 || rockPos.getX() >= w || rockPos.getY() >= h || rockPos.getZ() < 0)
                break;
        }
    }
    private static void updateVisualisation()
    {
        updateElevationGraph();
        updateTerrainCutGraph();
    }

    private static void updateElevationGraph()
    {
        if (elevationPoints.size() < 2)
            return;

        if (elevationFrame != null)
            elevationFrame.dispose();

        XYSeries elevationData = new XYSeries("Elevation");
        elevationData.add(0,0);
        for (Point3D point : elevationPoints)
        {
            ActuallyUsefulLine line = new ActuallyUsefulLine(new Point2D.Double(0,0),new Point2D.Double(point.getX(),point.getY()));
            elevationData.add(line.length()*PIXELS_PER_METER,point.getZ());
        }
        XYSeriesCollection ds = new XYSeriesCollection();
        ds.addSeries(elevationData);
        JFreeChart chart = ChartFactory.createXYLineChart("Elevation chart",
                "Distance [m]", "Heigh [m]",
                ds, PlotOrientation.VERTICAL,
                true, true, false); // legends, tooltips, urls
        NumberAxis Xaxis = (NumberAxis)((XYPlot)chart.getPlot()).getDomainAxis();
        double maxRange = Math.sqrt(w*w + h*h);
        Xaxis.setRange(0,maxRange*PIXELS_PER_METER);

        elevationFrame = new JFrame("Elevation graph");
        elevationFrame.add(new ChartPanel(chart));
        elevationFrame.pack();
        elevationFrame.setVisible(true);
        elevationFrame.setAlwaysOnTop(true);
        elevationFrame.setAlwaysOnTop(false);

    }

    private static void updateTerrainCutGraph()
    {
        if (trajectoryPoints.size() < 2)
            return;

        if (terrainFrame != null)
            terrainFrame.dispose();

        int maxHeigh = Integer.MIN_VALUE;

        XYSeries trajectoryData = new XYSeries("Trajectory");
        trajectoryData.add(0,map[(int)player.getX()][(int)player.getY()]);
        for (Point3D point : trajectoryPoints)
        {
            ActuallyUsefulLine line = new ActuallyUsefulLine(new Point2D.Double(player.getX(),player.getY()),new Point2D.Double(point.getX(),point.getY()));
            trajectoryData.add(line.length()*PIXELS_PER_METER,point.getZ());
            maxHeigh = Math.max(maxHeigh,(int)point.getZ());
        }

        XYSeries terrainData = new XYSeries("Terrain cut");
        int playerTargetDistance = (int)player.distance(target);
        terrainData.add(0,map[(int)player.getX()][(int)player.getY()]);

        for(int i = 1; i < playerTargetDistance; i++)
        {
            ActuallyUsefulLine line = new ActuallyUsefulLine(new Point2D.Double(player.getX(),player.getY()),new Point2D.Double(target.getX(),target.getY()));
            line.setLength(i);
            terrainData.add(i*PIXELS_PER_METER,map[(int)line.p2.getX()][(int)line.p2.getY()]);
            maxHeigh = Math.max(maxHeigh,map[(int)line.p2.getX()][(int)line.p2.getY()]);
        }
        terrainData.add(0,map[(int)target.getX()][(int)target.getY()]);

        JFreeChart chart;
        XYPlot plot = new XYPlot();

        XYSeriesCollection collection1 = new XYSeriesCollection();
        collection1.addSeries(trajectoryData);
        XYItemRenderer renderer1 = new XYLineAndShapeRenderer(true, false);
        ValueAxis domain = new NumberAxis("Distance [m]");
        domain.setRange(0,playerTargetDistance*PIXELS_PER_METER);
        ValueAxis range = new NumberAxis("Heigh [m]");
        range.setRange(0,maxHeigh + 50);

        plot.setDataset(0, collection1);
        plot.setRenderer(0, renderer1);
        plot.setDomainAxis(0, domain);
        plot.setRangeAxis(0, range);

        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        XYSeriesCollection collection2 = new XYSeriesCollection();
        collection2.addSeries(terrainData);
        XYItemRenderer renderer2 = new XYAreaRenderer(XYAreaRenderer.AREA);
        renderer2.setSeriesPaint(0,new Color(102,51,0));

        plot.setDataset(1, collection2);
        plot.setRenderer(1, renderer2);
        plot.setDomainAxis(1, domain);
        plot.setRangeAxis(1, range);

        plot.mapDatasetToDomainAxis(1, 1);
        plot.mapDatasetToRangeAxis(1, 1);

        chart = new JFreeChart("Terrain profile", JFreeChart.DEFAULT_TITLE_FONT, plot, true);


        terrainFrame = new JFrame("Elevation graph");
        terrainFrame.add(new ChartPanel(chart));
        terrainFrame.pack();
        terrainFrame.setVisible(true);
        terrainFrame.setAlwaysOnTop(true);
        terrainFrame.setAlwaysOnTop(false);


    }

    private static void loadMap(String[] args) throws IOException
    {
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream("./terrens/" + (args.length > 0 ? args[0] : "terrain257x257_300_600")+".ter"));

        w = dataInputStream.readInt();
        h = dataInputStream.readInt();
        player = new Point2D.Double(dataInputStream.readInt(),dataInputStream.readInt());
        target = new Point2D.Double(dataInputStream.readInt(),dataInputStream.readInt());

        map = new int[w][h];
        minH = Integer.MAX_VALUE;
        maxH = Integer.MIN_VALUE;

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

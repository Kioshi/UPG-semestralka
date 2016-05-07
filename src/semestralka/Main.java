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


class Main
{
    static final boolean DEBUG = false; // T-T zlatej #define DEBUG nebo aspon globalni konstanta
    static final int BLAST_RADIUS = 3;
    static final int MAX_WIND = 5;
    private static final int ATTEMPTS_PER_WIND = 5;
    private static final double METERS_PER_PIXEL = 10.0;
    private static final double ROCKET_SPEED_STEP = 10.0;
    private static double[][] map;
    private static int h;
    private static int w;
    static Point2D.Double player;
    static Point2D.Double target;
    static Point2D.Double blast;
    static Point2D.Double wind;
    static BufferedImage mapImage;
    final static ArrayList<Point3D> trajectoryPoints = new ArrayList<>();

    private static JFrame mainFrame;
    private static JFrame elevationFrame;
    private static JFrame maxTerrainFrame;
    private static JFrame terrainFrame;
    private static ChartPanel eleavtionCP;
    private static ChartPanel terrainCP;
    private static ChartPanel maxTerrainCP;

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
        if (maxTerrainFrame != null)
            maxTerrainFrame.dispose();
        if (terrainFrame != null)
            terrainFrame.dispose();
    }


    private static void play()
    {
        System.out.println("Player-Target air distance: " + player.distance(target)* METERS_PER_PIXEL);
        System.out.println("Player heigh: " + map[(int)player.x][(int)player.y]);
        System.out.println("Target heigh: " + map[(int)target.x][(int)target.y]);

        int attempts = 0;
        while(true)
        {
            System.out.print("Choose action (S - shoot, V - visualization): ");
            String action = scanner.next().toUpperCase();
            boolean isVisualisation = action.equals("V");
            trajectoryPoints.clear();

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

            System.out.print("Angle: ");
            double angle = scanner.nextDouble();
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
                updateVisualisation(angle, elevation);
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
        Point3D rockPos = new Point3D(player.x, player.y, map[(int)player.x][(int)player.y]);
        double deltaT = 0.01;
        Point3D rockSpd;
        double g = 10.0;
        double b = 0.05;
        Point3D windSpeed = forVisualization ? new Point3D(0,0,0) : new Point3D(wind.x, wind.y, 0);

        {
            ActuallyUsefulLine l1 = new ActuallyUsefulLine();
            l1.setAngle(elevation).setLength(startSpeed/METERS_PER_PIXEL);
            ActuallyUsefulLine l2 = new ActuallyUsefulLine();
            l2.setAngle(0).setLength(l1.p2.x).setAngle(angle);
            rockSpd = new Point3D(l2.p2.x, l2.p2.y, -l1.p2.y);
        }

        if (DEBUG)
            System.out.println(rockPos.toString() + " " + rockSpd.toString());

        while(true)
        {
            //first we use old speed
            Point3D newRockPos = rockPos.add(rockSpd.multiply(deltaT));

            //save new position
            trajectoryPoints.add(newRockPos);

            //recaluculate speed
            // y = vt + (0,0,-1)*g*deltaT
            Point3D newRockSpd = rockSpd.add(new Point3D(0,0,-1/METERS_PER_PIXEL).multiply(g*deltaT));
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

    private static double calculateElevationDistance(double rocketSpeed, double elevation)
    {
        Point3D rockPos = new Point3D(0,0,0);
        double deltaT = 0.01;
        Point3D rockSpd;
        double g = 10.0;
        double b = 0.05;
        Point3D windSpeed = new Point3D(0,0,0);

        {
            ActuallyUsefulLine l1 = new ActuallyUsefulLine();
            l1.setAngle(elevation).setLength(rocketSpeed/METERS_PER_PIXEL);
            ActuallyUsefulLine l2 = new ActuallyUsefulLine();
            l2.setAngle(0).setLength(l1.p2.x);
            rockSpd = new Point3D(l2.p2.x, l2.p2.y, -l1.p2.y);
        }

        while(true)
        {
            //first we use old speed
            // rocket speed is in mps but position is in pixels so we need to divide speed by ppm ratio
            Point3D newRockPos = rockPos.add(rockSpd.multiply(deltaT));

            //recaluculate speed
            // y = vt + (0,0,-1)*g*deltaT
            Point3D newRockSpd = rockSpd.add(new Point3D(0,0,-1).multiply(g*deltaT/METERS_PER_PIXEL));
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
        
        return new Point3D(0,0,0).distance(rockPos)* METERS_PER_PIXEL;
    }
    
    private static void updateVisualisation(double angle, double elevation)
    {
        updateElevationGraph(elevation);
        updateTerrainCutGraphs(angle);
    }

    private static void updateElevationGraph(double elevation)
    {
        if (elevationFrame != null)
            elevationFrame.dispose();

        XYSeries elevationData = new XYSeries("Elevation");
        for (int i = 1; i <= 20; i++)
            elevationData.add(ROCKET_SPEED_STEP*i,calculateElevationDistance(ROCKET_SPEED_STEP*i, elevation));

        XYSeriesCollection ds = new XYSeriesCollection();
        ds.addSeries(elevationData);
        JFreeChart chart = ChartFactory.createXYLineChart("Elevation chart",
                "Distance [m]", "Heigh [m]",
                ds, PlotOrientation.VERTICAL,
                true, true, false); // legends, tooltips, urls

        if (elevationFrame == null)
            elevationFrame = new JFrame("Elevation graph");

        if (eleavtionCP != null)
            elevationFrame.remove(eleavtionCP);
        eleavtionCP = new ChartPanel(chart);
        elevationFrame.add(eleavtionCP);
        elevationFrame.pack();
        elevationFrame.setVisible(true);
        elevationFrame.setAlwaysOnTop(true);
        elevationFrame.setAlwaysOnTop(false);

    }

    private static void updateTerrainCutGraphs(double angle)
    {
        if (trajectoryPoints.size() < 2)
            return;

        if (maxTerrainFrame != null)
            maxTerrainFrame.dispose();

        if (terrainFrame != null)
            terrainFrame.dispose();

        double maxHeigh = Double.MIN_VALUE;
        double trajectoryHeigh = Double.MIN_VALUE;

        XYSeries trajectoryData = new XYSeries("Trajectory");
        trajectoryData.add(0,map[(int)player.getX()][(int)player.getY()]);
        double trajectoryDistance = 1.0;
        for (Point3D point : trajectoryPoints)
        {
            ActuallyUsefulLine line = new ActuallyUsefulLine(new Point2D.Double(player.getX(),player.getY()),new Point2D.Double(point.getX(),point.getY()));
            trajectoryData.add(line.length()* METERS_PER_PIXEL,point.getZ()*METERS_PER_PIXEL);
            maxHeigh = Math.max(maxHeigh,point.getZ());
            trajectoryDistance = line.length();
        }

        XYSeries terrainData = new XYSeries("Max terrain cut");
        XYSeries terrainData2 = new XYSeries("Terrain cut");
        terrainData.add(0,map[(int)player.getX()][(int)player.getY()]);
        terrainData2.add(0,trajectoryPoints.get(0).getZ()*METERS_PER_PIXEL);
        int distance = 1;
        for(int i = 1; ; i++)
        {
            ActuallyUsefulLine line = new ActuallyUsefulLine();
            line.setP1(new Point2D.Double(player.getX(),player.getY()));
            line.setLength(i);
            line.setAngle(angle);
            if (line.p2.x >= w || line.p2.x < 0 || line.p2.y >= h || line.p2.y < 0)
                break;
            distance = i;
            terrainData.add(i* METERS_PER_PIXEL,map[(int)line.p2.getX()][(int)line.p2.getY()]* METERS_PER_PIXEL);
            maxHeigh = Math.max(maxHeigh,map[(int)line.p2.getX()][(int)line.p2.getY()]);
            if (i <= trajectoryDistance)
            {
                terrainData2.add(i* METERS_PER_PIXEL,map[(int)line.p2.getX()][(int)line.p2.getY()]* METERS_PER_PIXEL);
            }
        }
        terrainData2.add(trajectoryData.getMaxX(),trajectoryPoints.get(trajectoryPoints.size()-1).getZ()*METERS_PER_PIXEL);

        JFreeChart chart = makeTerrainChart(trajectoryData,distance,maxHeigh,terrainData,"Max terrain profile");
        JFreeChart chart2 = makeTerrainChart(trajectoryData,trajectoryDistance,-1.0,terrainData2, "Terrain profile");


        if (maxTerrainFrame == null)
            maxTerrainFrame = new JFrame("Max terrain cut");

        if (maxTerrainCP != null)
            maxTerrainFrame.remove(maxTerrainCP);
        maxTerrainCP = new ChartPanel(chart);
        maxTerrainFrame.add(maxTerrainCP);
        maxTerrainFrame.pack();
        maxTerrainFrame.setVisible(true);
        maxTerrainFrame.setAlwaysOnTop(true);
        maxTerrainFrame.setAlwaysOnTop(false);

        if (terrainFrame == null)
            terrainFrame = new JFrame("Terrain cut");
        if (terrainCP != null)
            terrainFrame.remove(terrainCP);
        terrainCP = new ChartPanel(chart2);
        terrainFrame.add(terrainCP);
        terrainFrame.pack();
        terrainFrame.setVisible(true);
        terrainFrame.setAlwaysOnTop(true);
        terrainFrame.setAlwaysOnTop(false);


    }
    private static JFreeChart makeTerrainChart(XYSeries trajectoryData, double distance, double maxHeigh, XYSeries terrainData, String name)
    {
        XYPlot plot = new XYPlot();

        XYSeriesCollection collection1 = new XYSeriesCollection();
        collection1.addSeries(trajectoryData);
        XYItemRenderer renderer1 = new XYLineAndShapeRenderer(true, false);
        ValueAxis domain = new NumberAxis("Distance [m]");
        domain.setRange(0,distance* METERS_PER_PIXEL);
        ValueAxis range = new NumberAxis("Heigh [m]");
        if (maxHeigh > 0)
            range.setRange(0,maxHeigh* METERS_PER_PIXEL + 50);

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

        return new JFreeChart(name, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    }

    private static void loadMap(String[] args) throws IOException
    {
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream("./terrens/" + (args.length > 0 ? args[0] : "terrain512x512_300_600")+".ter"));

        w = dataInputStream.readInt();
        h = dataInputStream.readInt();
        player = new Point2D.Double(dataInputStream.readInt(),dataInputStream.readInt());
        target = new Point2D.Double(dataInputStream.readInt(),dataInputStream.readInt());

        map = new double[w][h];
        int minH = Integer.MAX_VALUE;
        int maxH = Integer.MIN_VALUE;

        for (int j =0; j <h; j++)
            for (int i =0; i <w; i++)
            {
                int heigh = dataInputStream.readInt();
                map[i][j] = heigh / METERS_PER_PIXEL;

                minH = Math.min(minH,heigh);
                maxH = Math.max(maxH,heigh);
            }

        mapImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] srcPixels = ((DataBufferInt) mapImage.getRaster().getDataBuffer()).getData();

        for (int j =0; j <h; j++)
            for (int i =0; i <w; i++)
            {
                int color = (int)((((int)(map[i][j]*METERS_PER_PIXEL)-minH) / (double)(maxH-minH))*0xFF);
                if (minH == maxH)
                    color = 128;
                srcPixels[j * w + i] = new Color(color, color, color, 0xFF).getRGB();
            }
    }

}

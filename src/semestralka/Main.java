package semestralka;

import javafx.geometry.Point3D;
import javafx.util.Pair;
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
import java.util.*;
import java.util.List;

/**
 * Created by Štěpán Martínek on 13.03.2016.
 *
 */


class Main
{
    static final boolean DEBUG = false;
    private static final double BLAST_RADIUS = 4;
    private static final double BLAST_RADIUS_REDUCED = 2;
    static final int MAX_WIND = 5;
    private static final int ATTEMPTS_PER_WIND = 5;
    private static final double METERS_PER_PIXEL = 10.0;
    private static final int ELEVATION_VISUALISATION_STEP = 20;
    private static final int ELEVATION_VISUALISATION_STEPS = 30;
    private static double[][] map;
    private static double[][] originalMap;
    static double blastRadius = BLAST_RADIUS;
    static double trajectoryMinZ = 0.0;
    static double trajectoryMaxZ = 0.0;
    private static int h;
    private static int w;
    private static double minH;
    private static double maxH;
    private static boolean flat = false;
    static int rocketStep = -1;
    static int playersNr = 0;
    static Player[] players;
    static Player currentlyPlaying;
    static Point3D target;
    static Point2D.Double wind;
    static BufferedImage mapImage;
    static ArrayList<Rocket> rocketTypes;

    private static JFrame mainFrame;
    private static JFrame elevationFrame;
    private static JFrame maxTerrainFrame;
    private static JFrame terrainFrame;
    private static ChartPanel eleavtionCP;
    private static ChartPanel terrainCP;
    private static ChartPanel maxTerrainCP;

    private static Scanner scanner;

    public static void main(String[] args)
    {
        try
        {
            scanner = new Scanner(System.in);
            fillRockets();

            do
            {
                System.out.print("Number of players (1-9): ");
                try
                {
                    playersNr = scanner.nextInt();
                }
                catch (Exception e)
                {
                    scanner.next();
                }

            }
            while (playersNr < 1 || playersNr > 9);
            players = new Player[playersNr];
            for (int i = 0; i < playersNr; i++)
                players[i] = new Player();

            System.out.println("Loading...");
            try
            {
                loadMap(args);
            }
            catch (IOException e)
            {
                throw new IOException("Could not load map");
            }
            createWindow();

            map = copyMap(originalMap);
            generatePlayersPosition();
            do
            {
                updateMapImage();
                play();
                resetPlayers();

                System.out.println("Press Y to play again.");
            }
            while (scanner.next().toUpperCase().equals("Y"));
            mainFrame.dispose();
            if (elevationFrame != null)
                elevationFrame.dispose();
            if (maxTerrainFrame != null)
                maxTerrainFrame.dispose();
            if (terrainFrame != null)
                terrainFrame.dispose();
        }
        catch (IOException e)
        {
            System.out.println("File error: " + e.getMessage());
        }
        catch (Exception e)
        {
            System.out.println("Something went wrong, but i caught it. Please be more careful next time you play. Somebody could get hurt.");
            if (DEBUG)
                System.out.println(e.getMessage());
        }
    }

    private static void fillRockets()
    {
        rocketTypes = new ArrayList<>();
        rocketTypes.add(new Rocket("Classic"));
        rocketTypes.add(new Rocket("North Korea is the best Korea",10.0,1.0,3.0,5.0,0.01,10));
        rocketTypes.add(new Rocket("Made in China",7.0,1.5,2.5,10.0,0.005,15));
        rocketTypes.add(new Rocket("USA!USA!USA!USA!",15.0,2.5,3.5,1.0,0.0005,200));
        rocketTypes.add(new Rocket("Nyan cat",0.01,0.01,0.01,0.01,0.00005,1000));
        rocketTypes.add(new Rocket("Potato!",5.0,2.0,2.0,20.0,0.002,60));
        rocketTypes.add(new Rocket("Bill Bullet", 20.0, 20.0, 20.0, 0.0001, 0.000001,5));
    }

    private static void resetPlayers()
    {
        currentlyPlaying = null;
        for (Player p : players)
        {
            p.blast = null;
            p.trajectoryPoints.clear();
            p.stillPlaying = true;
            p.attempts = 0;
        }
    }

    private static boolean generatePlayersPosition()
    {
        if (playersNr == 1)
            return true;

        if (w < 20 || h < 20)
            return false;

        int playerRectW = (w - 10)/3;
        int playerRectH = (h - 10)/3;

        Stack<Point> positions = new Stack<>();
        for (int i = 0; i < 3; i++)
            for (int j=0; j < 3; j++)
                positions.add(new Point(5 + playerRectW*i, 5 + playerRectH*j));
        Collections.shuffle(positions);

        Stack<Pair<Color,String>> colors = new Stack<>();

        colors.add(new Pair<>(Color.RED,"Red"));
        colors.add(new Pair<>(Color.GREEN,"Green"));
        colors.add(new Pair<>(Color.BLUE,"Blue"));
        colors.add(new Pair<>(Color.orange,"Orange"));
        colors.add(new Pair<>(Color.PINK,"Pink"));
        colors.add(new Pair<>(Color.MAGENTA,"Magenta"));
        colors.add(new Pair<>(Color.CYAN,"Cyan"));
        colors.add(new Pair<>(Color.YELLOW,"Yellow"));
        colors.add(new Pair<>(new Color(0,128,0),"Dark green"));
        Collections.shuffle(colors);


        Random random = new Random();
        for (int i = 0; i < players.length; i++)
        {
            Player p = players[i];
            Point position = positions.pop();
            int x = (int)position.getX() + random.nextInt(playerRectW);
            int y = (int)position.getY() + random.nextInt(playerRectH);
            p.player = new Point3D(x,y,getMapHeigh(x,y));
            Pair<Color,String> pair = colors.pop();
            System.out.println("Player "+(i+1)+" have "+pair.getValue()+" color");
            p.setColor(pair.getKey());
        }
        return true;
    }

    public static double[][] copyMap(double[][] o)
    {
        double[][] m = new double[o.length][o[0].length];
        for (int i = 0; i < o.length; i++)
            for (int j = 0; j < o[i].length; j++)
                m[i][j] = o[i][j];
        return m;
    }


    private static void play() //throws InterruptedException
    {
        printRockets();
        if (playersNr == 1)
        {
            System.out.println("Player-Target air distance: " + players[0].player.distance(target) * METERS_PER_PIXEL);
            System.out.println("Player heigh: " + getMapHeigh(players[0].player) * METERS_PER_PIXEL);
            System.out.println("Target heigh: " + getMapHeigh(target) * METERS_PER_PIXEL);
        }
        int totalAttempts = 0;
        int alivePlayers = 2;
        while(alivePlayers > 1)
        {
            alivePlayers = playersNr;
            if (playersNr == 1)
            {
                alivePlayers++;
            }
            for (int index = 0; index < players.length; index++)
            {
                Player p = players[index];
                currentlyPlaying = p;
                p.trajectoryPoints.clear();

                if (!p.stillPlaying)
                {
                    alivePlayers--;
                    continue;
                }
                if (alivePlayers <2)
                    break;

                if (playersNr > 1)
                    System.out.println("Player " + (index + 1) + " is playing!");
                else
                    System.out.println("Money: "+ players[0].money + "$");

                while (true)
                {
                    System.out.print("Choose action (S - shoot, V - visualization" + (playersNr == 1 ? ", R - prints list of rocket): " : "): "));
                    String action = scanner.next().toUpperCase();
                    boolean isVisualisation = action.equals("V");

                    if (!isVisualisation)
                    {
                        if (playersNr == 1 && action.equals("R"))
                        {
                            printRockets();
                            continue;
                        }
                        if (!action.equals("S"))
                        {
                            System.out.println("Wrong action!");
                            continue;
                        }
                        if (totalAttempts++ % ATTEMPTS_PER_WIND * playersNr == 0)
                        {
                            generateWind();
                            repaint(true);
                        }
                    }

                    Rocket rocket;
                    if (playersNr > 1 || isVisualisation)
                        rocket = rocketTypes.get(0);
                    else
                    {
                        while(true)
                        {
                            System.out.print("Rocket: ");
                            try
                            {
                                int r = scanner.nextInt();
                                if (r < 0 || r >= rocketTypes.size())
                                    System.out.println("Wrong rocket index! Index should be from 0-"+(rocketTypes.size()-1));
                                else
                                {
                                    rocket = rocketTypes.get(r);
                                    break;
                                }
                            } catch (Exception e)
                            {
                                scanner.next();
                            }
                        }
                    }

                    double angle;
                    while(true)
                    {
                        System.out.print("Angle: ");
                        try
                        {
                            angle = scanner.nextDouble();
                            break;
                        } catch (Exception e)
                        {
                            scanner.next();
                        }
                    }
                    if (!isVisualisation)
                        angle = getDeviatedFlat(angle, rocket.MaxAngleDeviation);
                    double el;
                    while(true)
                    {
                        System.out.print("Elevation: ");
                        try
                        {
                            el = scanner.nextDouble();

                            if (el < 0.0 || el > 90.0)
                                System.out.println("Wrong elevation! Elevation must be in (0-90) interval");
                            else
                                break;
                        } catch (Exception e)
                        {
                            scanner.next();
                        }
                    }

                    double elevation = el;
                    if (!isVisualisation)
                        elevation = getDeviatedFlat(el, rocket.MaxElevationDeviation);

                    if (elevation < 0.0 || elevation > 90.0)
                        elevation = el;

                    double startSpeed;
                    while(true)
                    {
                        System.out.print("Rocket speed (Max 1000 m/s): ");
                        try
                        {
                            startSpeed = scanner.nextDouble();
                            if (startSpeed <= 1000.0 && startSpeed > 0.0)
                                break;
                            else
                                System.out.println("Wrong speed! Speed must be in (0-1000> interval");

                        } catch (Exception e)
                        {
                            scanner.next();
                        }
                    }
                    if (!isVisualisation)
                        startSpeed = getDeviatedPct(startSpeed, rocket.MaxSpeedDeviationPct);

                    p.payForRocket(rocket.cost);
                    launchRocket(p, angle, elevation, startSpeed, isVisualisation, rocket);

                    if (isVisualisation)
                    {
                        updateVisualisation(p, angle, elevation);
                        continue;
                    }

                    calculateMinMaxTrajectoryZ(p);

                    rocketStep = -1;
                    for (int i = 0; i <= p.trajectoryPoints.size(); i++)
                    {
                        rocketStep++;
                        repaint(false);
                        try
                        {
                            Thread.sleep(1);
                        }
                        catch (Exception e)
                        {
                            //do nothing
                        }
                    }
                    rocketStep = -1;

                    if (p.blast == null)
                    {
                        System.out.println("Rocket out of playground!");
                        System.out.println(missStrings.get(new Random().nextInt(missStrings.size())));
                        repaint(false);
                        break;
                    }

                    if (p.blast.getZ() > getMapHeigh(p.blast))
                    {
                        System.out.println("Rocked exploded mid-air. Next time don't buy rockets in North Korea!");
                    }

                    Random random = new Random();
                    if (random.nextDouble() * 100.0 < rocket.BlastRectuionChance)
                    {
                        System.out.println("Rocket failed! Blast radius was lesser than expected. Who the hell bought this cheap sh*t?");
                        blastRadius = BLAST_RADIUS_REDUCED;
                    } else
                        blastRadius = BLAST_RADIUS;

                    kaBOOM(p);

                    if (playersNr == 1)
                    {
                        if (target.distance(p.blast) <= BLAST_RADIUS)
                        {
                            p.stillPlaying = false;
                            System.out.println("You won!" + (p.attempts > 1 ? "If you really consider this win when it took you so many (" + p.attempts + ") attempts!" : ""));
                            System.out.println("Score: "+ calculateScore(p));
                            break;
                        }
                        if (p.player.distance(p.blast) <= BLAST_RADIUS)
                        {
                            p.stillPlaying = false;
                            System.out.println("Game over!");
                            break;
                        }

                        if (p.stillPlaying)
                            System.out.println("Nope. Try again");
                        else
                        {
                            System.out.println("Game over! Next time use your money wisely.");
                            break;
                        }
                    }
                    else
                    {
                        boolean success = false;
                        for (int i = 0 ; i < players.length; i++)
                        {
                            if (i == index)
                                continue;
                            Player pl =players[i];
                            if (pl.player.distance(p.blast) <= BLAST_RADIUS)
                            {
                                success = true;
                                pl.stillPlaying = false;
                                System.out.println("You eliminated player "+(i+1)+"!" + (p.attempts > 1 ? " But it took you " + p.attempts + " attempts... I could do it in half of that." : ""));
                            }
                        }
                        if (p.player.distance(p.blast) <= BLAST_RADIUS)
                        {
                            System.out.println("Good f*cking job, you just eliminated your self!");
                            p.stillPlaying = false;
                            alivePlayers--;
                        }
                        else if (!success)
                            System.out.println("Nope. Try again next turn!");

                        p.trajectoryPoints.clear();
                    }
                    break;
                }
                repaint(false);
            }
        }
        repaint(false);

        if (playersNr > 1)
        {
            System.out.println("Game Over");
            for (int i = 0; i < players.length; i++)
            {
                Player player = players[i];
                if (player.stillPlaying)
                    System.out.println("Player " + (i + 1) + " won!");
            }
        }
    }

    private static double getMapHeigh(Point3D p)
    {
        int x = Math.max(0,Math.min(w-1,(int)p.getX()));
        int y = Math.max(0,Math.min(h-1,(int)p.getY()));

        return map[x][y];
    }
    private static double getMapHeigh(int X, int Y)
    {
        int x = Math.max(0,Math.min(w-1,X));
        int y = Math.max(0,Math.min(h-1,Y));

        return map[x][y];
    }

    private static double getMapHeigh(int X, int Y, double[][] m)
    {
        int x = Math.max(0,Math.min(w-1,X));
        int y = Math.max(0,Math.min(h-1,Y));
        return m[x][y];
    }

    private static int calculateScore(Player p)
    {
        int score = p.money;
        score -= p.attempts*3;

        return score;
    }

    private static void printRockets()
    {
        for(int i = 0; i< rocketTypes.size(); i++)
        {
            Rocket rocket = rocketTypes.get(i);
            System.out.println(i+": "+rocket.toString());
            if (playersNr > 1)
                break;
        }
    }

    private static void kaBOOM(Player p)
    {
        if (flat)
            return;

        double[][] old = copyMap(map);
        for (int i = 0; i < 360; i++)
        {
            ActuallyUsefulLine l1 = new ActuallyUsefulLine();
            l1.setP1(new Point2D.Double(p.blast.getX(),p.blast.getY())).setAngle(i);
            for (int j = 0; j >= -90; j--)
            {
                ActuallyUsefulLine l2 = new ActuallyUsefulLine();
                l2.setAngle(0).setLength(blastRadius).setAngle(j);
                l1.setLength(l2.p2.getX());
                int x = (int)l1.p2.getX();
                int y = (int)l1.p2.getY();
                if (x < 0 || y < 0 || x >= w || y >= h)
                    continue;
                double orig = getMapHeigh(x,y);
                double before = getMapHeigh(x,y, old);
                double newZ = Math.max(minH,Math.min(orig, p.blast.getZ() - l2.p2.getY()));
                if (orig > newZ && before == orig)
                    map[x][y] = newZ;
            }
        }

        updateMapImage();
    }


    private static void calculateMinMaxTrajectoryZ(Player p)
    {
        trajectoryMaxZ = Double.MIN_VALUE;
        trajectoryMinZ = Double.MAX_VALUE;
        for (Point3D point : p.trajectoryPoints)
        {
            trajectoryMaxZ = Math.max(trajectoryMaxZ,point.getZ());
            trajectoryMinZ = Math.min(trajectoryMinZ,point.getZ());
        }
    }

    private static double getDeviatedFlat(double base, double maxDeviation)
    {
        Random random = new Random();
        double deviation = random.nextDouble()*(maxDeviation*2.0) - maxDeviation;
        return base + deviation;
    }

    private static double getDeviatedPct(double base, double maxPct)
    {
        Random random = new Random();
        double deviation = random.nextDouble()*maxPct/100.0 * (random.nextBoolean() ? -1 : 1);
        return base * (1.0 +deviation);
    }

    private static void launchRocket(Player p, double angle, double elevation, double startSpeed, boolean forVisualization, Rocket rocket)
    {
        p.trajectoryPoints.clear();
        Point3D rockPos = p.player;
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
            p.trajectoryPoints.add(newRockPos);

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
                    p.blast = null;
                break;
            }


            Random random = new Random();
            if (!forVisualization && ((random.nextDouble()*100.0) < rocket.MidAirExplosionPerStepChancePct))
            {
                p.blast = rockPos;
                break;
            }

            if (rockPos.getZ() <= getMapHeigh(rockPos))
            {
                if (!forVisualization)
                    p.blast = rockPos;
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

    private static void repaint(boolean toTheTOp)
    {
        if (toTheTOp)
        {
            mainFrame.toFront();
            if (!DEBUG)
            {
                mainFrame.setAlwaysOnTop(true);
                mainFrame.setAlwaysOnTop(false);
            }
        }
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
        if (DEBUG)
            mainFrame.setAlwaysOnTop(true);
        mainFrame.setVisible(true);
    }

    private static double calculateElevationDistance(double rocketSpeed, double elevation)
    {
        if (Math.abs(elevation - 90.0) < 1E-3 || elevation < 1E-3)
            return 0.0;

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

            if (rockPos.getZ() < 0)
                break;
        }

        return new Point3D(0,0,0).distance(rockPos)* METERS_PER_PIXEL;
    }

    private static void updateVisualisation(Player p, double angle, double elevation)
    {
        updateElevationGraph(elevation);
        updateTerrainCutGraphs(p,angle);
    }

    private static void updateElevationGraph(double elevation)
    {
        XYSeries elevationData = new XYSeries("Elevation");
        for (int i = 1; i <= ELEVATION_VISUALISATION_STEPS; i++)
            elevationData.add(i*ELEVATION_VISUALISATION_STEP,calculateElevationDistance(i*ELEVATION_VISUALISATION_STEP, elevation));

        XYSeriesCollection ds = new XYSeriesCollection();
        ds.addSeries(elevationData);
        JFreeChart chart = ChartFactory.createXYLineChart("Elevation chart",
                "Start Rocket Speed [m/s]", "Distance [m]",
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

    private static void updateTerrainCutGraphs(Player p, double angle)
    {
        double maxHeigh = Double.MIN_VALUE;

        XYSeries trajectoryData = new XYSeries("Trajectory");
        XYSeries trajectoryData2 = new XYSeries("Trajectory");
        trajectoryData.add(0,getMapHeigh(p.player)*METERS_PER_PIXEL);
        trajectoryData2.add(0,getMapHeigh(p.player)*METERS_PER_PIXEL);
        double trajectoryDistance = 1.0;
        double maxX = 1;
        double maxY = 1;
        for (Point3D point : p.trajectoryPoints)
        {
            ActuallyUsefulLine line = new ActuallyUsefulLine(new Point2D.Double(p.player.getX(),p.player.getY()),new Point2D.Double(point.getX(),point.getY()));
            trajectoryData.add(line.length()* METERS_PER_PIXEL,point.getZ()*METERS_PER_PIXEL);
            trajectoryData2.add(line.length()* METERS_PER_PIXEL,point.getZ()*METERS_PER_PIXEL);
            maxHeigh = Math.max(maxHeigh,point.getZ());
            trajectoryDistance = line.length();
            maxX = point.getX();
            maxY = point.getY();
        }

        XYSeries terrainData = new XYSeries("Max terrain cut");
        XYSeries terrainData2 = new XYSeries("Terrain cut");
        terrainData.add(0,getMapHeigh(p.player)*METERS_PER_PIXEL);
        terrainData2.add(0,p.trajectoryPoints.get(0).getZ()*METERS_PER_PIXEL);
        int distance = 1;
        for(int i = 1; ; i++)
        {
            ActuallyUsefulLine line = new ActuallyUsefulLine();
            line.setP1(new Point2D.Double(p.player.getX(),p.player.getY()));
            line.setLength(i);
            line.setAngle(angle);
            if (line.p2.x >= w || line.p2.x < 0 || line.p2.y >= h || line.p2.y < 0)
                break;
            distance = i;
            terrainData.add(i* METERS_PER_PIXEL,getMapHeigh(new Point3D(line.p2.getX(),line.p2.getY(),0))* METERS_PER_PIXEL);
            maxHeigh = Math.max(maxHeigh,getMapHeigh(new Point3D(line.p2.getX(),line.p2.getY(),0)));
            if (i <= trajectoryDistance)
            {
                terrainData2.add(i* METERS_PER_PIXEL,getMapHeigh(new Point3D(line.p2.getX(),line.p2.getY(),0))* METERS_PER_PIXEL);
            }
        }
        double t = getMapHeigh(new Point3D(maxX,maxY,0));
        terrainData2.add(trajectoryData2.getMaxX(),t*METERS_PER_PIXEL);

        JFreeChart chart = makeTerrainChart(trajectoryData,distance,maxHeigh,terrainData,"Max terrain profile");
        JFreeChart chart2 = makeTerrainChart(trajectoryData2,trajectoryDistance,-1.0,terrainData2, "Terrain profile");


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

        if (terrainData2.getMaxX() > 10)
        {
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
        else
        {
            System.out.println("Terrain is too shot to show a graph, use Max Terrain graph for orientation instead.");
        }


    }
    private static JFreeChart makeTerrainChart(XYSeries trajectoryData, double distance, double maxHeigh, XYSeries terrainData, String name)
    {
        XYPlot plot = new XYPlot();

        XYSeriesCollection collection1 = new XYSeriesCollection();
        collection1.addSeries(trajectoryData);
        XYItemRenderer renderer1 = new XYLineAndShapeRenderer(true, false);
        ValueAxis domain = new NumberAxis("Distance [m]");
        domain.setRange(0,Math.max(distance* METERS_PER_PIXEL,1));
        ValueAxis range = new NumberAxis("Heigh [m]");
        if (maxHeigh > 0)
            range.setRange(0,maxHeigh* METERS_PER_PIXEL + 50);
        else if (trajectoryData.getMaxX() == terrainData.getMaxX() &&
                Math.abs(terrainData.getY(terrainData.getItemCount()-1).doubleValue()-trajectoryData.getY(trajectoryData.getItemCount()-1).doubleValue()) > 1.0)
        {
            System.out.println("Visualisation missile out of map.");
        }

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
        Point p = new Point(dataInputStream.readInt(),dataInputStream.readInt());
        Point t = new Point(dataInputStream.readInt(),dataInputStream.readInt());

        originalMap = new double[w][h];
        minH = Double.MAX_VALUE;
        maxH = Double.MIN_VALUE;
        int miH = Integer.MAX_VALUE;
        int maH = Integer.MIN_VALUE;

        for (int j =0; j <h; j++)
            for (int i =0; i <w; i++)
            {
                int hh = dataInputStream.readInt();
                double heigh = hh / METERS_PER_PIXEL;
                originalMap[i][j] = heigh;

                minH = Math.min(minH,heigh);
                maxH = Math.max(maxH,heigh);
                miH = Math.min(miH,hh);
                maH = Math.max(maH,hh);
            }

        if (miH == maH)
            flat = true;

        players[0].player = new Point3D(p.x,p.y,originalMap[p.x][p.y]);
        target = new Point3D(t.x,t.y,originalMap[t.x][t.y]);
    }

    private static void updateMapImage()
    {
        for (int j =0; j <h; j++)
            for (int i =0; i <w; i++)
            {
                double heigh = (int)map[i][j];

                minH = Math.min(minH,heigh);
                maxH = Math.max(maxH,heigh);
            }

        mapImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] srcPixels = ((DataBufferInt) mapImage.getRaster().getDataBuffer()).getData();

        for (int j =0; j <h; j++)
            for (int i =0; i <w; i++)
            {
                int color = (int)(((map[i][j]-minH) / (maxH-minH))*0xFF);
                if (flat)
                    color = 128;
                srcPixels[j * w + i] = new Color(color, color, color, 0xFF).getRGB();
            }

        repaint(false);
    }

    //This is invisible, next few line you see contains only empty lines. Nothing to see here
    static final List<String> missStrings = Arrays.asList(
                    "Once upon a time there was a nice little village. Then some !@#$ dropped bomb on it.",
                    "Rocket hit orphanage nearby.",
                    "Brand new school is gone.",
                    "Puppy shelter is now located in next village. On many different places...",
                    "Only bridge to civilization wen KABOOM. I don't think people will survive this winter.",
                    "Who thought that create military rocket testing area near inhabited area is good idea?",
                    "0 days since last rocket accident.",
                    "There are unexploded rockets everywhere! On the bright side squirrels in this area are setting new world-wide jump records."
    );
}

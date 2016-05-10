package semestralka;

import java.awt.geom.Point2D;

/**
 * Created by Štěpán Martínek on 13.03.2016.
 */
class ActuallyUsefulLine
{
    private Point2D.Double p1;
    Point2D.Double p2;

    ActuallyUsefulLine(Point2D.Double P1, Point2D.Double P2)
    {
        p1 = P1;
        p2 = P2;
    }

    ActuallyUsefulLine()
    {
        p1 = new Point2D.Double(0,0);
        p2 = new Point2D.Double(1,1);
    }

    private static double deg2rad(double x)
    {
        return x * Math.PI/180.0;
    }

    private static double rad2deg(double x)
    {
        return x * 180.0/Math.PI;
    }

    double length()
    {
        double x = p2.x- p1.x;
        double y = p2.y - p1.y;
        return Math.sqrt(x*x + y*y);
    }


    ActuallyUsefulLine setLength(double len)
    {
        ActuallyUsefulLine v = unitVector();
        p2 = new Point2D.Double(p1.x + v.dx() * len, p1.y + v.dy() * len);
        return this;
    }

    double angle()
    {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;

        double theta = rad2deg(Math.atan2(-dy, dx));

        double theta_normalized = theta < 0 ? theta + 360 : theta;

        if (Math.abs(theta_normalized - 360.0) < 1.0e-200)
            return 0.0;
        else
            return deg2rad(theta_normalized);
    }

    ActuallyUsefulLine setAngle(double angle)
    {
        double angleR = deg2rad(angle);
        double l = length();

        double dx = Math.cos(angleR) * l;
        double dy = -Math.sin(angleR) * l;

        p2.x = p1.x + dx;
        p2.y = p1.y + dy;

        return this;
    }

    ActuallyUsefulLine setP1(Point2D.Double P1)
    {
        p1 = P1;
        return this;
    }

    ActuallyUsefulLine setP2(Point2D.Double P2)
    {
        p2 = P2;
        return this;
    }


    private ActuallyUsefulLine unitVector()
    {
        double x = p2.x- p1.x;
        double y = p2.y - p1.y;

        double len = Math.sqrt(x*x + y*y);
        return new ActuallyUsefulLine(p1, new Point2D.Double(p1.x + x/len, p1.y + y/len));
    }

    private double dx()
    {
        return p2.x - p1.x;
    }

    private double dy()
    {
        return p2.y - p1.y;
    }
}







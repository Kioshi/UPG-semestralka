package semestralka;

import java.awt.geom.Point2D;

/**
 * Created by Štěpán Martínek on 13.03.2016.
 */
public class ActuallyUsefulLine
{
    Point2D.Double p1;
    Point2D.Double p2;

    ActuallyUsefulLine(Point2D.Double P1, Point2D.Double P2)
    {
        p1 = P1;
        p2 = P2;
    }

    public ActuallyUsefulLine()
    {
        p1 = new Point2D.Double(0,0);
        p2 = new Point2D.Double(1,1);
    }

    static double deg2rad(double x)
    {
        return x * Math.PI/180.0;
    }

    static double rad2deg(double x)
    {
        return x * 180.0/Math.PI;
    }

    double length()
    {
        double x = p2.x- p1.x;
        double y = p2.y - p1.y;
        return Math.sqrt(x*x + y*y);
    }


    void setLength(double len)
    {
        ActuallyUsefulLine v = unitVector();
        p2 = new Point2D.Double(p1.x + v.dx() * len, p1.y + v.dy() * len);
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
            return theta_normalized;
    }

    void setAngle(double angle)
    {
        double angleR = deg2rad(angle);
        double l = length();

        double dx = Math.cos(angleR) * l;
        double dy = -Math.sin(angleR) * l;

        p2.x = p1.x + dx;
        p2.y = p1.y + dy;
    }

    void setP1(Point2D.Double P1)
    {
        p1 = P1;
    }

    void setP2(Point2D.Double P2)
    {
        p2 = P2;
    }


    ActuallyUsefulLine unitVector()
    {
        double x = p2.x- p1.x;
        double y = p2.y - p1.y;

        double len = Math.sqrt(x*x + y*y);
        return new ActuallyUsefulLine(p1, new Point2D.Double(p1.x + x/len, p1.y + y/len));
    }

    double dx()
    {
        return p2.x - p1.x;
    }

    double dy()
    {
        return p2.y - p1.y;
    }







































    //This is invisible, next few line you see contains only empty lines. Nothing to see here
    public static final int MAX_MISS_STRINGS = 8;
    public static final String[] missStrings =
    {
            "Once upon a time there was a nice little village. Then some !@#$ dropped bomb on it.",
            "Rocket hit orphanage nearby.",
            "Brand new school is gone.",
            "Puppy shelter is now located in next village. On many different places...",
            "Only bridge to civilization wen KABOOM. I don't think people will survive this winter.",
            "Who thought that create military rocket testing area near inhabited area is good idea?",
            "0 days since last rocket accident.",
            "There are unexploded rockets everywhere! On the bright side squirrels in this area are setting new world-wide jump records."
    };
}







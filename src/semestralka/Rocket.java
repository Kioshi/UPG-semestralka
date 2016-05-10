package semestralka;

/**
 * Created by Štěpán Martínek on 08.05.2016.
 */
public class Rocket
{
    String name;

    double MaxSpeedDeviationPct = 5.0;
    double MaxAngleDeviation = 1.0;
    double MaxElevationDeviation = 2.0;
    double BlastRectuionChance = 5.0;
    double MidAirExplosionPerStepChancePct = 0.005;
    int cost = 100;

    public Rocket(String name)
    {
        this.name = name;
    }

    public Rocket(String name, double maxSpeedDeviationPct, double maxAngleDeviation, double maxElevationDeviation, double blastRectuionChance, double midAirExplosionPerStepChancePct, int cost)
    {
        this.name = name;
        MaxSpeedDeviationPct = maxSpeedDeviationPct;
        MaxAngleDeviation = maxAngleDeviation;
        MaxElevationDeviation = maxElevationDeviation;
        BlastRectuionChance = blastRectuionChance;
        MidAirExplosionPerStepChancePct = midAirExplosionPerStepChancePct;
        this.cost = cost;
    }


    @Override
    public String toString()
    {
        return "Rocket{" + name + " - Cost: " + cost +
                "$, MaxSpeedDeviationPct=" + MaxSpeedDeviationPct +
                ", MaxAngleDeviation=" + MaxAngleDeviation +
                ", MaxElevationDeviation=" + MaxElevationDeviation +
                ", BlastRectuionChance=" + BlastRectuionChance +
                ", MidAirExplosionPerStepChancePct=" + MidAirExplosionPerStepChancePct +
                " }";
    }
}

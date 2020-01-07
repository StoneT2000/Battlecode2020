package bot1;

import battlecode.common.*;
public class HQ extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static RobotType unitToBuild = RobotType.MINER;
    static int[] BotCounts = new int[10];
    public static void run() throws GameActionException {
        System.out.println("TEAM SOUP: " + rc.getTeamSoup());


        // if we are to build a unit, proceed
        if (unitToBuild != null) {
            // keep trying to build in buildDir direction, rotate a little to find new build loc
            // TODO: Optimize this by building in direction of known soup locations or refineries?

            boolean builtUnit = false;
            for (int i = 9; --i >= 1;) {
                if (tryBuild(unitToBuild, buildDir)) {
                    builtUnit = true;
                    break;
                }
                else {
                    buildDir = buildDir.rotateRight();
                }
            }
            if (builtUnit) {
                BotCounts[unitToBuild.ordinal()] += 1;
            }
            // make next turns build direction different
            buildDir = buildDir.rotateRight();
        }
        // otherwise we dont build (stack up)
    }
    public static void decideOnUnitToBuild() throws GameActionException {
        unitToBuild = RobotType.MINER;
    }
    public static void setup() throws GameActionException {
        // announce self location on turn 1 (will always run)
        announceSelfLocation(0);
        // allows all other bots to refer to the HQ if needed by storeHQLocation()
    }
    // initializes the strategy through placement of first units and broadcasts
    public static void initializeStrategy() throws GameActionException {

    }
}

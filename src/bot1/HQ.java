package bot1;

import battlecode.common.*;
public class HQ extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static RobotType unitToBuild;
    static int[] BotCounts = new int[10];
    static MapLocation SoupLocation;
    public static void run() throws GameActionException {
        System.out.println("TEAM SOUP: " + rc.getTeamSoup());


        decideOnUnitToBuild();
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
        // otherwise we dont build (stock up)

        if (rc.getRoundNum() > 1) {
            Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
            checkBlockForSoupLocations(lastRoundsBlocks);
        }

    }
    public static void decideOnUnitToBuild() throws GameActionException {
        unitToBuild = null;
        if (rc.getRoundNum() <= 3) {
            unitToBuild = RobotType.MINER;
            return;
        }

        // if soup was found early on..., pause and wait for refinery
        if (rc.getRoundNum() < 60) {
            if (SoupLocation != null) {
                // unitToBuild = RobotType.MINER;
            }
            // if no early soup, stock up, wait for design school to be built
            else {

            }
        }
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

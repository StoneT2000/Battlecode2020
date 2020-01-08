package bot1;

import battlecode.common.*;
public class HQ extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static RobotType unitToBuild;
    static int[] BotCounts = new int[10];
    static MapLocation SoupLocation;
    static MapLocation mapCenter;
    static int mapSize;
    static boolean nearCenter;
    public static void run() throws GameActionException {
        if (debug) System.out.println("TEAM SOUP: " + rc.getTeamSoup());

        // shoot nearest robot
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        RobotInfo closestDroneBot = null;
        int closestEnemyDroneDist = 99999999;
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            // TODO: Check if info.getLocation() vs info.location is more efficient?
            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
            if (info.type == RobotType.DELIVERY_DRONE && dist < closestEnemyDroneDist) {
                closestEnemyDroneDist = dist;
                closestDroneBot = info;
            }
        }
        // if we found a closest bot
        if (closestDroneBot != null) {
            if (rc.canShootUnit(closestDroneBot.getID())) {
                rc.shootUnit(closestDroneBot.getID());
                if (debug) rc.setIndicatorDot(closestDroneBot.location, 255, 50,190);
            }
        }

        // decide on unit to build and set unitToBuild appropriately
        decideOnUnitToBuild();
        // if we are to build a unit, proceed
        if (unitToBuild != null) {
            // proceed with building unit using default heurstics
            build();
        }
        // otherwise we don't build (stock up)

        if (rc.getRoundNum() > 1) {
            Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
            checkBlockForSoupLocations(lastRoundsBlocks);
        }

    }
    public static void build() throws GameActionException {
        // TODO: optimize bytecode here
        if (rc.getRoundNum() == 1) {
            // preferred build direction is towards middle if castle is not really close to center
            if (!nearCenter) {
                buildDir = rc.getLocation().directionTo(mapCenter);
            }
        }
        else if (rc.getRoundNum() == 2) {

        }
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
    public static void decideOnUnitToBuild() throws GameActionException {
        unitToBuild = null;
        // produce 2 miners at start, leaving 200 - 140 = 60 soup left
        if (rc.getRoundNum() <= 2) {
            unitToBuild = RobotType.MINER;
            return;
        }

        // if soup was found early on...
        if (rc.getRoundNum() < 60) {
            if (SoupLocation != null) {
                //unitToBuild = RobotType.MINER;
            }
            // if no early soup, stock up, wait for design school to be built
            else {

            }
        }

        if (BotCounts[RobotType.MINER.ordinal()] <= mapSize / 100) {
            if (rc.getTeamSoup() >= RobotType.REFINERY.cost + 2 * RobotType.MINER.cost && rc.getRoundNum() < 200) {
                unitToBuild = RobotType.MINER;
            } else if (rc.getTeamSoup() >= 1100) {
                unitToBuild = RobotType.MINER;
            }
        }
    }
    public static void setup() throws GameActionException {
        // announce self location on turn 1 (will always run)
        announceSelfLocation(0);
        mapSize = rc.getMapWidth() * rc.getMapHeight();
        mapCenter =  new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        if (rc.getLocation().distanceSquaredTo(mapCenter) > 48) {
            nearCenter = false;
        }
        else {
            nearCenter = true;
        }
        // allows all other bots to refer to the HQ if needed by storeHQLocation()
    }
    // initializes the strategy through placement of first units and broadcasts
    public static void initializeStrategy() throws GameActionException {

    }
}

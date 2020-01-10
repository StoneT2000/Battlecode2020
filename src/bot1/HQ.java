package bot1;

import battlecode.common.*;
public class HQ extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static RobotType unitToBuild;
    static int minersBuilt = 0;
    static MapLocation SoupLocation;
    static MapLocation mapCenter;
    static int mapSize;
    static boolean surroundedByFlood = false;
    static int surroundedByFloodRound = -1;
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
        // TODO:, shoot closest one with our unit
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
        if (!surroundedByFlood && surroundedByFlood()) {
            surroundedByFlood = true;
            surroundedByFloodRound = rc.getRoundNum();
            announceBuildDrones();
            //TODO: once surrounded, announce to fulfillment centers to BUILD BUILD DRONES

        }
        // announce drone attack sometime before we would get overwhelemed by flood
        if (surroundedByFlood) {
            if (rc.getRoundNum() % 10 == 0 && rc.getRoundNum() >= surroundedByFloodRound + 125) {
                announceDroneAttack();
            }
            else if (rc.getRoundNum() % 50 == 0){
                announceBuildDrones();
            }
        }
        // otherwise we don't build (stock up)

        if (rc.getRoundNum() > 1) {
            Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
            //checkBlockForSoupLocations(lastRoundsBlocks);
            checkForEnemyBasesInBlocks(lastRoundsBlocks);
        }

    }
    static void announceBuildDrones() throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), BUILD_DRONES};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING BUILD DRONES!!!");
        // TODO: CHANGE COSTS HERE, put -1 and a max 50 or smth to get suggested cost
        if (rc.canSubmitTransaction(message, 10)) {
            rc.submitTransaction(message, 10);
        }
    }
    static void announceDroneAttack() throws GameActionException {
        int hashedLoc = -1;
        if (enemyBaseLocation != null) {
            hashedLoc = hashLoc(enemyBaseLocation);
        }
        int[] message = new int[] {generateUNIQUEKEY(), DRONES_ATTACK, hashedLoc};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING DRONE ATTACK ");
        // TODO: CHANGE COSTS HERE, put -1 and a max 50 or smth to get suggested cost
        if (rc.canSubmitTransaction(message, 10)) {
            rc.submitTransaction(message, 10);
        }
    }
    static boolean surroundedByFlood() throws GameActionException {
        int sideLength = (BASE_WALL_DIST+1) * 2 + 1;
        //int side2 = (BASE_WALL_DIST+1)*2 - 1;
        for (int i = 0; i < sideLength; i++) {
            if (!rc.senseFlooding(new MapLocation(rc.getLocation().x - BASE_WALL_DIST - 1 + i, rc.getLocation().y - BASE_WALL_DIST - 1))) {
                return false;
            }
            if (!rc.senseFlooding(new MapLocation(rc.getLocation().x - BASE_WALL_DIST - 1 + i, rc.getLocation().y + BASE_WALL_DIST + 1))) {
                return false;
            }
        }
        //TODO OPTIMIZE BYTECODE
        for (int i = 0; i < sideLength; i++) {
            if (!rc.senseFlooding(new MapLocation(rc.getLocation().x - BASE_WALL_DIST - 1, rc.getLocation().y - BASE_WALL_DIST + i))) {
                return false;
            }
            if (!rc.senseFlooding(new MapLocation(rc.getLocation().x + BASE_WALL_DIST + 1, rc.getLocation().y - BASE_WALL_DIST + i))) {
                return false;
            }
        }
        return true;
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
            minersBuilt++;
        }
        // make next turns build direction different
        buildDir = buildDir.rotateRight();
    }
    public static void decideOnUnitToBuild() throws GameActionException {
        unitToBuild = null;
        // produce 2 miners at start, leaving 200 - 140 = 60 soup left
        if (rc.getRoundNum() <= 20) {
            unitToBuild = RobotType.MINER;
            return;
        }



        // limit miners to 1/100 of map size and then periodically build them
        if (minersBuilt <= mapSize / 100) {
            // only produce miner if we have sufficient stock up and its early or if we have a lot of soup
            // how to determine if there is still demand for soup though?
            if (rc.getTeamSoup() >= RobotType.REFINERY.cost + 1.5 * RobotType.MINER.cost && rc.getRoundNum() < 300) {
                unitToBuild = RobotType.MINER;
            } else if (rc.getTeamSoup() >= 1100) {
                unitToBuild = RobotType.MINER;
            }
        }
        // build less and less miners over time
        else if (rc.getRoundNum() % ((int) (rc.getRoundNum() / 20 + 5)) == 0) {
            if (rc.getTeamSoup() >= RobotType.REFINERY.cost + 2 * RobotType.MINER.cost) {
                unitToBuild = RobotType.MINER;
            }
        }
    }

    public static void setup() throws GameActionException {
        // announce self location on turn 1 (will always run)
        announceSelfLocation(0);
        HQLocation = rc.getLocation();
        storeEnemyHQLocations();
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

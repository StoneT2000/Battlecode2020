package rush;

import battlecode.common.*;

public class HQ extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static RobotType unitToBuild;
    static int minersBuilt = 0;
    static int FulfillmentCentersBuilt = 0;
    static MapLocation SoupLocation;
    static MapLocation mapCenter;
    static int mapSize;
    static boolean criedForLandscapers = false;
    static boolean surroundedByFlood = false;
    static int surroundedByFloodRound = -1;
    static boolean nearCenter;
    static boolean existsSoup = false;
    static int MIN_DRONE_FOR_ATTACK = 14;
    static boolean criedForDroneHelp = false;
    static boolean saidNoMoreLandscapersNeeded = false;
    static int vaporatorsBuilt = 0;
    public static void run() throws GameActionException {
        if (debug) System.out.println("TEAM SOUP: " + rc.getTeamSoup() + " | Miners Built: " + minersBuilt + " FulfillmentCenters Built: " + FulfillmentCentersBuilt);

        // shoot nearest robot
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo closestDroneBot = null;
        int enemyLandscapers = 0;
        int enemyMiners = 0;
        int closestEnemyDroneDist = 99999999;
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            // TODO: Check if info.getLocation() vs info.location is more efficient?
            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
            if (info.type == RobotType.DELIVERY_DRONE && dist < closestEnemyDroneDist) {
                closestEnemyDroneDist = dist;
                closestDroneBot = info;
            }
            // nearby landscaper? RUSH, GET HELP!
            if (info.type == RobotType.LANDSCAPER) {
                enemyLandscapers++;
            }
            if (info.type == RobotType.MINER) {
                enemyMiners++;
            }
        }



        int wallBots = 0;
        int myDrones = 0;
        for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyFriendlyRobots[i];
            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
            if (info.type == RobotType.LANDSCAPER && dist <= 16) {
                wallBots ++;
            }
            if (info.type == RobotType.DELIVERY_DRONE) {
                myDrones++;
            }
        }
        if ((rc.getRoundNum() >= 300 || vaporatorsBuilt > 10) && wallBots < 20 && rc.getRoundNum() % 10 == 0) {
            announceWantLandscapers(20 - wallBots);
        }
        if (rc.getRoundNum() >= 300 && myDrones < 4 && rc.getRoundNum() % 10 == 0) {
            announceBuildDronesNow(4 - myDrones);
        }

        // if we see an enemy landscaper or enemy miner
        if (enemyLandscapers > 0 || enemyMiners > 0) {
            // announce I want drones and fulfillment center to build them if we have no drones and we dont know a center was built or every 20 turns
            // announce asap and then every 10 rounds
            if ((!criedForLandscapers || rc.getRoundNum() % 10 == 0) && wallBots < enemyLandscapers + enemyMiners + 1) {
                if (wallBots < 8) {
                    announceWantLandscapers(8 - wallBots);
                }
            }
            /*
            if ((!criedForDroneHelp || rc.getRoundNum() % 10 == 0) && myDrones == 0 && (FulfillmentCentersBuilt < 1 || rc.getRoundNum() % 20 == 0)) {
                announceWantDronesForDefence();
                criedForDroneHelp = true;
            }
            // not enough drones to combat, ask for more drones
            */
            if ((!criedForDroneHelp || rc.getRoundNum() % 10 == 0) && myDrones < enemyLandscapers) {
                announceBuildDronesNow(enemyLandscapers - myDrones);
                announceWantDronesForDefence();
                criedForDroneHelp = true;
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

        if (myDrones >= MIN_DRONE_FOR_ATTACK && rc.getRoundNum() % 20 == 0) {
            announceDroneAttack();
        }

        existsSoup = false;
        if (rc.getRoundNum() > 1) {
            Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
            //checkBlockForSoupLocations(lastRoundsBlocks);
            checkForEnemyBasesInBlocks(lastRoundsBlocks);
            for (int i = lastRoundsBlocks.length; --i >= 0; ) {
                Transaction block = lastRoundsBlocks[i];
                int[] msg = block.getMessage();
                decodeMsg(msg);
                if (isOurMessage((msg))) {
                    if (msg[1] == RobotType.VAPORATOR.ordinal()) {
                        vaporatorsBuilt++;
                    }
                    else if ((msg[1] ^ ANNOUNCE_SOUP_LOCATION) == 0) {
                        existsSoup = true;
                    }
                }
            }
        }

    }
    static void checkTransactionsForBuildCounts(Transaction[] transactions) throws GameActionException {
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {

                if (msg[1] == RobotType.FULFILLMENT_CENTER.ordinal()) {
                    FulfillmentCentersBuilt++;
                }
            }
        }
    }
    static void announceBuildDronesNow(int amount) throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), BUILD_DRONE_NOW, rc.getTeamSoup(), amount, randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING BUILD DRONES!!!");
        // TODO: CHANGE COSTS HERE, put -1 and a max 50 or smth to get suggested cost
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
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
    static void announceNoMoreLandscapersNeeded() throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), NO_MORE_LANDSCAPERS_NEEDED, randomInt(), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING NO MORE SCAPERS NEEDED!!!");
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    static void announceDroneAttack() throws GameActionException {
        int hashedLoc = -1;
        if (enemyBaseLocation != null) {
            hashedLoc = hashLoc(enemyBaseLocation);
        }
        int[] message = new int[] {generateUNIQUEKEY(), DRONES_ATTACK, hashedLoc, randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING DRONE ATTACK ");
        // TODO: CHANGE COSTS HERE, put -1 and a max 50 or smth to get suggested cost
        if (rc.canSubmitTransaction(message, 10)) {
            rc.submitTransaction(message, 10);
        }
    }
    static void announceWantDronesForDefence() throws GameActionException {
        // send teamsoup count to ensure we don't build too many drones?
        int [] message = new int[] {generateUNIQUEKEY(), NEED_DRONES_FOR_DEFENCE, rc.getTeamSoup(), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING WANT DRONES ");
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    static void announceWantLandscapers(int amount) throws GameActionException {
        // send teamsoup count to ensure we don't build too many landscapers
        int [] message = new int[] {generateUNIQUEKEY(), NEED_LANDSCAPERS_FOR_DEFENCE, rc.getTeamSoup(), amount, randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING WANT LANDSCAPERS ");
        if (rc.canSubmitTransaction(message, 1)) {
           rc.submitTransaction(message, 1);
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


        if (rc.getRoundNum() <= 15) {
            // build first 3 miners right away
            unitToBuild = RobotType.MINER;
            return;
        }
        if (vaporatorsBuilt * 4 + 4 >= minersBuilt && existsSoup) {
            unitToBuild = RobotType.MINER;
            return;
        }
        if (rc.getRoundNum() % 30 == 0 && rc.getTeamSoup() >= 350) {
            unitToBuild = RobotType.MINER;
        }



    }

    public static void setup() throws GameActionException {
        announceSelfLocation(1);
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

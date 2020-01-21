package Chow7;

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
    static boolean criedForDesignSchool = false;
    static boolean criedForFC = false;
    static boolean criedForLockAndDefend = false;
    static boolean surroundedByFlood = false;
    static int surroundedByFloodRound = -1;
    static boolean nearCenter;
    static boolean existsSoup = false;
    static boolean gettingRushed = false;
    static int MIN_DRONE_FOR_ATTACK = 14 + 24; // 24 for defence, 14 for attack
    static boolean criedForDroneHelp = false;
    static int designSchoolsBuilt = 0;
    static boolean saidNoMoreLandscapersNeeded = false;
    static int vaporatorsBuilt = 0;
    static int wallBotsMax = 20; // max landscapers that can be on wall and second wall
    static int wallSpaces = 20;
    public static void run() throws GameActionException {
        if (debug) System.out.println("TEAM SOUP: " + rc.getTeamSoup() + " | Miners Built: " + minersBuilt + " FulfillmentCenters Built: " + FulfillmentCentersBuilt);
        wallBotsMax = wallSpaces;
        // shoot nearest robot
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        MapLocation[] soupLocsNearby = rc.senseNearbySoup(-1);
        RobotInfo closestDroneBot = null;
        int enemyLandscapers = 0;
        int enemyMiners = 0;
        int enemyDesignSchools = 0;
        int enemyDrones = 0;
        int enemyNetGuns = 0;
        int closestEnemyDroneDist = 99999999;
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            // TODO: Check if info.getLocation() vs info.location is more efficient?
            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
            if (info.type == RobotType.DELIVERY_DRONE && dist < closestEnemyDroneDist) {
                closestEnemyDroneDist = dist;
                closestDroneBot = info;
            }
            switch (info.type) {
                case NET_GUN:
                    enemyNetGuns++;
                    break;
                case DESIGN_SCHOOL:
                    enemyDesignSchools++;
                    break;
                case MINER:
                    enemyMiners++;
                    break;
                case LANDSCAPER:
                    enemyLandscapers++;
                    break;
                case DELIVERY_DRONE:
                    enemyDrones++;
                    break;
            }
        }

        int closestSoupDist = 9999999;
        MapLocation closestSoupLoc = null;
        for (int i = soupLocsNearby.length; --i >= 0; ) {
            MapLocation soupLoc = soupLocsNearby[i];
            int distToSoup = rc.getLocation().distanceSquaredTo(soupLoc);
            if (distToSoup < closestSoupDist) {
                closestSoupDist = distToSoup;
                closestSoupLoc = soupLoc;
            }
        }



        int wallBots = 0;
        int myDrones = 0;
        int designSchools = 0;
        int fulfillmentCenters = 0;
        for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyFriendlyRobots[i];
            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
            if (info.type == RobotType.LANDSCAPER && dist <= 16) {
                wallBots ++;
            }
            if (info.type == RobotType.DELIVERY_DRONE) {
                myDrones++;
            }
            else if (info.type == RobotType.FULFILLMENT_CENTER) {
                fulfillmentCenters++;
                if (dist <= 8) {
                    wallBotsMax--;
                    // decrement one as a fulfillment center is in our wall area
                }
            }
            else if (info.type == RobotType.DESIGN_SCHOOL) {
                designSchools++;
                if (dist <= 8) {
                    wallBotsMax--;
                }
                // decrement one as a fulfillment center is in our wall area
                // count how many open tiles it can build on
                boolean blocked = true;
                int elevation = rc.senseElevation(info.location);
                for (int k = 1; k < directions.length; k++) {
                    MapLocation adjLoc = info.location.add(directions[k]);
                    if (rc.canSenseLocation(adjLoc)) {
                        int asideElevation = rc.senseElevation(adjLoc);
                        if (debug) System.out.println("School e: " + elevation + " | asideE "  + asideElevation );
                        if (asideElevation <= elevation + 3 && asideElevation >= elevation - 3 && !rc.isLocationOccupied(adjLoc)) {
                            if (debug) System.out.println("School at " + info.location + " has space");
                            blocked = false;
                            break;
                        }
                    }
                    else {
                        blocked = false;
                        break;
                    }

                }
                if (blocked) {
                    if (debug) System.out.println("School at " + info.location + " has no space");
                    designSchools--;
                }
                else {
                    if (debug) System.out.println("School at " + info.location + " has space");
                }
            }
        }
        if (debug) System.out.println("I need " + wallBotsMax + " wall bots");
        // make sure we get a school and FC all the time
        if (debug) System.out.println ("Asked for school: " + criedForDesignSchool + " soup atm:"  +rc.getTeamSoup());
        if ((!criedForDesignSchool || rc.getRoundNum() % 10 == 0) && rc.getRoundNum() >= 15 && designSchools == 0 && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost + 2) {
            announceBUILD_A_SCHOOL();
            criedForDesignSchool = true;
        }
        if ((!criedForFC || rc.getRoundNum() % 10 == 0) && fulfillmentCenters == 0 && designSchools > 0 && wallBots >= Math.min(4, wallBotsMax) && enemyLandscapers > 0 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost + 2) {
            announceBUILD_A_CENTER();
            criedForFC = true;
        }
        else if ((!criedForFC || rc.getRoundNum() % 10 == 0) && fulfillmentCenters == 0 && designSchools > 0 && wallBots >= Math.min(8, wallBotsMax) && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost + 2) {
            announceBUILD_A_CENTER();
            criedForFC = true;
        }
        // get some drones if we have more wall bots
        if (wallBots - 8 > myDrones && myDrones < 2) {
            announceBuildDronesNow(4 - myDrones);
        }
        else if ((rc.getRoundNum() >= 300 || vaporatorsBuilt > 10) && wallBots < wallBotsMax && rc.getRoundNum() % 10 == 0) {
            announceWantLandscapers(wallBotsMax - wallBots);
        }

        if (rc.getRoundNum() >= 300 && myDrones < 2 && rc.getRoundNum() % 10 == 0) {
            announceBuildDronesNow(4 - myDrones);
        }

        // if we reach the number of wall bots we want, get drone defenders
        if (wallBots >= wallBotsMax && rc.getRoundNum() % 15 == 0 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost) {
            if (myDrones <= 24) {
                //announceBuildDronesNow(24 - myDrones);
            }
            announceNoMoreLandscapersNeeded();
        }
        if (!criedForLockAndDefend && enemyDrones > 0) {
            announceLOCK_AND_DEFEND();
            criedForLockAndDefend = true;
        }
        else if (enemyDrones == 0) {
            criedForLockAndDefend = false;
        }


        if (!gettingRushed && enemyDesignSchools > 0) {
            gettingRushed = true;
        }
        // if we were rushed but no more design schools
        if (gettingRushed && enemyDesignSchools == 0) {

        }
        if (gettingRushed && enemyNetGuns == 0) {

        }

        // if we see an enemy landscaper or enemy miner
        if (enemyLandscapers > 0 || enemyMiners > 0) {
            // announce I want drones and fulfillment center to build them if we have no drones and we dont know a center was built or every 20 turns
            // announce asap and then every 10 rounds
            if ((!criedForLandscapers || rc.getRoundNum() % 10 == 0) && wallBots < enemyLandscapers + enemyMiners + 1) {
                if (wallBots < 8) {
                    announceWantLandscapers(8 - wallBots);
                    criedForLandscapers = true;
                }
            }
            if ((!criedForDroneHelp || rc.getRoundNum() % 10 == 0) && myDrones < enemyLandscapers && enemyNetGuns == 0) {
                announceBuildDronesNow(enemyLandscapers - myDrones);
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
        if (unitToBuild != null && (rc.getRoundNum() < 15 || (wallBots > 1 && designSchools > 0 ))) {
            // proceed with building unit using default heurstics
            if (debug) System.out.println("closest soup: " + closestSoupLoc);
            build(closestSoupLoc);
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
                    else if (msg[1] == RobotType.DESIGN_SCHOOL.ordinal()) {
                        designSchoolsBuilt++;
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
    static void announceLOCK_AND_DEFEND() throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), LOCK_AND_DEFEND, rc.getTeamSoup(), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING LOCK AND DEFEND!!!");
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    static void announceBUILD_A_CENTER() throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), BUILD_A_CENTER, rc.getTeamSoup(), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING BUILD CENTER!!!");
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    static void announceBUILD_A_SCHOOL() throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), BUILD_A_SCHOOL, rc.getTeamSoup(), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING BUILD SCHOOL!!!");
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
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
    public static void build(MapLocation closestSoupLoc) throws GameActionException {
        // TODO: optimize bytecode here
        if (rc.getRoundNum() == 1) {
            // preferred build direction is towards middle if castle is not really close to center
            if (!nearCenter) {
                buildDir = rc.getLocation().directionTo(mapCenter);
            }
        }
        else if (rc.getRoundNum() == 2) {

        }

        if (closestSoupLoc != null && !closestSoupLoc.equals(Direction.CENTER)) {
            buildDir = rc.getLocation().directionTo(closestSoupLoc);
        }
        if (buildDir.equals(Direction.CENTER)) {
            buildDir = Direction.NORTH;
        }
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
        if (vaporatorsBuilt * 4 + 4 >= minersBuilt && existsSoup && designSchoolsBuilt > 0) {
            unitToBuild = RobotType.MINER;
            return;
        }
        if (rc.getRoundNum() % 30 == 0 && rc.getTeamSoup() >= 350 && existsSoup) {
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
        // count number of open wall build block tiles
        wallSpaces = 0;
        for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i>= 0; ) {
            int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
            MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
            //FirstLandscaperPosAroundHQTable.add(loc);
            if (rc.onTheMap(loc)) {
                wallSpaces++;
            }
        }
        for (int i = Constants.LandscaperPosAroundHQ.length; --i>= 0; ) {
            int[] deltas = Constants.LandscaperPosAroundHQ[i];
            MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
            //FirstLandscaperPosAroundHQTable.add(loc);
            if (rc.onTheMap(loc)) {
                wallSpaces++;
            }
        }
    }
    // initializes the strategy through placement of first units and broadcasts
    public static void initializeStrategy() throws GameActionException {

    }
}

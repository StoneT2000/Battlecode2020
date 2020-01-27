package Chow10;

import Chow10.utils.HashTable;
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
    static int fulfillmentCentersBuilt = 0;
    static boolean saidNoMoreLandscapersNeeded = false;
    static int vaporatorsBuilt = 0;
    static int wallBotsMax = 20; // max landscapers that can be on wall and second wall
    static int wallSpaces = 20;

    static boolean announcedWalledin;

    static int buildQueueAmount = 0;
    static HashTable<Integer> idsOfRushUnitsCalledOut = new HashTable<>(10);

    public static void run() throws GameActionException {
        if (debug) System.out.println("TEAM SOUP: " + rc.getTeamSoup() + " | Miners Built: " + minersBuilt + " FulfillmentCenters Built: " + FulfillmentCentersBuilt + " | buildQueueAmount: " +buildQueueAmount);
        wallBotsMax = wallSpaces;
        // shoot nearest robot
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        MapLocation[] soupLocsNearby = rc.senseNearbySoup(-1);
        RobotInfo closestDroneBot = null;
        int closestEnemyDroneWithUnitDist = 9999999;
        RobotInfo closestDroneBotWithUnit = null;
        int enemyLandscapers = 0;
        int enemyMiners = 0;
        int enemyDesignSchools = 0;
        int enemyDrones = 0;
        int enemyNetGuns = 0;
        int miners = 0;
        int inHQSpaceEnemyLandscapers = 0;
        int closestEnemyDroneDist = 99999999;
        int distToClosestEarlyEnemyMiner = 99999999;
        RobotInfo closestEarlyEnemyMiner = null;
        int distToClosestEarlyEnemyDroneWithUnit = 99999999;
        RobotInfo closestEarlyEnemyDroneWithUnit = null;
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            // TODO: Check if info.getLocation() vs info.location is more efficient?
            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
            if (info.type == RobotType.DELIVERY_DRONE && dist < closestEnemyDroneDist) {
                closestEnemyDroneDist = dist;
                closestDroneBot = info;
            }
            if (info.type == RobotType.DELIVERY_DRONE && dist < closestEnemyDroneWithUnitDist && info.isCurrentlyHoldingUnit()) {
                closestDroneBotWithUnit = info;
                closestEnemyDroneWithUnitDist = dist;
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
                    // for rush dealing...
                    if (rc.getRoundNum() <= 300 && dist < distToClosestEarlyEnemyMiner) {
                        closestEarlyEnemyMiner = info;
                        distToClosestEarlyEnemyMiner = dist;
                    }
                    break;
                case LANDSCAPER:
                    enemyLandscapers++;
                    if (info.location.distanceSquaredTo(rc.getLocation()) <= HQ_LAND_RANGE) {
                        inHQSpaceEnemyLandscapers++;
                    }
                    break;
                case DELIVERY_DRONE:
                    enemyDrones++;
                    // for rush dealing...
                    if (rc.getRoundNum() <= 300 && dist < distToClosestEarlyEnemyDroneWithUnit && info.isCurrentlyHoldingUnit()) {
                        closestEarlyEnemyDroneWithUnit = info;
                        distToClosestEarlyEnemyDroneWithUnit = dist;
                    }
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

        // shout rush units out once
        if (closestEarlyEnemyMiner != null && !idsOfRushUnitsCalledOut.contains(closestEarlyEnemyMiner.getID())) {
            // tell drones to chase!
            announceATTACK_ENEMY_UNIT(closestEarlyEnemyMiner);
            idsOfRushUnitsCalledOut.add(closestEarlyEnemyMiner.getID());
        }
        if (closestEarlyEnemyDroneWithUnit != null && !idsOfRushUnitsCalledOut.contains(closestEarlyEnemyDroneWithUnit.getID())) {
            announceATTACK_ENEMY_UNIT(closestEarlyEnemyDroneWithUnit);
            idsOfRushUnitsCalledOut.add(closestEarlyEnemyDroneWithUnit.getID());
        }



        int mainWallBots = 0;
        int wallBots = 0;
        int myDrones = 0;
        int designSchools = 0;
        int myLandscapers = 0;
        int fulfillmentCenters = 0;
        int vaporators = 0;
        for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyFriendlyRobots[i];
            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
            if (info.type == RobotType.LANDSCAPER) {
                myLandscapers++;
                if (dist <= 8 && dist != 4) {
                    wallBots++;
                }
                if (dist <= 2) {
                    mainWallBots++;
                }
            }
            if (info.type == RobotType.DELIVERY_DRONE) {
                myDrones++;
            }
            else if (info.type == RobotType.FULFILLMENT_CENTER) {
                fulfillmentCenters++;
                if (dist > HQ_LAND_RANGE && dist <= 8) {
                    wallBotsMax--;
                    // decrement one as a fulfillment center is in our wall area
                }
            }
            else if (info.type == RobotType.MINER) {
                miners++;
            }
            else if (info.type == RobotType.VAPORATOR) {
                vaporators++;
            }
            else if (info.type == RobotType.NET_GUN) {
                if (dist > HQ_LAND_RANGE && dist <= 8) {
                    wallBotsMax--;
                }
            }
            else if (info.type == RobotType.DESIGN_SCHOOL) {
                designSchools++;
                if (dist > HQ_LAND_RANGE && dist <= 8) {
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
        if ((!criedForDesignSchool || rc.getRoundNum() % 10 == 0) && rc.getRoundNum() >= 15 && designSchools == 0 && fulfillmentCenters > 0 && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost + 2) {
            if (vaporatorsBuilt > 0 || (gettingRushed)) {
                announceBUILD_A_SCHOOL();
                criedForDesignSchool = true;
            }
        }
        if ((!criedForFC || rc.getRoundNum() % 10 == 0) && fulfillmentCenters == 0 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost + 2) {
            announceBUILD_A_CENTER();
            criedForFC = true;
        }

        // if platform is closer to completion, we want everyone on terraforming duties or we have our wall
        // if later in match, announce terraform time if wall bots is not enough
        if ((wallBots >= wallBotsMax && rc.getRoundNum() % 10 == 0) || (rc.getRoundNum() >= 500 && rc.getRoundNum() % 10 == 0)) {
            if (wallBots < wallBotsMax) {
                announceTERRAFORM_ALL_TIME();
            }
        }

        // triggered if its really late or we see enough landscapers in vision
        if ((rc.getRoundNum() >= 1400 || (myLandscapers - 5 >= wallBotsMax && vaporators >= 10)) && rc.getRoundNum() % 10 == 0) {
            // every 15 rounds check if we have all our bots or not
            if (debug) System.out.println("I see " + myLandscapers + " landscapers and " + wallBots + " are on wall positions");
            if (wallBots <= wallBotsMax) {
                // announce we are walling in
                announcedWalledin = true;
            }
        }
        // if we are walling in, announce every 10 rounds
        if (announcedWalledin && rc.getRoundNum() % 10 == 0) {
            if (wallBots >= wallBotsMax) {
                // keep terraforming and walling in.
                announceTERRAFORM_AND_WALL_IN();
            }
            else {
                announceWALL_IN();
            }
        }

        // see enemy? ask drones to lock themselves in and defend, // FIXME not best strat when we dont have enough drones
        if (!criedForLockAndDefend && enemyDrones > 0) {
            //announceLOCK_AND_DEFEND();
            criedForLockAndDefend = true;
        }
        // no more enemies when previously there were enemies.
        else if (enemyDrones == 0 && criedForLockAndDefend) {
            //announceMessage(STOP_LOCK_AND_DEFEND);
            criedForLockAndDefend = false;

        }

        if (!gettingRushed && enemyDesignSchools > 0) {
            gettingRushed = true;
            // set this once and announce once
            announceMessage(GETTING_RUSHED_HELP);
        }

        // if we were rushed but no more design schools
        if (debug) System.out.println("Rushed? " + gettingRushed + " | Enemy... schools: " + enemyDesignSchools + ", netguns: " + enemyNetGuns + " inHQSpaceLandscapers: " + inHQSpaceEnemyLandscapers);
        if (gettingRushed && enemyDesignSchools == 0 && enemyNetGuns == 0 && inHQSpaceEnemyLandscapers == 0) {
            gettingRushed = false;
            announceMessage(NO_LONGER_RUSHED);
        }
        if (gettingRushed && rc.getRoundNum() % 10 == 0) {
            announceMessage(GETTING_RUSHED_HELP);
        }
        if (gettingRushed && enemyNetGuns == 0) {

        }

        // probably have a good eco going, let it be known that we are gonna do fast wall building now
        if (vaporatorsBuilt >= 30 && rc.getRoundNum() % 50 == 0 && mainWallBots >= 8) {
            //announceMessage(FAST_WALL_BUILD);
        }


        // if we see an enemy landscaper or enemy miner
        if (enemyLandscapers > 0 || enemyMiners > 0) {
            // announce I want drones and fulfillment center to build them if we have no drones and we dont know a center was built or every 20 turns
            // announce asap and then every 10 rounds
            if ((!criedForLandscapers || rc.getRoundNum() % 10 == 0) && wallBots < enemyLandscapers + enemyMiners + 1) {
                if (wallBots < 8) {
                    //announceWantLandscapers(8 - wallBots);
                    criedForLandscapers = true;
                }
            }
            if ((!criedForDroneHelp || rc.getRoundNum() % 10 == 0) && myDrones < enemyLandscapers && enemyNetGuns == 0) {
                announceBuildDronesNow(enemyLandscapers - myDrones);
                criedForDroneHelp = true;
            }
        }

        if (closestDroneBotWithUnit != null) {
            if (rc.canShootUnit(closestDroneBotWithUnit.getID())) {
                rc.shootUnit(closestDroneBotWithUnit.getID());
                if (debug) rc.setIndicatorDot(closestDroneBotWithUnit.location, 255, 50,190);
                //closestDroneBot.isCurrentlyHoldingUnit();
            }
        }
        // if we found a closest bot
        else if (closestDroneBot != null) {
            if (rc.canShootUnit(closestDroneBot.getID())) {
                rc.shootUnit(closestDroneBot.getID());
                if (debug) rc.setIndicatorDot(closestDroneBot.location, 255, 50,190);
                //closestDroneBot.isCurrentlyHoldingUnit();
            }
        }

        existsSoup = false;
        boolean buildBecauseNeedMiners = false;
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
                        int soupThere = msg[3];
                        int minersThere = msg[4];
                        if (soupThere / (minersThere + 1) >= 300) {

                            buildBecauseNeedMiners = true;
                            if (debug) System.out.println("Try to build miner because exists soup loc with not enough miners");
                            // put a hard limit on max miners
                            if (minersBuilt > 4 * vaporatorsBuilt) {
                                buildBecauseNeedMiners = false;
                            }
                        }

                    }
                    else if (msg[1] == RobotType.DESIGN_SCHOOL.ordinal()) {
                        designSchoolsBuilt++;
                    }
                }
            }
        }

        // decide on unit to build and set unitToBuild appropriately
        decideOnUnitToBuild();
        // if we are to build a unit, proceed
        if (buildBecauseNeedMiners || (rc.getRoundNum() % 30 == 0 && miners == 0 && rc.getRoundNum() >= 300)) {
            if (rc.getRoundNum() > 300 || fulfillmentCenters > 0) {
                unitToBuild = RobotType.MINER;
            }
        }
        if (rc.getTeamSoup() >= 500 + 250 + 70 && rc.getRoundNum() % 10 == 0 && rc.getRoundNum() <= 500) {
            if (debug) System.out.println("Try to build because early and 750 + 70 soup");
            unitToBuild = RobotType.MINER;
        }
        if  (rc.getRoundNum() < 50 && minersBuilt < 4) {
            unitToBuild = RobotType.MINER;
        }
        // if we have minerCost + FC Cost, build miner and hope it builds FC. dont build more miners...
        if (unitToBuild != null) {
            unitToBuild = RobotType.MINER;
            // proceed with building unit using default heurstics
            if (debug) System.out.println("closest soup: " + closestSoupLoc);
            build(closestSoupLoc);
        }
        // if we see no miners and no soup is heard, build miner
        /*
        else if (miners == 0 && rc.getTeamSoup() >= RobotType.MINER.cost && !existsSoup) {
            unitToBuild = RobotType.MINER;
            build(closestSoupLoc);
            announceTERRAFORM_ALL_TIME(); // get everyone back
        }*/

        if (rc.getRoundNum() % 20 == 0 && rc.getRoundNum() >= 1800 && surroundedByFloodRound == -1) {
            announceDroneAttack();
        }
        if (rc.getRoundNum() >= 2000 && rc.getRoundNum() % 250 == 0 && surroundedByFloodRound == -1) {
            announceMessage(SWARM_IN);
        }
        if (rc.getRoundNum() >= 1800 && rc.getRoundNum() % 10 == 0 && surroundedByFloodRound == -1) {
            announceMessage(SWARM_WITH_UNITS);
        }
        if (surroundedByFlood() && (surroundedByFloodRound == -1 || rc.getRoundNum() % 10 == 0) && rc.getRoundNum() >= 1700) {
            surroundedByFloodRound = rc.getRoundNum();
            announceMessage(GET_DEFEND_DRONES); // get drones back for drone wall
            announceMessage(LOCK_AND_DEFEND);
        }



    }

    static boolean surroundedByFlood() throws GameActionException{

        for (int i = 0; i < 6; i++) {
            MapLocation x1 = new MapLocation(HQLocation.x + i - 3, HQLocation.y + 3);
            MapLocation x2 = new MapLocation(HQLocation.x + i - 3, HQLocation.y - 3);
            MapLocation x3 = new MapLocation(HQLocation.x + 3, HQLocation.y - 3 + i);
            MapLocation x4 = new MapLocation(HQLocation.x - 3, HQLocation.y - 3 + i);
            if (rc.canSenseLocation(x1) && rc.senseFlooding(x1) == false) {
                return false;
            }
            if (rc.canSenseLocation(x2) && rc.senseFlooding(x2) == false) {
                return false;
            }
            if (rc.canSenseLocation(x3) && rc.senseFlooding(x3) == false) {
                return false;
            }
            if (rc.canSenseLocation(x4) && rc.senseFlooding(x4) == false) {
                return false;
            }
        }


        return true;
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



    static void announceATTACK_ENEMY_UNIT(RobotInfo enemy) throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), ATTACK_ENEMY_UNIT_FOR_RUSH, hashLoc(enemy.location), enemy.getID(), enemy.type.ordinal(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING TO ATTACK UNIT AT " + enemy.location + " | ID: "+ enemy.ID);
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    static void announceMessage(int msg) throws GameActionException {
        // STOP_LOCK_AND_DEFEND
        int[] message = new int[] {generateUNIQUEKEY(), msg, rc.getTeamSoup(), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING " + msg);
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    static void announceTERRAFORM_AND_WALL_IN() throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), TERRAFORM_AND_WALL_IN, rc.getTeamSoup(), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING WALL IN!!!");
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    // announce that we want landscapers to start building HQ wall
    static void announceWALL_IN() throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), WALL_IN, rc.getTeamSoup(), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING WALL IN!!!");
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
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
    // announce that we want all units to work on platform now
    static void announceTERRAFORM_ALL_TIME() throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), TERRAFORM_ALL_TIME, randomInt(), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING ALL TERRAFORM!!!");
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
        if (vaporatorsBuilt * 1 + 4 >= minersBuilt && designSchoolsBuilt > 0 && existsSoup && fulfillmentCentersBuilt > 0) {
            unitToBuild = RobotType.MINER;
            return;
        }
        /*
        if (rc.getRoundNum() % 30 == 0 && rc.getTeamSoup() >= 350) {
            unitToBuild = RobotType.MINER;
        }*/



    }

    public static void setup() throws GameActionException {
        HQLocation = rc.getLocation();
        double optimalRadius = bestRToUse();
        MAX_TERRAFORM_DIST = (int) (optimalRadius * optimalRadius);

        //announceSelfLocation(1);
        // announce we are HQ, location, and max terraform dist.
        int[] message = new int[] {generateUNIQUEKEY(), rc.getType().ordinal(), hashLoc(rc.getLocation()), MAX_TERRAFORM_DIST, randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
        else {
        }



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

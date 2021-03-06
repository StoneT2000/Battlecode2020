package FinalChowBotStable;
import FinalChowBotStable.utils.HashTable;
import FinalChowBotStable.utils.LinkedList;
import FinalChowBotStable.utils.Node;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final Direction[] simpleDirections = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static final Direction[] directions = {Direction.CENTER, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};

    // maps ordinals to their robot types, 10 robot types, takes up 0-9
    static final RobotType[] ordinalToType = {RobotType.HQ, RobotType.MINER, RobotType.REFINERY, RobotType.VAPORATOR,
            RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.LANDSCAPER, RobotType.DELIVERY_DRONE, RobotType.NET_GUN, RobotType.COW};

    static MapLocation SoupLocation; // stores a target soup location to go and mine

    static MapLocation HQLocation; // our HQ location

    static LinkedList<MapLocation> enemyHQLocations =  new LinkedList<>(); // list of possibly enemy HQ locations
    static MapLocation enemyBaseLocation = null; // the enemy HQ location

    static MapLocation islandCenter = null; // island center to put miners on to attack drone walls
    static MapLocation islandCenterSide = null; // island center to put miners on to attack drone walls

    static final int MAX_CRUNCH_ROUNDS = 15;

    static int turnCount;
    static final boolean debug = false;
    static final int UNIQUEKEY = -1234917945;
    static Team enemyTeam; // enemy team enum

    static final int BASE_WALL_DIST = 1;

    static int closestToTargetLocSoFar = 9999999;
    // usually the position to move towards
    static MapLocation targetLoc;
    static Direction lastDirMove = Direction.NORTH;
    static MapLocation lastWallChecked = null;
    static LinkedList<MapLocation> lastLocs = new LinkedList<>();

    static int roundsSinceLastResetOfClosestTargetdist = 0;

    // SIGNAL Codes
    // 0 - 9 are for announcing what robot type was just created
    static final int ANNOUNCE_SOUP_LOCATION = 10;
    static final int NEED_LANDSCAPERS_FOR_DEFENCE = 11;
    static final int DRONES_ATTACK = 12;
    static final int ANNOUNCE_ENEMY_BASE_LOCATION = 13;
    static final int BUILD_DRONES = 14;
    static final int NEED_DRONES_FOR_DEFENCE = 15;
    static final int BUILD_DRONE_NOW = 16;
    static final int TERRAFORM_ALL_TIME = 17; // tell units we are focusing on terraforming
    static final int ANNOUNCE_NOT_ENEMY_BASE = 18; // tell us its not a enemy HQ
    static final int BUILD_A_SCHOOL = 19;
    static final int BUILD_A_CENTER = 20;
    static final int LOCK_AND_DEFEND = 21; // tell drones to lock and defend position
    static final int WALL_IN = 22; // tell landscapers and drones to build wall
    static final int TERRAFORM_AND_WALL_IN = 23; // terraform and wall when possible
    static final int STOP_LOCK_AND_DEFEND = 24; // tell drones to stop defending so hard.
    static final int SWARM_IN = 25; // tell drones to swarm in with their UNITS!!!

    static final int GETTING_RUSHED_HELP = 26; // HQ screaming for help
    static final int NO_LONGER_RUSHED = 27; // HQ saying not rushed anymore!

    static final int I_AM_DESIGNATED_BUILDER = 28; // miners saying tthey go back to build

    static final int SWARM_WITH_UNITS = 29;

    static final int ATTACK_ENEMY_UNIT_FOR_RUSH = 30; // tell drones where to go to defend rush

    static final int FAST_WALL_BUILD = 31; // tell landscapers on wall that they are ordered to disintegrate when they get 25 dirt

    static final int NET_GUN_LOCATION = 32;

    static final int GET_DEFEND_DRONES = 33;

    static final int ATTACKED_ENEMY_WALL = 34;

    static final int NO_LANDSCAPERS_LEFT_ON_ENEMY_HQ = 35;

    static int thisLandScapersDesiredHeightOffset = 0;

    static int MAX_TERRAFORM_DIST = 134; // was 94

    static int DESIRED_ELEVATION_FOR_TERRAFORM = 3;

    static int HQ_LAND_RANGE = 5; // how big in r2 HQ's land (that is untouched untill walling) is

    static int DROP_ZONE_RANGE_OF_HQ = 72; // how far away are we allowed to drop our units for attack from HQ.

    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        enemyTeam = rc.getTeam().opponent();
        turnCount = 0;
        if (debug) {
            System.out.println("Created " + rc.getType().ordinal() + ": " + rc.getType() + " init bytecount: " + Clock.getBytecodeNum());
        }

        // run setup code
        switch (rc.getType()) {
            case HQ:                 HQ.setup();                break;
            case MINER:              Miner.setup();             break;
            case REFINERY:           Refinery.setup();          break;
            case VAPORATOR:          Vaporator.setup();         break;
            case DESIGN_SCHOOL:      DesignSchool.setup();      break;
            case FULFILLMENT_CENTER: FulfillmentCenter.setup(); break;
            case LANDSCAPER:         Landscaper.setup();        break;
            case DELIVERY_DRONE:     DeliveryDrone.setup();     break;
            case NET_GUN:            NetGun.setup();            break;
        }
        while (true) {
            turnCount += 1;
            DESIRED_ELEVATION_FOR_TERRAFORM = Math.max(calculateWaterLevels() + 3, 8) + thisLandScapersDesiredHeightOffset;
            if (rc.getRoundNum() <= 500) {
                DESIRED_ELEVATION_FOR_TERRAFORM = Math.max(calculateWaterLevels() + 3, 5) + thisLandScapersDesiredHeightOffset;
            }


            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                if (debug) System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation() + " | CD: " + rc.getCooldownTurns());
                switch (rc.getType()) {
                    case HQ:                 HQ.run();                break;
                    case MINER:              Miner.run();             break;
                    case REFINERY:           Refinery.run();          break;
                    case VAPORATOR:          Vaporator.run();         break;
                    case DESIGN_SCHOOL:      DesignSchool.run();      break;
                    case FULFILLMENT_CENTER: FulfillmentCenter.run(); break;
                    case LANDSCAPER:         Landscaper.run();        break;
                    case DELIVERY_DRONE:     DeliveryDrone.run();     break;
                    case NET_GUN:            NetGun.run();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void setTargetLoc(MapLocation loc) {
        // if target is null or doesn't equal our new loc, set target
        if (targetLoc == null || !targetLoc.equals(loc)) {
            targetLoc = loc;
            closestToTargetLocSoFar = 99999999;
            if (targetLoc != null) {
                rc.getLocation().distanceSquaredTo(targetLoc);
            }
        }
    }

    static void rotateCircularly(MapLocation target) {
        if (rc.getID() % 2 == 0) {
            setTargetLoc(rc.adjacentLocation(rc.getLocation().directionTo(target).opposite().rotateRight().rotateRight()));
        }
        else {
            setTargetLoc(rc.adjacentLocation(rc.getLocation().directionTo(target).opposite().rotateLeft().rotateLeft()));
        }
    }
    static void rotateCircularlyOneDirection(MapLocation target) {
        setTargetLoc(rc.adjacentLocation(rc.getLocation().directionTo(target).opposite().rotateLeft().rotateLeft()));
    }
    static Direction getBugPathMove(MapLocation target, HashTable<Direction> dangerousDirections) throws GameActionException {
        roundsSinceLastResetOfClosestTargetdist++;
        if (rc.getLocation().equals(target)) {
            return Direction.CENTER;
        }
        // every 20 rounds, reset the closest distance
        if (roundsSinceLastResetOfClosestTargetdist >= 20) {
            roundsSinceLastResetOfClosestTargetdist = 0;
            closestToTargetLocSoFar = 99999999;
        }

        Direction dir = lastDirMove;
        Direction greedyDir = null;
        Direction wallDir = lastDirMove;
        if (lastWallChecked != null) {
            wallDir = rc.getLocation().directionTo(lastWallChecked);
            dir = rc.getLocation().directionTo(lastWallChecked);
        }

        boolean foundNonDangerousDir = false;
        boolean wallDirSet = false;
        boolean lastWallUpdated = false;
        int closestGreedyDist = 99999999;
        if (debug) System.out.println("Last dir: " + lastDirMove + " | Last Wall checked " + lastWallChecked + " | Closest Target Dist So Far " + closestToTargetLocSoFar);
        for (int i = 8; --i >= 0; ) {
            if (debug) System.out.println("Checking dir: " + dir);
            if (!dangerousDirections.contains(dir)) {
                MapLocation greedyLoc = rc.adjacentLocation(dir);
                int greedyDist = greedyLoc.distanceSquaredTo(target);

                // if it is closer than the closest we have ever been, we can sense it as well and its not flooding
                if (greedyDist < closestGreedyDist && rc.canSenseLocation(greedyLoc) && !rc.senseFlooding(greedyLoc)) {
                    if (debug) System.out.println(dir +" is greedier " + greedyDist);
                    // if we can move there
                    if (rc.canMove(dir)) {
                        if (debug) System.out.println(dir +" can move ");
                        // update and move
                        closestGreedyDist = greedyDist;
                        greedyDir = dir;
                        foundNonDangerousDir = true;
                    }
                }
                if (!wallDirSet) {
                    if (rc.canSenseLocation(greedyLoc) && !rc.senseFlooding(greedyLoc) && rc.canMove(dir)) {
                        wallDir = dir;
                        wallDirSet = true;
                        foundNonDangerousDir = true;
                        if (!lastWallUpdated) {
                            // not updated ever?
                            lastWallChecked = null;
                        }
                        if (debug) System.out.println(dir + ": new wall near direction now set ");
                    }
                    else {
                        lastWallChecked = greedyLoc; // last wall checked that is blocked
                        lastWallUpdated = true;
                    }
                }
            }
            else {
                if (debug) System.out.println("Dir " + dir + " is dangerous!");
            }
            dir = dir.rotateRight();

        }
        if (closestGreedyDist < closestToTargetLocSoFar) {
            closestToTargetLocSoFar = closestGreedyDist;
            lastDirMove = greedyDir;
            return greedyDir;
        }

        if (foundNonDangerousDir) {
            lastDirMove = wallDir;
            return wallDir;
        }
        else {
            return Direction.CENTER;
        }

        // closestToTargetLocSoFar
    }

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    // store hq location
    static void storeHQLocationAndGetConstants() throws GameActionException {
        int roundCheck = 1;
        theLoop: {
            while (HQLocation == null && roundCheck < 10) {
                if (rc.getRoundNum() <= roundCheck) {
                    break;
                }
                Transaction[] blocks = rc.getBlock(roundCheck);
                for (int i = blocks.length; --i >= 0; ) {
                    int[] msg = blocks[i].getMessage();
                    decodeMsg(msg);
                    if (isOurMessage((msg))) {
                        if (msg[1] == 0) {
                            HQLocation = parseLoc(msg[2]);
                            if (debug) System.out.println("Stored HQ Location: " +HQLocation + " | len " + msg.length + " | round: " + roundCheck);
                            MAX_TERRAFORM_DIST = msg[3];
                            break theLoop;
                        }
                    }
                }
                roundCheck++;
            }
        }
    }

    // store possible enemy hq positions into list
    static void storeEnemyHQLocations() throws GameActionException {
        // flip vertical, horizontal, and both

        if (HQLocation != null) {
            int flippedY = rc.getMapHeight() - HQLocation.y - 1;
            int flippedX = rc.getMapWidth() - HQLocation.x - 1;
            if (rc.getID() % 2 == 0) {
                enemyHQLocations.add(new MapLocation(HQLocation.x, flippedY));
                enemyHQLocations.add(new MapLocation(flippedX, flippedY));
                enemyHQLocations.add(new MapLocation(flippedX, HQLocation.y));
            }
            else {
                enemyHQLocations.add(new MapLocation(flippedX, HQLocation.y));
                enemyHQLocations.add(new MapLocation(flippedX, flippedY));
                enemyHQLocations.add(new MapLocation(HQLocation.x, flippedY));


            }
        }
    }

    /**
     * Code to deal with blockchain communication
     * key generator and scrambler to sign off messages whilst retaining content?
     * a key checker to check if message is our own
     * a decoder to unscramble a message
     * always decode -> check -> use data
     */
    static boolean isOurMessage(int[] msg) {
        return msg[0] == UNIQUEKEY;
    }

    // decodes the message to its original form
    static void decodeMsg(int[] msg) {
        // decode...
    }
    static void encodeMsg(int[] msg) {
        // encode...
    }

    // generate a unique key for this game to sign off messages
    static int generateUNIQUEKEY() {
        return UNIQUEKEY;
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
    // announces location of robot and type into block chain hopefully
    static boolean announceSelfLocation(int cost) throws GameActionException {

        // announce robot type, and x, y coords
        // sign it off with a UNIQUEKEY
        int[] message = new int[] {generateUNIQUEKEY(), rc.getType().ordinal(), hashLoc(rc.getLocation()),randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (rc.canSubmitTransaction(message, cost)) {
            rc.submitTransaction(message, cost);
            return true;
        }
        else {
            return false;
        }
    }

    static int randomInt() {
        return (int) (Math.random() * 10000000);
    }

    // announces the soup location with that cost, how ,uch soup is nearby and miners nearby
    static boolean announceSoupLocation(MapLocation loc, int cost, int soupCountApprox, int minerCount) throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), ANNOUNCE_SOUP_LOCATION, hashLoc(loc), soupCountApprox, minerCount, randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING LOCATION " + loc + " hash " + hashLoc(loc));

        if (rc.canSubmitTransaction(message, cost)) {
            rc.submitTransaction(message, cost);
            return true;
        }
        else {
            return false;
        }
    }

    static void announceNET_GUN_LOCATION(MapLocation loc) throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), NET_GUN_LOCATION, hashLoc(loc), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING NET GUN LOCATION " + loc + " hash " + hashLoc(loc));

        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    // announce location of enemy base
    static boolean announceEnemyBase(MapLocation loc) throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), ANNOUNCE_ENEMY_BASE_LOCATION, hashLoc(loc), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING ENEMY BASE LOCATION " + loc + " hash " + hashLoc(loc));

        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
            return true;
        }
        else {
            return false;
        }
    }

    // announce if location is not a enemy base
    // units receiving this should remove the location from their list of enemyHQLocations
    static boolean announceNotEnemyBase(MapLocation loc) throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), ANNOUNCE_NOT_ENEMY_BASE, hashLoc(loc), randomInt(), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING NOT ENEMY BASE LOCATION " + loc + " hash " + hashLoc(loc));

        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
            return true;
        }
        else {
            return false;
        }
    }

    // checks for updates to enemy HQ location
    static void checkForEnemyBasesInBlocks(Transaction [] transactions) throws GameActionException {
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                if ((msg[1] ^ ANNOUNCE_ENEMY_BASE_LOCATION) == 0) {
                    enemyBaseLocation = parseLoc(msg[2]);
                }
                else if ((msg[1] ^ ANNOUNCE_NOT_ENEMY_BASE) == 0) {
                    // remove said base from enemy base locations
                    MapLocation notBaseLocation = parseLoc(msg[2]);

                    // iterate over list
                    Node<MapLocation> node = enemyHQLocations.head;

                    Node<MapLocation> nodeToRemove = null;
                    if (node != null) {
                        for (int j = 0; j++ < enemyHQLocations.size; ) {
                            if (node.val.equals(notBaseLocation)) {
                                nodeToRemove = node;
                                break;
                            }
                            node = node.next;

                        }
                    }
                    if (nodeToRemove != null) {
                        enemyHQLocations.remove(nodeToRemove);
                    }
                }
            }
        }
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    // attempts to run out of the water if it was in it
    static void getOutOfWater() throws GameActionException {
        if (rc.senseFlooding(rc.getLocation())) {
            if (debug) System.out.println("About to drown, trying to jump out");
            for (int i = directions.length; --i >= 1; ) {
                tryMove(directions[i]);
            }
        }
    }

    // tries to build robot in given direction.
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static int calculateWaterLevels() {
        int waterLevel = 0;
        //e0.0028x − 1.38sin (0.00157x − 1.73) + 1.38sin ( − 1.73) − 1,
        waterLevel = (int) Math.pow(Math.E, 0.0028 * rc.getRoundNum() - 1.38 * Math.sin(0.00157 * rc.getRoundNum() - 1.73) + 1.38*Math.sin(-1.73)) - 1;
        return waterLevel;
    }
    static double calculateWaterLevelChangeRate() {
        // \left(e^{0.0028x-1.38\sin(0.00157x-1.73)+1.38\sin(-1.73)}\right)\cdot\left(0.0028-1.38\cos\left(0.00157x-1.73\right)0.00157\right)
        double waterLevelChange = 0;
        int x = rc.getRoundNum();
        waterLevelChange = Math.pow(Math.E, (0.0028 * x - 1.38 * Math.sin(0.00157 * x - 1.73) + 1.38 * Math.sin(-1.73))) *
                (0.0028 - 1.38 * Math.cos(0.00157 * x - 1.73) * 0.00157);
        return waterLevelChange;
    }
    static boolean isBuilding(RobotInfo info) {
        if (info.type == RobotType.HQ || info.type == RobotType.FULFILLMENT_CENTER || info.type == RobotType.NET_GUN || info.type == RobotType.REFINERY || info.type == RobotType.DESIGN_SCHOOL || info.type == RobotType.VAPORATOR) {
            return true;
        }
        return false;
    }
    // location related
    // hash a location
    static int hashLoc(MapLocation loc) {
        return loc.x  + (loc.y << 6);
    }
    // parse a hashed location
    static MapLocation parseLoc(int hash) {
        return new MapLocation(hash % 64, hash >> 6);
    }
    static boolean isDigLocation(MapLocation loc) {
        //if (loc.x)
        if (loc.x % 3 == HQLocation.x % 3  && loc.y % 3 == HQLocation.y % 3 && loc.distanceSquaredTo(HQLocation) > 9) {
            return true;
        }
        return false;
    }

    static boolean locHasLandAdjacent(MapLocation loc) throws GameActionException {
        int i = 0;
        Direction dir = Direction.NORTH;
        while (i++ < 8) {
            MapLocation adjLoc = loc.add(dir);
            if (rc.canSenseLocation(adjLoc)) {
                if (!rc.senseFlooding(adjLoc)) {
                    return true;
                }
            }
            dir = dir.rotateRight();
        }
        return false;
    }
    static boolean locHasFloodAdjacent(MapLocation loc) throws GameActionException {
        int i = 0;
        Direction dir = Direction.NORTH;
        while (i++ < 8) {
            MapLocation adjLoc = loc.add(dir);
            if (rc.canSenseLocation(adjLoc)) {
                if (rc.senseFlooding(adjLoc)) {
                    return true;
                }
            }
            dir = dir.rotateRight();
        }
        return false;
    }
    static double minArea = Math.PI * 134;

    // r is not r2, is regular radius
    // used to determine what MAX_TERRAFORM_DIST should be so our platform is always at around minArea large.
    static boolean checkIfRadiusIsGood(double r) {
        //11.5758369028 = sqrt(134)
        double dx = -1;
        double dy = -1;
        dx = Math.min(Math.abs(HQLocation.x - 0), Math.abs(HQLocation.x - rc.getMapWidth()));
        dy = Math.min(Math.abs(HQLocation.y - 0), Math.abs(HQLocation.y - rc.getMapHeight()));
        if (dx >= r && dy >= r) {
            // enclosed, not intersecting map borders...
            if (r * r >= 134) {
                return true; // this is a valid r
            }
        }
        else if (dx >= r && dy < r) {
            // y-wise, intersecting border
            double angle = Math.acos(dy / r);
            double sy = Math.sin(angle) * r;
            double trueArea = Math.PI * r * r - angle * r * r + dy * sy;
            if (trueArea >= minArea - 1) {
                return true;
            }
            else {
                return false;
            }
        }
        else if (dx < r && dy >= r) {
            // x-wise, intersecting border
            double angle = Math.acos(dx / r);
            double sx = Math.sin(angle) * r;
            double trueArea = Math.PI * r * r - angle * r * r + dx * sx;
            if (trueArea >= minArea - 1) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            //y-wise and x-wise, intersecting border
            // determine first if corner point is in radius or not
            int[][] cornerLocs = {{0, 0}, {0, rc.getMapHeight()}, {rc.getMapWidth(), 0}, {rc.getMapWidth(), rc.getMapHeight()}};
            int[] myLoc = {rc.getLocation().x, rc.getLocation().y};
            double r2 = r * r;
            int[] cornerLocInRadius = null;
            for (int i = cornerLocs.length; --i>= 0; ) {
                int[] cornerLoc = cornerLocs[i];
                if (r2Dist(cornerLoc, myLoc) <= r2) {
                    cornerLocInRadius = cornerLoc;
                    break;
                }

            }

            if (cornerLocInRadius != null) {
                // corner is in the radius, we need to treat it differently then.
                double angley = Math.acos(dy / r);
                double anglex = Math.acos(dx / r);
                double sy = Math.sin(angley) * r;
                double sx = Math.sin(angley) * r;
                double areaOfTriangleY = dy * sy / 2;
                double areaOfTriangleX = dx * sx / 2;
                double enclosedRect = Math.abs((cornerLocInRadius[0] - myLoc[0]) * (cornerLocInRadius[1] - myLoc[1]));
                double sectorArea = (Math.PI / 2 + anglex + angley) * r * r / 2;

                double trueArea = Math.PI * r * r - sectorArea + enclosedRect + areaOfTriangleX + areaOfTriangleY;
                if (trueArea >= minArea - 1) {
                    return true;
                }
                else {
                    return false;
                }

            }
            // no corner in radius, solve as usual
            else {
                double angley = Math.acos(dy / r);
                double sy = Math.sin(angley) * r;
                double anglex = Math.acos(dx / r);
                double sx = Math.sin(anglex) * r;
                double trueArea = Math.PI * r * r - angley * r * r + dx * sx - anglex * r * r + dy * sy;
                if (trueArea >= minArea - 1) {
                    return true;
                }
                else {
                    return false;
                }
            }

        }
        return false;
    }

    static boolean hasEmptyTileAround(MapLocation loc) throws GameActionException {
        for (Direction dir: directions) {
            MapLocation checkLoc = loc.add(dir);
            if (rc.canSenseLocation(checkLoc)) {
                if (!rc.senseFlooding(checkLoc)) {
                    return true;
                }
            }
            else if (rc.onTheMap(loc)){
                return false;
            }
        }
        return false;
    }
    static double r2Dist(int[] x1, int[] x2) {
        double dx = x2[0] - x1[0];
        double dy = x2[1] - x1[1];
        return dx * dx + dy * dy;
    }

    static double bestRToUse() {
        double bestRadius = 11;
        while (bestRadius < 20) {
            // test 11 + i;
            boolean good = checkIfRadiusIsGood(bestRadius);
            if (debug) System.out.println("Radius: " + (bestRadius) + " is " + good);
            if (good) {
                break;
            }
            bestRadius += 0.5;
        }
        return bestRadius;
    }

    static RobotInfo getClosestRobot(LinkedList<RobotInfo> robots, MapLocation sourceLoc) {
        if (robots.size == 0) {
            return null;
        }
        int closestDist = 99999999;
        RobotInfo closestRobot = null;
        Node<RobotInfo> node = robots.head;
        while (node != null) {
            int dist = sourceLoc.distanceSquaredTo(node.val.location);
            if (dist < closestDist) {
                closestDist = dist;
                closestRobot = node.val;
            }
            node = node.next;
        }
        return closestRobot;
    }

    static RobotInfo getClosestRobot(HashTable<RobotInfo> robots, MapLocation sourceLoc) {
        if (robots.size == 0) {
            return null;
        }
        int closestDist = 99999999;
        RobotInfo closestRobot = null;
        Node<RobotInfo> node = robots.next();
        while (node != null) {
            if (debug) System.out.println("Robot at " + node.val.location);
            int dist = sourceLoc.distanceSquaredTo(node.val.location);
            if (dist < closestDist) {
                closestDist = dist;
                closestRobot = node.val;
            }
            node = robots.next();
        }
        robots.resetIterator();
        return closestRobot;
    }
    static MapLocation getClosestLoc(HashTable<MapLocation> robotLocs, MapLocation sourceLoc) {
        if (robotLocs.size == 0) {
            return null;
        }
        int closestDist = 99999999;
        MapLocation closestRobotLoc = null;
        Node<MapLocation> node = robotLocs.next();
        while (node != null) {
            if (debug) System.out.println("Robot Loc at " + node.val);
            int dist = sourceLoc.distanceSquaredTo(node.val);
            if (dist < closestDist) {
                closestDist = dist;
                closestRobotLoc = node.val;
            }
            node = robotLocs.next();
        }
        robotLocs.resetIterator();
        return closestRobotLoc;
    }

    static boolean locHasAdjacentBuildableLand(MapLocation loc) throws GameActionException {
        int i = 0;
        Direction dir = Direction.NORTH;
        int baseElevation = rc.senseElevation(loc);
        while (i++ < 8) {
            MapLocation adjLoc = loc.add(dir);
            if (rc.canSenseLocation(adjLoc)) {
                int elevation = rc.senseElevation(adjLoc);
                if (!rc.senseFlooding(adjLoc) && !rc.isLocationOccupied(adjLoc) && (elevation + 3 >= baseElevation && baseElevation >= elevation - 3)) {
                    return true;
                }
            }
            dir = dir.rotateRight();
        }
        return false;
    }
    static void setIslandCenterAndOtherConstants() {
        if (enemyBaseLocation != null) {
            islandCenter = new MapLocation(enemyBaseLocation.x, enemyBaseLocation.y);
            Direction enemyDirToHQ = enemyBaseLocation.directionTo(HQLocation);
            islandCenter = islandCenter.add(enemyDirToHQ);
            islandCenter = islandCenter.add(enemyDirToHQ);
            islandCenter = islandCenter.add(enemyDirToHQ);
            islandCenter = islandCenter.add(enemyDirToHQ);
            islandCenter = islandCenter.add(enemyDirToHQ); // 5 tiles away
        }
    }

}

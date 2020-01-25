package farmbot1;
import battlecode.common.*;
import Chow3.utils.LinkedList;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final Direction[] simpleDirections = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static final Direction[] directions = {Direction.CENTER, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};
    // maps ordinals to their robot types, 10 robot types, takes up 0-9
    static final RobotType[] ordinalToType = {RobotType.HQ, RobotType.MINER, RobotType.REFINERY, RobotType.VAPORATOR,
            RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.LANDSCAPER, RobotType.DELIVERY_DRONE, RobotType.NET_GUN, RobotType.COW};
    // static MapLocation[] SoupLocations = new MapLocation[100];
    static MapLocation SoupLocation; // stores a target soup location to go and mine
    static MapLocation HQLocation;
    static LinkedList<MapLocation> enemyHQLocations =  new LinkedList<>();
    static int turnCount;
    static final boolean debug = false;
    static final int UNIQUEKEY = -92390123;
    static Team enemyTeam;

    // signal codes
    static final int ANNOUNCE_SOUP_LOCATION = 10;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
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
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                if (debug) System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
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

    // returns best move to greedily move to target. ALWAYS returns some direction
    static Direction getGreedyMove(MapLocation target) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(target);
        // FIXME: Remove senseFlooding in future
        if (!rc.canMove(dir) || rc.senseFlooding((rc.adjacentLocation((dir))))) {
            int minDist = 999999;
            for (int i = directions.length; --i >= 0; ) {
                // if distance to target from this potential direction is smaller, set it
                int dist = target.distanceSquaredTo(rc.adjacentLocation(directions[i]));
                // FIXME: Remove sensefLooding in future
                if (dist < minDist && rc.canMove(directions[i]) && !rc.senseFlooding(rc.adjacentLocation(directions[i]))) {
                    dir = directions[i];
                    minDist = dist;
                    if (debug) System.out.println("I chose " + dir + " instead in order to go to " + target);
                }
            }
        }

        return dir;
    }

    /**
     * Stores HQ location sent out by HQ earlier
     */
    static void storeHQLocation() throws GameActionException {
        // gets the HQ of this unit;
        Transaction[] first = rc.getBlock(1);
        for (int i = first.length; --i >= 0;) {
            int[] msg = first[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                HQLocation = new MapLocation(msg[2], msg[3]);
                break;
            }
        }
    }
    static void storeEnemyHQLocations() throws GameActionException {
        // flip vertical, horizontal, and both

        int flippedY = rc.getMapHeight() - HQLocation.y - 1;
        int flippedX = rc.getMapWidth() - HQLocation.x - 1;
        enemyHQLocations.add(new MapLocation(HQLocation.x, flippedY));
        enemyHQLocations.add(new MapLocation(flippedX, HQLocation.y));
        enemyHQLocations.add(new MapLocation(flippedX, flippedY));
        if (debug) System.out.println("Flipped enemy pos? [" + flippedX + ", " + flippedY + "]");
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
    /**
        Announcement code
     */
    // announces location into block chain hopefully
    // announces type and location
    // returns true if submitted to block chain
    // 0: HQ, 1: MINER
    static boolean announceSelfLocation(int cost) throws GameActionException {

        // announce robot type, and x, y coords
        // sign it off with a UNIQUEKEY // could also store info in transaction cost
        int[] message = new int[] {generateUNIQUEKEY(), rc.getType().ordinal(), rc.getLocation().x, rc.getLocation().y};
        encodeMsg(message);
        // attempt to submit with
        if (rc.canSubmitTransaction(message, cost)) {
            rc.submitTransaction(message, cost);
            return true;
        }
        else {
            return false;
        }
    }

    // announces the location with that cost.
    static boolean announceSoupLocation(MapLocation loc, int cost, int soupCountApprox) throws GameActionException {
        // announce x y coords and round number,
        // and how many miners are there already?
        // how much soup left?
        int[] message = new int[] {generateUNIQUEKEY(), ANNOUNCE_SOUP_LOCATION, hashLoc(loc), soupCountApprox};
        encodeMsg(message);

        if (rc.canSubmitTransaction(message, cost)) {
            rc.submitTransaction(message, cost);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Read announcement code and store in SoupLocation the new soup location found
     * Store closest one
     */
    static void checkBlockForSoupLocations(Transaction[] transactions) throws GameActionException {
        int minDist = 99999;
        if (SoupLocation != null) {
            minDist = rc.getLocation().distanceSquaredTo(SoupLocation);
        }
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                // if it is announce SOUP location message
                if ((msg[1] ^ ANNOUNCE_SOUP_LOCATION) == 0) {
                    // TODO: do something with msg[4], the round number, determine outdatedness?
                    MapLocation potentialLoc = parseLoc(msg[2]);
                    int dist = rc.getLocation().distanceSquaredTo(potentialLoc);
                    if (dist < minDist) {

                        SoupLocation = potentialLoc;
                        minDist = dist;
                        if (debug) System.out.println("Found closer soup location in messages: " + SoupLocation);
                    }
                    else {
                        // already have soup location target that still exists, continue with mining it
                        // TODO: Do something about measuring how much soup is left, and announcing it.
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
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        // FIXME: Remove senseFlooding once API is fixed and canMove actually determines it
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    // attempts to run out of the water if it was in it
    static void getOutOfWater() throws GameActionException {
        if (rc.senseFlooding(rc.getLocation())) {
            if (debug) System.out.println("About to drown, trying to jump out");
            for (int i = directions.length; --i >= 1; ) {
                //MapLocation checkLoc = rc.adjacentLocation(directions[i]);
                tryMove(directions[i]);
            }
        }
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }


    // location related

    // checks if unit is within unit distance away from map edge
    // e.g withinMapEdge(Math.min(rc.getMapWidth()/4, rc.getMapHeight()/4))
    // would check if unit is closer to center or edge sorta.
    static boolean withinMapEdge(int distance) {
        if (rc.getLocation().x <= distance || rc.getLocation().y <= distance
                || rc.getLocation().x >= rc.getMapWidth() - distance || rc.getLocation().y >= rc.getMapHeight() - distance) {
            return true;
        }
        return false;
    }

    static int hashLoc(MapLocation loc) {
        return loc.x  + (loc.y << 6);
    }
    static MapLocation parseLoc(int hash) {
        return new MapLocation(hash % 64, hash >> 6);
    }

}
package bot1;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};
    static RobotType[] ordinalToType = {RobotType.HQ, RobotType.MINER};
    static MapLocation[] RefineryLocations = new MapLocation[1000];
    static int turnCount;
    static final boolean debug = true;
    static final int UNIQUEKEY = -92390123;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;
        if (debug) {
            System.out.println("Created " + rc.getType().ordinal() + ": " + rc.getType() + " init bytecount: " + Clock.getBytecodeNum());
        }

        // store HQ position of this miner
        getHome();
        // store enemy HQ position
        //storeEnemyHQ();
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
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
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

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
    }

    static void runNetGun() throws GameActionException {

    }

    // moves robot to a target greedily, null if no good positions (possibly blocked)
    // byte code cost is:
    static Direction moveToGreedy(MapLocation target) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(target);

        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
        }
        return dir;
    }

    // returns best move to greedily move to target. Returns null if greedy move doesn't work
    static Direction getGreedyMove(MapLocation target) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(target);

        if (!rc.isReady() || !rc.canMove(dir)) {
            dir = null;
        }
        return dir;
    }

    static void getHome() throws GameActionException {
        // gets the home of this unit and stores in known refineries list

    }

    static int generateUNIQUEKEY() {
        return UNIQUEKEY;
    }
    // announces location into block chain hopefully
    // announces type and location
    // 0: HQ, 1: MINER
    static void announceLocation(int cost) throws GameActionException {

        // announce robot type, and x, y coords
        // sign it off with a UNIQUEKEY
        int[] message = new int[] {generateUNIQUEKEY(), rc.getType().ordinal(), rc.getLocation().x, rc.getLocation().y};
        /*
            add some scramble function to scramble message a bit based on round
         */
        // attempt to submit with cost 2.
        if (rc.canSubmitTransaction(message, cost)) {

        }
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
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
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
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

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[10];
            for (int i = 0; i < 10; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }
}

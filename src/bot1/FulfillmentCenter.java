package bot1;

import battlecode.common.*;

public class FulfillmentCenter extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static boolean inEndGame = false;
    static int dronesBuilt = 0;
    static boolean confirmBuild = false;
    public static void run() throws GameActionException {
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        checkForBuildInfo(lastRoundsBlocks);
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
        }

        /* SCOUTING CODE */

        /* BIG BFS LOOP ISH */
        for (int i = 0; i < Constants.BFSDeltas24.length; i++) {
            int[] deltas = Constants.BFSDeltas24[i];
            MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
            // TODO: instead of canSenseLocation, maybe do the math and choose the right BFS deltas to iterate over
            if (rc.canSenseLocation(checkLoc)) {

            }
            else {
                // if we can no longer sense location, break out of for loop then as all other BFS deltas will be unsensorable
                break;
            }
        }

        if (rc.getRoundNum() % 5 == 0) {
            if (rc.getTeamSoup() >= 1100) {
                confirmBuild = true;
            }
        }
        if (rc.getTeamSoup() >= 850 && rc.getRoundNum() % 30 == 29 && rc.getRoundNum() >= 500) {
            confirmBuild = true;
        }
        if (dronesBuilt < 1 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost) {
            confirmBuild = true;
        }
        if (inEndGame && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 200) {
            confirmBuild = true;
        }
        if (confirmBuild) {
            boolean builtUnit = false;
            for (int i = 9; --i >= 1; ) {
                if (tryBuild(RobotType.DELIVERY_DRONE, buildDir)) {
                    builtUnit = true;
                    break;
                } else {
                    buildDir = buildDir.rotateRight();
                }
            }
            if (builtUnit) {
                dronesBuilt++;
            }
            buildDir = buildDir.rotateRight();
        }
        // make false for next round
        confirmBuild = false;
    }
    static void checkForBuildInfo(Transaction[] transactions) {
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                // if it is announce SOUP location message
                if ((msg[1] ^ BUILD_DRONES) == 0) {
                    inEndGame = true;
                }
                else if ((msg[1] ^ BUILD_DRONE_NOW) == 0) {
                    int origSoup = msg[2];
                    int soupSpent = origSoup - rc.getTeamSoup();
                    // if soup spent / number of landscapers needed is greater than cost
                    if (soupSpent / msg[3] < RobotType.DELIVERY_DRONE.cost) {
                        confirmBuild = true;
                    }

                }
            }
        }
    }
    public static void setup() throws GameActionException {
        storeHQLocation();
        announceSelfLocation(1);
        if (rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + RobotType.MINER.cost * 2) {
            boolean builtUnit = false;
            for (int i = 9; --i >= 1; ) {
                if (tryBuild(RobotType.DELIVERY_DRONE, buildDir)) {
                    builtUnit = true;
                    break;
                } else {
                    buildDir = buildDir.rotateRight();
                }
            }
            if (builtUnit) {
                dronesBuilt++;
            }
            buildDir = buildDir.rotateRight();
        }
    }
}

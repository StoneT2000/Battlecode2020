package Chow10;

import battlecode.common.*;

public class FulfillmentCenter extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static boolean inEndGame = false;
    static int dronesBuilt = 0;
    static int vaporatorsBuilt = 0;
    static boolean confirmBuild = false;
    static boolean terraformingTime = false;
    static boolean wallIn = false;
    public static void run() throws GameActionException {
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        checkForBuildInfo(lastRoundsBlocks);
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        RobotInfo[] nearbyFriendRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        int nearbyEnemyLandscapers = 0;
        int dronesNearby = 0;
        int enemyMiners =  0;
        int netGunsnearby = 0;
        // count bots nearby
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            if (info.getType() == RobotType.LANDSCAPER) {
                nearbyEnemyLandscapers++;
            }
            else if (info.getType() == RobotType.NET_GUN) {
                netGunsnearby++;
            }
            else if (info.getType() == RobotType.MINER) {
                enemyMiners ++;
            }
        }

        // count nearby drones, netGuns
        for (int i = nearbyFriendRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyFriendRobots[i];
            if (info.getType() == RobotType.DELIVERY_DRONE) {
                dronesNearby++;
            }

        }

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
        // build one asap
        if (dronesBuilt < 1 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost && netGunsnearby == 0) {
            confirmBuild = true;
            if (debug) System.out.println("Building first one");
        }
        // in end game keep building
        /*
        if (inEndGame && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 200) {
            confirmBuild = true;
        }
        */
        // build if there are enemy landscapers nearby
        if ((nearbyEnemyLandscapers + enemyMiners > dronesNearby) && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost) {
            confirmBuild = true;
            if (debug) System.out.println("Building to fight landscapers");
        }
        if (vaporatorsBuilt * 1 > dronesBuilt && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 350) {
            confirmBuild = true;
            if (debug) System.out.println("Building balance");
        }
        if (rc.getTeamSoup() > 1000 && rc.getRoundNum() % 2 == 1) {
            confirmBuild = true;
            if (debug) System.out.println("Building cuz excess soup");
        }
        if (netGunsnearby > 0) {
            if (debug) System.out.println("Not Building because netguns");
            confirmBuild = false;
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
        // make false for next round to be confirmed again
        confirmBuild = false;

        if (rc.getLocation().distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE && wallIn) {
            rc.disintegrate();
        }
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
                        if (debug) System.out.println("Building because HQ wants them");
                    }
                }
                else if (msg[1] == RobotType.VAPORATOR.ordinal()) {
                    vaporatorsBuilt++;
                }
                else if ((msg[1] ^ TERRAFORM_ALL_TIME) == 0) {
                    terraformingTime = true;
                }
                else if ((msg[1] ^ WALL_IN) == 0 || (msg[1] ^ TERRAFORM_AND_WALL_IN) == 0) {
                    wallIn = true;
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

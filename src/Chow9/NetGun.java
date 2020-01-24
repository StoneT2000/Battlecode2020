package Chow9;

import battlecode.common.*;

public class NetGun extends RobotPlayer {
    static boolean wallIn = true;
    public static void run() throws GameActionException {

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

        // if we found a closest enemy drone
        if (closestDroneBot != null) {
            if (rc.canShootUnit(closestDroneBot.getID())) {
                rc.shootUnit(closestDroneBot.getID());
                if (debug) rc.setIndicatorDot(closestDroneBot.location, 255, 50,190);
            }
        }

        /* BIG BFS LOOP ISH */
        for (int i = 0; i < Constants.BFSDeltas24.length; i++) {
            int[] deltas = Constants.BFSDeltas24[i];
            MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
            if (rc.canSenseLocation(checkLoc)) {

            }
            else {
                break;
            }
        }

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
                if ((msg[1] ^ WALL_IN) == 0 || (msg[1] ^ TERRAFORM_AND_WALL_IN) == 0) {
                    wallIn = true;
                }
            }
        }
    }
    public static void setup() throws GameActionException {
        storeHQLocation();
    }
}

package Chow9;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.Transaction;

public class Vaporator extends RobotPlayer {
    static boolean announcedSelf = false;
    static boolean wallIn = false;
    public static void run() throws GameActionException {

        if (!announcedSelf && rc.getTeamSoup() >= 1) {
            announceSelfLocation(1);
            announcedSelf = true;
        }
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        checkForBuildInfo(lastRoundsBlocks);

        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
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
        if (rc.getTeamSoup() >= 1) {
            announceSelfLocation(1);
            announcedSelf = true;
        }
    }
}

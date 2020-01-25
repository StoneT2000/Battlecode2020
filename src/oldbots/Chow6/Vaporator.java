package Chow6;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class Vaporator extends RobotPlayer {
    static boolean announcedSelf = false;
    public static void run() throws GameActionException {

        if (!announcedSelf && rc.getTeamSoup() >= 1) {
            announceSelfLocation(1);
            announcedSelf = true;
        }

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
    }
    public static void setup() throws GameActionException {
        if (rc.getTeamSoup() >= 1) {
            announceSelfLocation(1);
            announcedSelf = true;
        }
    }
}

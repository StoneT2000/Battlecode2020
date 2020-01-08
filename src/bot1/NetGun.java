package bot1;

import battlecode.common.*;

public class NetGun extends RobotPlayer {
    public static void run() throws GameActionException {
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        RobotInfo closestBot = null;
        int closestEnemyDist = 99999999;
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            // TODO: Check if info.getLocation() vs info.location is more efficient?
            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
            if (dist < closestEnemyDist) {
                closestEnemyDist = dist;
                closestBot = info;
            }
        }
        // if we found a closest bot
        if (closestBot != null) {
            if (rc.canShootUnit(closestBot.getID())) {
                rc.shootUnit(closestBot.getID());
                if (debug) rc.setIndicatorDot(closestBot.location, 255, 50,190);
            }
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
    }
    public static void setup() throws GameActionException {
        storeHQLocation();
    }
}

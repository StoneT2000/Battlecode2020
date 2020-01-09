package bot1;

import battlecode.common.*;

public class FulfillmentCenter extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static int dronesBuilt = 0;
    public static void run() throws GameActionException {
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
    public static void setup() throws GameActionException {
        storeHQLocation();
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

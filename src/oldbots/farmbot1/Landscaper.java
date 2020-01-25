package farmbot1;

import battlecode.common.*;

public class Landscaper extends RobotPlayer {
    static MapLocation targetLoc;
    public static void run() throws GameActionException {
        // atm, swarm at an enemy base or smth and just hella try to bury it
        // make a path as needed if short range path finding yields no good way to get around some wall

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
        storeEnemyHQLocations();
    }
}

package bot1;

import battlecode.common.*;

public class DeliveryDrone extends RobotPlayer {
    static final int DEFEND = 1;
    static final int ATTACK = 0;
    static int role = ATTACK;
    static MapLocation attackLoc;
    public static void run() throws GameActionException {
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        RobotInfo closestEnemy = null;
        int closestEnemyDist = 99999999;
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            switch (role) {
                case ATTACK:
                    switch(info.type) {
                        case LANDSCAPER:
                            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
                            if (dist < closestEnemyDist) {
                                closestEnemyDist = dist;
                                closestEnemy = info;
                            }
                            break;
                    }
                    break;
            }
        }


        /* SCOUTING CODE */

        /* BIG BFS LOOP ISH */
        /*
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
        }*/

        // if attacking, move towards nearest enemy
        if (role == ATTACK) {

            // if there is enemy, engage!
            if (closestEnemy != null) {
                int distToEnemy = rc.getLocation().distanceSquaredTo(closestEnemy.location);
                if (distToEnemy == 0) {
                    // we are on top, pick it up and prepare for destroy procedure
                }
            }
            // otherwise hover around attackLOC and fuzzy
            else {
                int distToAttackLoc = rc.getLocation().distanceSquaredTo(attackLoc);
                if (distToAttackLoc <= 9) {
                    //fuzzy
                    targetLoc = rc.adjacentLocation(randomDirection());
                }
                else {
                    targetLoc = attackLoc; // move towards attack loc first
                }
            }
        }

        // whatever targetloc is, try to go to it
        if (targetLoc != null) {
            Direction greedyDir = getGreedyMove(targetLoc); //TODO: should return a valid direction usually???
            if (debug) System.out.println("Moving to " + rc.adjacentLocation((greedyDir)) + " to get to " + targetLoc);
            tryMove(greedyDir); // wasting bytecode probably here
        }
    }
    public static void setup() throws GameActionException {
        storeHQLocation();
        storeEnemyHQLocations();
        attackLoc = HQLocation;
    }
}

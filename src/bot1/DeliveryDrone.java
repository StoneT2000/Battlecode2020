package bot1;

import battlecode.common.*;

public class DeliveryDrone extends RobotPlayer {
    static final int DEFEND = 1;
    static final int ATTACK = 0;
    static final int DUMP_BAD_GUY = 2;
    static int role = ATTACK;
    static MapLocation attackLoc;
    static MapLocation waterLoc;
    public static void run() throws GameActionException {
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        RobotInfo closestEnemyLandscaper = null;
        RobotInfo closestEnemyMiner = null;
        int closestEnemyLandscaperDist = 99999999;
        int closestEnemyMinerDist = 999999;
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            switch (role) {
                case ATTACK:
                    switch(info.type) {
                        case LANDSCAPER:
                            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
                            if (dist < closestEnemyLandscaperDist) {
                                closestEnemyLandscaperDist = dist;
                                closestEnemyLandscaper = info;
                                if (debug) System.out.println("Found closer enemy landscaper at " + info.location);
                            }
                            break;
                        case MINER:
                            int dist2 = rc.getLocation().distanceSquaredTo(info.getLocation());
                            if (dist2 < closestEnemyMinerDist) {
                                closestEnemyMinerDist = dist2;
                                closestEnemyMiner = info;
                                if (debug) System.out.println("Found closer enemy miner at " + info.location);
                            }
                            break;
                    }
                    break;
            }
        }


        /* SCOUTING CODE */

        /* BIG BFS LOOP ISH */
        int minDistToFlood = 999999999;
        for (int i = 0; i < Constants.BFSDeltas24.length; i++) {
            int[] deltas = Constants.BFSDeltas24[i];
            MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
            // TODO: instead of canSenseLocation, maybe do the math and choose the right BFS deltas to iterate over
            if (rc.canSenseLocation(checkLoc)) {
                if (rc.senseFlooding(checkLoc)) {
                    int dist = rc.getLocation().distanceSquaredTo(checkLoc);
                    if (dist < minDistToFlood) {
                        minDistToFlood = dist;
                        waterLoc = checkLoc;
                    }
                }
            }
            else {
                // if we can no longer sense location, break out of for loop then as all other BFS deltas will be unsensorable
                break;
            }
        }

        if (role == DUMP_BAD_GUY) {
            // if currently holding unit, it should be a bad guy
            if (debug) System.out.println("DUMPING BAD UNIT");
            if (rc.isCurrentlyHoldingUnit()) {
                // find water and drop that thing
                if (waterLoc != null) {
                    targetLoc = waterLoc;
                    if (rc.getLocation().isAdjacentTo(waterLoc)) {
                        // adjacent to waterLoc, drop that thing!
                        Direction dropDir = rc.getLocation().directionTo(waterLoc);
                        if (rc.canDropUnit(dropDir)) {
                            rc.dropUnit(dropDir);
                            role = ATTACK;
                        }
                    }
                }
                // TODO: doesnt know any water sources? do what then?
                else {
                    targetLoc = rc.adjacentLocation(randomDirection());
                }
            }
            else {
                // this shouldnt ever happen
            }
        }
        // if attacking, move towards nearest enemy
        else if (role == ATTACK) {

            // if there is enemy, engage!
            if (closestEnemyMiner != null || closestEnemyLandscaper != null) {
                RobotInfo enemyToEngage = closestEnemyLandscaper;
                if (enemyToEngage == null) enemyToEngage = closestEnemyMiner;

                if (debug) System.out.println("ENGAGING ENEMY at " + enemyToEngage.location);
                int distToEnemy = rc.getLocation().distanceSquaredTo(enemyToEngage.location);
                if (distToEnemy <= 2) {
                    // we are adjacent, pick it up and prepare for destroy procedure
                    if (rc.canPickUpUnit(enemyToEngage.getID())) {
                        rc.pickUpUnit(enemyToEngage.getID());
                        role = DUMP_BAD_GUY;
                        targetLoc = waterLoc;
                    }
                }
                else {
                    // not near enemy yet, set targetLoc to this so we move towards enemey.
                    targetLoc = enemyToEngage.location;
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
                    targetLoc = attackLoc; // move towards attack loc first if not near it yet.
                }
            }
        }

        // whatever targetloc is, try to go to it
        if (targetLoc != null) {
            Direction dir = rc.getLocation().directionTo(targetLoc);
            if (!rc.canMove(dir)) {
                int minDist = 999999;
                for (int i = directions.length; --i >= 0; ) {
                    // if distance to target from this potential direction is smaller, set it
                    int dist = targetLoc.distanceSquaredTo(rc.adjacentLocation(directions[i]));
                    if (dist < minDist && rc.canMove(directions[i]) && !rc.senseFlooding(rc.adjacentLocation(directions[i]))) {
                        dir = directions[i];
                        minDist = dist;
                        if (debug) System.out.println("I chose " + dir + " instead in order to go to " + targetLoc);
                    }
                }
            }

            if (debug) System.out.println("Moving to " + rc.adjacentLocation((dir)) + " to get to " + targetLoc);
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }
    public static void setup() throws GameActionException {
        storeHQLocation();
        storeEnemyHQLocations();
        attackLoc = HQLocation;
    }
}

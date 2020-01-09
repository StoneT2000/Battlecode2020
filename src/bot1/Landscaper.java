package bot1;

import battlecode.common.*;
import bot1.utils.*;

public class Landscaper extends RobotPlayer {
    static final int ATTACK = 0;
    static final int DEFEND_HQ = 1;
    static int role = DEFEND_HQ;
    static final int BASE_WALL_DIST = 2;
    static MapLocation enemyBaseLocation = null;
    static MapLocation bestWallLocForDefend = null;
    static MapLocation closestWallLocForDefend = null;
    static MapLocation leastElevatedWallLocForDefend = null;
    public static void run() throws GameActionException {
        // atm, swarm at an enemy base or smth and just hella try to bury it
        // make a path as needed if short range path finding yields no good way to get around some wall
        int waterLevel = calculateWaterLevels();
        /* BIG BFS LOOP ISH */

        int minDistToWall = 999999999;
        int minDistToBestBuildLoc = 99999999;
        int leastElevation = 999999999;
        for (int i = 0; i < Constants.BFSDeltas24.length; i++) {
            int[] deltas = Constants.BFSDeltas24[i];
            MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
            // TODO: instead of canSenseLocation, maybe do the math and choose the right BFS deltas to iterate over
            if (rc.canSenseLocation(checkLoc)) {
                switch(role) {
                    case DEFEND_HQ:
                        // find position in square around base to build wall
                        int dist = rc.getLocation().distanceSquaredTo(checkLoc);
                        if (bestWallLocForDefend == null && validBuildWallLoc(checkLoc)) {

                            // look for first elevation that is not good enough yet and higher than the water
                            int locElevation = rc.senseElevation(checkLoc);
                            if (locElevation < waterLevel + 10) {
                                if (dist < minDistToBestBuildLoc) {
                                    bestWallLocForDefend = checkLoc;
                                    minDistToBestBuildLoc = dist;
                                }
                            }
                            // also find least elevated build location
                            if (locElevation < leastElevation) {
                                leastElevatedWallLocForDefend = checkLoc;
                                leastElevation = locElevation;
                            }

                        }

                        // find closest wall as well
                        if (closestWallLocForDefend == null && validBuildWallLoc(checkLoc)) {

                            if (dist < minDistToWall) {
                                closestWallLocForDefend = checkLoc;
                                minDistToWall = dist;
                            }
                        }
                        break;
                }
            }
            else {
                // if we can no longer sense location, break out of for loop then as all other BFS deltas will be unsensorable
                break;
            }
        }


        if (role == ATTACK) {
            // find nearest enemy hq location to go and search and store map location
            Node<MapLocation> node = enemyHQLocations.head;
            Node<MapLocation> closestMaybeHQNode = enemyHQLocations.head;
            int minDistToMaybeHQ = 9999999;
            MapLocation closestMaybeHQ = null;
            if (node != null) {
                closestMaybeHQ = node.val;
                for (int i = 0; i++ < enemyHQLocations.size; ) {
                    int dist = rc.getLocation().distanceSquaredTo(node.val);
                    if (dist < minDistToMaybeHQ) {
                        minDistToMaybeHQ = dist;
                        closestMaybeHQ = node.val;
                        closestMaybeHQNode = node;
                    }
                    node = node.next;

                }
            }
            else {
                // dont swarm?
            }

            // if we can check location we are trying to head to, determine if its a enemy HQ or not
            if (rc.canSenseLocation(closestMaybeHQ)) {
                if (rc.isLocationOccupied(closestMaybeHQ)) {
                    RobotInfo unit = rc.senseRobotAtLocation(closestMaybeHQ);
                    if (unit.type == RobotType.HQ && unit.team == enemyTeam) {
                        // FOUND HQ!
                        enemyBaseLocation = closestMaybeHQ;
                        if (debug) System.out.println("FOUND ENEMY HQ AT " + closestMaybeHQ);
                    }
                    else {
                        // remove this location from linked list
                        enemyHQLocations.remove(closestMaybeHQNode);
                    }
                }
                else {
                    enemyHQLocations.remove(closestMaybeHQNode);
                }
            }
            if (!rc.getLocation().isAdjacentTo(closestMaybeHQ)) {
                targetLoc = closestMaybeHQ;
                // if can
            }
            else {
                // adjacent to HQ now
                Direction dirToHQ = rc.getLocation().directionTo(closestMaybeHQ);
                if (rc.getDirtCarrying() > 0) {
                    if (rc.canDepositDirt(dirToHQ)) {
                        rc.depositDirt(dirToHQ);
                    }
                }
                else {
                    Direction digDir = dirToHQ.opposite();
                    if (rc.canDigDirt(digDir)) {
                        rc.digDirt(digDir);
                    }
                }
            }
        }
        else if (role == DEFEND_HQ) {
            if (debug) System.out.println("Best defend build loc " + bestWallLocForDefend + " | otherwise " + closestWallLocForDefend);
            if (bestWallLocForDefend != null ||  leastElevatedWallLocForDefend != null || closestWallLocForDefend != null) {

                // we prefer the bestBuildLoc first, then use closest one
                targetLoc = bestWallLocForDefend;
                if (targetLoc == null) targetLoc = leastElevatedWallLocForDefend;
                if (targetLoc == null) {
                    targetLoc = closestWallLocForDefend;
                }
                // if adjacent to targetLoc, start digging at it
                if (rc.getLocation().distanceSquaredTo(targetLoc) <= 2) {

                    // set to null so we can reevaluate next round where to build
                    bestWallLocForDefend = null;
                    closestWallLocForDefend = null;

                    if (debug) System.out.println("Close and building wall at " + targetLoc);
                    // deposit onto wall

                    int myElevation = rc.senseElevation(rc.getLocation());
                    int targetElevation = rc.senseElevation(targetLoc);

                    // see if we can move on top of wall we want to build on
                    if (rc.getLocation().distanceSquaredTo(targetLoc) > 0 && myElevation <= targetElevation + 3 && myElevation >= targetElevation - 3) {
                        //targetLoc = null;
                        if (debug) System.out.println("Can still move to target wall loc " + targetLoc);
                    }
                    //if we can't move on top of wall
                    else {

                        // now perform wall building maneuvers
                        if (rc.getDirtCarrying() > 0) {
                            Direction depositDir = rc.getLocation().directionTo(targetLoc);
                            if (rc.canDepositDirt(depositDir)) {
                                rc.depositDirt(depositDir);
                            }
                        }
                        // find point that is not on wall to take dirt from
                        else {
                            Direction digDir = Direction.CENTER;
                            for (Direction dir : directions) {
                                MapLocation checkLoc = rc.adjacentLocation(dir);
                                if (!validBuildWallLoc(checkLoc) && rc.canDigDirt(dir)) {
                                    digDir = dir;
                                }
                            }
                            if (rc.canDigDirt(digDir)) {
                                rc.digDirt((digDir));
                            }
                        }
                        // set to null so we stop moving generally
                        targetLoc = null;
                    }



                }
            }
            // otherwise we aren't near enough to find a location to build the wall
            else {
                targetLoc = HQLocation;
            }

        }





        // whatever targetloc is, try to go to it
        if (targetLoc != null) {
            Direction greedyDir = getGreedyMove(targetLoc); //TODO: should return a valid direction usually???
            if (debug) System.out.println("Moving to " + rc.adjacentLocation((greedyDir)) + " to get to " + targetLoc);
            tryMove(greedyDir); // wasting bytecode probably here
        }


    }

    // determine if a position is a valid place to build a wall for HQ
    static boolean validBuildWallLoc(MapLocation loc) {
        if (loc.y == HQLocation.y - BASE_WALL_DIST || loc.y == HQLocation.y + BASE_WALL_DIST) {
            if (loc.x <= HQLocation.x + BASE_WALL_DIST && loc.x >= HQLocation.x - BASE_WALL_DIST) {
                return true;
            }
        }
        else if (loc.x == HQLocation.x - BASE_WALL_DIST || loc.x == HQLocation.x + BASE_WALL_DIST) {
            if (loc.y <= HQLocation.y + BASE_WALL_DIST && loc.y >= HQLocation.y - BASE_WALL_DIST) {
                return true;
            }
        }
        return false;
    }
    static void checkBlockForActions(Transaction[] transactions) {
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                if ((msg[1] ^ NEED_LANDSCAPERS_FOR_DEFENCE) == 0) {
                    // go run to HQ
                    role = DEFEND_HQ;
                    targetLoc = HQLocation;
                }
            }
        };
    }
    public static void setup() throws GameActionException {
        storeHQLocation();
        storeEnemyHQLocations();
    }
}

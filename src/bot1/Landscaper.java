package bot1;

import battlecode.common.*;
import bot1.utils.*;

public class Landscaper extends RobotPlayer {
    static MapLocation targetLoc;
    public static void run() throws GameActionException {
        // atm, swarm at an enemy base or smth and just hella try to bury it
        // make a path as needed if short range path finding yields no good way to get around some wall

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
                    System.out.println("FOUND ENEMY HQ AT " + closestMaybeHQ);
                }
                else {
                    // remove this location from linked list
                    enemyHQLocations.remove(closestMaybeHQNode);
                    System.out.println("Removed possible HQ AT " + closestMaybeHQ);
                }
            }
            else {
                enemyHQLocations.remove(closestMaybeHQNode);
                System.out.println("Removed possible HQ AT " + closestMaybeHQ);
                System.out.println(enemyHQLocations.head.val + " | " + enemyHQLocations.end.val);
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




        // whatever targetloc is, try to go to it
        if (targetLoc != null) {
            Direction greedyDir = getGreedyMove(targetLoc); //TODO: should return a valid direction usually???
            System.out.println("Moving to " + rc.adjacentLocation((greedyDir)) + " to get to " + targetLoc);
            tryMove(greedyDir); // wasting bytecode probably here
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

    }
    public static void setup() throws GameActionException {
        storeHQLocation();
        storeEnemyHQLocations();
    }
}

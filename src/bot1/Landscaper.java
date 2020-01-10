package bot1;

import battlecode.common.*;
import bot1.utils.*;

public class Landscaper extends RobotPlayer {
    static final int ATTACK = 0;
    static final int DEFEND_HQ = 1;
    static int role = DEFEND_HQ;

    static MapLocation bestWallLocForDefend = null;
    static MapLocation closestWallLocForDefend = null;
    static MapLocation leastElevatedWallLocForDefend = null;
    static RobotInfo nearestEnemy = null;
    static int nearestEnemyDist = 9999999;
    public static void run() throws GameActionException {
        // atm, swarm at an enemy base or smth and just hella try to bury it
        // make a path as needed if short range path finding yields no good way to get around some wall
        int waterLevel = calculateWaterLevels();

        int friendlyLandscaperCount = 0;
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyFriendlyRobots[i];
            switch (info.type) {
                case LANDSCAPER:
                    friendlyLandscaperCount++;
                    break;
            }
        }
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            switch (info.type) {
                case DESIGN_SCHOOL:
                case FULFILLMENT_CENTER:
                case NET_GUN:
                case REFINERY:
                case VAPORATOR:
                    // TODO, USE A SCORE FUNCTION TO WEIGHT SOME BUILDINGS HIGHER THAN OTHERS
                    int dist = rc.getLocation().distanceSquaredTo(info.location);
                    if  (dist < nearestEnemyDist) {
                        nearestEnemy = info;
                        nearestEnemyDist = dist;
                    }
                    break;
            }
        }
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        checkForEnemyBasesInBlocks(lastRoundsBlocks);

        /* BIG BFS LOOP ISH */

        int minDistToWall = 999999999;
        int minDistToBestBuildLoc = 99999999;
        //int farthestDistToBuildLoc
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
                        // TODO: reevaluate this method of defending

                        boolean occupied = true;
                        if (!rc.isLocationOccupied(checkLoc)) {
                            occupied = false;
                        }
                        else {
                            RobotInfo senseRobot = rc.senseRobotAtLocation(checkLoc);
                            if (senseRobot != null && (senseRobot.ID == rc.getID() || senseRobot.type != RobotType.LANDSCAPER)) {
                                occupied = false;
                            }
                        }
                        if (validBuildWallLoc(checkLoc) && !occupied) {

                            // look for first elevation that is not good enough yet and is not at least 3
                            int locElevation = rc.senseElevation(checkLoc);
                            if (locElevation <= 3) {
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
                        // check if wall is valid and if its empty or its urself

                        if (validBuildWallLoc(checkLoc) && !occupied) {
                            if (dist < minDistToWall) {
                                if (debug) System.out.println(checkLoc);
                                closestWallLocForDefend = checkLoc;
                                minDistToWall = dist;
                            }
                        }
                        break;
                }
            }
            else {
                switch(role) {
                    case DEFEND_HQ:
                        int dist = rc.getLocation().distanceSquaredTo(checkLoc);
                        if (validBuildWallLoc(checkLoc)) {
                            if (dist < minDistToWall) {
                                closestWallLocForDefend = checkLoc;
                                minDistToWall = dist;
                            }
                        }
                        break;
                }
                // if we can no longer sense location, break out of for loop then as all other BFS deltas will be unsensorable

            }
        }

        // always check for enemy base and do this recon
        MapLocation closestMaybeHQ = null;
        // dont know where base is, then look around for it.
        if (enemyBaseLocation == null) {
            Node<MapLocation> node = enemyHQLocations.head;

            Node<MapLocation> closestMaybeHQNode = enemyHQLocations.head;
            int minDistToMaybeHQ = 9999999;

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
            } else {
                // dont swarm?
            }

            // if we can check location we are trying to head to, determine if its a enemy HQ or not
            if (rc.canSenseLocation(closestMaybeHQ)) {
                if (rc.isLocationOccupied(closestMaybeHQ)) {
                    RobotInfo unit = rc.senseRobotAtLocation(closestMaybeHQ);
                    if (unit.type == RobotType.HQ && unit.team == enemyTeam) {
                        // FOUND HQ!
                        enemyBaseLocation = closestMaybeHQ;
                        announceEnemyBase(enemyBaseLocation);
                        if (debug) System.out.println("FOUND ENEMY HQ AT " + closestMaybeHQ);
                        if (debug) rc.setIndicatorDot(enemyBaseLocation, 100, 29, 245);
                    } else {
                        // remove this location from linked list
                        enemyHQLocations.remove(closestMaybeHQNode);
                    }
                } else {
                    enemyHQLocations.remove(closestMaybeHQNode);
                }
            }
        }

        if (role == ATTACK) {
            // find nearest enemy hq location to go and search and store map location

            MapLocation attackLoc = null;
            if (enemyBaseLocation == null) {
                attackLoc = closestMaybeHQ;
            }
            else {
                attackLoc = enemyBaseLocation;
            }
            // prioritize destructing buildings
            if (nearestEnemy != null) {
                attackLoc = nearestEnemy.location;

            }
            if (debug) rc.setIndicatorLine(rc.getLocation(), attackLoc, 130, 20, 240);
            // move towards maybe enemy HQ if not next to it.
            if (!rc.getLocation().isAdjacentTo(attackLoc)) {
                targetLoc = attackLoc;
            }
            else {
                // adjacent to attack loc now
                Direction dirToAttack = rc.getLocation().directionTo(attackLoc);
                if (rc.getDirtCarrying() > 0) {
                    if (rc.canDepositDirt(dirToAttack)) {
                        rc.depositDirt(dirToAttack);
                        // after depositing, if building robot is gone, we reset nearestEnemy if we used that
                        if (!rc.isLocationOccupied(attackLoc) && nearestEnemy != null) {
                            nearestEnemy = null;
                        }
                    }
                }
                else {
                    // dig down to make wall building harder
                    Direction digDir = Direction.CENTER; //dirToHQ.opposite();
                    // if we are attacking a normal building, take dirt from elsewhere
                    if (nearestEnemy != null) {
                        // loop to 1 so we don't include center
                        for (int i = directions.length; --i>=1; ) {
                            Direction testDir = directions[i];
                            MapLocation testLoc = rc.adjacentLocation(testDir);
                            // dig out from location that is not occupied, and if it is, it is occupied by enemy bot
                            if (rc.canDigDirt(testDir) && (!rc.isLocationOccupied(testLoc) || rc.senseRobotAtLocation(testLoc).team == enemyTeam)) {
                                rc.digDirt(directions[i]);
                                break;
                            }
                        }
                    }
                    // otherwise proceed to dig down as we are digging enemy HQ
                    else if (rc.canDigDirt(digDir)) {
                        rc.digDirt(digDir);
                    }
                }
            }
        }
        else if (role == DEFEND_HQ) {
            if (debug) System.out.println("Best defend build loc " + bestWallLocForDefend + " | closest wall loc " + closestWallLocForDefend);
            if (bestWallLocForDefend != null || closestWallLocForDefend != null) {

                // we prefer the bestBuildLoc first, then use closest one
                targetLoc = bestWallLocForDefend;
                if (targetLoc == null) targetLoc = closestWallLocForDefend;
                // if adjacent to targetLoc, start digging at it
                bestWallLocForDefend = null;
                closestWallLocForDefend = null;
                int distToTarget = rc.getLocation().distanceSquaredTo(targetLoc);
                if (distToTarget == 0) {

                    // set to null so we can reevaluate next round where to build



                    if (debug) System.out.println("Close and building wall at " + targetLoc);
                    // deposit onto wall

                    // check if base is getting burried
                    Direction dirToBase = rc.getLocation().directionTo(HQLocation);
                    if (rc.canDigDirt(dirToBase)) {
                        //targetLoc = null;
                        if (debug) System.out.println("Digging base out");
                        rc.digDirt(dirToBase);
                    }
                    // otherwise proceed with building wall nicely
                    else {

                        // now perform wall building maneuvers
                        if (rc.getDirtCarrying() > 0) {
                            // build on least elevated part that has unit on it
                            Direction bestDepositDir = rc.getLocation().directionTo(targetLoc);
                            int lowestElevation = rc.senseElevation(rc.getLocation());
                            for (Direction depositDir: directions) {
                                MapLocation loc = rc.adjacentLocation(depositDir);
                                // must be a build wall loc, occupied, and have landscaper there
                                if (rc.canSenseLocation(loc) && validBuildWallLoc(loc) && rc.isLocationOccupied(loc) && rc.senseRobotAtLocation(loc).type == RobotType.LANDSCAPER) {
                                    int thisE = rc.senseElevation(loc);
                                    if (thisE < lowestElevation) {
                                        lowestElevation = thisE;
                                        bestDepositDir = depositDir;
                                    }
                                }
                            }
                            if (rc.canDepositDirt(bestDepositDir)) {
                                rc.depositDirt(bestDepositDir);
                            }
                        }
                        // find point that is not on wall to take dirt from
                        else {
                            // take from right outside base
                            Direction digDir = rc.getLocation().directionTo(HQLocation).opposite();
                            if (rc.canDigDirt(digDir)) {
                                rc.digDirt((digDir));
                            }
                            // if for some reason u cant dig, go dig elsewhere...
                            else {
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
                        }
                        // set to null so we stop moving generally
                        targetLoc = null;
                    }

                    // if there are wayy too many friendly landscapers, go on the attack
                    if (friendlyLandscaperCount >= ((BASE_WALL_DIST + 1) * 4 + 4)/ 3) {
                        //role = ATTACK;
                    }


                }
                // if we havent reached the build place, check it out
                else {
                    // if adjacent..., BURY IT probably
                    if (distToTarget <= 2 && rc.canSenseLocation(targetLoc) && rc.isLocationOccupied(targetLoc)) {
                        RobotInfo info = rc.senseRobotAtLocation(targetLoc);
                        if (info.type == RobotType.FULFILLMENT_CENTER || info.type == RobotType.DESIGN_SCHOOL || info.type == RobotType.NET_GUN) {
                            if (debug) System.out.println("Found building on wall loc, trying to bury " + targetLoc);
                            Direction dirToBuilding = rc.getLocation().directionTo(targetLoc);
                            if (rc.canDepositDirt(dirToBuilding)) {
                                rc.depositDirt(dirToBuilding);
                            }
                            else {
                                Direction digDir = null;
                                for (Direction dir : directions) {
                                    MapLocation checkLoc = rc.adjacentLocation(dir);
                                    if (!validBuildWallLoc(checkLoc) && rc.canDigDirt(dir)) {
                                        digDir = dir;
                                    }
                                }
                                if (digDir != null && rc.canDigDirt(digDir)) {
                                    rc.digDirt((digDir));
                                }
                            }
                            targetLoc = null; // don't move, just try to bury
                        }
                    }
                }
            }
            // otherwise we aren't near enough to find a location to build the wall
            else {
                targetLoc = HQLocation;
                if (rc.getLocation().distanceSquaredTo(HQLocation) <= 8) {
                    if (closestWallLocForDefend == null) {
                        role = ATTACK;
                    }
                }
            }

        }





        // whatever targetloc is, try to go to it
        if (targetLoc != null) {
            Direction greedyDir = getBugPathMove(targetLoc); //TODO: should return a valid direction usually???
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

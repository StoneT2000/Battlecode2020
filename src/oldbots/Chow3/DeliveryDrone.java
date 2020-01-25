package Chow3;

import battlecode.common.*;
import Chow3.utils.Node;

public class DeliveryDrone extends RobotPlayer {
    static final int DEFEND = 1;
    static final int ATTACK = 0;
    static final int DUMP_BAD_GUY = 2;
    static int role = ATTACK;
    static MapLocation attackLoc;
    static MapLocation waterLoc;
    static boolean attackHQ = false;
    static int receivedAttackHQMessageRound = -1;
    static int roundsToWaitBeforeAttack = 0;
    public static void run() throws GameActionException {
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        for (int i = lastRoundsBlocks.length; --i >= 0; ) {
            int[] msg = lastRoundsBlocks[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                if ((msg[1] ^ DRONES_ATTACK) == 0) {
                    if (msg[2] != -1) {
                        enemyBaseLocation = parseLoc(msg[2]);
                        //role = ATTACK;
                        // TODO: handle case when we dont know enemy base location
                        attackLoc = enemyBaseLocation;
                        attackHQ = true;
                        receivedAttackHQMessageRound = rc.getRoundNum();
                        // + 50 turns to wait for drones to reform a circle around ENEMY
                        roundsToWaitBeforeAttack = (int) (2 * Math.max(Math.abs(attackLoc.x - HQLocation.x), Math.abs(HQLocation.y - attackLoc.y))) + 70;
                    }
                    else {
                        // we don't know where enemy is, SCOUT!
                        attackLoc = null;
                        attackHQ  = true;
                    }
                }
                else if ((msg[1] ^ ANNOUNCE_ENEMY_BASE_LOCATION) == 0) {
                    enemyBaseLocation = parseLoc(msg[2]);
                    // if we foudn enemy base and we are attackiong HQ but dont know where HQ is
                    if (attackHQ && attackLoc == null) {
                        attackLoc = enemyBaseLocation;
                        receivedAttackHQMessageRound = rc.getRoundNum();
                        roundsToWaitBeforeAttack = (int) (2 * Math.max(Math.abs(attackLoc.x - HQLocation.x), Math.abs(HQLocation.y - attackLoc.y))) + 70;

                    }
                }
            }
        }


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
                        case HQ:
                            if (enemyBaseLocation == null) {
                                announceEnemyBase(info.location);
                                enemyBaseLocation = info.location;
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
                    if (dist < minDistToFlood && dist != 0) {
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
            if (debug) System.out.println("DUMPING BAD UNIT to " + waterLoc);
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
                // this shouldn't ever happen
            }
        }
        // if attacking, move towards nearest enemy
        else if (role == ATTACK) {

            if (attackHQ == false) {
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
                    } else {
                        // not near enemy yet, set targetLoc to this so we move towards enemey.
                        targetLoc = enemyToEngage.location;
                    }
                }
                // otherwise hover around attackLOC and fuzzy
                else {
                    int distToAttackLoc = rc.getLocation().distanceSquaredTo(attackLoc);
                    // stick around, don't move in
                    if (distToAttackLoc <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED + 2) {
                        //fuzzy
                        targetLoc = rc.adjacentLocation(randomDirection());
                    } else {
                        targetLoc = attackLoc; // move towards attack loc first if not near it yet.
                        //rc.setIndicatorLine(rc.getLocation(), targetLoc, 200, 200, 10);
                    }
                }
            }
            else {
                if (attackLoc != null) {
                    int distToAttackLoc = rc.getLocation().distanceSquaredTo(attackLoc);
                    // stick around, don't move in
                    if (distToAttackLoc > GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED + 10) {
                        //move to just edge of base attack range.
                        targetLoc = attackLoc;
                        rc.setIndicatorDot(rc.getLocation(), 10, 20,200);
                    } else {
                        targetLoc = null; // don't move there if it isn't time yet
                        if (roundsToWaitBeforeAttack <= rc.getRoundNum() - receivedAttackHQMessageRound) {
                            // if we waited long enough, begin attacking

                            if (closestEnemyMiner != null || closestEnemyLandscaper != null) {

                                RobotInfo enemyToEngage = closestEnemyLandscaper;
                                if (enemyToEngage == null) enemyToEngage = closestEnemyMiner;
                                if (debug) System.out.println("ENGAGING ENEMY at " + enemyToEngage.location);
                                if (debug) rc.setIndicatorLine(rc.getLocation(), enemyToEngage.location, 100, 200, 10);
                                int distToEnemy = rc.getLocation().distanceSquaredTo(enemyToEngage.location);
                                if (distToEnemy <= 2) {
                                    // we are adjacent, pick it up and prepare for destroy procedure
                                    if (rc.canPickUpUnit(enemyToEngage.getID())) {
                                        rc.pickUpUnit(enemyToEngage.getID());
                                        role = DUMP_BAD_GUY;
                                        targetLoc = waterLoc;
                                    }
                                } else {
                                    // not near enemy yet, set targetLoc to this so we move towards enemey.
                                    targetLoc = enemyToEngage.location;
                                }
                            }
                        }
                    }
                }
                else {
                    // got attack msg, no hq to go and kill
                    // search for it then
                    MapLocation closestMaybeHQ = null;
                    // dont know where base is, then look around for it.
                    if (enemyBaseLocation == null) {
                        if (debug) System.out.println("finding closest HQ TO LOOK FOR");
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
                                    attackLoc = enemyBaseLocation;
                                } else {
                                    // remove this location from linked list
                                    enemyHQLocations.remove(closestMaybeHQNode);
                                }
                            } else {
                                enemyHQLocations.remove(closestMaybeHQNode);
                            }
                        }
                    }
                    targetLoc = closestMaybeHQ;
                }
            }
        }

        // whatever targetloc is, try to go to it
        movement:
        {
            if (targetLoc != null && rc.isReady()) {
                Direction dir = rc.getLocation().directionTo(targetLoc);
                if (rc.canSenseLocation((rc.adjacentLocation(dir))) && !rc.isLocationOccupied((rc.adjacentLocation(dir)))) {
                    rc.move(dir);
                    break movement;
                } else {
                    int minDist = 999999;
                    for (int i = directions.length; --i >= 0; ) {
                        // if distance to target from this potential direction is smaller, set it
                        int dist = targetLoc.distanceSquaredTo(rc.adjacentLocation(directions[i]));
                        if (dist < minDist && rc.canSenseLocation((rc.adjacentLocation(dir))) && !rc.isLocationOccupied((rc.adjacentLocation(dir)))) {
                            dir = directions[i];
                            minDist = dist;
                            if (debug) System.out.println("I chose " + dir + " instead in order to go to " + targetLoc);
                        }
                    }
                }

                if (debug) System.out.println("Moving to " + rc.adjacentLocation((dir)) + " to get to " + targetLoc);
                if (rc.canSenseLocation((rc.adjacentLocation(dir))) && !rc.isLocationOccupied((rc.adjacentLocation(dir)))) {
                    rc.move(dir);
                    break movement;
                } else {
                    for (int i = 7; --i >= 0; ) {
                        dir = dir.rotateLeft();
                        if (rc.canSenseLocation((rc.adjacentLocation(dir))) && !rc.isLocationOccupied((rc.adjacentLocation(dir)))) {
                            rc.move(dir);
                            break movement;
                        }
                    }
                }
            }
        }
    }
    public static void setup() throws GameActionException {
        storeHQLocation();
        storeEnemyHQLocations();
        attackLoc = HQLocation;
    }
}

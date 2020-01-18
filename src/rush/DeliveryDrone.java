package rush;

import Chow5.Constants;
import Chow5.RobotPlayer;
import Chow5.utils.Node;
import battlecode.common.*;

public class DeliveryDrone extends RobotPlayer {
    static final int DEFEND = 1;
    static final int ATTACK = 0;
    static final int DUMP_BAD_GUY = 2;
    static int role = ATTACK;
    static MapLocation attackLoc;
    static MapLocation waterLoc;
    static boolean attackHQ = false;
    static boolean holdingCow = false;
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
                        // TODO: handle case when we dont know enemy base location
                        attackLoc = enemyBaseLocation;
                        attackHQ = true;
                        receivedAttackHQMessageRound = rc.getRoundNum();
                        // + 70 turns to wait for drones to reform a circle around ENEMY
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
                    // if we found enemy base and we are attacking HQ but don't know where HQ is
                    if (attackHQ && attackLoc == null) {
                        attackLoc = enemyBaseLocation;
                        receivedAttackHQMessageRound = rc.getRoundNum();
                        roundsToWaitBeforeAttack = (int) (2 * Math.max(Math.abs(attackLoc.x - HQLocation.x), Math.abs(HQLocation.y - attackLoc.y))) + 70;

                    }
                }
                else if ((msg[1] ^ ANNOUNCE_NOT_ENEMY_BASE) == 0) {
                    // remove said base from enemy base locations
                    MapLocation notBaseLocation = parseLoc(msg[2]);

                    // iterate over list
                    Node<MapLocation> node = enemyHQLocations.head;

                    Node<MapLocation> nodeToRemove = null;
                    if (node != null) {
                        for (int j = 0; j++ < enemyHQLocations.size; ) {
                            if (node.val.equals(notBaseLocation)) {
                                nodeToRemove = node;
                                break;
                            }
                            node = node.next;

                        }
                    }
                    if (nodeToRemove != null) {
                        enemyHQLocations.remove(nodeToRemove);
                    }
                }
                else if ((msg[1] ^ BUILD_DRONE_NOW) == 0) {
                    if (!attackHQ) {
                        attackLoc = HQLocation;
                    }

                }
            }
        }


        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] nearbyNeutralRobots = rc.senseNearbyRobots(-1, Team.NEUTRAL);

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
                        case NET_GUN:

                            break;
                    }
                    break;
            }
        }

        int friendlyDrones = 0;
        RobotInfo nearestCow = null;
        int distToNearestCow =  9999999;
        for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
            RobotInfo info  =nearbyFriendlyRobots[i];
            switch(info.type) {
                case DELIVERY_DRONE:
                    friendlyDrones++;
                    break;
            }
        }
        if (debug) System.out.println("There are " + nearbyNeutralRobots.length + " neutrals (cows?) nearby ");
        for (int i = nearbyNeutralRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyNeutralRobots[i];
            switch(info.type) {
                case COW:
                    if (debug) System.out.println("Found COW at " + info.location);
                    int dist = rc.getLocation().distanceSquaredTo(info.location);
                    if (dist < distToNearestCow) {
                        nearestCow = info;
                        distToNearestCow = dist;
                    }
            }
        }

        // if many friend drones, go to HQ?
        if (friendlyDrones > 3) {
            //attackLoc = HQLocation;
        }


        /* SCOUTING CODE */

        /* BIG BFS LOOP ISH */
        int minDistToFlood = 999999999;
        for (int i = 0; i < Chow5.Constants.BFSDeltas24.length; i++) {
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

        // Store closest enemy HQ position
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
            if (debug) System.out.println("Closest possible enemy HQ: " + closestMaybeHQ);

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

                        // announce to everyone its not an HQ
                        announceNotEnemyBase(closestMaybeHQNode.val);
                        // remove this location from linked list
                        enemyHQLocations.remove(closestMaybeHQNode);

                    }
                } else {
                    // announce to everyone its not an HQ
                    announceNotEnemyBase(closestMaybeHQNode.val);
                    enemyHQLocations.remove(closestMaybeHQNode);
                }
            }
        }
        else {
            closestMaybeHQ = enemyBaseLocation;
        }

        if (role == DUMP_BAD_GUY) {
            // if currently holding unit, it should be a bad guy

            if (rc.isCurrentlyHoldingUnit()) {
                if (!holdingCow) {
                    // find water and drop the enemy unit
                    if (waterLoc != null) {
                        if (debug) System.out.println("DUMPING BAD UNIT to " + waterLoc);
                        targetLoc = waterLoc;
                        if (rc.getLocation().isAdjacentTo(waterLoc)) {
                            // if drone is adjacent to waterLoc, drop the enemy unit
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
                    targetLoc = closestMaybeHQ;
                    if (enemyBaseLocation != null) {
                        // drop on nearby land if possible, otherwise SEARCH for nearest place we can drop this cow
                        if (rc.getLocation().distanceSquaredTo(enemyBaseLocation) <= 8) {
                            int i = 0;
                            Direction dropDir = rc.getLocation().directionTo(enemyBaseLocation);
                            while (++i <= 8) {
                                if (rc.canDropUnit(dropDir)) {
                                    rc.dropUnit(dropDir);
                                    role = ATTACK;
                                    holdingCow = false;
                                }
                                else {
                                    dropDir = dropDir.rotateLeft();
                                }
                            }
                        }
                    }
                    else {

                    }
                }
            }
            else {
                // this shouldn't ever happen
            }
        }
        else if (role == ATTACK) {

            // if not ordered to attack enemy HQ, do normal defending and attack
            if (attackHQ == false) {

                // if there is enemy, engage!
                if (closestEnemyMiner != null || closestEnemyLandscaper != null || nearestCow != null) {
                    RobotInfo enemyToEngage = closestEnemyLandscaper;
                    if (enemyToEngage == null) enemyToEngage = closestEnemyMiner;
                    if (enemyToEngage == null) enemyToEngage = nearestCow;

                    if (debug) System.out.println("ENGAGING ENEMY at " + enemyToEngage.location);
                    int distToEnemy = rc.getLocation().distanceSquaredTo(enemyToEngage.location);
                    if (distToEnemy <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
                        // we are adjacent, pick it up and prepare for destroy procedure
                        if (rc.canPickUpUnit(enemyToEngage.getID())) {
                            rc.pickUpUnit(enemyToEngage.getID());
                            if (enemyToEngage.getType() != RobotType.COW) {
                                role = DUMP_BAD_GUY;
                                targetLoc = waterLoc;
                            }
                            else {
                                role = DUMP_BAD_GUY;
                                holdingCow = true;
                                targetLoc = closestMaybeHQ;
                            }
                        }
                    } else {
                        // not near enemy yet, set targetLoc to this so we move towards enemey.
                        targetLoc = enemyToEngage.location;
                    }
                }

                // otherwise hover around attackLOC and fuzzy move
                else {
                    int distToAttackLoc = rc.getLocation().distanceSquaredTo(attackLoc);
                    // stick around, don't move in too close
                    if (distToAttackLoc <= RobotType.DELIVERY_DRONE.sensorRadiusSquared) {
                        //fuzzy
                        targetLoc = rc.adjacentLocation(randomDirection());
                    } else {
                        targetLoc = attackLoc; // move towards attack loc first if not near it yet.
                    }

                    // check if we should still hover around this location
                    // if we can still sense it and it is occupied by a friendly unit, keep hovering over it
                    // otherwise set attackLoc to HQLocation (basically defence the HQ)
                    if (rc.canSenseLocation(attackLoc)) {
                        if (rc.isLocationOccupied(attackLoc)) {
                            RobotInfo info = rc.senseRobotAtLocation(attackLoc);
                            if (info.team != rc.getTeam()) {
                                attackLoc = HQLocation;
                            }
                        }
                        else {
                            if (debug) System.out.println("Not occupied! going to HQ");
                            attackLoc = HQLocation;
                        }
                    }
                }
            }
            // otherwise we are ordered to try and attack enemy HQ
            else {
                if (attackLoc != null) {
                    int distToAttackLoc = rc.getLocation().distanceSquaredTo(attackLoc);
                    // stick around, don't move in
                    if (distToAttackLoc > GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED + 10) {
                        //move to just edge of base attack range.
                        targetLoc = attackLoc;
                        if (debug) rc.setIndicatorDot(rc.getLocation(), 10, 20,200);
                    } else {
                        targetLoc = null; // don't move there if it isn't time yet
                        // if we waited long enough,
                        if (roundsToWaitBeforeAttack <= rc.getRoundNum() - receivedAttackHQMessageRound) {

                            // begin attacking nearest enemy unit
                            if (closestEnemyMiner != null || closestEnemyLandscaper != null) {

                                RobotInfo enemyToEngage = closestEnemyLandscaper;
                                if (enemyToEngage == null) enemyToEngage = closestEnemyMiner;
                                if (debug) System.out.println("ENGAGING ENEMY at " + enemyToEngage.location);
                                if (debug) rc.setIndicatorLine(rc.getLocation(), enemyToEngage.location, 100, 200, 10);
                                int distToEnemy = rc.getLocation().distanceSquaredTo(enemyToEngage.location);
                                if (distToEnemy <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
                                    // we are near enough, pick it up and prepare for destroy procedure
                                    if (rc.canPickUpUnit(enemyToEngage.getID())) {
                                        rc.pickUpUnit(enemyToEngage.getID());
                                        role = DUMP_BAD_GUY;
                                        targetLoc = waterLoc;
                                    }
                                } else {
                                    // not near enemy yet, set targetLoc to enemy location so we move towards enemey.
                                    targetLoc = enemyToEngage.location;
                                }
                            }
                        }
                    }
                }
                else {
                    // got attackHQ message, but no attackLoc provided, so go to closestMaybeHQ we know of.
                    targetLoc = closestMaybeHQ;
                }
            }
        }

        // whatever targetLoc is, try to go to it
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
                        }
                    }
                }

                // try to go in the closest direction
                if (debug) System.out.println("Moving to " + rc.adjacentLocation((dir)) + " to get to " + targetLoc);
                if (rc.canSenseLocation((rc.adjacentLocation(dir))) && !rc.isLocationOccupied((rc.adjacentLocation(dir)))) {
                    rc.move(dir);
                    break movement;
                } else {
                    // otherwise rotate left
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
        // find FC that produced us, and set it as attackLoc.
        for (Direction dir: directions) {
            MapLocation potentialCenterLoc = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(potentialCenterLoc) && rc.isLocationOccupied(potentialCenterLoc)) {
                RobotInfo info = rc.senseRobotAtLocation(potentialCenterLoc);
                if (info.type == RobotType.FULFILLMENT_CENTER && info.team == rc.getTeam()) {
                    attackLoc = potentialCenterLoc;
                    break;
                }
            }
        }
        // For now, go to HQ anyway
        attackLoc = HQLocation;
    }
}

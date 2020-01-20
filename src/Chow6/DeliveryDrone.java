package Chow6;

import Chow6.utils.*;
import battlecode.common.*;

import java.awt.*;
import java.util.Map;

public class DeliveryDrone extends RobotPlayer {
    static final int DEFEND = 1;
    static final int ATTACK = 0;
    static final int DUMP_BAD_GUY = 2;
    static final int MOVE_LANDSCAPER = 3;
    static final int MOVE_OUR_UNIT_OUT = 4;
    static boolean lockAndDefend = false;
    static int role = ATTACK;
    static MapLocation attackLoc;

    static MapLocation waterLoc;

    static int wallSpaces = 0;
    static boolean wallHasEmptySpot = true;
    static boolean wallSpotLeft = false;
    static boolean attackHQ = false;
    static boolean holdingCow = false;
    static int receivedAttackHQMessageRound = -1;
    static int roundsToWaitBeforeAttack = 0;
    static HashTable<MapLocation> MainWall = new HashTable<>(8);
    static HashTable<MapLocation> SecondWall = new HashTable<>(12);
    static HashTable<MapLocation> BuildPositionsTaken = new HashTable<>(10);
    public static void run() throws GameActionException {
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        for (int i = lastRoundsBlocks.length; --i >= 0; ) {
            int[] msg = lastRoundsBlocks[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                if ((msg[1] ^ DRONES_ATTACK) == 0) {
                    int distToHQ = rc.getLocation().distanceSquaredTo(HQLocation);
                    if ((distToHQ <= 13 && distToHQ >= 8) || distToHQ == 18) {
                        // stay on wall dont do anything
                    }
                    else {
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
        HashTable<Direction> dangerousDirections = new HashTable<>(4); // directioons that when moved in, will result in netgun death
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
            int dist = rc.getLocation().distanceSquaredTo(info.getLocation());
            if (dist <= 25 && info.type == RobotType.NET_GUN) {
                // dangerous netgun, move somewhere not in range!
                Direction badDir = rc.getLocation().directionTo(info.location);
                dangerousDirections.add(badDir);
                Direction badDirLeft = badDir.rotateLeft();
                Direction badDirRight = badDir.rotateRight();
                if (rc.adjacentLocation(badDirLeft).distanceSquaredTo(info.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                    if (debug) System.out.println("Gonna avoid " + badDirLeft);
                    dangerousDirections.add(badDirLeft);
                }
                if (rc.adjacentLocation(badDirRight).distanceSquaredTo(info.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                    dangerousDirections.add(badDirRight);
                    if (debug) System.out.println("Gonna avoid " + badDirRight);
                }
                if (debug) System.out.println("Gonna avoid " + rc.getLocation().directionTo(info.location));
            }
        }
        // look for nearest empty wall
        int closestEmptyWallLocDist = 9999999;
        MapLocation closestEmptyWallLoc = null;
        MapLocation closesetEmptyMAINWALL = null;
        boolean thisRoundFoundSpot = false;
        for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i>= 0; ) {
            int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
            MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
            if (rc.canSenseLocation(loc)) {
                // determine if it has our landscaper or not, if not occupied and not flooded bring our unit in there, if occupied take it out
                int dist = rc.getLocation().distanceSquaredTo(loc);
                if (!rc.isLocationOccupied(loc) && !rc.senseFlooding(loc)) {
                    wallSpotLeft = true;
                    thisRoundFoundSpot = true;
                    if (dist < closestEmptyWallLocDist) {
                        closestEmptyWallLoc = loc;
                        closestEmptyWallLocDist = dist;
                        closesetEmptyMAINWALL = loc;
                    }
                }
                /*
                else if(rc.isLocationOccupied(loc)) {
                    RobotInfo info = rc.senseRobotAtLocation(loc);
                    if (info.type == RobotType.LANDSCAPER && info.team == rc.getTeam()) {
                        BuildPositionsTaken.add(loc);
                    }
                }*/
            }
        }
        if (closestEmptyWallLoc == null) {
            for (int i = Constants.LandscaperPosAroundHQ.length; --i>= 0; ) {
                int[] deltas = Constants.LandscaperPosAroundHQ[i];
                MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
                if (rc.canSenseLocation(loc)) {
                    // determine if it has our landscaper or not, if not occupied bring our unit in there, if occupied take it out
                    int dist = rc.getLocation().distanceSquaredTo(loc);
                    if (!rc.isLocationOccupied(loc) && !rc.senseFlooding(loc)) {
                        wallSpotLeft = true;
                        thisRoundFoundSpot = true;
                        if (dist < closestEmptyWallLocDist) {
                            closestEmptyWallLoc = loc;
                            closestEmptyWallLocDist = dist;
                        }
                    }
                    /*
                    else if(rc.isLocationOccupied(loc)) {
                        RobotInfo info = rc.senseRobotAtLocation(loc);
                        if (info.type == RobotType.LANDSCAPER && info.team == rc.getTeam()) {
                            BuildPositionsTaken.add(loc);
                        }
                    }*/
                }
            }
        }
        if (!thisRoundFoundSpot) {
            wallSpotLeft = false;
        }

        int friendlyDrones = 0;
        RobotInfo nearestCow = null;
        int distToNearestCow =  9999999;
        int distToNearestLandscaper = 99999999;
        RobotInfo nearestLandscaper = null; // nearest not on wall / available
        for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyFriendlyRobots[i];
            switch(info.type) {
                case DELIVERY_DRONE:
                    friendlyDrones++;
                    break;
                case LANDSCAPER:
                    // if not on man wall, and we have a empty loc on main wall, consider it
                    // if not on second wall either, consider ir. WE only consider if we think there is wall space left
                    if (wallSpotLeft && !MainWall.contains(info.location)) {
                        if (!SecondWall.contains(info.location)) {
                            int dist = rc.getLocation().distanceSquaredTo(info.location);
                            if (dist < distToNearestLandscaper) {
                                distToNearestLandscaper = dist;
                                nearestLandscaper = info;
                            }
                        }
                    }
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

        RobotInfo blockingFriendlyUnit = null;
        // drone must always try and help our own landscapers
        if (nearestLandscaper != null) {
            if (debug) System.out.println("found near landscaper that needs help? " + nearestLandscaper.location);

            // look at main wall first and move them there
            for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i>= 0; ) {
                int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
                MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
                if (!BuildPositionsTaken.contains(loc) && rc.canSenseLocation(loc)) {
                    // determine if it has our landscaper or not, if not occupied bring our unit in there, if occupied take it out
                    if (rc.isLocationOccupied(loc)) {
                       RobotInfo info = rc.senseRobotAtLocation(loc);
                       if (info.type != RobotType.LANDSCAPER && info.team == rc.getTeam()) {
                           if (debug) System.out.println("Found unit to move out");
                           if (rc.getLocation().isAdjacentTo(info.location)) {
                               blockingFriendlyUnit = info;
                               if (rc.canPickUpUnit(info.getID())) {
                                   rc.pickUpUnit(info.getID());
                                   role = MOVE_OUR_UNIT_OUT;

                               }
                           }
                           else {
                               blockingFriendlyUnit = info;
                               setTargetLoc(info.location);
                               //role = MOVE_OUR_UNIT_OUT;
                           }
                       }
                    }
                    else {
                        if (debug) System.out.println("Found unit to try and help");
                        // take nearest landscaper
                        if (distToNearestLandscaper <= 2) {
                            // adjacent, pick it up, drop in right place
                            if (rc.canPickUpUnit(nearestLandscaper.getID())) {
                                rc.pickUpUnit(nearestLandscaper.getID());
                                role = MOVE_LANDSCAPER;
                                break;
                            }
                        }
                        else {
                            // go towards landscaper to pick it up maybe
                            setTargetLoc(nearestLandscaper.location);
                            break;
                        }
                    }
                }
            }
            for (int i = Constants.LandscaperPosAroundHQ.length; --i>= 0; ) {
                int[] deltas = Constants.LandscaperPosAroundHQ[i];
                MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
                if (!BuildPositionsTaken.contains(loc) && rc.canSenseLocation(loc)) {
                    // determine if it has our landscaper or not, if not occupied bring our unit in there, if occupied take it out
                    if (rc.isLocationOccupied(loc)) {

                    }
                    else {
                        if (debug) System.out.println("Found unit to try and help");
                        // take nearest landscaper
                        if (distToNearestLandscaper <= 2) {
                            // adjacent, pick it up, drop in right place
                            if (rc.canPickUpUnit(nearestLandscaper.getID())) {
                                rc.pickUpUnit(nearestLandscaper.getID());
                                role = MOVE_LANDSCAPER;
                                break;
                            }
                        }
                        else {
                            // go towards landscaper to pick it up maybe
                            setTargetLoc(nearestLandscaper.location);
                            break;
                        }
                    }
                }
            }
        }

        if (role == MOVE_LANDSCAPER) {

            // if no open spot, move on and drop unit
            if (closestEmptyWallLoc == null) {
                int i = 0;
                Direction dropDir = rc.getLocation().directionTo(HQLocation);
                while(i++ < 8) {
                    if (rc.canDropUnit(dropDir)) {
                        rc.dropUnit(dropDir);
                        role = ATTACK;
                        break;
                    }
                    dropDir = dropDir.rotateLeft();
                }
            }
            else {
                // adjacent? drop unit
                if (closestEmptyWallLocDist <= 2) {
                    Direction dropDir = rc.getLocation().directionTo(closestEmptyWallLoc);
                    if (rc.canDropUnit(dropDir)) {
                        rc.dropUnit(dropDir);
                        role = ATTACK;
                    }
                }
                else {
                    setTargetLoc(closestEmptyWallLoc);
                }
            }
        }
        else if (role == MOVE_OUR_UNIT_OUT) {
            // drop our unit anywhere but on the wall
            Direction dropDir = rc.getLocation().directionTo(HQLocation).opposite();
            int i = 0;
            while(i++ < 8) {
                MapLocation dropLoc = rc.adjacentLocation(dropDir);
                if (!MainWall.contains(dropLoc) && !SecondWall.contains(dropLoc)) {
                    if (rc.canDropUnit(dropDir)) {
                        rc.dropUnit(dropDir);
                        role = ATTACK;
                        break;
                    }
                }
                dropDir = dropDir.rotateLeft();
            }
        }
        else if (role == DUMP_BAD_GUY) {
            // if currently holding unit, it should be a bad guy

            if (rc.isCurrentlyHoldingUnit()) {
                if (!holdingCow) {
                    // find water and drop the enemy unit
                    if (waterLoc != null) {
                        if (debug) System.out.println("DUMPING BAD UNIT to " + waterLoc);
                        //targetLoc = waterLoc;
                        setTargetLoc(waterLoc);
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
                        setTargetLoc(rc.adjacentLocation(randomDirection()));
                    }
                }
                else {
                    //targetLoc = closestMaybeHQ;
                    setTargetLoc(closestMaybeHQ);
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
                // engage cows if there are 0 enemies and friend drones
                if (closestEnemyMiner != null || closestEnemyLandscaper != null || (nearestCow != null && nearbyEnemyRobots.length == 0 && friendlyDrones > 1)) {
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
                                //targetLoc = waterLoc;
                                setTargetLoc(waterLoc);
                            }
                            else {
                                role = DUMP_BAD_GUY;
                                holdingCow = true;
                                //targetLoc = closestMaybeHQ;
                                setTargetLoc(closestMaybeHQ);
                            }
                        }
                    } else {
                        // not near enemy yet, set targetLoc to this so we move towards enemey.
                        //targetLoc = enemyToEngage.location;
                        setTargetLoc(enemyToEngage.location);
                    }
                }

                // otherwise hover around attackLOC and fuzzy move
                else {
                    int distToAttackLoc = rc.getLocation().distanceSquaredTo(attackLoc);
                    // stick around, don't move in too close
                    if (lockAndDefend && attackLoc.equals(HQLocation)) {

                        if ((distToAttackLoc <= 13 && distToAttackLoc >= 8) || distToAttackLoc == 18) {
                            // don't move, stay
                            setTargetLoc(null);
                        }
                        else {
                            // otherwise move
                            setTargetLoc(attackLoc);
                        }
                    }
                    else if (distToAttackLoc <= RobotType.DELIVERY_DRONE.sensorRadiusSquared) {
                        //fuzzy
                        //targetLoc = rc.adjacentLocation(randomDirection());

                        if (attackLoc.equals(HQLocation)) {
                            // don't fuzzy, stay
                        }
                        else {
                            setTargetLoc(rc.adjacentLocation(randomDirection()));
                        }
                    } else {
                        //targetLoc = attackLoc; // move towards attack loc first if not near it yet.
                        setTargetLoc(attackLoc);
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
                    if (distToAttackLoc >= 36) {
                        //move to just edge of base attack range.
                        //targetLoc = attackLoc;
                        setTargetLoc(attackLoc);
                        if (debug) rc.setIndicatorDot(rc.getLocation(), 10, 20,200);
                    } else {
                        //targetLoc = null; // don't move there if it isn't time yet
                        setTargetLoc(null);
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
                                        //targetLoc = waterLoc;
                                        setTargetLoc(waterLoc);
                                    }
                                } else {
                                    // not near enemy yet, set targetLoc to enemy location so we move towards enemey.
                                    //targetLoc = enemyToEngage.location;
                                    setTargetLoc(enemyToEngage.location);
                                }
                            }
                        }
                    }
                }
                else {
                    // got attackHQ message, but no attackLoc provided, so go to closestMaybeHQ we know of.
                    //targetLoc = closestMaybeHQ;
                    setTargetLoc(closestMaybeHQ);
                }
            }
        }

        // whatever targetLoc is, try to go to it
        movement:
        {
            if (targetLoc != null && rc.isReady()) {
                Direction dir = getBugPathMoveDrone(targetLoc, dangerousDirections);
                if (!dir.equals(Direction.CENTER)) {
                    rc.move(dir);
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
        wallSpaces = 0;
        for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i>= 0; ) {
            int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
            MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
            if (rc.onTheMap(loc)) {
                wallSpaces++;
                MainWall.add(loc);
            }
        }
        for (int i = Constants.LandscaperPosAroundHQ.length; --i>= 0; ) {
            int[] deltas = Constants.LandscaperPosAroundHQ[i];
            MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
            if (rc.onTheMap(loc)) {
                SecondWall.add(loc);
                wallSpaces++;
            }

        }

        wallSpotLeft = true;
    }
    static Direction getBugPathMoveDrone(MapLocation target, HashTable<Direction> dangerousDirections) throws GameActionException {

        Direction dir = rc.getLocation().directionTo(target);
        MapLocation greedyLoc = rc.adjacentLocation(dir);
        int greedyDist = greedyLoc.distanceSquaredTo(target);
        if (debug) System.out.println("Target: " + target + " | greedyLoc " + greedyLoc +
                " | closestSoFar " + closestToTargetLocSoFar + " | this dist " + greedyDist);
        if (!dangerousDirections.contains(dir) && greedyDist < closestToTargetLocSoFar && rc.canSenseLocation(greedyLoc)) {
            if (rc.canMove(dir)) {
                closestToTargetLocSoFar = greedyDist;
                return dir;
            }
        }
        Direction firstDirThatWorks = Direction.CENTER;
        for (int i = 7; --i >= 0; ) {
            dir = dir.rotateLeft();
            MapLocation adjLoc = rc.adjacentLocation(dir);
            if (!dangerousDirections.contains(dir) && rc.canSenseLocation(adjLoc)) {
                if (rc.canMove(dir)) {
                    firstDirThatWorks = dir;
                    //lastDirMove = dir;
                    //lastLoc = adjLoc;

                    // store past 2 positions
                    /*
                    if (lastLocs.size < 2) {
                        lastLocs.add(adjLoc);
                    }
                    else {
                        lastLocs.dequeue();
                        lastLocs.add(adjLoc);
                    }*/
                    int dist = adjLoc.distanceSquaredTo(target);
                    if (dist < closestToTargetLocSoFar) {
                        closestToTargetLocSoFar = greedyDist;
                        return dir;
                    }
                }
            }

        }
        //lastLocs.dequeue();
        return firstDirThatWorks;
    }
}

package Chow8;

import Chow8.utils.HashTable;
import Chow8.utils.Node;
import battlecode.common.*;

public class DeliveryDrone extends RobotPlayer {
    static final int DEFEND = 1;
    static final int ATTACK = 0;
    static final int DUMP_BAD_GUY = 2;
    static final int MOVE_LANDSCAPER = 3;
    static final int MOVE_OUR_UNIT_OUT = 4;
    static final int MOVE_UNIT_HIGH_LAND = 5;
    static final int MOVING_TO_UNIT_TO_MOVE = 6;
    static boolean terraformTime = false;
    static final int MOVE_OUR_UNIT_FAR = 7;
    static boolean moveMinersToHighland = false;
    static boolean lockAndDefend = false;
    static int role = ATTACK;
    static MapLocation attackLoc;

    static MapLocation waterLoc;

    static boolean gettingRushed = false; // HQ tells us this
    static int circledHQTimes = 0;

    static Direction initialDirectionToHQ;
    static boolean canCheckForCircling = false;

    static boolean wallIn = false; // whether or not HQ told us to start walling in BASE using landscapers

    static int wallSpaces = 0;
    static boolean toldToLockAndDefendByHQ = false;
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
        processBlocks(lastRoundsBlocks);


        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] nearbyNeutralRobots = rc.senseNearbyRobots(-1, Team.NEUTRAL);
        HashTable<Direction> dangerousDirections = new HashTable<>(4); // directioons that when moved in, will result in netgun death
        RobotInfo closestEnemyLandscaper = null;
        RobotInfo closestEnemyMiner = null;
        int closestEnemyLandscaperDist = 99999999;
        int closestEnemyMinerDist = 999999;

        if (nearbyEnemyRobots.length > 0) {
            lockAndDefend = true;
        }
        else if (!toldToLockAndDefendByHQ) {
            lockAndDefend = false;
        }
        RobotInfo enemyInHQSpace = null;
        int closestEnemyInHQSpace = 888888889;
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
                            if (info.location.distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE) {
                                if (dist < closestEnemyInHQSpace) {
                                    closestEnemyInHQSpace = dist;
                                    enemyInHQSpace = info;
                                }
                            }
                            break;
                        case MINER:
                            int dist2 = rc.getLocation().distanceSquaredTo(info.getLocation());
                            if (dist2 < closestEnemyMinerDist) {
                                closestEnemyMinerDist = dist2;
                                closestEnemyMiner = info;
                                if (debug) System.out.println("Found closer enemy miner at " + info.location);
                            }
                            if (info.location.distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE) {
                                if (dist2 < closestEnemyInHQSpace) {
                                    closestEnemyInHQSpace = dist2;
                                    enemyInHQSpace = info;
                                }
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
            if (dist <= 25 && (info.type == RobotType.NET_GUN) && info.getCooldownTurns() <= 2) {
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
        int distToNearestMiner = 9999999;
        int distToNearestMinerAdjacentToHQ = 9999999;
        int distToNearestAdjacentToHQLandscaper = 999999;
        RobotInfo nearestAdjacentToHQMiner = null;
        RobotInfo nearestLowMiner = null;
        RobotInfo nearestLandscaper = null; // nearest not on wall / available
        RobotInfo nearestAdjacentToHQLandscaper = null;
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
                    if (info.location.distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE) {
                        int dist = rc.getLocation().distanceSquaredTo(info.location);
                        if (dist < distToNearestAdjacentToHQLandscaper) {
                            distToNearestAdjacentToHQLandscaper = dist;
                            nearestAdjacentToHQLandscaper = info;
                        }
                    }
                    break;
                case MINER:
                    int dist = rc.getLocation().distanceSquaredTo(info.location);
                    if (rc.senseElevation(info.location) < DESIRED_ELEVATION_FOR_TERRAFORM - 2) {
                        if (dist < distToNearestMiner) {
                            distToNearestMiner = dist;
                            nearestLowMiner = info;
                        }
                    }
                    // find nearest miner adjacent to HQ to remove, do so if they aren't carrying soup
                    if (debug) System.out.println("Found miner at " + info.location + " | soup: " + info.getSoupCarrying());
                    if (info.location.distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE && info.getSoupCarrying() == 0) {

                        if (dist < distToNearestMinerAdjacentToHQ) {
                            distToNearestMinerAdjacentToHQ = dist;
                            nearestAdjacentToHQMiner = info;
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
        MapLocation nearestEmptyHighLand = null;
        int distToHighLand = 999999999;
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
                int elevation = rc.senseElevation(checkLoc);
                if (elevation >= DESIRED_ELEVATION_FOR_TERRAFORM && !rc.isLocationOccupied(checkLoc)) {
                    int dist = rc.getLocation().distanceSquaredTo(checkLoc);
                    // closest highland that isn't in HQ breahting space
                    if (dist < distToHighLand && checkLoc.distanceSquaredTo(HQLocation) > HQ_LAND_RANGE) {
                        nearestEmptyHighLand = checkLoc;
                        distToHighLand = dist;
                        if (debug) System.out.println(checkLoc + " is empty high land");
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
        // drone must always try and help our own landscapers when we decide to start walling in
        if (nearestLandscaper != null && wallIn) {
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

        // pick up miners on low land
        if (moveMinersToHighland) {
            if (nearestLowMiner != null) {
                int distToMiner = rc.getLocation().distanceSquaredTo(nearestLowMiner.location);
                if (distToMiner <= 2) {
                    if (rc.canPickUpUnit(nearestLowMiner.getID())) {
                        rc.pickUpUnit(nearestLowMiner.getID());
                        role = MOVE_UNIT_HIGH_LAND;
                    }
                }
            }
        }
        boolean skipAttack = false;

        // if there is a adjacent miner to HQ, then take it out ( assumed to be valid to take out )

        if (nearestAdjacentToHQMiner != null && nearestEmptyHighLand != null) {
            if (debug)
            if (rc.canPickUpUnit(nearestAdjacentToHQMiner.getID())) {
                // pick them up
                rc.pickUpUnit(nearestAdjacentToHQMiner.getID());
                // set role
                //role = MOVE_OUR_UNIT_OUT;
                role = MOVE_UNIT_HIGH_LAND;
            }
            // otherwise go to them
            else {
                setTargetLoc(nearestAdjacentToHQMiner.location);
                //role = MOVING_TO_UNIT_TO_MOVE;
                skipAttack = true;
            }
        }

        // we only do this if there is no enemy
        if (nearestAdjacentToHQLandscaper != null && !wallIn && enemyInHQSpace == null && nearestEmptyHighLand != null) {
            if (rc.canPickUpUnit(nearestAdjacentToHQLandscaper.getID())) {
                // pick them up
                rc.pickUpUnit(nearestAdjacentToHQLandscaper.getID());
                // set role
                //role = MOVE_OUR_UNIT_OUT;
                role = MOVE_UNIT_HIGH_LAND;
            }
            // otherwise go to them
            else {
                setTargetLoc(nearestAdjacentToHQLandscaper.location);
                //role = MOVING_TO_UNIT_TO_MOVE;
                skipAttack = true;
            }
        }

        if (role == MOVING_TO_UNIT_TO_MOVE) {

        }
        else if (role == MOVE_UNIT_HIGH_LAND) {
            if (nearestEmptyHighLand == null) {
                setTargetLoc(HQLocation);
            }
            else {
                if (debug) System.out.println("moving a miner to " + nearestEmptyHighLand);
                if (rc.getLocation().isAdjacentTo(nearestEmptyHighLand)) {
                    Direction dropDir = rc.getLocation().directionTo(nearestEmptyHighLand);
                    if (rc.canDropUnit(dropDir)) {
                        rc.dropUnit(dropDir);
                        role = ATTACK;
                    }
                    else {
                        // drop on any high land tile
                        int i = 0;
                        while(i++ < 8) {
                            dropDir = dropDir.rotateLeft();
                            MapLocation adjLoc = rc.adjacentLocation(dropDir);
                            if (rc.canSenseLocation(adjLoc)) {
                                int ele = rc.senseElevation(adjLoc);
                                if (ele >= DESIRED_ELEVATION_FOR_TERRAFORM) {
                                    if (rc.canDropUnit(dropDir)) {
                                        rc.dropUnit(dropDir);
                                        role = ATTACK;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    setTargetLoc(nearestEmptyHighLand);
                }
            }
        }
        // move landscaper onto a wall location
        else if (role == MOVE_LANDSCAPER) {

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
        // move a unit out of our wall
        else if (role == MOVE_OUR_UNIT_OUT) {
            // drop our unit anywhere but on the wall
            // if we are not in terraform time, we need to drop farther out outside of platform
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
        // move a unit out of our wall
        else if (role == MOVE_OUR_UNIT_FAR) {
            // drop our unit anywhere but on the wall
            // if we are not in terraform time, we need to drop farther out outside of platform
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
                //if (!holdingCow) {
                    // find water and drop the enemy unit, drop cows as well. Literally dropping cows on enemy is bad because
                    // we lose vision and die to netguns
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
                //}
                /*
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
                */
            }
            else {
                // this shouldn't ever happen
            }
        }
        else if (role == ATTACK && !skipAttack) {

            if (circledHQTimes >= 2 && !gettingRushed && friendlyDrones > 2) {
                attackLoc = closestMaybeHQ; // always attempt to attack enemy HQ after we go once around our OWN HQ
            }
            else if (gettingRushed) {
                attackLoc = HQLocation;
            }
            else {
                // check if we finished a circle
                Direction dirToHQ = rc.getLocation().directionTo(HQLocation);
                if (!canCheckForCircling) {
                    if (!dirToHQ.equals(initialDirectionToHQ)) {
                        canCheckForCircling = true;
                    }
                }
                else if (rc.getLocation().directionTo(HQLocation) == initialDirectionToHQ) {
                    circledHQTimes ++;
                }
            }
            // if not ordered to attack enemy HQ, do normal defending and attack
            if (attackHQ == false) {

                // if there is enemy, engage!
                // engage cows if there are 0 enemies and friend drones
                // don't engage if its near HQ

                if (closestEnemyMiner != null || closestEnemyLandscaper != null || (nearestCow != null && nearbyEnemyRobots.length == 0)) {
                    RobotInfo enemyToEngage = closestEnemyMiner;

                    // pick a new enemy to engage if we didnt find one or if the one we found is too close to enemy base
                    if (enemyToEngage == null || (enemyBaseLocation != null && enemyToEngage.location.distanceSquaredTo(enemyBaseLocation) <= 7)) enemyToEngage = closestEnemyLandscaper;
                    if (enemyToEngage == null || (enemyBaseLocation != null && enemyToEngage.location.distanceSquaredTo(enemyBaseLocation) <= 7)) enemyToEngage = nearestCow;
                    if (debug) System.out.println(enemyToEngage + " | enemy base?: " + enemyBaseLocation + " | enemy at? ");
                    if (enemyToEngage != null && (enemyBaseLocation == null || enemyToEngage.location.distanceSquaredTo(enemyBaseLocation) >= 8)) {
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
                                } else {
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

                            // add dangerous directions to HQ
                            if (enemyBaseLocation != null) {
                                Direction dirToEnemyBase = rc.getLocation().directionTo(enemyBaseLocation);
                                dangerousDirections.add(dirToEnemyBase);
                                dangerousDirections.add(dirToEnemyBase.rotateLeft());
                                dangerousDirections.add(dirToEnemyBase.rotateRight());
                            }
                        }
                    }
                    else {
                        // no dice?
                        setTargetLoc(rc.adjacentLocation(rc.getLocation().directionTo(attackLoc).opposite().rotateLeft().rotateLeft()));
                    }
                }

                // otherwise hover around attackLOC and fuzzy move
                else {
                    int distToAttackLoc = rc.getLocation().distanceSquaredTo(attackLoc);
                    // stick around, don't move in too close
                    if (lockAndDefend && attackLoc.equals(HQLocation)) {
                        if (debug) System.out.println("Defending HQ and told to lock and defend");
                        if ((distToAttackLoc <= 13 && distToAttackLoc >= 8) || distToAttackLoc == 18) {
                            // don't move, stay
                            setTargetLoc(null);
                        }
                        else {
                            // otherwise move
                            setTargetLoc(attackLoc);
                        }
                    }
                    else if (distToAttackLoc <= RobotType.DELIVERY_DRONE.sensorRadiusSquared + 8 ) {
                        //fuzzy
                        //targetLoc = rc.adjacentLocation(randomDirection());
                        if (debug) System.out.println("moving randomly around attack location");
                        // move outside range
                        setTargetLoc(rc.adjacentLocation(rc.getLocation().directionTo(attackLoc).opposite().rotateLeft().rotateLeft()));
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
                    // stick around farther if we know where enemy base is
                    // stick around farther also if it isnt time to attack
                    if (((enemyBaseLocation == null && distToAttackLoc >= 36) || (enemyBaseLocation != null && distToAttackLoc >= 48))) {
                        //move to just edge of base attack range.
                        setTargetLoc(attackLoc);
                        if (debug) rc.setIndicatorDot(rc.getLocation(), 10, 20,200);

                        // see enemy along the way, dump it
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

                    } else {
                        // don't move there if it isn't time yet, just move around like vultures
                        setTargetLoc(rc.adjacentLocation(rc.getLocation().directionTo(attackLoc).opposite().rotateRight().rotateRight()));
                        // if we waited long enough,
                        if (roundsToWaitBeforeAttack <= rc.getRoundNum() - receivedAttackHQMessageRound) {
                            if (debug) rc.setIndicatorDot(rc.getLocation(), 100, 20,200);
                            setTargetLoc(attackLoc);
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

        initialDirectionToHQ = rc.getLocation().directionTo(HQLocation); // used to circle around HQ once

        wallSpotLeft = true;
        for (int i = 8; --i>= 1; ) {
            int rn = rc.getRoundNum() -i;
            if (rn > 0) {
                processBlocks(rc.getBlock(rn));
            }
        }
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
    static void processBlocks(Transaction[] blocks)  {
        for (int i = blocks.length; --i >= 0; ) {
            int[] msg = blocks[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                // havent received message yet and is now receiving it
                if ((msg[1] ^ DRONES_ATTACK) == 0 && receivedAttackHQMessageRound == -1) {
                    int distToHQ = rc.getLocation().distanceSquaredTo(HQLocation);
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
                else if ((msg[1] ^ LOCK_AND_DEFEND) == 0) {
                    lockAndDefend = true;
                    toldToLockAndDefendByHQ = true;
                }
                else if ((msg[1] ^ STOP_LOCK_AND_DEFEND) == 0) {
                    lockAndDefend = false;
                    toldToLockAndDefendByHQ = false;
                }
                else if ((msg[1] ^ TERRAFORM_ALL_TIME) == 0) {
                    // means that we want miners to stop mining and work on building stuff on platform
                    moveMinersToHighland = true;
                    terraformTime = true;
                    // pick up any miners and drop on height 10 tiles

                }
                else if ((msg[1] ^ WALL_IN) == 0 || (msg[1] ^ TERRAFORM_AND_WALL_IN) == 0) {
                    // time to put landscapers in their spots!
                    wallIn = true;
                }
                else if ((msg[1] ^ GETTING_RUSHED_HELP) == 0) {
                    gettingRushed = true;
                }
                else if ((msg[1] ^ NO_LONGER_RUSHED) == 0) {
                    gettingRushed = false;
                }
            }
        }
    }
}

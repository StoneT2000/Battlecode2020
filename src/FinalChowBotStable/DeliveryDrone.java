package FinalChowBotStable;

import FinalChowBotStable.utils.HashTable;
import FinalChowBotStable.utils.Node;
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


    static boolean pickUpMinersForAttack = true;
    static boolean buildIsland = false;

    static int roundsSpentCrunching = 0;
    static final int RUSH_DEFEND = 8;
    static MapLocation locOfRushUnit = null;
    static int IDOfRushUnit = -1;

    static boolean skipHelping = false;

    static boolean moveMinersToHighland = false;
    static boolean lockAndDefend = false;
    static int role = ATTACK;
    static MapLocation attackLoc;
    static boolean swarmIn = false;

    static RobotInfo friendlyUnitHeld = null;
    static boolean attackWithAllUnits = false;

    static MapLocation waterLoc;

    static boolean gettingRushed = false; // HQ tells us this
    static int circledHQTimes = 0;

    static Direction initialDirectionToHQ;
    static boolean canCheckForCircling = false;

    static boolean wallIn = false; // whether or not HQ told us to start walling in BASE using landscapers

    static MapLocation lastSoupLocAnnounced = null;

    static boolean noMoreLandscapersToAttack = false;

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
    static HashTable<MapLocation> enemyNetguns = new HashTable<>(50);


    static Direction lastDir = null;
    public static void run() throws GameActionException {
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        processBlocks(lastRoundsBlocks);


        HashTable<Direction> dangerousDirections = new HashTable<>(4); // directioons that when moved in, will result in netgun death

        RobotInfo closestEnemyLandscaper = null;
        RobotInfo closestEnemyMiner = null;
        int closestEnemyLandscaperDist = 99999999;
        int closestEnemyMinerDist = 999999;


        RobotInfo enemyInHQSpace = null;
        int closestEnemyInHQSpace = 888888889;

        RobotInfo rushUnitToAttack = null;

        // look for nearest empty wall
        int closestEmptyWallLocDist = 9999999;
        MapLocation closestEmptyWallLoc = null;
        MapLocation closesetEmptyMAINWALL = null;
        boolean thisRoundFoundSpot = false;

        // look for empty walls to put units on if we are close to HQ
        if (rc.getLocation().distanceSquaredTo(HQLocation) <= MAX_TERRAFORM_DIST) {
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
        }
        if (!thisRoundFoundSpot) {
            wallSpotLeft = false;
        }

        int friendlyDrones = 0;
        RobotInfo nearestCow = null;
        int distToNearestCow =  9999999;
        int distToNearestLandscaper = 99999999;
        //int distToNearestMiner = 9999999;
        //int distToNearestMinerAdjacentToHQ = 9999999;
        //int distToNearestAdjacentToHQLandscaper = 999999;
        RobotInfo nearestAdjacentToHQMiner = null;
        RobotInfo nearestLowMiner = null;
        RobotInfo nearestLandscaper = null; // nearest not on wall / available
        RobotInfo nearestLandscaperForAttack = null;
        RobotInfo nearestMinerForAttack = null;
        //int distToNearestMinerForAttack = 9999999;
        int distToNearestLandscaperForAttack = 9999999;
        RobotInfo nearestAdjacentToHQLandscaper = null;
        boolean designatedDrone = true;
        if (rc.getLocation().distanceSquaredTo(HQLocation) > 36) {
            designatedDrone = false;
        }
        int minerCount = 0;
        // turn off cow pickups...
        /*
        for (int i = nearbyNeutralRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyNeutralRobots[i];
            switch(info.type) {
                case COW:
                    if (debug) System.out.println("Found COW at " + info.location);
                    int dist = rc.getLocation().distanceSquaredTo(info.location);
                    if (info.location.distanceSquaredTo(HQLocation) <= MAX_TERRAFORM_DIST + 24 && dist < distToNearestCow) {
                        nearestCow = info;
                        distToNearestCow = dist;
                    }
            }
        }
        */

        /* SCOUTING CODE */

        /* BIG BFS LOOP ISH */
        int minDistToFlood = 99999999;
        if (waterLoc != null) {
            minDistToFlood = rc.getLocation().distanceSquaredTo(waterLoc);
        }
        MapLocation nearestEmptyHighLand = null;
        //int distToHighLand = 999999999;

        int enemyDrones = 0;
        MapLocation soupLoc = null;
        //MapLocation nearestDropZoneLoc = null;
        //int distToNearestDropZoneLoc = 99999999;

        int soupNearby = 0;
        if (debug) System.out.println("BFS Start: " + Clock.getBytecodeNum());
        for (int i = 0; i < Constants.BFSDeltas24.length; i++) {
            int[] deltas = Constants.BFSDeltas24[i];
            MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
            if (rc.canSenseLocation(checkLoc)) {
                int dist = rc.getLocation().distanceSquaredTo(checkLoc);
                RobotInfo info = rc.senseRobotAtLocation(checkLoc);
                // if flooding and minDist is not that good
                if (rc.senseFlooding(checkLoc)) {
                    if (dist < minDistToFlood && dist != 0 && info == null) {
                        minDistToFlood = dist;
                        waterLoc = checkLoc;
                    }
                }
                /* for finding dropzones when attacking
                else {
                    if (enemyBaseLocation != null && dist < distToNearestDropZoneLoc && checkLoc.distanceSquaredTo(enemyBaseLocation) <= DROP_ZONE_RANGE_OF_HQ) {
                        nearestDropZoneLoc = checkLoc;
                        distToNearestDropZoneLoc = dist;
                    }
                }*/
                if (info == null) {
                    enemyNetguns.remove(checkLoc);

                    if (nearestEmptyHighLand == null) {
                        // not occupied and good elevation, consider it. must be high enough and not too high
                        int elevation = rc.senseElevation(checkLoc);
                        if (elevation >= DESIRED_ELEVATION_FOR_TERRAFORM && elevation <= DESIRED_ELEVATION_FOR_TERRAFORM + 6) {
                            // closest highland that isn't in HQ breathing space
                            if (checkLoc.distanceSquaredTo(HQLocation) > HQ_LAND_RANGE) {
                                nearestEmptyHighLand = checkLoc;
                                //distToHighLand = dist;
                            }
                        }
                    }
                }
                else {

                    if (info.type != RobotType.NET_GUN || info.team == rc.getTeam()) {
                        enemyNetguns.remove(checkLoc);
                    }
                    // everything below is merged from sense nearby robots stuff
                    if (info.team == rc.getTeam()) {
                        switch (info.type) {
                            case DELIVERY_DRONE:
                                friendlyDrones++;
                                if (info.getID() < rc.getID()) {
                                    designatedDrone = false;
                                }
                                break;
                            case LANDSCAPER:

                                // if not on man wall, and we have a empty loc on main wall, consider it
                                // if not on second wall either, consider ir. WE only consider if we think there is wall space left
                                if (nearestLandscaper == null && wallSpotLeft  && !MainWall.contains(info.location) ) {
                                    if (!SecondWall.contains(info.location)) {
                                        distToNearestLandscaper = dist;
                                        nearestLandscaper = info;
                                    }
                                }
                                int distToHQ = info.location.distanceSquaredTo(HQLocation);
                                // find nearest landscaper for attack
                                if (attackWithAllUnits && nearestLandscaperForAttack == null) {
                                    //nearestLandscaper
                                    if (enemyBaseLocation != null && dist < distToNearestLandscaperForAttack && distToHQ >= 16 && info.location.distanceSquaredTo(enemyBaseLocation) > DROP_ZONE_RANGE_OF_HQ) {
                                        if (noMoreLandscapersToAttack || info.getDirtCarrying() >= 20) {
                                            nearestLandscaperForAttack = info;
                                            distToNearestLandscaperForAttack = dist;
                                        }
                                    }
                                }
                                if (nearestAdjacentToHQLandscaper == null && distToHQ <= HQ_LAND_RANGE) {
                                    //distToNearestAdjacentToHQLandscaper = dist;
                                    nearestAdjacentToHQLandscaper = info;
                                }
                                break;
                            case MINER:
                                if (nearestLowMiner == null && rc.senseElevation(info.location) < DESIRED_ELEVATION_FOR_TERRAFORM - 2) {
                                    //distToNearestMiner = dist;
                                    nearestLowMiner = info;
                                }
                                minerCount++;
                                // find nearest miner adjacent to HQ to remove, do so if they aren't carrying soup
                                if (nearestAdjacentToHQMiner == null && info.location.distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE && info.getSoupCarrying() == 0) {
                                    //distToNearestMinerAdjacentToHQ = dist;
                                    nearestAdjacentToHQMiner = info;
                                }
                                // if rounds spent crunching is past max crunching rounds (and basically we haven't won yet)
                                // pick up miners, and only the even ID ones
                                if (pickUpMinersForAttack && attackWithAllUnits && nearestMinerForAttack == null && info.getID() % 2 == 0) {
                                    // pick up miners for attack if they arent in the drop zone for attakc
                                    if (debug) System.out.println("Miner is contender for attack");
                                    if (enemyBaseLocation != null && info.location.distanceSquaredTo(enemyBaseLocation) > DROP_ZONE_RANGE_OF_HQ) {
                                        if (debug) System.out.println("Found nearest attack miner");
                                        nearestMinerForAttack = info;
                                    }
                                }
                                break;
                        }
                    }
                    else if (info.team == enemyTeam) {
                        // is an enemy unit
                        switch (role) {
                            case RUSH_DEFEND:
                                if (info.getID() == IDOfRushUnit) {
                                    // we found the rushing unit
                                    // if rushing unit is a miner, switch roles and proceed as if we were in ATTACK mode
                                    rushUnitToAttack = info;
                                    if (info.type == RobotType.MINER) {
                                        role = ATTACK;

                                        // code copied from miner switch case when attacking
                                        if (dist < closestEnemyMinerDist) {
                                            closestEnemyMinerDist = dist;
                                            closestEnemyMiner = info;
                                            if (debug) System.out.println("Found closer enemy miner at " + info.location);
                                        }
                                        if (info.location.distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE) {
                                            if (dist < closestEnemyInHQSpace) {
                                                closestEnemyInHQSpace = dist;
                                                enemyInHQSpace = info;
                                            }
                                        }
                                    }
                                    // else if delivery drone, stay in role because in this role we follow that delivery drone until we see a miner
                                    else if (info.type == RobotType.DELIVERY_DRONE) {
                                        // if rushing drone isn't carrying anything, stop, go back to attack mode
                                        if (!info.isCurrentlyHoldingUnit()) {
                                            role = ATTACK;
                                        }
                                    }
                                }
                                // don't break here, we want rush defenders to look for closest units as well.
                            case ATTACK:
                                switch(info.type) {
                                    case LANDSCAPER:
                                        if (closestEnemyLandscaper == null) {
                                            closestEnemyLandscaperDist = dist;
                                            closestEnemyLandscaper = info;
                                            if (debug) System.out.println("Found closer enemy landscaper at " + info.location);
                                        }
                                        if (enemyInHQSpace == null && info.location.distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE) {
                                            if (dist < closestEnemyInHQSpace) {
                                                closestEnemyInHQSpace = dist;
                                                enemyInHQSpace = info;
                                            }
                                        }
                                        break;
                                    case MINER:
                                        if (closestEnemyMiner == null && dist < closestEnemyMinerDist) {
                                            closestEnemyMinerDist = dist;
                                            closestEnemyMiner = info;
                                            if (debug) System.out.println("Found closer enemy miner at " + info.location);
                                        }
                                        if (enemyInHQSpace == null && info.location.distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE) {
                                            if (dist < closestEnemyInHQSpace) {
                                                closestEnemyInHQSpace = dist;
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
                                    case DELIVERY_DRONE:
                                        enemyDrones++;
                                        break;
                                }
                                break;

                        }
                        if (info.type == RobotType.NET_GUN) {
                            if (enemyNetguns.add(info.location)) {
                                announceNET_GUN_LOCATION(info.location);
                            }
                            if (dist <= 25) {
                                // dangerous netgun, move somewhere not in range!

                                Direction badDir = rc.getLocation().directionTo(info.location);
                                dangerousDirections.add(badDir);
                                Direction badDirLeft = badDir.rotateLeft();
                                Direction badDirRight = badDir.rotateRight();
                                if (rc.adjacentLocation(badDirLeft).distanceSquaredTo(info.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                                    dangerousDirections.add(badDirLeft);
                                }
                                if (rc.adjacentLocation(badDirRight).distanceSquaredTo(info.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                                    dangerousDirections.add(badDirRight);
                                }
                            }
                        }

                    }
                    else {
                        if (rc.getRoundNum() >= 1400 && nearestCow == null)
                            nearestCow = info;
                    }
                }

                int soupHere = rc.senseSoup(checkLoc);
                soupNearby += soupHere;
                if (soupLoc == null && soupHere > 0) {
                    soupLoc = checkLoc;
                }
            }
            else {
                // check for enemy guns
                if (enemyNetguns.contains(checkLoc)) {
                    dangerousDirections.add(rc.getLocation().directionTo(checkLoc)); // add this dir to dangers
                    dangerousDirections.add(rc.getLocation().directionTo(checkLoc).rotateLeft());
                    dangerousDirections.add(rc.getLocation().directionTo(checkLoc).rotateRight());
                }
            }
        }
        if (debug) System.out.println("BFS End: " + Clock.getBytecodeNum());
        if (lastSoupLocAnnounced == null || rc.getLocation().distanceSquaredTo(lastSoupLocAnnounced) >= 16) {
            // if more soup per miner here, announce it
            if (soupNearby / (minerCount + 0.1) >= 200 && hasEmptyTileAround(soupLoc)) {
                announceSoupLocation(rc.getLocation(), 1, soupNearby, minerCount);
                lastSoupLocAnnounced = rc.getLocation();
            }
        }

        // Store closest enemy HQ position
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
                           if (rc.canPickUpUnit(info.getID())) {
                               rc.pickUpUnit(info.getID());
                               role = MOVE_OUR_UNIT_OUT;

                           }
                           else {
                               setTargetLoc(info.location);
                           }
                       }
                    }
                    else {
                        if (debug) System.out.println("Found unit to try and help");
                        if (rc.canPickUpUnit(nearestLandscaper.getID())) {
                            rc.pickUpUnit(nearestLandscaper.getID());
                            role = MOVE_LANDSCAPER;
                            break;
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
                        if (debug) System.out.println("Found unit to try and help, place on support loc");

                        // adjacent, pick it up, drop in right place
                        if (rc.canPickUpUnit(nearestLandscaper.getID())) {
                            rc.pickUpUnit(nearestLandscaper.getID());
                            role = MOVE_LANDSCAPER;
                            break;
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
                if (rc.canPickUpUnit(nearestLowMiner.getID())) {
                    rc.pickUpUnit(nearestLowMiner.getID());
                    friendlyUnitHeld = nearestLowMiner;
                    role = MOVE_UNIT_HIGH_LAND;
                }
            }
        }
        boolean skipAttack = false;

        if (rc.getRoundNum() <= 230) {
            skipHelping = true;
        }
        // if there is a adjacent miner to HQ, then take it out ( assumed to be valid to take out )

        if (nearestAdjacentToHQMiner != null && nearestEmptyHighLand != null && enemyInHQSpace == null && !skipHelping) {
            if (rc.canPickUpUnit(nearestAdjacentToHQMiner.getID())) {
                // pick them up
                rc.pickUpUnit(nearestAdjacentToHQMiner.getID());
                friendlyUnitHeld = nearestAdjacentToHQMiner;
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
        if (nearestAdjacentToHQLandscaper != null && !wallIn && enemyInHQSpace == null && nearestEmptyHighLand != null && !skipHelping) {
            if (rc.canPickUpUnit(nearestAdjacentToHQLandscaper.getID())) {
                // pick them up
                rc.pickUpUnit(nearestAdjacentToHQLandscaper.getID());
                friendlyUnitHeld = nearestAdjacentToHQLandscaper;
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

        // special case of ATTACK. We don't circle, we are given an enemy coordinate and we go there
        if (role == RUSH_DEFEND) {
            setTargetLoc(locOfRushUnit);
            if (rc.canSenseLocation(locOfRushUnit) && rushUnitToAttack == null) {
                // if we can see the posted location and see no rush unit and we are still in this role, something is clearly wrong
                role = ATTACK;
            }
            else if (rushUnitToAttack != null) {
                if (debug) System.out.println("Following rushing drone at " + rushUnitToAttack.location);
                // found rushing unit, it is a drone so follow it
                setTargetLoc(rushUnitToAttack.location);

            }
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
                        friendlyUnitHeld = null;
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
                                        friendlyUnitHeld = null;
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
                        friendlyUnitHeld = null;
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
                        friendlyUnitHeld = null;
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
                        friendlyUnitHeld = null;
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
                        friendlyUnitHeld = null;
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
                        // if can't dump, look around adjacent locs
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
        }
        else if (role == ATTACK && !skipAttack) {

            if (circledHQTimes >= 0 && !gettingRushed) {
                if ((rc.getRoundNum() > 240 && friendlyDrones >= 1 && !designatedDrone) || (rc.getRoundNum() < 240 && !designatedDrone)) {
                    if (!toldToLockAndDefendByHQ) {
                        attackLoc = closestMaybeHQ; // always attempt to attack enemy HQ after we go once around our OWN HQ
                    }
                }
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

            if (enemyBaseLocation != null && rc.getLocation().distanceSquaredTo(enemyBaseLocation) <= 25 && swarmIn == false) {
                Direction dirToEnemyBase = rc.getLocation().directionTo(enemyBaseLocation);
                dangerousDirections.add(dirToEnemyBase);
                dangerousDirections.add(dirToEnemyBase.rotateLeft());
                dangerousDirections.add(dirToEnemyBase.rotateRight());
                if (debug) System.out.println("Adding enemy base bad dirs: " + dirToEnemyBase + ", " + dirToEnemyBase.rotateRight() + ", " + dirToEnemyBase.rotateLeft());
            }

            // if not ordered to attack enemy HQ, do normal defending and attack
            if (attackHQ == false) {

                // if there is enemy, engage!
                // engage cows if there are 0 enemies and friend drones
                // don't engage if its near HQ

                if (closestEnemyMiner != null || closestEnemyLandscaper != null) {
                    RobotInfo enemyToEngage = closestEnemyMiner;

                    // pick a new enemy to engage if we didnt find one or if the one we found is too close to enemy base
                    if (enemyToEngage == null || (enemyBaseLocation != null && enemyToEngage.location.distanceSquaredTo(enemyBaseLocation) <= 7)) enemyToEngage = closestEnemyLandscaper;
                    if (enemyToEngage == null || (enemyBaseLocation != null && enemyToEngage.location.distanceSquaredTo(enemyBaseLocation) <= 7)) enemyToEngage = nearestCow;
                    if (debug) System.out.println(enemyToEngage + " | enemy base: " + enemyBaseLocation + " | enemy at? ");
                    if (enemyToEngage != null && (enemyBaseLocation == null || enemyToEngage.location.distanceSquaredTo(enemyBaseLocation) >= 8)) {
                        if (debug) System.out.println("ENGAGING ENEMY at " + enemyToEngage.location);
                        if (rc.canPickUpUnit(enemyToEngage.getID())) {
                            rc.pickUpUnit(enemyToEngage.getID());
                            if (enemyToEngage.getType() != RobotType.COW) {
                                role = DUMP_BAD_GUY;
                                //targetLoc = waterLoc;
                                setTargetLoc(waterLoc);

                                // if we were previously skipping helping code, enable it again
                                if (skipHelping) {
                                    skipHelping = false;
                                }
                            } else {
                                role = DUMP_BAD_GUY;
                                //holdingCow = true;
                                //targetLoc = closestMaybeHQ;
                                setTargetLoc(closestMaybeHQ);
                                if (skipHelping) {
                                    skipHelping = false;
                                }
                            }
                        }
                        else {
                            // not near enemy yet, set targetLoc to this so we move towards enemey.
                            //targetLoc = enemyToEngage.location;
                            setTargetLoc(enemyToEngage.location);

                            // add dangerous directions to HQ
                            if (debug) System.out.println("going closer to enemy");

                        }
                    }
                    else {
                        // no dice?
                        rotateCircularly(attackLoc);
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
                    // circularly move around if it is HQLocation
                    else if (attackLoc.equals(HQLocation) && distToAttackLoc <= RobotType.DELIVERY_DRONE.sensorRadiusSquared + 8 ) {
                        //fuzzy
                        //targetLoc = rc.adjacentLocation(randomDirection());
                        if (debug) System.out.println("moving randomly around attack location");
                        // move outside range
                        rotateCircularly(attackLoc);
                    } else {
                        //targetLoc = attackLoc; // move towards attack loc first if not near it yet.
                        setTargetLoc(attackLoc);
                    }
                    // early game stay close
                    if (attackLoc.equals(HQLocation) && rc.getRoundNum() <= 250) {
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
                // if we are ordered to attackWithAllUnits, then pickup nearest landscaper and then head over
                // pick up if unit not within 16 of enemy
                boolean tryingToPickupUnit = false;
                if (attackWithAllUnits && enemyBaseLocation != null) {
                    if (nearestMinerForAttack != null) {
                        if (rc.canPickUpUnit(nearestMinerForAttack.getID())) {
                            rc.pickUpUnit(nearestMinerForAttack.getID());
                            friendlyUnitHeld = nearestMinerForAttack;
                        }
                        else {
                            setTargetLoc(nearestMinerForAttack.location);
                            tryingToPickupUnit = true;
                        }
                    }
                    else if (nearestLandscaperForAttack != null && nearestLandscaperForAttack.location.distanceSquaredTo(enemyBaseLocation) > 16) {
                        if (rc.canPickUpUnit(nearestLandscaperForAttack.getID())) {
                            rc.pickUpUnit(nearestLandscaperForAttack.getID());
                            friendlyUnitHeld = nearestLandscaperForAttack;
                        }
                        else {
                            setTargetLoc(nearestLandscaperForAttack.location);
                            tryingToPickupUnit = true;
                        }
                    }
                }
                // no nearby units to take from? go home
                /*
                if (nearestLandscaperForAttack == null && nearestMinerForAttack == null && attackWithAllUnits && !rc.isCurrentlyHoldingUnit()) {
                    setTargetLoc(HQLocation);

                } */

                if (attackLoc != null && !tryingToPickupUnit) {
                    int distToAttackLoc = rc.getLocation().distanceSquaredTo(attackLoc);
                    // stick around, don't move in
                    // stick around farther if we know where enemy base is (ourside of HQ vision approx)
                    // stick around farther also if it isnt time to attack
                    // stick around distance depends on positioning
                    // no unit drones stay closes to pick out landscapers
                    // drones with landscapers come next to drop landscapers on walls or make island
                    // thne drone swith miners
                    int strayDist = 48;
                    if (noMoreLandscapersToAttack) {
                        strayDist = 92;
                    }
                    if (friendlyUnitHeld != null) {
                        if (friendlyUnitHeld.type == RobotType.LANDSCAPER) {

                            // keep far stray distance if there are landscapers on their wall still
                            if (!noMoreLandscapersToAttack) {
                                strayDist = 65;
                            }
                            // if no more landscapers, but yes drones, stay farther...
                            else {
                                strayDist = 48;
                            }
                        }
                        else if (friendlyUnitHeld.type == RobotType.MINER) {
                            strayDist = 65;
                        }
                    }
                    // make sure we are still in prepare to crunch and crunch mode, so in this time period, we always move to edge of base attack range
                    if (roundsSpentCrunching <= MAX_CRUNCH_ROUNDS && ((enemyBaseLocation == null && distToAttackLoc >= 36) || (enemyBaseLocation != null && distToAttackLoc >= strayDist))) {
                        // when still crunching,  and not at edge of attack range... move to edge (stray dist)
                        setTargetLoc(attackLoc);
                        if (debug) rc.setIndicatorDot(rc.getLocation(), 10, 20,200);

                        // see enemy along the way, dump it
                        if (!rc.isCurrentlyHoldingUnit() && (closestEnemyMiner != null || closestEnemyLandscaper != null)) {
                            RobotInfo enemyToEngage = closestEnemyLandscaper;
                            if (enemyToEngage == null) enemyToEngage = closestEnemyMiner;
                            if (debug) System.out.println("ENGAGING ENEMY at " + enemyToEngage.location);
                            if (debug) rc.setIndicatorLine(rc.getLocation(), enemyToEngage.location, 100, 200, 10);
                            if (rc.canPickUpUnit(enemyToEngage.getID())) {
                                rc.pickUpUnit(enemyToEngage.getID());
                                role = DUMP_BAD_GUY;
                                //targetLoc = waterLoc;
                                setTargetLoc(waterLoc);

                            } else {
                                // not near enemy yet, set targetLoc to enemy location so we move towards enemey.
                                //targetLoc = enemyToEngage.location;
                                setTargetLoc(enemyToEngage.location);
                            }
                        }

                        if (!rc.isCurrentlyHoldingUnit()) {
                            if (noMoreLandscapersToAttack) {
                                attackLoc = HQLocation;
                                setTargetLoc(HQLocation);
                            }
                        }

                    }
                    // keep this stray distance if we aren't swarming
                    // if swarming, we go to next block and move in appropriately
                    // we STOP swarming in if no landscapers and no unit.
                    else if(enemyBaseLocation != null && distToAttackLoc < strayDist && !swarmIn) {
                        setTargetLoc(rc.adjacentLocation(rc.getLocation().directionTo(enemyBaseLocation).opposite()));
                    }
                    // otherwise if we are in the edge of attack range or if rounds spent crunching is past the maximum
                    else {
                        // don't move there if it isn't time yet, just move around like vultures
                        rotateCircularlyOneDirection(attackLoc);

                        // if we waited long enough, and told to swarm by HQ
                        if (swarmIn) {
                            if (debug) rc.setIndicatorDot(rc.getLocation(), 100, 20,200);
                            setTargetLoc(attackLoc);
                            roundsSpentCrunching++;
                            if (roundsSpentCrunching > MAX_CRUNCH_ROUNDS) {
                                setIslandCenterAndOtherConstants();
                                pickUpMinersForAttack = true; // begin island making + miners to demolish drones!!
                                buildIsland = true; // begin island making
                            }
                            // no drones seen? dont build island
                            if (enemyDrones <= 0) {
                                buildIsland = false;
                            }

                            if (rc.getRoundNum() >= 1500 && rc.getLocation().distanceSquaredTo(enemyBaseLocation) <= 5) {
                                announceMessage(ATTACKED_ENEMY_WALL);
                                if (closestEnemyLandscaper == null) {
                                    announceMessage(NO_LANDSCAPERS_LEFT_ON_ENEMY_HQ);
                                }
                            }


                            // begin attacking nearest enemy unit

                            if (!rc.isCurrentlyHoldingUnit() && (closestEnemyMiner != null || closestEnemyLandscaper != null)) {

                                RobotInfo enemyToEngage = closestEnemyLandscaper;
                                if (enemyToEngage == null) enemyToEngage = closestEnemyMiner;
                                if (debug) System.out.println("ENGAGING ENEMY at " + enemyToEngage.location);
                                if (debug) rc.setIndicatorLine(rc.getLocation(), enemyToEngage.location, 100, 200, 10);
                                if (rc.canPickUpUnit(enemyToEngage.getID())) {
                                    rc.pickUpUnit(enemyToEngage.getID());
                                    role = DUMP_BAD_GUY;
                                    if (enemyToEngage.location.isAdjacentTo(enemyBaseLocation)) {
                                        // announce we attacked main enemy wall as we are on attack HQ mode and swarming and we picked it up
                                        announceMessage(ATTACKED_ENEMY_WALL);
                                    }
                                    //targetLoc = waterLoc;
                                    setTargetLoc(waterLoc);

                                } else {
                                    // not near enemy yet, set targetLoc to enemy location so we move towards enemey.
                                    //targetLoc = enemyToEngage.location;
                                    setTargetLoc(enemyToEngage.location);
                                }

                            }
                            if (!rc.isCurrentlyHoldingUnit()) {
                                if (pickUpMinersForAttack && buildIsland) {
                                    // move away from island center
                                    setTargetLoc(HQLocation);
                                    attackLoc = HQLocation;
                                }
                                if (noMoreLandscapersToAttack) {
                                    attackLoc = HQLocation;
                                    setTargetLoc(HQLocation);
                                }
                            }
                            if (attackWithAllUnits && rc.isCurrentlyHoldingUnit() && friendlyUnitHeld != null && enemyBaseLocation != null) {
                                // drop onto wall land near enemy HQ
                                // 1. drop onto enemy wall
                                // 2. drop adjacent to location of high land (happening because the wall is all filled up)
                                // 3. head towards nearest drop zone location

                                if (friendlyUnitHeld.type == RobotType.LANDSCAPER) {
                                    boolean droppedUnit = false;
                                    for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i >= 0; ) {
                                        int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
                                        MapLocation checkLoc = enemyBaseLocation.translate(deltas[0], deltas[1]);
                                        if (rc.onTheMap(checkLoc) && rc.getLocation().isAdjacentTo(checkLoc)) {
                                            Direction dirToWallLoc = rc.getLocation().directionTo(checkLoc);
                                            if (rc.canSenseLocation(checkLoc)) {
                                                if (!rc.senseFlooding(checkLoc) && rc.canDropUnit(dirToWallLoc)) {
                                                    rc.dropUnit(dirToWallLoc);
                                                    droppedUnit = true;
                                                    friendlyUnitHeld = null;
                                                    announceMessage(ATTACKED_ENEMY_WALL);
                                                    break;
                                                }

                                            }
                                        }
                                    }
                                    // if can't drop on enemy wall in like 100 turns after we were told to swarm in, all drop near this specific loc
                                    // which is about 6 tiles from enemy base in direction to my base (easy coordination)

                                    if (!droppedUnit && roundsSpentCrunching > MAX_CRUNCH_ROUNDS && buildIsland) {
                                        // island locations...

                                        if (rc.getLocation().distanceSquaredTo(islandCenter) > 2) {
                                            setTargetLoc(islandCenter);
                                        }
                                        else {
                                            if (rc.canSenseLocation(islandCenter)) {
                                                if (rc.senseFlooding(islandCenter)) {
                                                    Direction dirToCenter = rc.getLocation().directionTo(islandCenter);
                                                    if (rc.canDropUnit(dirToCenter)) {
                                                        rc.dropUnit(dirToCenter);
                                                    }
                                                    else if (dirToCenter.equals(Direction.CENTER)) {
                                                        rc.disintegrate();
                                                    }

                                                }
                                                else {
                                                    // not flooded, plop anywhere adjacent to island center then
                                                    for (int i = directions.length; --i>=1;) {
                                                        Direction dropDir = directions[i];
                                                        MapLocation dropLoc = islandCenter.add(dropDir);
                                                        if (rc.senseFlooding(dropLoc)) {
                                                            if (rc.canDropUnit(dropDir)) {
                                                                rc.dropUnit(dropDir);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    }

                                }
                                else if (friendlyUnitHeld.type == RobotType.MINER) {
                                    // drop on non flooded place with no buildings nearby and has buildable land
                                    int i = 0;
                                    Direction dropDir = Direction.NORTH;
                                    while (i++ < 8) {
                                        MapLocation dropLoc = rc.adjacentLocation(dropDir);
                                        // drop on place not flooding and is close enough to enemy base and has open land adjacent
                                        int dropLocDistToHQ = dropLoc.distanceSquaredTo(enemyBaseLocation);
                                        if (rc.canSenseLocation(dropLoc) && (dropLocDistToHQ <= DROP_ZONE_RANGE_OF_HQ || (!buildIsland && enemyDrones <= 0 && dropLocDistToHQ <= 2)) && locHasAdjacentBuildableLand(dropLoc)) {
                                            // noMoreLandscapersToAttack
                                            if (!rc.senseFlooding(dropLoc)) {
                                                if (rc.canDropUnit(dropDir)) {
                                                    rc.dropUnit(dropDir);
                                                    friendlyUnitHeld = null;
                                                    break;
                                                }
                                            }
                                        }

                                        dropDir = dropDir.rotateLeft();
                                    }
                                    // check center, if center works, then disintegrate
                                    MapLocation dropCenterLoc = rc.getLocation();
                                    // drop on place not flooding and is close enough to enemy base and has open land adjacent
                                    if (dropCenterLoc.distanceSquaredTo(enemyBaseLocation) <= DROP_ZONE_RANGE_OF_HQ && locHasAdjacentBuildableLand(dropCenterLoc) && (enemyDrones > 0 || buildIsland)) {

                                        if (debug) System.out.println("I'm in range!");
                                        if (!rc.senseFlooding(dropCenterLoc)) {
                                            if (debug) System.out.println("I'm on flood!");
                                            rc.disintegrate();
                                        }
                                    }
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
                if (!dir.equals(Direction.CENTER) && rc.canMove(dir)) {
                    lastDir = dir;
                    rc.move(dir);
                }
                else {
                    /*
                    //were ready but didn't move...
                    if (rc.isReady()) {
                        lastDir = null;
                    }*/
                }
            }
        }
    }
    public static void setup() throws GameActionException {
        storeHQLocationAndGetConstants();
        storeEnemyHQLocations();
        attackLoc = HQLocation;
        // find FC that produced us, and set it as attackLoc.
        for (Direction dir: directions) {
            MapLocation potentialCenterLoc = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(potentialCenterLoc)) {
                if (rc.isLocationOccupied(potentialCenterLoc)) {
                    RobotInfo info = rc.senseRobotAtLocation(potentialCenterLoc);
                    if (info.type == RobotType.FULFILLMENT_CENTER && info.team == rc.getTeam()) {
                        attackLoc = potentialCenterLoc;
                        break;
                    }
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
        // go with most greedy move

        for (int i = Constants.DroneBlindSpots.length; --i>= 0; ) {
            int[] deltas = Constants.DroneBlindSpots[i];
            MapLocation spot = rc.getLocation().translate(deltas[0], deltas[1]);
            if (enemyNetguns.contains(spot)) {
                dangerousDirections.add(rc.getLocation().directionTo(spot)); // add this dir to dangers
                dangerousDirections.add(rc.getLocation().directionTo(spot).rotateLeft());
                dangerousDirections.add(rc.getLocation().directionTo(spot).rotateRight());
            }
        }

        Direction greedyDir = Direction.CENTER;
        int closestDist = 999999999;
        for (int i = 7; --i >= 0; ) {

            MapLocation adjLoc = rc.adjacentLocation(dir);
            int dist = adjLoc.distanceSquaredTo(target);
            if (!dangerousDirections.contains(dir) && rc.canSenseLocation(adjLoc) && (lastDir == null || !greedyDir.equals(lastDir))) {
                // check if its too close to enemy net guns

                if (rc.canMove(dir)) {
                    if (dist < closestDist) {
                        greedyDir = dir;
                        closestDist = dist;
                    }
                }
            }
            dir = dir.rotateLeft();
        }
        return greedyDir;
    }
    static void processBlocks(Transaction[] blocks) throws GameActionException {
        for (int i = blocks.length; --i >= 0; ) {
            int[] msg = blocks[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                switch (msg[1]) {
                    case DRONES_ATTACK:

                        int distToHQ = rc.getLocation().distanceSquaredTo(HQLocation);
                        if (msg[2] != -1) {
                            if (lockAndDefend) {
                                if (distToHQ >= 16) {
                                    enemyBaseLocation = parseLoc(msg[2]);
                                    attackLoc = enemyBaseLocation;
                                    attackHQ = true;
                                }
                            }
                            else {
                                enemyBaseLocation = parseLoc(msg[2]);
                                attackLoc = enemyBaseLocation;
                                attackHQ = true;
                            }
                        }
                        else {
                            // we don't know where enemy is, SCOUT!
                            attackLoc = null;
                            attackHQ  = true;
                        }
                        break;
                    case SWARM_WITH_UNITS:
                        attackWithAllUnits = true;
                        break;
                    case SWARM_IN:
                        swarmIn = true;
                        break;
                    case ANNOUNCE_ENEMY_BASE_LOCATION:
                        enemyBaseLocation = parseLoc(msg[2]);
                        // if we found enemy base and we are attacking HQ but don't know where HQ is
                        if (attackHQ && attackLoc == null) {
                            attackLoc = enemyBaseLocation;
                        }
                        break;
                    case ANNOUNCE_NOT_ENEMY_BASE:
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
                        break;
                    case BUILD_DRONE_NOW:

                        break;
                    case LOCK_AND_DEFEND:
                        if (!rc.isCurrentlyHoldingUnit()) {
                            lockAndDefend = true;
                            toldToLockAndDefendByHQ = true;
                            attackLoc = HQLocation;
                            attackHQ = false;
                        }
                        break;
                    case STOP_LOCK_AND_DEFEND:
                        lockAndDefend = false;
                        toldToLockAndDefendByHQ = false;
                        break;
                    case TERRAFORM_ALL_TIME:
                        // means that we want miners to stop mining and work on building stuff on platform
                        moveMinersToHighland = true;
                        terraformTime = true;
                        // pick up any miners and drop on height 10 tiles
                        break;
                    case WALL_IN:
                    case TERRAFORM_AND_WALL_IN:
                        // time to put landscapers in their spots!
                        wallIn = true;
                        break;
                    case GETTING_RUSHED_HELP:
                        gettingRushed = true;
                        break;
                    case NO_LONGER_RUSHED:
                        gettingRushed = false;
                        break;
                    case ATTACK_ENEMY_UNIT_FOR_RUSH:
                        MapLocation loc = parseLoc(msg[2]);
                        int id = msg[3];
                        int type = msg[4];
                        role = RUSH_DEFEND;
                        IDOfRushUnit = id;
                        locOfRushUnit = loc;
                        skipHelping = true;
                        if (rc.isCurrentlyHoldingUnit() && friendlyUnitHeld != null) {
                            // drop it!
                            int k = 0;
                            Direction dir = Direction.NORTH;
                            while(k++ < 8) {
                                if (rc.canDropUnit(dir)) {
                                    rc.dropUnit(dir);
                                    friendlyUnitHeld = null;
                                }
                                dir = dir.rotateRight();
                            }
                        }
                        break;
                    case NET_GUN_LOCATION:
                        enemyNetguns.add(parseLoc(msg[2]));
                        break;
                    case ATTACKED_ENEMY_WALL:
                        roundsSpentCrunching = 0; // reset this value as we are giving our drone more time to keep crunching up until a maximum
                        break;
                    case NO_LANDSCAPERS_LEFT_ON_ENEMY_HQ:

                        if (!noMoreLandscapersToAttack && rc.getRoundNum() >= 1950) {
                            // basically reset params to what it was before we swarmed in
                            // this should only happen once anyway
                            swarmIn = false;
                            roundsSpentCrunching = 0;
                            noMoreLandscapersToAttack = true;
                        }

                        break;
                }
            }
        }
    }
}

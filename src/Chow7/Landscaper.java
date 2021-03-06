package Chow7;

import Chow7.utils.HashTable;
import Chow7.utils.Node;
import battlecode.common.*;

public class Landscaper extends RobotPlayer {
    // roles
    static final int ATTACK = 0;
    static final int DEFEND_HQ = 1;
    static final int TERRAFORM = 2;
    static int role = DEFEND_HQ;
    static boolean onSupportBlockDoNotMove = false;
    static boolean shouldDig = true;
    static RobotInfo nearestEnemy = null;
    static boolean circling = false;
    static int nearestEnemyDist = 9999999;
    static int startedCirclingRound = -1;
    static int terraformDistAwayFromHQ = 36; // how far landscaper should go
    static HashTable<MapLocation> DigDeltasAroundHQTable =  new HashTable<>(5);
    static HashTable<MapLocation> FirstLandscaperPosAroundHQTable =  new HashTable<>(9);
    static HashTable<MapLocation> BuildPositionsTaken = new HashTable<>(10);
    public static void run() throws GameActionException {
        int waterLevel = calculateWaterLevels();

        int friendlyLandscaperCount = 0;
        int friendlyMiners = 0;
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyFriendlyRobots[i];
            switch (info.type) {
                case LANDSCAPER:
                    friendlyLandscaperCount++;
                    break;
                case MINER:
                    friendlyMiners++;
                    break;
            }
        }
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            int dist = rc.getLocation().distanceSquaredTo(info.location);
            switch (info.type) {
                case DESIGN_SCHOOL:
                case FULFILLMENT_CENTER:

                case REFINERY:
                case VAPORATOR:
                    // TODO, USE A SCORE FUNCTION TO WEIGHT SOME BUILDINGS HIGHER THAN OTHERS
                    if  (dist < nearestEnemyDist) {
                        nearestEnemy = info;
                        nearestEnemyDist = dist;
                    }
                    break;
                case NET_GUN:
                    if  (dist/2 < nearestEnemyDist) {
                        nearestEnemy = info;
                        nearestEnemyDist = dist;
                    }
                    break;

            }
        }
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        checkForEnemyBasesInBlocks(lastRoundsBlocks); // checks for where bases are and removes ones where they dont exist
        checkBlockForActions(lastRoundsBlocks);
        MapLocation attackLoc = null;

        /* BIG BFS LOOP ISH */

        int closestTerraformDist = 9999999;
        MapLocation locToTerraform = null;

        int soupNearby = 0;
        if (debug) System.out.println("BFS start: " + Clock.getBytecodeNum());
        if (debug) System.out.println("Are we on wall? " + FirstLandscaperPosAroundHQTable.contains(rc.getLocation()));
        if (!FirstLandscaperPosAroundHQTable.contains(rc.getLocation())) {
            for (int i = 0; i < Constants.BFSDeltas24.length; i++) {
                int[] deltas = Constants.BFSDeltas24[i];
                MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
                if (rc.canSenseLocation(checkLoc)) {
                    switch (role) {
                        case TERRAFORM:
                            int elevation = rc.senseElevation(checkLoc);

                            int distToHQ = checkLoc.distanceSquaredTo(HQLocation);
                            // make sure its not too deep, not near HQ, but within some dist of HQ, and is not a dig location, and is lower than desired
                            if (elevation < DESIRED_ELEVATION_FOR_TERRAFORM && !isDigLocation(checkLoc) && elevation > -30 && distToHQ < terraformDistAwayFromHQ && distToHQ > 8) {
                                if (rc.isLocationOccupied(checkLoc)) {
                                    RobotInfo info = rc.senseRobotAtLocation(checkLoc);
                                    if ((!isBuilding(info) || info.team == enemyTeam) || info.getID() == rc.getID()) {
                                        int distToLoc = rc.getLocation().distanceSquaredTo(checkLoc);
                                        if (distToLoc < closestTerraformDist) {
                                            closestTerraformDist = distToLoc;
                                            locToTerraform = checkLoc;
                                        }
                                    }
                                } else {
                                    int distToLoc = rc.getLocation().distanceSquaredTo(checkLoc);
                                    if (distToLoc < closestTerraformDist) {
                                        closestTerraformDist = distToLoc;
                                        locToTerraform = checkLoc;
                                    }
                                }

                            }
                    }

                    soupNearby += rc.senseSoup(checkLoc);
                }
            }
        }
        if (locToTerraform == null) {
            // still no terraform location?
            // increase search dist
            //
            //attackLoc = HQLocation;
            if(debug) System.out.println("No terraform loc!");

            if (!circling) {
                startedCirclingRound = rc.getRoundNum();
                circling = true;
            }
            if (circling && rc.getRoundNum() - startedCirclingRound >= 10) {
                terraformDistAwayFromHQ = (int) Math.pow((Math.sqrt(terraformDistAwayFromHQ) + 1), 2);
                circling = false;
                //attackLoc = HQL;
            }
        }

        if (debug) System.out.println("BFS end: " + Clock.getBytecodeNum());
        if (debug) System.out.println("Terraform range: " + terraformDistAwayFromHQ);

        // announce soup loc
        if (friendlyMiners == 0 && rc.getRoundNum() % 10 == 0 && soupNearby >= 100) {
            announceSoupLocation(rc.getLocation(), 1, soupNearby, friendlyMiners);
        }

        // always check for enemy base and do this recon
        MapLocation closestMaybeHQ = null;

        // If we don't know where base is, then look around for it.
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

            // if we can check location we are trying to head to, determine if its a enemy HQ or not
            if (closestMaybeHQ != null && rc.canSenseLocation(closestMaybeHQ)) {
                if (rc.isLocationOccupied(closestMaybeHQ)) {
                    RobotInfo unit = rc.senseRobotAtLocation(closestMaybeHQ);
                    if (unit.type == RobotType.HQ && unit.team == enemyTeam) {
                        // FOUND HQ!
                        enemyBaseLocation = closestMaybeHQ;
                        announceEnemyBase(enemyBaseLocation);
                        if (debug) System.out.println("FOUND ENEMY HQ AT " + closestMaybeHQ);
                        if (debug) rc.setIndicatorDot(enemyBaseLocation, 100, 29, 245);
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

        // when do we protect?, when to attack??
        // if we have nearby enemy,
        if (nearestEnemy != null) {
            // if adjacent, proceed with dump procedure
            if (rc.getLocation().isAdjacentTo(nearestEnemy.location)) {
                if (debug) rc.setIndicatorLine(rc.getLocation(), nearestEnemy.location, 100, 10, 240);
                // > 0 so we unload all dirt asap
                if (rc.getDirtCarrying() > 0) {
                    Direction dirToAttack = rc.getLocation().directionTo(nearestEnemy.location);

                    if (rc.canDepositDirt(dirToAttack)) {
                        rc.depositDirt(dirToAttack);
                        // after depositing, if building robot is gone, we reset nearestEnemy if we used that
                        if (!rc.isLocationOccupied(nearestEnemy.location) && nearestEnemy != null) {
                            nearestEnemy = null;
                        }
                    }
                }
                // otherwise if not enough dirt, collect some
                else {
                    // loop to 1 so we don't include Direction.CENTER;
                    for (int i = directions.length; --i>=1; ) {
                        Direction testDir = directions[i];
                        MapLocation testLoc = rc.adjacentLocation(testDir);
                        // dig out from location that is not occupied or occupied by enemy bot
                        if (rc.isLocationOccupied(testLoc)) {
                            RobotInfo info = rc.senseRobotAtLocation(testLoc);
                            if (info.team == enemyTeam) {
                                // only dig out non buildings
                                if (!isBuilding(info)) {
                                    if (rc.canDigDirt(testDir)) {
                                        rc.digDirt(testDir);
                                    }
                                }
                            }
                            break;
                        }
                        else {
                            if (rc.canDigDirt(testDir)) {
                                rc.digDirt(testDir);
                            }
                        }
                    }
                }
            }
            else {
                //targetLoc = nearestEnemy.location;
                setTargetLoc(nearestEnemy.location);
            }
        }

        if (role == ATTACK) {
            // find nearest enemy hq location to go and search and store map location


            if (enemyBaseLocation == null) {
                //attackLoc = closestMaybeHQ;
            }
            else {
                //attackLoc = enemyBaseLocation;
            }
            // prioritize destructing buildings
            if (nearestEnemy != null) {
                attackLoc = nearestEnemy.location;

            }

            if (!rc.getLocation().isAdjacentTo(attackLoc)) {
                //targetLoc = attackLoc;
                setTargetLoc(attackLoc);
            }
            else {
                if (attackLoc.equals(HQLocation)) {
                    role = DEFEND_HQ;
                }
                else {
                    // adjacent to attack loc now
                    Direction dirToAttack = rc.getLocation().directionTo(attackLoc);
                    if (rc.getDirtCarrying() > 0) {
                        // > 0, so unload all dirt to destory enemy
                        if (rc.canDepositDirt(dirToAttack)) {
                            rc.depositDirt(dirToAttack);
                            // after depositing, if building robot is gone, we reset nearestEnemy if we used that
                            if (!rc.isLocationOccupied(attackLoc) && nearestEnemy != null) {
                                nearestEnemy = null;
                            }
                        }
                    } else {
                        // dig down to make wall building harder
                        Direction digDir = Direction.CENTER; //dirToHQ.opposite();
                        // if we are attacking a normal building, take dirt from elsewhere
                        if (nearestEnemy != null) {
                            // loop to 1 so we don't include center
                            for (int i = directions.length; --i >= 1; ) {
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
        }
        else if (role == TERRAFORM) {
            if (locToTerraform != null) {
                // no enemy in sight!, TERRAFORM
                if (debug) System.out.println("Terraform mode: elevating " + locToTerraform);
                //targetLoc = locToTerraform;
                setTargetLoc(locToTerraform);
                //RobotType.LANDSCAPER.dirtLimit
                if (shouldDig) {
                    Direction dirToDig = Direction.NORTH;
                    for (int i = 0; i++ < 8; ) {
                        MapLocation digLoc = rc.adjacentLocation(dirToDig);
                        if (debug) System.out.println("Trying to dig " + digLoc + " | " + isDigLocation(digLoc));
                        if (digLoc.distanceSquaredTo(HQLocation) > 8 && isDigLocation(digLoc)) {
                            if (rc.canDigDirt(dirToDig)) {
                                rc.digDirt(dirToDig);
                                if (rc.getDirtCarrying() == 25) {
                                    shouldDig = false;
                                }
                                break;
                            }
                        }
                        dirToDig = dirToDig.rotateRight();
                    }
                }
                if (!shouldDig && rc.getLocation().isAdjacentTo(locToTerraform)) {
                    Direction dirToLoc = rc.getLocation().directionTo(locToTerraform);
                    if (rc.canDepositDirt(dirToLoc)) {
                        rc.depositDirt(dirToLoc);
                        if (rc.getDirtCarrying() <= 0) {
                            shouldDig = true;
                        }
                    }
                }
            }
            else {
                // otherwise just move in direction of enemy;
                setTargetLoc(rc.adjacentLocation(rc.getLocation().directionTo(HQLocation).opposite()));
            }

        }
        else if (role == DEFEND_HQ) {

            // TODO: make some better defence code...
            // FIRST THING FIRST DIG OUT HQ if adjacent to it

            if (rc.getLocation().isAdjacentTo(HQLocation)) {
                Direction dirToHQ = rc.getLocation().directionTo(HQLocation);
                if (rc.canDigDirt(dirToHQ)) {
                    rc.digDirt(dirToHQ);
                    if (debug) System.out.println("Digging out HQ at " + HQLocation);
                    if (debug) rc.setIndicatorLine(rc.getLocation(), HQLocation, 120, 0, 230);
                }
            }
            // ALSO IF U CAN DEPOSIT DIRT ON ADJACENT ENEMY DESIGN SCHOOL or NETGUN DO SO
            for (int i = directions.length; --i >= 1; ) {
                MapLocation adjacentLoc = rc.adjacentLocation(directions[i]);
                if (rc.canSenseLocation(adjacentLoc) && rc.isLocationOccupied(adjacentLoc)) {
                    RobotInfo info = rc.senseRobotAtLocation(adjacentLoc);
                    if (info.team == enemyTeam && (info.type == RobotType.NET_GUN || info.type == RobotType.DESIGN_SCHOOL)) {
                        if (rc.canDepositDirt(directions[i])) {
                            rc.depositDirt(directions[i]);
                            if (debug) System.out.println("Depositing at " + adjacentLoc);
                            if (debug) rc.setIndicatorLine(rc.getLocation(), adjacentLoc, 20, 50, 130);
                        }
                    }
                }
            }
            // THEN DIG OUT UR OWN BUILDINGS NEARBY IF POSSIBLE
            Direction dirToDigBuildingOut = rc.getLocation().directionTo(HQLocation);
            for (int i = directions.length; --i>=0; ) {
                MapLocation adjLoc = rc.adjacentLocation(dirToDigBuildingOut);
                if (rc.onTheMap(adjLoc) && rc.isLocationOccupied(adjLoc)) {
                    RobotInfo info = rc.senseRobotAtLocation(adjLoc);
                    if (info.team == rc.getTeam() && isBuilding(info)) {
                        if (rc.canDigDirt(dirToDigBuildingOut)) {
                            rc.digDirt(dirToDigBuildingOut);
                        }
                    }
                }
                dirToDigBuildingOut = dirToDigBuildingOut.rotateLeft();
            }
            // heuristic against defending RUSHES
            // Always look for closest build wall loc
            // If you can see a enemy DESIGN SCHOOL ADJACENT to HQ, choose closest build loc adjacent to the DESIGN SCHOOL


            // find one of the spread out wall positions, and try to go there first
            MapLocation closerOppositeBuildLoc = null;
            if (!onSupportBlockDoNotMove) {
                MapLocation[] locsToCheck = {HQLocation.translate(-1, 0),  HQLocation.translate(1, 0)};

                int closestOppositeBuildLocDist = 9999999;
                for (int i = 0; i < locsToCheck.length; i++ ) {
                    MapLocation checkLoc = locsToCheck[i];
                    boolean valid = false;
                    int distToLoc = rc.getLocation().distanceSquaredTo(checkLoc);
                    if (!BuildPositionsTaken.contains(checkLoc) && rc.onTheMap(checkLoc)) {
                        if (rc.canSenseLocation(checkLoc)) {
                            if (rc.isLocationOccupied(checkLoc)) {
                                RobotInfo info = rc.senseRobotAtLocation(checkLoc);
                                if (info.type == RobotType.LANDSCAPER && info.team == rc.getTeam() && rc.getID() != info.getID()) {
                                    // found bot on wall that is our own landscaper that isn't itself
                                    valid = false;
                                    // this wall is taken, add to taken list
                                    BuildPositionsTaken.add(checkLoc);
                                } else if (info.type == RobotType.LANDSCAPER && info.team == enemyTeam) {
                                    valid = false;
                                } else {
                                    valid = true;
                                }
                            } else {
                                valid = true;
                            }
                        } else {
                            valid = true;
                        }
                    }
                    else {
                        valid = false;
                    }
                    if (valid && distToLoc < closestOppositeBuildLocDist) {
                        closestOppositeBuildLocDist = distToLoc;
                        closerOppositeBuildLoc = checkLoc;
                    }
                }

            }
            if (debug) System.out.println("Closest available opposite build loc: " + closerOppositeBuildLoc);

            int minDist = 99999999;
            int minDistToDefendRushLoc = 99999999;
            int maxDiffWalls = 0;
            // take position with most adjacent high walls > 3 or low walls < 3
            // Find closest location adjacent to HQ to build on
            MapLocation mostCloggedBuildLoc = null;
            MapLocation closestBuildLoc = null;
            MapLocation closestDefendRushLoc = null;
            // don't search if we are on wall or support bloc
            if (!onSupportBlockDoNotMove || FirstLandscaperPosAroundHQTable.contains(rc.getLocation())) {
                for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i >= 0; ) {
                    int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
                    MapLocation checkLoc = HQLocation.translate(deltas[0], deltas[1]);
                    int dist = rc.getLocation().distanceSquaredTo(checkLoc);
                    // valid build location if we can't see it or not occupied
                    // valid if we see it and there's no friendly landscaper on it (or if there is one its not self)
                    // valid if table of taken walls doesn't contain checkLoc
                    boolean valid = false;
                    if (!BuildPositionsTaken.contains(checkLoc) && rc.onTheMap(checkLoc)) {
                        if (rc.canSenseLocation(checkLoc)) {
                            if (rc.isLocationOccupied(checkLoc)) {
                                RobotInfo info = rc.senseRobotAtLocation(checkLoc);
                                if (info.type == RobotType.LANDSCAPER && info.team == rc.getTeam() && rc.getID() != info.getID()) {
                                    // found bot on wall that is our own landscaper that isn't itself
                                    valid = false;
                                    // this wall is taken, add to taken list
                                    BuildPositionsTaken.add(checkLoc);
                                }
                                else if (info.type == RobotType.LANDSCAPER && info.team == enemyTeam) {
                                    valid = false;
                                }
                                else {
                                    valid = true;
                                }
                                // find defend positions against rushes
                                if (info.type == RobotType.DESIGN_SCHOOL && info.team == enemyTeam) {
                                    // enemy design school right around HQ?, defend near position
                                    // search for adjacent locations to HQ and design school
                                    for (int j = directions.length; --j >= 0; ) {
                                        Direction dir = directions[j];
                                        MapLocation adjToSchool = checkLoc.add(dir);
                                        if (adjToSchool.isAdjacentTo(HQLocation)) {
                                            boolean proceed = true;
                                            if (rc.canSenseLocation(adjToSchool) && rc.isLocationOccupied(adjToSchool)) {
                                                //RobotInfo occupiedBot = rc.senseRobotAtLocation(adjToSchool);
                                                proceed = false;
                                            }
                                            if (proceed) {
                                                int distToAdjToSchool = rc.getLocation().distanceSquaredTo(adjToSchool);
                                                if (distToAdjToSchool < minDistToDefendRushLoc) {
                                                    closestDefendRushLoc = adjToSchool;
                                                    minDistToDefendRushLoc = distToAdjToSchool;
                                                }
                                            }
                                        }
                                    }

                                }

                            } else {
                                valid = true;
                            }
                        } else {
                            valid = true;
                        }
                    } else {
                        valid = false;
                    }
                    // if build location is buildable, and closer
                    if (valid && dist < minDist) {
                        minDist = dist;
                        closestBuildLoc = checkLoc;
                    }
                    /*
                    if (valid) {
                        // count number of walls around
                        int locElevation = rc.senseElevation(rc.getLocation());
                        int diffWalls = 0;
                        for (int j = directions.length; --j >= 0; ) {
                            Direction dir = directions[j];
                            MapLocation adjToWallLoc = checkLoc.add(dir);
                            if (rc.canSenseLocation(adjToWallLoc) && !FirstLandscaperPosAroundHQTable.contains(adjToWallLoc) && adjToWallLoc!= HQLocation) {
                                int elevation = rc.senseElevation(adjToWallLoc);
                                if (elevation + 3 < locElevation || elevation - 3 > locElevation) {
                                    diffWalls++;
                                }
                            }
                        }
                        if (diffWalls > maxDiffWalls && diffWalls <= 5) {
                            mostCloggedBuildLoc = checkLoc;
                            maxDiffWalls = diffWalls;
                        }
                    }*/
                }
            }
            // if no closest one found from this set, check supporting build locations
            // when on these positions, you don't build on self, you build on wall.
            MapLocation closestSupportLoc = null;
            if (closestBuildLoc == null) {
                for (int i = Constants.LandscaperPosAroundHQ.length; --i >= 0; ) {
                    int [] deltas = Constants.LandscaperPosAroundHQ[i];
                    MapLocation checkLoc = HQLocation.translate(deltas[0], deltas[1]);

                    // valid build location if we can't see it
                    // valid if we see it and there's no friendly landscaper on it (or if there is one its not self)
                    boolean valid = false;
                    if (!BuildPositionsTaken.contains(checkLoc) && rc.onTheMap(checkLoc)) {
                        if (rc.canSenseLocation(checkLoc)) {
                            if (rc.isLocationOccupied(checkLoc)) {
                                RobotInfo info = rc.senseRobotAtLocation(checkLoc);
                                if (info.type == RobotType.LANDSCAPER && info.team == rc.getTeam() && rc.getID() != info.getID()) {
                                    valid = false;
                                    BuildPositionsTaken.add(checkLoc);
                                }
                                else if (isBuilding(info) && info.team == rc.getTeam()) {
                                    BuildPositionsTaken.add(checkLoc);
                                    valid = false;
                                }
                                else {
                                    valid = true;
                                }
                            } else {
                                // valid if not occupied
                                valid = true;
                            }
                        } else {
                            // valid if can't sense but its not taken and on map
                            valid = true;
                        }
                    }
                    else {
                        // invalid if taken or not on map
                        valid = false;
                    }
                    // if build location is buildable, and closer
                    if (valid) {
                        int dist = rc.getLocation().distanceSquaredTo(checkLoc);
                        if (dist < minDist) {
                            minDist = dist; // we can still use minDist cuz it was never reset as closestBuildLoc == null
                            closestSupportLoc = checkLoc;
                        }
                    }
                }

            }
            // prioritize defending rush loc if we aren't on the closest buildloc (if we are on closest build loc, don't budge)
            if (closestDefendRushLoc != null && !rc.getLocation().equals(closestBuildLoc)) {
                setTargetLoc(closestDefendRushLoc);
            }
            // then prioritize the two well spread opposite locations
            else if (closerOppositeBuildLoc != null) {
                setTargetLoc(closerOppositeBuildLoc);
            }
            // then prioritize most clogged building location
            else if (mostCloggedBuildLoc != null && !rc.getLocation().equals(closestBuildLoc)) {
                setTargetLoc(mostCloggedBuildLoc);
            }
            else {
                setTargetLoc(closestBuildLoc);
            }

            //targetLoc = closestBuildLoc;

            int distToBuildLoc = -1;
            // store distance if not null
            if (targetLoc != null) {
                distToBuildLoc = rc.getLocation().distanceSquaredTo(targetLoc);
            }
            double waterChangeRate = calculateWaterLevelChangeRate();
            if (debug) System.out.println("Going to build loc " + targetLoc + " | Closest rush defence loc " + closestDefendRushLoc  + "| most clogged <= 5 wall loc at " + maxDiffWalls +" - " + mostCloggedBuildLoc +  " | closest support loc " + closestSupportLoc + " | water change rate: " + waterChangeRate + " | levels: " + calculateWaterLevels());
            // if landscaper is on top of build loc
            if (distToBuildLoc == 0) {
                // build wall only when we have max dirt, so to stock up and prevent rushes faster mayb e
                if (rc.getDirtCarrying() > 24) {
                    // deposit on places lower than you and has friend landscaper and is on a valid location
                    // we only spread the wall dirt provided everywhere is filled with landscapers, or its pretty late
                    // game and water level is near height of unfilled position
                    int lowestElevation = rc.senseElevation(rc.getLocation());
                    Direction depositDir = Direction.CENTER;
                    for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i >= 0; ) {
                        int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
                        MapLocation checkLoc = HQLocation.translate(deltas[0], deltas[1]);
                        if (rc.onTheMap(checkLoc)) {
                            if (rc.getLocation().isAdjacentTo(checkLoc)) {
                                boolean spread = false;
                                if (rc.isLocationOccupied(checkLoc)) {
                                    RobotInfo info = rc.senseRobotAtLocation(checkLoc);
                                    if ((info.type == RobotType.LANDSCAPER && info.team == rc.getTeam()) || rc.getRoundNum() > 1400) {
                                        // one of us?, spread the dirt
                                        spread = true;
                                    }
                                    if (info.type == RobotType.HQ && info.team == rc.getTeam()) {
                                        spread = false; // stop our units from depositing onto our base
                                    }
                                } else {
                                    // if its too low or high rounds, spread
                                    if (waterLevel + 2 + waterChangeRate > rc.senseElevation(checkLoc) || rc.getRoundNum() > 1200) {
                                        spread = true;
                                    }
                                }
                                if (spread) {
                                    if (rc.senseElevation(checkLoc) < lowestElevation) {
                                        lowestElevation = rc.senseElevation(checkLoc);
                                        depositDir = rc.getLocation().directionTo(checkLoc);
                                    }
                                }

                            }
                        }
                    }
                    // we also check adjacent tiles to make sure we have a platform to walk around
                    Direction dir2 = Direction.NORTH;
                    for (int i = 0; i++ < 8; ) {

                        MapLocation checkLoc = rc.adjacentLocation(dir2);
                        if (rc.canSenseLocation(checkLoc)) {
                            int elevation = rc.senseElevation(checkLoc);
                            boolean proceed = true;
                            // make sure we don't fill up a dig spot
                            if (DigDeltasAroundHQTable.contains(checkLoc)) {
                                proceed = false;
                            }
                            if (rc.isLocationOccupied(checkLoc)) {
                                RobotInfo info = rc.senseRobotAtLocation(checkLoc);
                                if (info.team == rc.getTeam() && isBuilding(info)) {
                                    proceed = false;
                                }
                            }
                            if (proceed && elevation < lowestElevation && elevation < 4) {
                                lowestElevation = elevation;
                                depositDir = rc.getLocation().directionTo(checkLoc);
                            }
                        }
                        dir2 = dir2.rotateLeft();
                    }
                    if (debug) System.out.println("Trying to deposit at " + depositDir);
                    if (rc.canDepositDirt(depositDir)) {
                        rc.depositDirt(depositDir);
                    }
                }
                else {
                    Direction digDir = getDigDirectionForDefending();
                    if (debug) System.out.println("Trying to dig at " + digDir);
                    if (rc.canDigDirt(digDir)) {
                        rc.digDirt(digDir);
                    }
                }
            }
            // we are adjacent to our intended build location. Now we figure out why we aren't there yet
            else if (distToBuildLoc >= 1 && distToBuildLoc <= 2) {
                if (rc.getDirtCarrying() <= 0) {
                    Direction digDir = getDigDirectionForDefending();
                    if (rc.canDigDirt(digDir)) {
                        rc.digDirt(digDir);
                    }
                }
                else {
                    // otherwise we have dirt on us to use

                    if (rc.canSenseLocation(targetLoc)) {
                        // if occupied
                        if (rc.isLocationOccupied(targetLoc)) {
                            RobotInfo info = rc.senseRobotAtLocation(targetLoc);
                            // if this is some stupid friendly unit, DISINTEGRATE IT?
                            // if it is a building, BURY IT if possible
                            if (info.team == enemyTeam && isBuilding(info)) {
                                Direction depositDir = rc.getLocation().directionTo(targetLoc);
                                if (rc.canDepositDirt(depositDir)) {
                                    rc.depositDirt(depositDir);
                                }
                            }
                        }

                        // not occupied?, build up to location or build location up
                        else {

                            int thatElevation = rc.senseElevation(targetLoc);
                            int myElevation = rc.senseElevation(rc.getLocation());
                            if (debug)
                                System.out.println("My elevation: " + myElevation + " Intended elevation: " + thatElevation);
                            // if my elevation is too low, build myself upwards
                            if (thatElevation > myElevation + 3) {
                                // otherwise its normal, no unit on it, we are next to it, build up
                                if (debug) System.out.println("depositing on self");
                                if (rc.getDirtCarrying() > 0) {
                                    if (rc.canDepositDirt(Direction.CENTER)) {
                                        rc.depositDirt(Direction.CENTER);
                                    }
                                }
                            }
                            // if too low, fill that low place up
                            else if (thatElevation < thatElevation - 3) {
                                if (rc.getDirtCarrying() > 0) {
                                    Direction dirToLowElevation = rc.getLocation().directionTo(targetLoc);
                                    if (rc.canDepositDirt(dirToLowElevation)) {
                                        rc.depositDirt(dirToLowElevation);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // otherwise our we didn't have a closestBuildLoc, so we use closestSupportLoc
            else if (distToBuildLoc == -1 && closestSupportLoc != null) {
                //targetLoc = closestSupportLoc;
                setTargetLoc(closestSupportLoc);
                int distToSupportLoc = rc.getLocation().distanceSquaredTo(closestSupportLoc);
                if (distToSupportLoc == 0) {
                    // STAY, DONT MOVE
                    onSupportBlockDoNotMove = true;
                    if (rc.getDirtCarrying() > 24) {
                        // iterate over HQ wall areas, and find lowest one adjacent
                        Direction depositDir = Direction.CENTER;
                        // we deposit in CENTER if it is worth more than depositing to a wall
                        // calculate rate of change of water level, if water level increases by more than 2, we should deposit on wall
                        // double levelChange = calculateWaterLevelChangeRate();// FIXME
                        if (waterLevel + 3 < rc.senseElevation(rc.getLocation())) {
                            int lowestElevation = 99999;
                            for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i >= 0; ) {
                                int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
                                MapLocation checkLoc = HQLocation.translate(deltas[0], deltas[1]);
                                if (rc.getLocation().isAdjacentTo(checkLoc)) {
                                    if (rc.senseElevation(checkLoc) < lowestElevation) {
                                        lowestElevation = rc.senseElevation(checkLoc);
                                        depositDir = rc.getLocation().directionTo(checkLoc);
                                    }
                                }
                            }
                        }
                        if (debug) System.out.println("Trying to deposit at " + depositDir);
                        if (rc.canDepositDirt(depositDir)) {
                            rc.depositDirt(depositDir);
                        }
                    }
                    else {
                        Direction digDir = getDigDirectionForDefending();
                        if (debug) System.out.println("Trying to dig at " + digDir);
                        if (rc.canDigDirt(digDir)) {
                            rc.digDirt(digDir);
                        }
                    }
                }
            }
            // if no build loc is found, go away
            if (targetLoc == null) {
                role = TERRAFORM;
            }
            // if none found
        }





        // whatever targetloc is, try to go to it
        if (targetLoc != null) {
            Direction greedyDir = getBugPathMove(targetLoc); //TODO: should return a valid direction usually???
            if (debug) System.out.println("Moving to " + rc.adjacentLocation((greedyDir)) + " to get to " + targetLoc);
            tryMove(greedyDir); // wasting bytecode probably here

            // if didnt work, probably dig myself out
            // PRIORITy, dig self out
            int myElevation = rc.senseElevation(rc.getLocation());
            boolean digSelfOut = true;
            for (int i = directions.length; --i >= 0; ) {
                Direction dir = directions[i];
                MapLocation loc = rc.adjacentLocation(dir);
                if (rc.canSenseLocation(loc)) {
                    int thatElevation = rc.senseElevation(loc);
                    if (thatElevation <= myElevation + 3 && thatElevation >= myElevation - 3) {
                        digSelfOut = false;
                        break;
                    }
                }
            }
            if (digSelfOut) {
                if (rc.canDigDirt(Direction.CENTER)) {
                    rc.digDirt(Direction.CENTER);
                }
            }
        }
        if (debug) System.out.println(" Carrying " + rc.getDirtCarrying() + " dirt | Cooldown: " + rc.getCooldownTurns());

    }

    static boolean isBuilding(RobotInfo info) {
        if (info.type == RobotType.HQ || info.type == RobotType.FULFILLMENT_CENTER || info.type == RobotType.NET_GUN || info.type == RobotType.REFINERY || info.type == RobotType.DESIGN_SCHOOL || info.type == RobotType.VAPORATOR) {
            return true;
        }
        return false;
    }
    // heuristic

    // dig in targeted areas around HQ. don't dig enemy buildings
    static Direction getDigDirectionForDefending() throws GameActionException {
        for (int i = Constants.DigDeltasAroundHQ.length; --i >= 0; ) {
            int[] deltas = Constants.DigDeltasAroundHQ[i];
            MapLocation checkLoc = HQLocation.translate(deltas[0], deltas[1]);
            Direction testDir = rc.getLocation().directionTo(checkLoc);
            // try the targeted dig sites, dig if its empty or is enemy team
            if (rc.getLocation().distanceSquaredTo(checkLoc) <= 2 && rc.canSenseLocation(checkLoc)) {
                if (!rc.isLocationOccupied(checkLoc)) {
                    if (rc.canDigDirt(testDir)) {
                        return testDir;
                    }
                }
                else {
                    RobotInfo info = rc.senseRobotAtLocation(checkLoc);
                    if (rc.canDigDirt(testDir)) {
                        return testDir;
                    }
                }
            }
        }
        return bestDigDir();
    }
    static Direction bestDigDir() throws GameActionException {
        Direction dir = rc.getLocation().directionTo(HQLocation).opposite();
        int i =0;
        while(++i < 8) {
            MapLocation checkLoc = rc.adjacentLocation(dir);
            if (okToDig(checkLoc)) {
                return dir;
            }
        }
        return Direction.CENTER;
    }
    // location is ok to dig at if no units on it, not on hq build wall, is not occupied by our landscaper
    // and not enemy building
    static boolean okToDig(MapLocation loc) throws GameActionException {
        if (validBuildWallLoc(loc)) {
            // replace with hashmap
            // FIXME REPLACE NOW
            return false;
        }
        if (rc.canSenseLocation(loc)) {
            if (rc.isLocationOccupied(loc)) {
                RobotInfo info = rc.senseRobotAtLocation(loc);
                if (info.team == rc.getTeam() && info.type != RobotType.LANDSCAPER) {
                    return true;
                }
                else if (info.team == enemyTeam && !isBuilding(info)) {
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return true;
            }
        }
        return true;
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
                    //targetLoc = HQLocation;
                    setTargetLoc(HQLocation);
                }
                else if ((msg[1] ^ NO_MORE_LANDSCAPERS_NEEDED) == 0) {

                    // go away and attack if not near HQ
                    if (rc.getLocation().distanceSquaredTo(HQLocation) > 16) {
                        role = TERRAFORM;
                    }
                }
            }
        };
    }
    public static void setup() throws GameActionException {
        storeHQLocation();
        storeEnemyHQLocations();
        for (int i = Constants.DigDeltasAroundHQ.length; --i>= 0; ) {
            int[] deltas = Constants.DigDeltasAroundHQ[i];
            MapLocation digLoc = HQLocation.translate(deltas[0], deltas[1]);
            DigDeltasAroundHQTable.add(digLoc);
        }

        for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i>= 0; ) {
            int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
            MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
            FirstLandscaperPosAroundHQTable.add(loc);
        }

    }
}

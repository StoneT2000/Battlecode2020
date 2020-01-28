package Chow10;

import Chow10.utils.HashTable;
import Chow10.utils.LinkedList;
import Chow10.utils.Node;
import battlecode.common.*;
public class Miner extends RobotPlayer {
    static final int SCOUT = 0; // default to search for patches of soup and what not
    static final int MINER = 1; // default to go and mine nearest souplocation it knows
    static final int RETURNING = 2; // RETURNING TO SOME REFINERY OR HQ TO DEPOSIT
    static final int BUILDING = 3;

    static final int ATTACK = 4;

    static int roundsOfCantBuild = 0;
    static Direction minedDirection;
    static int FulfillmentCentersBuilt = 0;
    static boolean firstFulfillmentCenterBuilt = false;
    static int DesignSchoolsBuilt = 0; // how many design schools this robot knows have been built ??
    static boolean firstDesignSchoolBuilt = false;

    static boolean terraformTime = false;

    static MapLocation lastDepositedRefinery;
    static MapLocation enemyBaseLocation = null;
    // score of the souplocation it is probably heading towards
    static double soupLocScore = 0;

    // exploration stuff
    static int timeSpentOnExploreLoc = 0;
    static MapLocation[] exploreLocs;
    static int exploreLocIndex = 0;

    static HashTable<MapLocation> MainHQWall = new HashTable<>(8);
    static RobotType unitToBuild; // unit to build if role is building

    static int stuckRounds = 0;
    static boolean blocked = false; // whether or not unit couldn't determine a path to goal last round

    static int role = MINER; // default ROLE
    static int HQParity; // parity of HQLocation.x + HQLocation.y

    static final int roundsOfDesignatedBuilder = 600;

    public static void run() throws GameActionException {
        // try to get out of water, checks if in water for you
        getOutOfWater();

        // always read last round's blocks
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        boolean mined = false;

        // if getting build information, use it
        checkBlockForBuildInfo(lastRoundsBlocks);
        checkForEnemyBasesInBlocks(lastRoundsBlocks);

        // look for enemey location and see if its there
        if (enemyBaseLocation == null) {
            Node<MapLocation> node = enemyHQLocations.head;
            for (int i = 0; i++ < enemyHQLocations.size; ) {
                // check if there is enemey base
                if (rc.canSenseLocation(node.val)) {
                    if (rc.isLocationOccupied(node.val)) {
                        RobotInfo maybeEnemyHQ = rc.senseRobotAtLocation(node.val);
                        if (maybeEnemyHQ.type == RobotType.HQ && maybeEnemyHQ.team == enemyTeam) {
                            if (debug) System.out.println("MINER FOUND ENEMY HQ at " + node.val);
                            enemyBaseLocation = maybeEnemyHQ.location;
                            break;
                        }
                    }
                }
                node = node.next;

            }
        }

        // if mining, always try to mine
        if (role == MINER) {
            // Strat: MINE if possible!
            // TODO: can do with mining optimization? Mine furthest tile away from friends?
            // try to mine if mining max rate one turn won't go over soup limit (waste of mining power)
            if (rc.getSoupCarrying() <= RobotType.MINER.soupLimit - GameConstants.SOUP_MINING_RATE) {
                for (Direction dir : directions) {
                    // for each direction, check if there is soup in that direction
                    MapLocation newLoc = rc.adjacentLocation(dir);
                    if (rc.canMineSoup(dir)) {
                        rc.mineSoup(dir);
                        minedDirection = dir;
                        mined = true;
                        if (debug) {
                            System.out.println("Turn: " + turnCount + " - I mined " + newLoc + "; Now have " + rc.getSoupCarrying());
                        }
                        break;
                    }
                }
            }
            // else if we are near full, we go to nearest refinery known, otherwise go to HQ
            else {
                //targetLoc = lastDepositedRefinery;
                setTargetLoc(lastDepositedRefinery);
                role = RETURNING;
            }
        }

        // if its high rounds and we are near enemy HQ, attack mode
        if (rc.getRoundNum() >= 1650 && enemyBaseLocation != null && rc.getLocation().distanceSquaredTo(enemyBaseLocation) <= DROP_ZONE_RANGE_OF_HQ + 40) {
            role = ATTACK;
        }



        /* BIG FRIENDLY BOTS SEARCH LOOP thing */
        int EnemyDroneCount = 0;
        int closeEnemyDroneCount = 0;
        boolean moveAway = false;
        HashTable<Direction> dangerousDirections = new HashTable<>(6); // directions that when moved in, will result in being picked
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            switch (info.type) {
                case DELIVERY_DRONE:
                    // if drone too close, run
                    if (!info.isCurrentlyHoldingUnit() && rc.getLocation().distanceSquaredTo(info.location) <= 13) {
                        EnemyDroneCount++;
                        Direction dirToDrone = rc.getLocation().directionTo(info.location);
                        dangerousDirections.add(dirToDrone);
                        dangerousDirections.add(dirToDrone.rotateRight());
                        dangerousDirections.add(dirToDrone.rotateLeft());
                        dangerousDirections.add(Direction.CENTER); // don't idle
                        moveAway = true;
                    }
                    break;
            }
        }




        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());

        LinkedList<RobotInfo> nearbyNetguns = new LinkedList<>();

        int RefineryCount = 0;
        int NetGunCount = 0;
        int MinerCount = 0;
        int DesignSchoolCount = 0;
        int closeNetguns = 0;
        int VaporatorCount = 0;
        int FulfillmentCenterCount = 0;
        int nearbyFriendlyLandscapers = 0;
        MapLocation nearestRefinery = HQLocation;
        boolean designatedBuilder = true;
        int minDist = rc.getLocation().distanceSquaredTo(HQLocation);
        for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyFriendlyRobots[i];
            switch (info.type) {
                case REFINERY:
                    RefineryCount++;
                    // if bot is returning, locate nearest refinery as well
                    if (role == RETURNING) {
                        int dist = rc.getLocation().distanceSquaredTo(info.location);
                        if (dist < minDist) {
                            minDist = dist;
                            //targetLoc = info.location;
                            setTargetLoc(info.location);
                        }
                    }
                    break;
                case NET_GUN:
                    NetGunCount++;
                    if (rc.getLocation().distanceSquaredTo(info.location) <= 2) {
                        closeNetguns++;
                    }
                    nearbyNetguns.add(info);
                    break;
                case DESIGN_SCHOOL:
                    DesignSchoolCount++;
                    break;
                case MINER:
                    MinerCount++;
                    if (rc.getLocation().distanceSquaredTo(HQLocation) > info.getLocation().distanceSquaredTo(HQLocation)) {
                        designatedBuilder = false;
                    }
                    break;
                case FULFILLMENT_CENTER:
                    FulfillmentCenterCount++;
                    break;
                case VAPORATOR:
                    VaporatorCount++;
                    break;
                case LANDSCAPER:
                    nearbyFriendlyLandscapers++;
                    break;
            }
        }
        if (!designatedBuilder && role != ATTACK && rc.getRoundNum() <= roundsOfDesignatedBuilder) {
            if (unitToBuild == RobotType.FULFILLMENT_CENTER || unitToBuild == RobotType.DESIGN_SCHOOL || unitToBuild == RobotType.VAPORATOR) {
                role = MINER;
                unitToBuild = null;
            }
        }
        if (role == ATTACK) {
            // build net guns in face of drones
            if (debug) System.out.println("ATTACKING | Soup: " + rc.getTeamSoup());
            if (EnemyDroneCount > 0 && closeNetguns == 0) {
                int i = 0;
                Direction buildDir = Direction.NORTH;

                while(i++ < 8) {
                    if (debug) System.out.println("Trying to build NETGUN in dir: " + buildDir);
                    if (rc.canBuildRobot(RobotType.NET_GUN, buildDir)) {
                        rc.buildRobot(RobotType.NET_GUN, buildDir);
                        break;
                    }
                    buildDir = buildDir.rotateRight();
                }
            }
            if (DesignSchoolCount <= 0 && enemyBaseLocation != null) {
                // otherwise no design schools nor drones? build one on hq wall.

                for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i >= 0; ) {
                    int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
                    MapLocation buildLoc = enemyBaseLocation.translate(deltas[0], deltas[1]);
                    Direction buildDir = rc.getLocation().directionTo(buildLoc);
                    if (debug) System.out.println("Trying to build SCHOOL in dir: " + buildDir);
                    // build school if adjacent to wall location
                    if (rc.getLocation().isAdjacentTo(buildLoc) && rc.canBuildRobot(RobotType.DESIGN_SCHOOL, buildDir)) {
                        rc.buildRobot(RobotType.DESIGN_SCHOOL, buildDir);
                        break;
                    }
                }
            }
            // if no where to build, STOP! and disintegrate
            boolean canBuildSomewhere = false;
            int i = 0;
            Direction buildDir = Direction.NORTH;

            int myElevation = rc.senseElevation(rc.getLocation());
            // if surrounded by my own units, disintegrate
            while(i++ < 8) {

                MapLocation adjLoc = rc.adjacentLocation(buildDir);
                int elevation = rc.senseElevation(adjLoc);
                // in good elevation
                if (rc.canSenseLocation(adjLoc) && (elevation + 3 >= myElevation && myElevation >= elevation - 3)) {
                    // occupied by enemy is ok
                    if (rc.isLocationOccupied(adjLoc)) {
                        RobotInfo robThere = rc.senseRobotAtLocation(adjLoc);
                        if (robThere.team == enemyTeam) {
                            // enemy team? can still build
                            canBuildSomewhere = true;
                            break;
                        }
                    }
                    else {
                        canBuildSomewhere = true;
                        break;
                    }
                }
                buildDir = buildDir.rotateRight();
            }
            if (!canBuildSomewhere) {
                roundsOfCantBuild++;

            }
            else {
                roundsOfCantBuild = 0;
            }
            if (roundsOfCantBuild >= 8) {
                rc.disintegrate();
            }
            return;
        }

        if (role == BUILDING) {
            // if we are trying to build but we already have one, stop, or if we already built it cuz soup went down, STOP
            if (debug) System.out.println("Trying to build " + unitToBuild +" | soup rn: "+ rc.getTeamSoup());

            if (unitToBuild == RobotType.REFINERY && RefineryCount > 0) {
                role = MINER;
            }
            if (unitToBuild == RobotType.DESIGN_SCHOOL && DesignSchoolCount > 0) {
                role = MINER;
            }
            if (unitToBuild == RobotType.FULFILLMENT_CENTER && FulfillmentCenterCount > 0) {
                role = MINER;
            }
            if (unitToBuild == RobotType.VAPORATOR && rc.getTeamSoup() < 450) {
                role = MINER;
            }
        }

        /* BIG BFS LOOP ISH */
        // do everything needed with bfs here
        int soupNearbyCount = 0; // amount of soup nearby in BFS search range
        int minDistToNearestSoup = 99999999;
        boolean newLocation = false;
        if (SoupLocation == null) {
            newLocation = true;
        }
        if (SoupLocation != null) {
            minDistToNearestSoup = rc.getLocation().distanceSquaredTo(SoupLocation);
        }
        boolean soupLocIsFree = false;
        for (int i = 0; i < Constants.BFSDeltas35.length; i++) {
            int[] deltas = Constants.BFSDeltas35[i];
            MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
            // TODO: instead of canSenseLocation, maybe do the math and choose the right BFS deltas to iterate over
            if (rc.canSenseLocation(checkLoc)) {
                switch(role) {
                    case MINER:
                        // TODO: maybe change minimum to higher or dependent on team soup (if we are rich, don't mine less than x etc.)
                        if (rc.senseSoup(checkLoc) > 0) {
                            soupNearbyCount += rc.senseSoup(checkLoc);
                            int dist = rc.getLocation().distanceSquaredTo(checkLoc);
                            // BUG FIXME: Dont check if flooding, check if flooding and not surrounded by empty reachable tile
                            if (!rc.senseFlooding(checkLoc) || hasEmptyTileAround(checkLoc)){
                                if (dist < minDistToNearestSoup){
                                    SoupLocation = checkLoc;
                                    soupLocIsFree = true;
                                    minDistToNearestSoup = dist; // set this so we wont reset SoupLocation as we add soupNearbyCount
                                    if (debug) System.out.println("Found soup location at " + checkLoc);
                                }

                            } else {
                                // TODO: handle when we find a flooded patch, how do we mark it for clearing by landscapers?
                                // found a tile with soup, but its flooded
                                // announceSoupLocation(checkLoc, 0, false);
                            }
                        }
                        break;
                    case RETURNING:
                        break;
                    case BUILDING:
                        break;
                }

            }
        }



        // alwways prepare to build refinery
        int distToHQ = rc.getLocation().distanceSquaredTo(HQLocation);

        if (EnemyDroneCount > 0 && closeNetguns == 0  && rc.getTeamSoup() >= RobotType.NET_GUN.cost) {
            role = BUILDING;
            unitToBuild = RobotType.NET_GUN;
        }

        // running away if not attacking or not building, run away if not building net gun
        if (moveAway && role != ATTACK && (role != BUILDING || unitToBuild != RobotType.NET_GUN)) {
            // go to target with consideration of dangers
            // go to nearest netgun/HQ?
            Direction greedyDir = getBugPathMove(HQLocation, dangerousDirections); //TODO: should return a valid direction usually???
            if (debug) System.out.println("Running away! To " + rc.adjacentLocation((greedyDir)) + " to get to " + targetLoc);
            tryMove(greedyDir);
        }

        // Build a refinery if there is enough nearby soup, no refineries nearby, and we just mined
        // 800 - something, subtract distance. Subtract less for the higher amount soup mined
        if (lastDepositedRefinery.equals(HQLocation)) {
            // if refinery deposited at is HQ location, go all out to build this refinery at least
            if (mined && RefineryCount == 0 && rc.getTeamSoup() >= RobotType.REFINERY.cost && firstFulfillmentCenterBuilt && rc.getLocation().distanceSquaredTo(lastDepositedRefinery) >= 36) {
                role = BUILDING;
                unitToBuild = RobotType.REFINERY;
            }
        }
        else {
            if (mined && soupNearbyCount > 800 - Math.sqrt(rc.getLocation().distanceSquaredTo(lastDepositedRefinery)) && RefineryCount == 0 && rc.getTeamSoup() >= RobotType.REFINERY.cost && rc.getRoundNum() >= 75 && rc.getLocation().distanceSquaredTo(lastDepositedRefinery) >= 36) {
                role = BUILDING;
                unitToBuild = RobotType.REFINERY;
            }
        }

        // build netguns out of necessity to combat drones
        // FIXME: MAKE SURE WE BUILD IN RIGHT PLACES AND NOT JUST CHECK NETGUNCOUNT == 0


        if (role == MINER) {
            // TODO: cost of announcement should be upped in later rounds with many units.
            // announce soup location if we just made a new soup location
            if (SoupLocation != null && newLocation && hasEmptyTileAround(SoupLocation)) {
                // YELLOW means we found soup location, and we make announcement!
                if (debug) rc.setIndicatorDot(SoupLocation, 255, 200, 20);
                announceSoupLocation(SoupLocation, 1, soupNearbyCount, MinerCount + 1);
            }

            // reset soup score if needed, otherwise set target
            if (SoupLocation != null && terraformTime == false) {

                // check if we sense the place, if not, we continue branch, otherwise check if there is soup left
                if (!rc.canSenseLocation(SoupLocation) || rc.senseSoup(SoupLocation) > 0) {
                    // if not close enough to soup location, move towards it as it still has soup there
                    if (SoupLocation.distanceSquaredTo(rc.getLocation()) > 1) {
                        if (debug)
                            System.out.println("Heading to soup location " + SoupLocation + " with score " + soupLocScore);
                        //targetLoc = SoupLocation;
                        setTargetLoc(SoupLocation);
                    } else {
                        // close enough...
                    }
                } else {
                    // no soup left at location
                    SoupLocation = null;
                    soupLocScore = 0;
                    if (debug) System.out.println("No soup left, reset score");
                }

            }

            // check messages for soup locations, possibly closer
            checkBlockForSoupLocations(lastRoundsBlocks);


            // early game
            // TODO: TUNE PARAM!
            if (rc.getRoundNum() <= roundsOfDesignatedBuilder) {
                // only designated builder builds vaporators
                if (designatedBuilder && rc.getTeamSoup() >= 500 + 150) {
                    role = BUILDING;
                    unitToBuild = RobotType.VAPORATOR;
                    //announceI_AM_DESIGNATED_BUILDER();
                }

            }
            // only designated builder builds vaporators
            else if (rc.getTeamSoup() >= 500 + 150) {
                if ((!terraformTime && designatedBuilder) || rc.senseElevation(rc.getLocation()) >= DESIRED_ELEVATION_FOR_TERRAFORM - 2) {
                    role = BUILDING;
                    unitToBuild = RobotType.VAPORATOR;
                    //announceI_AM_DESIGNATED_BUILDER();
                }
            }
            // build net guns around enemy base!
            if (enemyBaseLocation != null && rc.getLocation().distanceSquaredTo(enemyBaseLocation) <= 24 && NetGunCount < 1 && rc.getTeamSoup() >= RobotType.NET_GUN.cost + 400) {
                role = BUILDING;
                unitToBuild = RobotType.NET_GUN;
            }

            // build net guns on our base
            if (closeNetguns == 0 && distToHQ <= MAX_TERRAFORM_DIST && distToHQ >= 36 && terraformTime == true && (VaporatorCount >= 2 || nearbyFriendlyLandscapers > 3)) {
                role = BUILDING;
                unitToBuild = RobotType.NET_GUN;
            }

            if (FulfillmentCenterCount == 0 && terraformTime == true && rc.getTeamSoup() > 2000 && rc.getRoundNum() > 1600 && distToHQ >= MAX_TERRAFORM_DIST - 20) {
                role = BUILDING;
                unitToBuild = RobotType.FULFILLMENT_CENTER;
            }

            // EXPLORE if still no soup found and we aren't terraforming
            if (SoupLocation == null && terraformTime == false) {
                if (debug) System.out.println("Exploring to " + exploreLocs[exploreLocIndex]);
                //targetLoc = rc.adjacentLocation(getExploreDir());
                setTargetLoc(rc.adjacentLocation(getExploreDir(dangerousDirections)));
            }
            // otherwise we approach the soup location.
            else {
            }
        }
        // must be ready, if not ready and still on cooldown, we wait till next turn basically
        else if (role == BUILDING && rc.isReady()) {
            if (debug) System.out.println("Ready and trying to build a " + unitToBuild);
            boolean proceedWithBuild = true;
            Direction buildDir = Direction.NORTH;
            if (minedDirection != null && minedDirection != Direction.CENTER) {
                if (unitToBuild == RobotType.REFINERY) {
                    buildDir = minedDirection;
                } else {
                    buildDir = minedDirection.opposite();
                }
            }
            if (unitToBuild == RobotType.DESIGN_SCHOOL || unitToBuild == RobotType.FULFILLMENT_CENTER) {
                buildDir = rc.getLocation().directionTo(HQLocation);
            }

            if (unitToBuild == RobotType.VAPORATOR && rc.getRoundNum() <= 250 && rc.getLocation().distanceSquaredTo(HQLocation) > HQ_LAND_RANGE) {
                // make sure miner goes back to near HQ to build this
                proceedWithBuild = false;
                setTargetLoc(HQLocation);
            }
            if (unitToBuild == RobotType.VAPORATOR && rc.getRoundNum() > 250 && rc.getRoundNum() < 500 && rc.getLocation().distanceSquaredTo(HQLocation) >= 36) {
                // make sure miner goes back to near HQ to build this
                proceedWithBuild = false;
                setTargetLoc(HQLocation);
            }
            if (unitToBuild == RobotType.VAPORATOR && rc.getRoundNum() >= 500 && rc.getLocation().distanceSquaredTo(HQLocation) >= MAX_TERRAFORM_DIST + 8) {
                // make sure miner goes back to near HQ to build this
                proceedWithBuild = false;
                setTargetLoc(HQLocation);
            }
            if (unitToBuild == RobotType.DESIGN_SCHOOL && rc.getRoundNum() <= 300 && rc.getLocation().distanceSquaredTo(HQLocation) > HQ_LAND_RANGE) {
                // make sure miner goes back to HQ to build this
                proceedWithBuild = false;
                setTargetLoc(HQLocation);
            }
            if (unitToBuild == RobotType.DESIGN_SCHOOL && rc.getRoundNum() > 300 && rc.getLocation().distanceSquaredTo(HQLocation) > 36) {
                // make sure miner goes back to HQ to build this
                proceedWithBuild = false;
                setTargetLoc(HQLocation);
            }
            if (unitToBuild == RobotType.FULFILLMENT_CENTER && rc.getRoundNum() <= 300 && rc.getLocation().distanceSquaredTo(HQLocation) > 8) {
                // make sure miner goes back to HQ to build this
                proceedWithBuild = false;
                setTargetLoc(HQLocation);
            }
            if (unitToBuild == RobotType.FULFILLMENT_CENTER && rc.getRoundNum() > 300 && rc.getLocation().distanceSquaredTo(HQLocation) > 36) {
                // make sure miner goes back to HQ to build this
                proceedWithBuild = false;
                setTargetLoc(HQLocation);
            }
            // special case, building FCs on the edge of platform if high rounds and high soup (meaning we have little space
            if (unitToBuild == RobotType.FULFILLMENT_CENTER && rc.getRoundNum() > 1600 && rc.getTeamSoup() > 2000) {
                // make sure miner goes back to HQ to build this
                proceedWithBuild = true;
            }

            if (proceedWithBuild) {
                // if building a building, only build on odd x odd
                boolean builtUnit = false;
                for (int i = 9; --i >= 1; ) {
                    MapLocation buildLoc = rc.adjacentLocation(buildDir);
                    // same parity and must not be too close

                    // if school or FC, just build asap, otherwise build on grid, not dig locations, and can't be next to flood, if next to flood, height must be 12
                    if (rc.onTheMap(buildLoc)) {
                        if (debug) System.out.println("Checkign build dir " + buildDir);
                        if ((unitToBuild == RobotType.REFINERY || unitToBuild == RobotType.DESIGN_SCHOOL || unitToBuild == RobotType.FULFILLMENT_CENTER ||
                                ((buildLoc.x % 2 != HQLocation.x % 2 && buildLoc.y % 2 != HQLocation.y % 2)
                                        && (!locHasFloodAdjacent(buildLoc) || rc.senseElevation(buildLoc) >= 5)
                                )
                        ) && !isDigLocation(buildLoc)) {
                            boolean proceed = true;
                            if (terraformTime) {
                                // only build on higher land
                                if (!rc.canSenseLocation(buildLoc) || rc.senseElevation(buildLoc) < DESIRED_ELEVATION_FOR_TERRAFORM - 2) {
                                    proceed = false;
                                }
                            }
                            else {
                                // has to be adjacent if early on and is school or FC
                                if (!buildLoc.isAdjacentTo(HQLocation) && (unitToBuild == RobotType.DESIGN_SCHOOL || unitToBuild == RobotType.FULFILLMENT_CENTER) && rc.getRoundNum() <= 200) {
                                    proceed = false;
                                }
                            }


                            if (unitToBuild == RobotType.NET_GUN) {
                                // make sure we don;t build too close to other friendly net guns
                                RobotInfo closestNetGun = getClosestRobot(nearbyNetguns, buildLoc);
                                if (closestNetGun != null) {
                                    if (buildLoc.distanceSquaredTo(closestNetGun.location) <= 16) {
                                        proceed = false;
                                    }
                                    else {
                                        if (debug) System.out.println("Building net gun at " + buildLoc + " is ok, not near other netguns");
                                    }
                                }
                            }


                            if (proceed && tryBuild(unitToBuild, buildDir)) {
                                builtUnit = true;
                                break;
                            }

                        }
                    }
                    buildDir = buildDir.rotateRight();
                }
                if (builtUnit) {
                    switch (unitToBuild) {
                        case DESIGN_SCHOOL:
                            DesignSchoolsBuilt++;
                            break;
                        case FULFILLMENT_CENTER:
                            FulfillmentCentersBuilt++;
                            break;
                    }
                    // add to refinery locations list
                    //RefineryLocations.add(rc.adjacentLocation(buildDir));
                }
                // if we built a refinery, we also try and build a vaporator given funds

                // go back to miner role
                role = MINER;
            }
        }
        else if (role == RETURNING) {
            // targetLoc should be place miner tries to return to
            MapLocation depositLoc = targetLoc;
            if (rc.getLocation().isAdjacentTo(HQLocation)) {
                depositLoc = HQLocation;
            }
            if (rc.getLocation().isAdjacentTo(depositLoc)) {
                // else we are there, deposit and start mining again
                Direction depositDir = rc.getLocation().directionTo(depositLoc);
                // TODO: do something if we can't deposit for some reason despite already next to refinery/HQ and right direction
                if (rc.canDepositSoup(depositDir)) {
                    rc.depositSoup(depositDir, rc.getSoupCarrying());
                    if (debug) System.out.println("Deposited soup to " + depositLoc);
                    lastDepositedRefinery = targetLoc; // update to targetLoc so we don't accidentally set HQ as a possible deposit loc

                    // reset roles
                    role = MINER;
                    targetLoc = null;
                    closestToTargetLocSoFar = 9999999;

                }
            }
            /*
            for (Direction dir : directions) {
                if (rc.canDepositSoup(dir)) {
                    rc.depositSoup(dir, rc.getSoupCarrying());
                    if (debug) System.out.println("Deposited soup to " + targetLoc);
                    role = MINER;
                    targetLoc = null;
                };
            }*/
        }

        // whatever targetloc is, try to go to it
        if (targetLoc != null) {
            // don't go to enemy!
            Direction greedyDir = getBugPathMove(targetLoc, dangerousDirections); //TODO: should return a valid direction usually???
            if (debug) System.out.println("Moving to " + rc.adjacentLocation((greedyDir)) + " to get to " + targetLoc);
            tryMove(greedyDir); // wasting bytecode probably here
        }
        else {
            // no targetLoc and is a miner, if on map edge,
            if (role == MINER) {

            }
        }

        if (debug) {
            System.out.println("Miner " + role + " - Bytecode used: " + Clock.getBytecodeNum() +
                    " | Bytecode left: " + Clock.getBytecodesLeft() +
                    " | SoupLoc Target: " + SoupLocation + " | targetLoc: " + targetLoc +
                    " | Cooldown: " + rc.getCooldownTurns() +" | soup: " + rc.getSoupCarrying());
        }

        // check if miner is in a build wall loc and STUCK
        /*
        if (debug) System.out.println(" Mined? " + mined + " | In wall?" + MainHQWall.contains(rc.getLocation()));
        if (!mined && MainHQWall.contains(rc.getLocation()) && turnCount > 15) {
            stuckRounds++;
            if (stuckRounds > 10) {
                if (debug) System.out.println("been stuck for 4 or more rounds, disintegrate please");
                rc.disintegrate();
            }
        }
        else {
            stuckRounds = 0;
        }*/
    }


    // find closest robots from list of robots to a location.



    // algorithm to allow miner to explore and attempt to generally move to new spaces
    // fuzzy pathing, go in general direction and sway side to side
    // general direction is direction away from HQ
    static Direction getExploreDir(HashTable<Direction> dangerousDirections) throws GameActionException {
        if (timeSpentOnExploreLoc > Math.max(rc.getMapHeight(), rc.getMapWidth()) + 5) {
            exploreLocIndex = (exploreLocIndex + 1) % exploreLocs.length;
            timeSpentOnExploreLoc = 0;
        }
        Direction generalDir = rc.getLocation().directionTo(exploreLocs[exploreLocIndex]);

        double p = Math.random();
        if (p < 0.35) {
            generalDir = generalDir.rotateLeft();
            if (p < 0.05) {
                generalDir = generalDir.rotateLeft();
            }
        }
        else if (p > 0.65) {
            generalDir = generalDir.rotateRight();
            if (p > 0.95) {
                generalDir = generalDir.rotateRight();
            }
        }
        Direction dir = getBugPathMove(rc.adjacentLocation(generalDir), dangerousDirections);
        timeSpentOnExploreLoc++;
        return dir;
    }

    static void checkBlockForBuildInfo(Transaction[] transactions) throws GameActionException {
        int distToHQ = rc.getLocation().distanceSquaredTo(HQLocation);
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                // if it is announce SOUP location message
                if ((msg[1] ^ BUILD_A_CENTER) == 0 && role != ATTACK) {
                    int soupThen = msg[2];
                    if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost && soupThen - rc.getTeamSoup() < RobotType.FULFILLMENT_CENTER.cost / 2) {
                        role = BUILDING;
                        unitToBuild = RobotType.FULFILLMENT_CENTER;
                    }
                }
                else if ((msg[1] ^ BUILD_A_SCHOOL) == 0 && role != ATTACK) {
                    int soupThen = msg[2];
                    if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && soupThen - rc.getTeamSoup() < RobotType.DESIGN_SCHOOL.cost / 2) {
                        role = BUILDING;
                        unitToBuild = RobotType.DESIGN_SCHOOL;
                        if (debug) System.out.println("Told to build a school");
                    }
                }
                else if (msg[1] == RobotType.DESIGN_SCHOOL.ordinal()) {
                    firstDesignSchoolBuilt = true;
                    if (debug) System.out.println("i think school was built");
                }
                else if (msg[1] == RobotType.REFINERY.ordinal()) {
                    MapLocation locOfRefinery = parseLoc(msg[2]);
                    if (rc.getLocation().distanceSquaredTo(locOfRefinery) < rc.getLocation().distanceSquaredTo(lastDepositedRefinery)) {
                        lastDepositedRefinery = locOfRefinery;
                    }
                }
                else if (msg[1] == RobotType.FULFILLMENT_CENTER.ordinal()) {
                    firstFulfillmentCenterBuilt = true;
                }
                else if ((msg[1] ^ TERRAFORM_ALL_TIME) == 0) {
                    // return to base if not near soup loc
                    if (SoupLocation == null || rc.getLocation().distanceSquaredTo(SoupLocation) >= 45) {
                        setTargetLoc(HQLocation);
                        if (debug) System.out.println("going back to base for terraforamtion");
                        terraformTime = true;
                    }
                }
                else if ((msg[1] ^ I_AM_DESIGNATED_BUILDER) == 0 && role != ATTACK) {
                    MapLocation locOfOtherMiner = parseLoc(msg[3]);
                    if (rc.getLocation().distanceSquaredTo(locOfOtherMiner) < 92) {
                        // if two miners are close, resolve with ID
                        int otherID = msg[2];
                        if (otherID < rc.getID()) {
                            if (role == BUILDING && unitToBuild == RobotType.VAPORATOR) {
                                role = MINER;
                            }
                        }
                    }
                }
            }
        }
    }
    static void announceI_AM_DESIGNATED_BUILDER() throws GameActionException {
        int[] message = new int[] {generateUNIQUEKEY(), I_AM_DESIGNATED_BUILDER, rc.getID(), hashLoc(rc.getLocation()), randomInt(), randomInt(), randomInt()};
        encodeMsg(message);
        if (debug) System.out.println("ANNOUNCING I_AM_DESIGNATED_BUILDER");

        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
        }
    }
    /**
     * Read announcement code and store in SoupLocation the new soup location found
     * Store closest one
     */
    static void checkBlockForSoupLocations(Transaction[] transactions) throws GameActionException {
        int minDist = 99999;
        double highScore = soupLocScore; // choose the highest score to go to
        // weights miner count and soup amount and distance. less distance, high soup, less miners
        // 1/dist + soup * 1/ (miners)
        // distance function plus soup per miner
        if (SoupLocation != null) {
           // highScore = (1 / rc.getLocation().distanceSquaredTo(SoupLocation);
        }
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                // if it is announce SOUP location message
                if ((msg[1] ^ ANNOUNCE_SOUP_LOCATION) == 0) {
                    int minersNearby = msg[4];
                    int soupNearby = msg[3];
                    MapLocation potentialLoc = parseLoc(msg[2]);
                    int dist = rc.getLocation().distanceSquaredTo(potentialLoc);

                    // weight soup per miner very high. We dont need too many miners per soup depo.
                    // weight distance negatively early game, less over time,
                    double score = 0;
                    // TODO: the distWeight should change based on round num and map size,
                    //  we approximate when our miners probably have explored the whole map and should now focus on
                    //  mining more, using the weighted soup/miners value
                    double distWeight = 0.2;
                    if (rc.getRoundNum() <= 400) {
                        score = -Math.pow((Math.sqrt(soupNearby)) * (dist + 1), distWeight) + Math.pow(soupNearby * (1.0 / (minersNearby + 1)), 1.5);
                    }
                    else {
                        distWeight = 0.2;
                        score = -Math.pow((Math.sqrt(soupNearby)) * (dist + 1), distWeight) + Math.pow(soupNearby * (1.0 / (minersNearby + 1)), 1.5);
                    }
                    if (debug) System.out.println("Found soup location in messages: " + potentialLoc + " score: " + score +
                            " | NearbySoup: " + soupNearby + " | MinersNearby: " + minersNearby + " | Dist: "+ dist);
                    if (score > highScore) {
                        SoupLocation = potentialLoc;
                        highScore = score;
                    }
                    else {
                        // already have soup location target that still exists, continue with mining it
                        // TODO: Do something about measuring how much soup is left, and announcing it.
                    }
                }
            }
        }
        soupLocScore = highScore;
    }


    public static void setup() throws GameActionException {
        storeHQLocationAndGetConstants();

        // if null cuz too early in the game, find by search
        if (HQLocation == null) {
            // find by searching. NOTE MINERS ARE THE ONLY ONES WHO CAN FIND HQ ON THEIR OWN
            RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
                RobotInfo info = nearbyFriendlyRobots[i];
                switch (info.type) {
                    case HQ:
                        HQLocation = info.location;
                        break;
                }
            }
        }
        storeEnemyHQLocations();
        HQParity = (HQLocation.x + HQLocation.y) % 2;
        if (debug) System.out.println("HQ at " + HQLocation);
        // needs to determine a direction to go explore in

        lastDepositedRefinery = HQLocation;
        // 4 corners and center
        exploreLocs = new MapLocation[]{
                new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2),
                new MapLocation(0, 0),
                new MapLocation(0, rc.getMapHeight()),
                new MapLocation(rc.getMapWidth(), 0),
                new MapLocation(rc.getMapWidth(), rc.getMapHeight())
        };
        exploreLocIndex = (int) (Math.random() * exploreLocs.length);

        for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i >= 0; ) {
            int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
            MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
            MainHQWall.add(loc);
        }
        
    }
}

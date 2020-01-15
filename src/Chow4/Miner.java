package Chow4;

import Chow4.utils.LinkedList;
import Chow4.utils.Node;
import battlecode.common.*;

public class Miner extends RobotPlayer {
    static final int SCOUT = 0; // default to search for patches of soup and what not
    static final int MINER = 1; // default to go and mine nearest souplocation it knows
    static final int RETURNING = 2; // RETURNING TO SOME REFINERY OR HQ TO DEPOSIT
    static final int BUILDING = 3;

    static Direction minedDirection;
    static int FulfillmentCentersBuilt = 0;
    static int DesignSchoolsBuilt = 0; // how many design schools this robot knows have been built ??

    static MapLocation enemyBaseLocation = null;
    // score of the souplocation it is probably heading towards
    static double soupLocScore = 0;

    // exploration stuff
    static int timeSpentOnExploreLoc = 0;
    static MapLocation[] exploreLocs;
    static int exploreLocIndex = 0;

    static RobotType unitToBuild; // unit to build if role is building

    static boolean blocked = false; // whether or not unit couldn't determine a path to goal last round

    static int role = MINER; // default ROLE
    static int HQParity; // parity of HQLocation.x + HQLocation.y
    static LinkedList<MapLocation> RefineryLocations = new LinkedList<>();

    public static void run() throws GameActionException {
        // try to get out of water, checks if in water for you
        getOutOfWater();

        // always read last round's blocks
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        boolean mined = false;

        // if getting build information, use it
        checkBlockForBuildInfo(lastRoundsBlocks);

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
                targetLoc = HQLocation;
                role = RETURNING;
            }
        }

        /* BIG FRIENDLY BOTS SEARCH LOOP thing */
        int EnemyDroneCount = 0;
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
            switch (info.type) {
                case DELIVERY_DRONE:
                    EnemyDroneCount++;
                    break;

            }
        }
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        int RefineryCount = 0;
        int NetGunCount = 0;
        int MinerCount = 0;
        int DesignSchoolCount = 0;
        int VaporatorCount = 0;
        int FulfillmentCenterCount = 0;
        MapLocation nearestRefinery = HQLocation;
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
                            targetLoc = info.location;
                        }
                    }
                    break;
                case NET_GUN:
                    NetGunCount++;
                    break;
                case DESIGN_SCHOOL:
                    DesignSchoolCount++;
                    break;
                case MINER:
                    MinerCount++;
                    break;
                case FULFILLMENT_CENTER:
                    FulfillmentCenterCount++;
                    break;
                case VAPORATOR:
                    VaporatorCount++;
                    break;
            }
        }
        if (role == BUILDING) {
            // if we are trying to build but we already have one, stop
            if (unitToBuild == RobotType.DESIGN_SCHOOL && DesignSchoolCount > 0) {
                role = MINER;
            }
            else if (unitToBuild == RobotType.FULFILLMENT_CENTER && FulfillmentCenterCount > 0) {
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
                            if (!rc.senseFlooding(checkLoc) && dist < minDistToNearestSoup) {
                                SoupLocation = checkLoc;
                                minDistToNearestSoup = dist; // set this so we wont reset SoupLocation as we add soupNearbyCount
                                if (debug) System.out.println("Found soup location at " + checkLoc);

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
            else {
                // if we can no longer sense location, break out of for loop then as all other BFS deltas will be unsensorable
                break;
            }
        }

        // look for enemey location and see if its there
        if (enemyBaseLocation == null) {
            Node<MapLocation> node = enemyHQLocations.head;
            for (int i = 0; i++ < enemyHQLocations.size; ) {
                // check if there is enemey base
                if (rc.canSenseLocation(node.val) && rc.isLocationOccupied(node.val)) {
                    RobotInfo maybeEnemyHQ = rc.senseRobotAtLocation(node.val);
                    if (maybeEnemyHQ.type == RobotType.HQ && maybeEnemyHQ.team == enemyTeam) {
                        if (debug) System.out.println("MINER FOUND ENEMY HQ at " + node.val);
                        enemyBaseLocation = maybeEnemyHQ.location;
                        break;
                    }
                }
                node = node.next;

            }
        }

        if (role == MINER) {
            // TODO: cost of announcement should be upped in later rounds with many units.
            // announce soup location if we just made a new soup location
            if (SoupLocation != null && newLocation) {
                // YELLOW means we found soup location, and we make announcement!
                if (debug) rc.setIndicatorDot(SoupLocation, 255, 200, 20);
                announceSoupLocation(SoupLocation, 1, soupNearbyCount, MinerCount);
            }

            // check messages for soup locations, possibly closer
            checkBlockForSoupLocations(lastRoundsBlocks);

            // Build a refinery if there is enough nearby soup, no refineries nearby, and we just mined
            if (mined && soupNearbyCount > 1250 && RefineryCount == 0 && rc.getTeamSoup() >= RobotType.REFINERY.cost) {
                role = BUILDING;
                unitToBuild = RobotType.REFINERY;
            }
            // early game
            // TODO: TUNE PARAM!
            else if (rc.getRoundNum() <= 300) {
                if (rc.getTeamSoup() >= 1000) {
                    role = BUILDING;
                    unitToBuild = RobotType.VAPORATOR;
                }
                else if ((mined || RefineryCount > 0) && VaporatorCount > 0 && DesignSchoolCount == 0 && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost + 350) {
                    unitToBuild = RobotType.DESIGN_SCHOOL;
                    role = BUILDING;
                }
                else if (RefineryCount > 0 && VaporatorCount > 0 && FulfillmentCentersBuilt < 1 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost + 350) {
                    role = BUILDING;
                    unitToBuild = RobotType.FULFILLMENT_CENTER;
                }

            }
            // only build a design school if bot just mined or there is more than one refinery nearby to encourage refinery building first?????
            else if ((mined || RefineryCount > 0) && VaporatorCount > 0 && rc.getRoundNum() % 20 == 1 && DesignSchoolCount == 0 && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost + 300) {
                unitToBuild = RobotType.DESIGN_SCHOOL;
                role = BUILDING;
            }
            else if (RefineryCount > 0 && VaporatorCount > 0 && rc.getRoundNum() % 20 == 2 && FulfillmentCenterCount == 0 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost + 300) {
                role = BUILDING;
                unitToBuild = RobotType.FULFILLMENT_CENTER;
            }

            else if (rc.getTeamSoup() >= 1000) {
                role = BUILDING;
                unitToBuild = RobotType.VAPORATOR;
            }
            // build net guns around enemy base!
            if (enemyBaseLocation != null && rc.getLocation().distanceSquaredTo(enemyBaseLocation) <= 24 && NetGunCount < 1 && rc.getTeamSoup() >= RobotType.NET_GUN.cost + 400) {
                role = BUILDING;
                unitToBuild = RobotType.NET_GUN;
            }

            // build netguns out of necessity to combat drones
            if (NetGunCount == 0 && EnemyDroneCount > 0  && rc.getTeamSoup() >= RobotType.NET_GUN.cost) {
                role = BUILDING;
                unitToBuild = RobotType.NET_GUN;
            }

            // EXPLORE if still no soup found
            if (SoupLocation == null) {
                if (debug) System.out.println("Exploring to " + exploreLocs[exploreLocIndex]);
                targetLoc = rc.adjacentLocation(getExploreDir());
            }
            // otherwise we approach the soup location.
            else {

                // check if we sense the place, if not, we continue branch, otherwise check if there is soup left
                if (!rc.canSenseLocation(SoupLocation) || rc.senseSoup(SoupLocation) > 0) {
                    // if not close enough to soup location, move towards it as it still has soup there
                    if (SoupLocation.distanceSquaredTo(rc.getLocation()) > 1) {
                        if (debug) System.out.println("Heading to soup location " + SoupLocation + " with score " + soupLocScore);
                        targetLoc = SoupLocation;
                    } else {
                        // close enough...
                    }
                } else {
                    // no soup left at location
                    SoupLocation = null;
                    soupLocScore = 0;
                }

            }
        }
        else if (role == BUILDING) {
            Direction buildDir = Direction.NORTH;
            if (minedDirection != null) {
                if (unitToBuild == RobotType.REFINERY) {
                    buildDir = minedDirection;
                }
                else {
                    buildDir = minedDirection.opposite();
                }
            }
            // if building a building, only build on odd x odd
            boolean builtUnit = false;
            for (int i = 9; --i >= 1;) {
                MapLocation buildLoc = rc.adjacentLocation(buildDir);
                // same parity and must not be too close
                if ((buildLoc.x + buildLoc.y) % 2 == HQParity && HQLocation.distanceSquaredTo(buildLoc) > 8) {
                    if (tryBuild(unitToBuild, buildDir)) {
                        builtUnit = true;
                        break;
                    } else {

                        // TODO: optimize

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
        else if (role == RETURNING) {
            // targetLoc should be place miner tries to return to
            if (rc.getLocation().distanceSquaredTo(targetLoc) > 1) {

                //Direction greedyDir = getGreedyMove(targetLoc);
                //if (debug) System.out.println("Heading to soup depo at " + targetLoc + " by moving to " + rc.adjacentLocation(greedyDir));
                //tryMove(greedyDir);
            }
            else {
                // else we are there, deposit and start mining again
                Direction depositDir = rc.getLocation().directionTo(targetLoc);
                // TODO: do something if we can't deposit for some reason despite already next to refinery/HQ and right direction
                if (rc.canDepositSoup(depositDir)) {
                    rc.depositSoup(depositDir, rc.getSoupCarrying());
                    if (debug) System.out.println("Deposited soup to " + targetLoc);
                    // reset roles
                    role = MINER;
                    targetLoc = null;
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
            Direction greedyDir = getBugPathMove(targetLoc); //TODO: should return a valid direction usually???
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
                    " | Cooldown: " + rc.getCooldownTurns());
        }
    }


    // algorithm to allow miner to explore and attempt to generally move to new spaces
    // fuzzy pathing, go in general direction and sway side to side
    // general direction is direction away from HQ
    static Direction getExploreDir() throws GameActionException {
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
        Direction dir = getBugPathMove(rc.adjacentLocation(generalDir));
        timeSpentOnExploreLoc++;
        return dir;
    }

    static void checkBlockForBuildInfo(Transaction[] transactions) throws GameActionException {
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                // if it is announce SOUP location message
                if ((msg[1] ^ NEED_DRONES_FOR_DEFENCE) == 0) {
                    //int origSoup = msg[2];
                    //int soupSpent = origSoup - rc.getTeamSoup();
                    //if (debug) System.out.println("Building drone because HQ said so? Soup spent so far since message " + soupSpent);
                    // if soup spent / number of landscapers needed is greater than cost

                    // ensure we have enough soup to build fulfillment center
                    if (rc.getTeamSoup() > RobotType.DELIVERY_DRONE.cost + RobotType.FULFILLMENT_CENTER.cost) {
                        role = BUILDING;
                        unitToBuild = RobotType.FULFILLMENT_CENTER;
                    }
                }
            }
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
                    double distWeight = 1.2;
                    if (rc.getRoundNum() <= 400) {
                        score = -Math.pow((Math.sqrt(soupNearby)) * (dist + 1), distWeight) + Math.pow(soupNearby * (1.0 / (minersNearby + 1)), 1.5);
                    }
                    else {
                        distWeight = 0.5;
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
        storeHQLocation();

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

        // 4 corners and center
        exploreLocs = new MapLocation[] {
                new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2),
                new MapLocation(0, 0),
                new MapLocation(0, rc.getMapHeight()),
                new MapLocation(rc.getMapWidth(), 0),
                new MapLocation(rc.getMapWidth(), rc.getMapHeight())
        };
        exploreLocIndex = (int) (Math.random() * exploreLocs.length);
    }
}

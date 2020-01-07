package bot1;

import battlecode.common.*;
import bot1.utils.LinkedList;

public class Miner extends RobotPlayer {
    static final int SCOUT = 0; // default to search for patches of soup and what not
    static final int MINER = 1; // default to go and mine nearest souplocation it knows
    static final int RETURNING = 2; // RETURNING TO SOME REFINERY OR HQ TO DEPOSIT
    static final int BUILDING = 3;
    static RobotType unitToBuild;
    // if no soup location known, acts as scout
    static int role = MINER; // default ROLE
    static LinkedList<MapLocation> RefineryLocations = new LinkedList<>();
    static MapLocation targetLoc; // location to head towards
    public static void run() throws GameActionException {
        // try to get out of water, checks if in water for you
        getOutOfWater();

        if (role == BUILDING) {
            Direction buildDir = Direction.NORTH;
            boolean builtUnit = false;
            for (int i = 9; --i >= 1;) {
                if (tryBuild(unitToBuild, buildDir)) {
                    builtUnit = true;
                    break;
                }
                else {
                    buildDir = buildDir.rotateRight();
                }
            }
            if (builtUnit) {
                // add to refinery locations list
                RefineryLocations.add(rc.adjacentLocation(buildDir));
            }
            // go back to miner role
            role = MINER;
            //unitToBuild = null;
        }



        // always read last round's blocks
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        // we always search for soup patches and move around if we don't find any in vicinity
        // once we found one, we announce the soup location
        // we also check for announcements to see if there is one found already...
        if (role == MINER) {
            // Strat: MINE if possible!
            // TODO: can do with mining optimization? Mine furthest tile away from friends?
            // try to mine if mining max rate one turn won't go over soup limit (waste of mining power)
            if (rc.getSoupCarrying() <= RobotType.MINER.soupLimit - GameConstants.SOUP_MINING_RATE) {
                for (Direction dir: directions) {
                    // for each direction, check if there is soup in that direction
                    MapLocation newLoc = rc.adjacentLocation(dir);
                    if(rc.canMineSoup(dir)) {
                        rc.mineSoup(dir);
                        if (debug) {
                            System.out.println("Turn: " + turnCount + " - I mined " + newLoc +"; Now have " + rc.getSoupCarrying());
                        }
                        // if the location no longer has soup, set SoupLocation to null as we have no target
                        if (rc.senseSoup(newLoc) <= 0) {
                            SoupLocation = null;
                        }
                        break;
                    }
                }
            }
            // else if we are near full, we go to nearest refinery known, otherwise go to HQ
            else {
                targetLoc = getNearestDropsite();
                role = RETURNING;
            }

            int soupNearbyCount = 0; // amount of soup nearby in BFS search range

            // Search with BFS
            search:
            {
                int minDist = 99999999;
                boolean newLocation = false;
                if (SoupLocation == null) {
                    newLocation = true;
                }
                if (SoupLocation != null) {
                    minDist = rc.getLocation().distanceSquaredTo(SoupLocation);
                }
                /*
                  TODO: searches 162 locations if we go through entire array. lots of bytecode...
                   could optimize by searching locations within 2 units and those 6 >= units >=4 away...
                */
                // iterate backwards to start from outer most field of view to search for patch of soup
                //for (int i = Constants.BFSDeltas35.length; --i >= 0; ) {
                // finds closest soup location in sensor range and also find total soup nearby

                for (int i = 0; i < Constants.BFSDeltas35.length; i++) {
                    int[] deltas = Constants.BFSDeltas35[i];
                    MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
                    // TODO: instead of canSenseLocation, maybe do the math and choose the right BFS deltas to iterate over
                    if (rc.canSenseLocation(checkLoc)) {
                        // TODO: maybe change minimum to higher or dependent on team soup (if we are rich, don't mine less than x etc.)
                        if (rc.senseSoup(checkLoc) > 0) {
                            soupNearbyCount += rc.senseSoup(checkLoc);
                            int dist = rc.getLocation().distanceSquaredTo(checkLoc);
                            if (!rc.senseFlooding(checkLoc) && dist < minDist) {
                                SoupLocation = checkLoc;
                                minDist = dist; // set this so we wont reset SoupLocation as we add soupNearbyCount
                                if (debug) System.out.println("I found soup location at " + checkLoc);

                                //break search;
                            } else {
                                // TODO: handle when we find a flooded patch, how do we mark it for clearing by landscapers?
                                // found a tile with soup, but its flooded
                                // announceSoupLocation(checkLoc, 0, false);
                            }
                        }

                    }
                    else {
                        // if we can no longer sense location, break out of for loop then as all other BFS deltas will be unsensorable
                        break;
                    }
                }
                // TODO: cost of announcement should be upped in later rounds with many units.
                // announce soup location if we just made a new soup location
                if (SoupLocation != null && newLocation) {
                    // YELLOW means we found soup location, and we make announcement!
                    if (debug) rc.setIndicatorDot(SoupLocation, 255, 200, 20);
                    announceSoupLocation(SoupLocation, 0, soupNearbyCount);
                }


                System.out.println(soupNearbyCount + " soup nearby");
                // if we haven't broken out of this search: tag, then we haven't found soup
                // look through last rounds announcements, see whats there
                checkBlockForSoupLocations(lastRoundsBlocks);

            }

            // TODO move this function to RobotPlayer.java for general searching of robots?
            // Search sensor range friendly robots
            int RefineryCount = 0;
            MapLocation nearestRefinery = HQLocation;
            int minDist = rc.getLocation().distanceSquaredTo(HQLocation);
            for (int i = nearbyFriendlyRobots.length; --i >= 0;) {
                RobotInfo info = nearbyFriendlyRobots[i];
                if (info.type == RobotType.REFINERY) {
                    RefineryCount++;
                    // if bot is returning, locate nearest refinery as well
                    if (role == RETURNING) {
                        int dist = rc.getLocation().distanceSquaredTo(info.location);
                        if (dist < minDist) {
                            minDist = dist;
                            targetLoc = info.location;
                        }
                    }
                }
            }


            // set up for building refinery next turn
            if (soupNearbyCount > 500 && RefineryCount == 0 && rc.getTeamSoup() >= RobotType.REFINERY.cost) {
                role = BUILDING;
                unitToBuild = RobotType.REFINERY;
            }


            // EXPLORE if still no soup found
            if (SoupLocation == null) {
                if (debug) System.out.println("Exploring");
                explore();
            }
            // otherwise we approach the soup location.
            else {

                // check if we sense the place, if not, we continue branch, otherwise check if there is soup left
                if (!rc.canSenseLocation(SoupLocation) || rc.senseSoup(SoupLocation) > 0) {
                    // if not close enough to soup location, move towards it as it still has soup there
                    if (SoupLocation.distanceSquaredTo(rc.getLocation()) > 1) {
                        if (debug) System.out.println("Heading to soup location " + SoupLocation);
                        Direction greedyDir = getGreedyMove(SoupLocation);
                        tryMove(greedyDir);
                    } else {
                        // close enough...
                    }
                } else {
                    // no soup left at location
                    SoupLocation = null;
                }

            }
        }
        else if (role == RETURNING) {
            // targetLoc should be place miner tries to return to
            if (rc.getLocation().distanceSquaredTo(targetLoc) > 1) {

                Direction greedyDir = getGreedyMove(targetLoc);
                if (debug) System.out.println("Heading to soup depo at " + targetLoc + " by moving to " + rc.adjacentLocation(greedyDir));
                tryMove(greedyDir);
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
        }
        // search enemy robots and scout
        //
        // rc.senseNearbyRobots(-1, enemyTeam);

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
    static void explore() throws GameActionException {
        Direction generalDir = rc.getLocation().directionTo(HQLocation).opposite();
        if (rc.getLocation().x <= 5) {

        }
        double p = Math.random();
        if (p < 0.35) {
            generalDir = generalDir.rotateLeft();
            if (p < 0.05) {
                generalDir = generalDir.rotateLeft();
            }
        }
        else {
            generalDir = generalDir.rotateRight();
            if (p > 0.65) {
                generalDir = generalDir.rotateRight();
            }
        }
        Direction dir = getGreedyMove(rc.adjacentLocation(generalDir));
        // first try a good general direction
        if (tryMove(dir));
        // then try some other one
        System.out.println("Attempted to move to " + rc.adjacentLocation(generalDir));
    }

    static MapLocation getNearestDropsite() throws GameActionException {
        // TODO: add refineries
        int minDist = rc.getLocation().distanceSquaredTo(HQLocation);
        // for searching through known refineries, which might be constantly podcasting their location idk
        // also use rc.getLocation().isWithinDistanceSquared(refinery.location, minDist);
        // saves byte code instead of using if statement and wtv
        targetLoc = HQLocation;

        return targetLoc;
    }

    public static void setup() throws GameActionException {
        storeHQLocation();
        if (debug) System.out.println("HQ at " + HQLocation);
        // needs to determine a direction to go explore in
    }
}

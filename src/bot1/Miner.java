package bot1;

import battlecode.common.*;

public class Miner extends RobotPlayer {
    static final int SCOUT = 0; // default to search for patches of soup and what not
    static final int MINER = 1; // default to go and mine nearest souplocation it knows
    static final int RETURNING = 2; // RETURNING TO SOME REFINERY OR HQ TO DEPOSIT
    // if no soup location known, acts as scout
    static int role = MINER; // default ROLE
    static MapLocation[] RefineryLocations = new MapLocation[100];
    static MapLocation targetLoc; // location to head towards
    // BFS deltas
    static final int[][] BFS = new int[][] {{0,0},{1,0},{0,-1},{-1,0},{0,1},{2,0},{1,-1},{0,-2},{-1,-1},{-2,0},{-1,1},{0,2},{1,1}};
    // static final int[][] BFSFlipped = new int[][]{{6,6}};
    public static void run() throws GameActionException {

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




        // if bot has no target mining patch, continue in scout mode
        // we search for soup patches and move around if we dont find any in vicinity
        // once we found one, we announce the soup location
        // we also check for announcements to see if there is one found already...
        if (role == MINER) {
            search:
            {
                if (SoupLocation == null) {
                /*
                  TODO: searches 162 locations if we go through entire array. lots of bytecode...
                   could optimize by searching locations within 2 units and those 6 >= units >=4 away...
                */
                    // TODO: BIG: WHEN POLLUTED, CANT SENSE THAT FAR : ISSUE ISSUE ISSUE
                    // iterate backwards to start from outer most field of view to search for patch of soup
                    for (int i = Constants.BFSDeltas35.length; --i >= 0; ) {
                        int[] deltas = Constants.BFSDeltas35[i];
                        MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
                        // TODO: instead of canSenseLocation, maybe do the math and choose the right BFS deltas to iterate over
                        if (rc.canSenseLocation(checkLoc)) {
                            // TODO: maybe change minimum to higher or dependent on team soup (if we are rich, don't mine less than x etc.)
                            if (rc.senseSoup(checkLoc) > 0) {
                                if (!rc.senseFlooding(checkLoc)) {
                                    SoupLocation = checkLoc;
                                    // TODO: cost of announcement should be upped in later rounds with many units.
                                    // announce soup location if it is not flooding and it has soup.
                                    announceSoupLocation(checkLoc, 0);

                                    if (debug) System.out.println("I found soup location at " + checkLoc);
                                    // YELLOW means they found soup location!
                                    if (debug) rc.setIndicatorDot(checkLoc, 255, 200, 20);
                                    break search;
                                } else {
                                    // TODO: handle when we find a flooded patch, how do we mark it for clearing by landscapers?
                                    // found a tile with soup, but its flooded
                                    // announceSoupLocation(checkLoc, 0, false);
                                }
                            }

                        }
                    }
                    // if we haven't broken out of this search: tag, then we haven't found soup
                    // look through last rounds announcements, see whats there
                    Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
                    checkBlockForSoupLocations(lastRoundsBlocks);
                }
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
                if (debug) System.out.println("Heading to soup depo at " + targetLoc);
                Direction greedyDir = getGreedyMove(targetLoc);
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
                    " | SoupLoc Target: " + SoupLocation + " | targetLoc: " + targetLoc);
        }
    }

    // algorithm to allow miner to explore and attempt to generally move to new spaces
    // fuzzy pathing, go in general direction and sway side to side
    static void explore() throws GameActionException {
        Direction dir = randomDirection();
        if (tryMove(dir));
        System.out.println("Attempted to move to " + rc.adjacentLocation(dir));
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

package Chow10;

import Chow10.utils.*;
import battlecode.common.*;

public class DesignSchool extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static int landscapersBuilt = 0;
    static int vaporatorsBuilt = 0;
    static boolean needLandscaper = false;
    static boolean dontBuild = false;
    static boolean wallIn = false;
    static boolean terraformingTime = false;
    static HashTable<MapLocation> MainWall = new HashTable<>(8);
    public static void run() throws GameActionException {

        // look for enemy location and see if its there
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


        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
        }
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        boolean willBuild = false;
        if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost && landscapersBuilt < 2) {
            willBuild = true;
        }
        checkMessages(lastRoundsBlocks);
        if (needLandscaper) {
            willBuild = true;
            needLandscaper = false;
            if (debug) System.out.println("Building landscaper as asked by HQ ");
        }
        /*
        if (vaporatorsBuilt * 2 + 2 > landscapersBuilt && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 250) {
            willBuild = true;
            if (debug) System.out.println("Building landscaper matching vaporators");
        }*/
        if (rc.getTeamSoup() > 1000 && rc.getRoundNum() % 4 == 0) {
            willBuild = true;
        }
        if (dontBuild) {
            willBuild = false;
            dontBuild = false;
        }

        if (terraformingTime && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 250 && rc.getLocation().distanceSquaredTo(HQLocation) <= 48) {
            if (vaporatorsBuilt > 8) {
                if (vaporatorsBuilt * 1.15 + 5 > landscapersBuilt) {
                    willBuild = true;
                }
            }
            else {
                if (vaporatorsBuilt * 1 > landscapersBuilt) {
                    willBuild = true;
                }
            }
            if (rc.getRoundNum() >= 800 && vaporatorsBuilt >= 4) {
                if (vaporatorsBuilt * 1.1 + 5 > landscapersBuilt) {
                    willBuild = true;
                }
            }

        }

        // if built next to enemy base, build landscapers
        if (rc.getRoundNum() >= 1650 && enemyBaseLocation != null && enemyBaseLocation.distanceSquaredTo(rc.getLocation()) <= 2) {
            if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost) {
                willBuild = true;
            }
        }

        if (debug) System.out.println("Trying to build: " + willBuild);
        if (willBuild) {
            // should rely on some signal
            boolean builtUnit = false;
            buildDir = rc.getLocation().directionTo(HQLocation);
            for (int i = 9; --i >= 1; ) {
                if (tryBuild(RobotType.LANDSCAPER, buildDir) && !isDigLocation(rc.adjacentLocation(buildDir))) {
                    builtUnit = true;
                    break;
                } else {
                    buildDir = buildDir.rotateRight();
                }
            }
            if (builtUnit) {
                landscapersBuilt++;
            }
            buildDir = buildDir.rotateRight();
        }

        // check if i should disintegrate
        Direction dirCheck = Direction.NORTH;
        int i = 0;
        int elevation = rc.senseElevation(rc.getLocation());
        boolean blocked = true;
        while(i++ < 8) {
            MapLocation adjLoc = rc.adjacentLocation(dirCheck);

            // within elevation and ( not occupied or if occupied by unit not building, or if by unit, is not on HQ walls)
            if (rc.onTheMap(adjLoc)) {
                int asideElevation = rc.senseElevation(adjLoc);
                if (asideElevation <= elevation + 3 && asideElevation >= elevation - 3) {
                    if (!rc.isLocationOccupied(adjLoc)) {
                        blocked = false;
                    }
                    else {
                        RobotInfo info = rc.senseRobotAtLocation(adjLoc);
                        // not blocked if unit there is not on mainwall
                        if (!isBuilding(info) && !MainWall.contains(adjLoc)) {
                            blocked = false;
                        }
                    }
                    break;
                }
            }

            dirCheck = dirCheck.rotateLeft();
        }
        // if blocked, high rounds, and within platform with some buffer distance, disintegrate as needed
        if (blocked && rc.getRoundNum() >= 1000 && rc.getLocation().distanceSquaredTo(HQLocation) <= MAX_TERRAFORM_DIST + 20) {
            rc.disintegrate();
        }
        if (rc.getLocation().distanceSquaredTo(HQLocation) <= HQ_LAND_RANGE && wallIn) {
            rc.disintegrate();
        }
    }
    static void checkMessages(Transaction[] transactions) throws GameActionException {
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                // FIXME: Buggy, better way?
                if ((msg[1] ^ NEED_LANDSCAPERS_FOR_DEFENCE) == 0) {
                    int origSoup = msg[2];
                    int soupSpent = origSoup - rc.getTeamSoup();
                    // if soup spent / number of landscapers needed is greater than cost
                    if (soupSpent / msg[3] < RobotType.LANDSCAPER.cost) {
                        needLandscaper = true;
                    }
                }
                else if ((msg[1] ^ BUILD_DRONE_NOW) == 0) {
                    dontBuild = true;
                }
                else if (msg[1] == RobotType.VAPORATOR.ordinal()) {
                    vaporatorsBuilt++;
                }
                else if ((msg[1] ^ TERRAFORM_ALL_TIME) == 0) {
                    terraformingTime = true;
                }
                else if ((msg[1] ^ WALL_IN) == 0 || (msg[1] ^ TERRAFORM_AND_WALL_IN) == 0) {
                    wallIn = true;
                    vaporatorsBuilt -= 4;
                }
            }
        }

    }
    public static void setup() throws GameActionException {
        storeHQLocationAndGetConstants();
        announceSelfLocation(1);
        for (int i = Constants.FirstLandscaperPosAroundHQ.length; --i>= 0; ) {
            int[] deltas = Constants.FirstLandscaperPosAroundHQ[i];
            MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
            if (rc.onTheMap(loc)) {
                MainWall.add(loc);
            }
        }
        for (int i = Constants.LandscaperPosAroundHQ.length; --i>= 0; ) {
            int[] deltas = Constants.LandscaperPosAroundHQ[i];
            MapLocation loc = HQLocation.translate(deltas[0], deltas[1]);
            if (rc.onTheMap(loc)) {
                MainWall.add(loc);
            }

        }
    }
}

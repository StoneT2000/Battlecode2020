package Chow7;

import battlecode.common.*;

public class DesignSchool extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static int landscapersBuilt = 0;
    static int vaporatorsBuilt = 0;
    static boolean needLandscaper = false;
    static boolean dontBuild = false;
    public static void run() throws GameActionException {
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
        if (vaporatorsBuilt * 2 + 2 > landscapersBuilt && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 250) {
            willBuild = true;
            if (debug) System.out.println("Building landscaper matching vaporators");
        }
        if (rc.getTeamSoup() > 1000) {
            willBuild = true;
        }
        if (dontBuild) {
            willBuild = false;
            dontBuild = false;
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
            }
        }

    }
    public static void setup() throws GameActionException {
        storeHQLocation();
        announceSelfLocation(1);
    }
}

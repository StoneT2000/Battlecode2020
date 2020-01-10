package bot1;

import battlecode.common.*;

public class DesignSchool extends RobotPlayer {
    static Direction buildDir = Direction.NORTH;
    static int landscapersBuilt = 0;
    public static void run() throws GameActionException {
        RobotInfo[] nearbyEnemyRobots = rc.senseNearbyRobots(-1, enemyTeam);

        for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyEnemyRobots[i];
        }
        Transaction[] lastRoundsBlocks = rc.getBlock(rc.getRoundNum() - 1);
        boolean willBuild = false;
        if ((rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 100 && landscapersBuilt < 1) || (rc.getTeamSoup() >= 1300 && rc.getRoundNum() % 10 == 0)
        || (rc.getTeamSoup() >= 700 && rc.getRoundNum() % 40 == 0)
                || (rc.getRoundNum() < 300  && rc.getTeamSoup() >= 300)
        ) {
            willBuild = true;
        }
        if (checkForBuildRequestFromHQ(lastRoundsBlocks)) {
            willBuild = true;
        }

        if (willBuild) {
            // should rely on some signal
            boolean builtUnit = false;
            for (int i = 9; --i >= 1; ) {
                if (tryBuild(RobotType.LANDSCAPER, buildDir)) {
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
    static boolean checkForBuildRequestFromHQ(Transaction[] transactions) throws GameActionException {
        for (int i = transactions.length; --i >= 0;) {
            int[] msg = transactions[i].getMessage();
            decodeMsg(msg);
            if (isOurMessage((msg))) {
                // if it is announce SOUP location message
                if ((msg[1] ^ NEED_LANDSCAPERS_FOR_DEFENCE) == 0) {
                    int origSoup = msg[2];
                    int soupSpent = origSoup - rc.getTeamSoup();
                    // if soup spent / number of landscapers needed is greater than cost
                    if (soupSpent / msg[3] < RobotType.LANDSCAPER.cost) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public static void setup() throws GameActionException {
        storeHQLocation();
    }
}

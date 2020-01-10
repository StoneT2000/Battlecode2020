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

        if ((rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 100 && landscapersBuilt < 1) ||
        (rc.getTeamSoup() >= 1300 && rc.getRoundNum() % 10 == 0) || (rc.getTeamSoup() >= 700 && rc.getRoundNum() % 40 == 0)
                || (rc.getRoundNum() < 300  && rc.getTeamSoup() >= 300)
        ) {
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
    public static void setup() throws GameActionException {
        storeHQLocation();
    }
}

package bot1;

import battlecode.common.*;
public class HQ extends RobotPlayer{
    public static void run() throws GameActionException {
        System.out.println("TEAM SOUP: " + rc.getTeamSoup());
        for (Direction dir : directions)
            tryBuild(RobotType.MINER, dir);
    }
    public static void setup() throws GameActionException {
        // announce self location on turn 1 (will always run)
        announceSelfLocation(0);
        // allows all other bots to refer to the HQ if needed by storeHQLocation()
    }
    // initializes the strategy through placement of first units and broadcasts
    public static void initializeStrategy() throws GameActionException {

    }
}

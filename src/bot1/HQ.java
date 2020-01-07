package bot1;

import battlecode.common.*;
public class HQ extends RobotPlayer{
    public static void run() throws GameActionException {

        for (Direction dir : directions)
            tryBuild(RobotType.MINER, dir);
    }
    public static void setup() throws GameActionException {

    }
}

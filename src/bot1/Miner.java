package bot1;

import battlecode.common.*;

public class Miner extends RobotPlayer {
    public static void run() throws GameActionException {
        if (rc.getCooldownTurns() >= 1) {
            return;
        }


        // TODO: can do with mining optimization? Mine furthest tile away from friends?
        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
            for (Direction dir: directions) {
                // for each direction, check if there is soup in that direction
                MapLocation newLoc = rc.getLocation().add(dir);
                if(rc.canMineSoup(dir)) {
                    rc.mineSoup(dir);
                    if (debug) {
                        System.out.println("Turn: " + turnCount + " - I mined " + newLoc +"; Now have " + rc.getSoupCarrying());
                    }
                    break;
                }
            }
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
        if (debug) {
            System.out.println("Bytecode used: " +  Clock.getBytecodeNum() + " | Bytecode left: " + Clock.getBytecodesLeft());
        }
    }

    public static void setup() throws GameActionException {

    }
}

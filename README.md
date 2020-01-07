# Battlecode 2020 - Bowl of Chowder

Notes:

Max 210/70 = 3 miners at start of game, first movement of first miner is at round 10



# Strategies

## Early

Send a miner to the middle of the map to try and steal any stuff there?

Signal that this is a contested resource and we should steal that one

# Blockchain stuff

Should encode into 7 positions of 32 bit values, 224 bits total to use to send data

AT THE MOMENT, we encode a unique KEY into the first position, encode the type of message in the second position usually

leaving 5 slots left to store data.

## Signals we should send

`announceSoupLocation` - Announce location of soup so miners can swarm there if they didn't find any soup.

`announceWall` - announce location of a high impassable wall blocking a miner from it's target (after doing some pathing)

## Determining transaction fee to use

Store past few rounds and look at average transaction fees of transactions that are not ours
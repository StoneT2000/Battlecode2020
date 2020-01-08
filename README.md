# Battlecode 2020 - Bowl of Chowder

Notes:

Max 210/70 = 3 miners at start of game, first movement of first miner is at round 10



# Programming a bot and reducing bytecode

Generally for bots that need to do a BFS search (or any search of every surrounding visible tile), or the search through the blockchain at round `n` or search through visible bots

Have one general loop, and do a switch case inside to decide what to do at each iteration

NOTE, the cost is approximately `162 * average bytecode cost per iteration` for miners as 162 tiles to search max

# Movement

All functions that should do movement, must return a VALID DIRECTION

# Strategies

## Cheap strategies

Rush with miners, look for enemy base, send signal out about where it is, then surround HQ with miners. have some miners continue to mine and then have surrounding miners build net guns

If they have landscapers then build some drones first? Although HQ can shoot it down

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
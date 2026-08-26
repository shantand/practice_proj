## Introduction
This repository shows the leaderboard creation and concurrent operation handling using Redis. Used Jedis to connect to Redis running locally through Java threads. ZINCRBY is the function used to increment values. ZREVRANGE (0, -1) is used to get the latest 
leaderboard every 2 seconds.
- Each Thread is emitting the records carrying real score for the player
- React frontend using vite is used to display leaderbaord.

# Edit 1
We changed the emission model instead of each thread emitting for 1 player, Now each thread can emit recod for player id between 1 to 1million. And we will get the top 100 only on leaderboard.


## Interesting Points
1. Vite starts devserver at 5371 port and calls APIs on 8080 port to connect to http server.
2. With increase in players the memory requirements of Redis increases. It is hard to maintain leaderboard of 1 million players
3. skiplists performs fast operations of lookup and insert both are O(logn) makes redis suitable for this case.
4. Since, Redis uses sorted sets, even requesting top 100 will always produce the correct results because the operations are atomic.

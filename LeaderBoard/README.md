## Introduction
This repository shows the leaderboard creation and concurrent operation handling using Redis. Used Jedis to connect to Redis running locally through Java threads. ZINCRBY is the function used to increment values. ZREVRANGE (0, -1) is used to get the latest 
leaderboard every 2 seconds.
- Each Thread is emitting the records carrying real score for the player
- React frontend using vite is used to display leaderbaord.

## Interesting Points
1. Vite starts devserver at 5371 port and calls APIs on 8080 port to connect to http server.
2. With increase in players the memory requirements of Redis increases. It is hard to maintain leaderboard of 1 million players
3. 

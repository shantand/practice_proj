## Introduction
This distributor is simulation of actual unique ID generator in distributed systems like snowflake id that takes into account timestamp, machine_id(requestor) and sequence number.

## Interesting Observations-
1. reducing sequence bits can increase collisions. with more than 10 bits its start reducing
2. Timestamp is long value can not fit in 41 bits. For that need to store the difference from custom offset which can be stored avoiding leading zeros
3. Total 63 bits can produce approximately 8*10^18 combinations which are fairly large. And storing all would require 64000 Petabytes which is very huge number, for all practical purpose we may not need t store all the combinations,

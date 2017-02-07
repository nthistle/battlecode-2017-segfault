# battlecode-2017-segfault
Team Segfault's submission for Battlecode 2017.
More information can be found at: https://www.battlecode.org/

5th Place in Highschool division.

## Overall Strategy ##
As we very quickly realized that in most cases the fastest way to win was through attack, with only difficult barrier like maps preventing an efficient assault, we focused our strategy on being able to reach and destroy the enemy as fast as possible. We did allow for some cases where building was more efficient, but in general through extensive testing we found these to be very, very infrequent as efficient troops would almost always be able to reach the enemy by turn 800, which was the turn pure turtling with no defense ended up winning based on victory points. For specific attack plans, our units would attempt to attack the nearest archon to them and then subsequently move on to the next one once it was dead. This progression allowed for an effective approach to handling cases where there were multiple archons that were often paired off.

## RobotBase / General Things ##
RobotBase was the class each unit extended in order to allow for writing generic methods that would be used accross the board. A lot of it involved preprocessing, such as assigning an ID to each unit upon birth and sorting the friendly and enemy archons. The three most commonly used things beyond this included daily tasks, moving, and shooting. Daily tasks were primarily making sure that if there were enough resources to win on victory points, then any surviving unit should try to cash them in. Moving was two-fold, involving dodging and pathfinding. Dodging was used in the face of enemy fire. Essentially, each bullet and its course was treated as a line segment and the nearest point on that line segment was calculated to an attempted move location. In the instance that this was less than the radius, indiciating a collision, the move was penalized. The checked moves were determined by checking in 45 degree offsets with some random stride length ranging from .5 to 1 times the max length. Pathfinding was used to move to specific locations while avoiding barriers. It utilized an A-star approach to move towards the target location. Due to the continuous nature of the map, moves where only checked within one stride radius at a time. In addition, past locations were penalized, repelling away the unit in order to prevent it from getting stuck against walls or holes.

## Archon ##
The archon played a fairly straightforward role, spawning gardeners when it was necessary. This was determined based on if the previous gardeners were done building their trees or if a certain number of turns had passed. It also attempted to move around to prevent being chocked off by gardener pods. Lastly, it displayed a doge in the early turns using indicator dots as an attempt to win any special awards.

## Gardener ##
The gardeners were primarily responsible for building units. Upon spawning, they first determined the ideal space to build a pod based on distance from archon and nearby space. This was used to move them away from the archon and into open areas to prevent choking either the archon or the gardeners. They then build a hexagon pod, with 2 spaces left open to deploy units (usually facing the enemy). They used a ratio determined by the distance to nearby enemy archons and space observed to figure out when to build a unit versus a tree. They then used a second ratio with similar parameters to determine when to build a lumberjack versus a soldier. If a tank was ever buildable, it was always prioritized over soldiers. However, in order to better deal with specific cases, it had hard-coded instructions called cases based on map parameters for the first 200 turns. This was only done if it was the first gardener.

## Soldier ##
The soldiers were the primary combat units. Their logic was fairly straightforward. In the case of combat, or in other words bullets being detected, they would call dodging methods to avoid the fire. Only bullets with an angle less than 90 degrees between their velocity vector and the position vector from the unit to the bullet were counted in order to remove past bullets or bullets fired by this unit. In the case of combat, soldiers would always fire triad shots unless they were very close to a unit in which case they would fire pentad shots. Our soldiers prioritized backing away from enemy troops and then continuing to fire at the last known location. This worked very well as once it would back out of sight range, enemy troops would stop firing as they wouldn't detect us. However, we would continue firing at them and score several hits, giving a huge combat advantage. If no bullets were detected, the unit would then attempt to shoot at any enemy units detected. Utilizing the fact that bullets are point particles, our algorithm would find the tiniest of spaces between obstacles and be able to effectively snipe away at target units, including gardeners in shielded by pods, giving our troops very efficient shooting code. Lastly, in the case of no nearby units, the soldier would pathfind to the next enemy archon.

## Tank ##
The tanks behaved very similarly to our soldiers. The primary difference is that they would also shoot at trees in their way given. This was because tank bullets can output 2.5 times as much damage as a soldier bullet and can therefore greatly clear large barriers in the late game period, working even faster than lumberjacks. They also did not ever attempt to dodge given their slow speed and large size.

## Lumberjack ##
The lumberjacks were primarily used early on to clear space for gardener pods and to allow our units to expand outward. Firstly, they attempted to strike, using their area of effect attack. This attack was scored based on friendly damage and enemy damage, and if above a certain threshold was used instead of tree chopping. This meant that in cases of enemy attacks or with numerous small trees where the AoE attack would be better suited, it was used over the normal tree chopping technique. If this threhold was not met, then lumberjacks prioritized trees to cut based on their proximity to ally archons and enemy archons, effectively clearing a circle around friendly archons and then homing in towards enemy archons to forge a path. They also utilized the distance from them to the target tree to prevent all the lumberjacks from homing onto the same tree. Lastly, the tree scores were fed through multipliers based on their size and potentially contained units. Trees with units were generally prioritized higher as they would allow for faster growth, while big trees where decreased in priority given that the ratio of space cleared to time taken was greater for them. The lumberjacks attempted to pathfind towards their target tree. If there were no trees in sight radius, they attempted to pathfind towards the nearest enemy unit. If this was still not an option, they would move around randomly until they found something to do.

## Scout ##
Our scouts were the simplest of all of our units, only being spawned in the beginning. They would attempt to collect any starting resources on bullet trees in the beginning while dodging any enemy fire. They would also report back any tree data collected to tune the ratios for gardeners.
  
<b>Note:</b> This is primarily an overall summary of what our troops did. There are several more technical things such as soldiers backing up from attacking lumberjacks or gardeners creating emergency orders that were not included in order to prevent a several page report.

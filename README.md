# Regenerating Blocks
- Originally from https://gitlab.com/Quentin3010/regenerating-ores by JeSuisMister
- Continued under the permissive MIT license as per the original repo.

# Behavior
- Plugin adds regenerating variants of normal blocks that respawn after a time delay. 
- Clean and mod friendly / compatible implementation.

# Motivations
- Updated primarily for personal usage similar to the original developer. Regenerating blocks don't get destroyed when broken, instead recovering back to their original state.
- I wanted to use it with ReliableReplacer so that players inhabit worlds properly rather than just flattening everything and behaving like a plague of locusts on otherwise pristine landscapes. 
- I think having a reason to visit the same area over and over encourages people to "settle in" and improve an area. 
- Not being able to mine most "hard" blocks should encourage building on the land and traverse caves etc, not just digging direct paths (encouraging organic development more like the real world).
- Makes skilling implementations more functional if you're able to revisit / farm certain areas.
- I don't currently have plans to port it to later / earlier versions or other modding frameworks. It will likely only ever target versions supported by the Create mod, and most likely only if/when I personally roll up to that version. 

# Major modifications from ancestor repository
- Imported onto GitHub as preferred source management arrangement.
- Moved entire system to a dynamic resource pack which is runtime generated for ease of extension and compatibility. 
- Straightforward configuration. Add the block names to regenerating_blocks.json config and get a regenerating block version.
- Support for modded blocks from other namespaces.
- Regenerating blocks visually repair themselves after being mined.
- Regenerating blocks respect tool properties and enchantments (supports all tools, not just pickaxes). 
- Regenerating blocks match source block drop experience.
- Regenerating blocks are not destroyed by explosions.
- Regenerating blocks can be broken by creative game mode players allowing removal.
- Regenerating blocks inherits all source block tags. 
  Can be checked using similar command: 
  /execute if block x y z #minecraft:mineable/pickaxe run say Tag Mirroring Active) 
  This also fixes things like checks for moss being able to spread to those blocks.
- Regenerating blocks inherit all properties from ancestor (ie hardness, explosion resistance, sounds etc)
- Best efforts have been made to ensure that source block state is respected and compatible.
- Regenerating blocks sparkle when they regenerate (with config toggle)

# Caveats
- Implementations that directly check/compare against a block (ie Block = Blocks.STONE) will not resolve since the regenerating block is a distinct block.
- The above can be fixed by proper use of tags and demonstrates why they should be preferred for compatibility instead of direct comparisons like this. 
- Prefer loading this late in a mod order if possible. I haven't had bad times due to load order in my personal usage so this can be ignored for most users. Probably.

# Bugs
- Much more likely to be addressed if you raise an issue.

# Performance Notes
- I don't like slow code. This has been implemented in a performance sensitive manner.
- Repairing of broken blocks is done using natively implemented scheduled ticks.
- Underlying break data tables don't get walked, always use fast direct key lookups.
- Break status is stored in memory, resets if server is shutdown.
- State is failure safe, resets to ready to be harvested state ensuring blocks don't get locked regenerating.

# Dev notes
- IDE: IntelliJ & Gradle tab reload top right 
- Debugging: Run > Debug Client
- Terminal: .\gradlew clean build
- Artifacts: "\build\libs\regenerating_blocks-x.x.jar"

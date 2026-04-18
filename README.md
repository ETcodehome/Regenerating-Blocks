![Regenerating Blocks title banner](banner.png)

# Regenerating Blocks
- Originally from https://gitlab.com/Quentin3010/regenerating-ores by JeSuisMister
- Continued under the permissive MIT license as per the original repo.

# Behavior
- Plugin makes configured block types regenerate a set time after being broken.
- Clean and mod friendly / compatible implementation.

# Motivations
- Updated primarily for personal usage similar to the original developer. Regenerating blocks don't get destroyed when broken, instead recovering back to their original state over time.
- The hope is to encourage inhabiting worlds properly rather than just flattening everything and behaving like a plague of locusts across pristine landscapes. 
- I think having a reason to visit the same area over and over encourages people to "settle in" and improve an area. 
- Not being able to mine most "hard" blocks should encourage building on the land and traverse caves etc, not just digging direct paths (encouraging organic development more like the real world).
- Makes skilling implementations more functional if you're able to revisit / farm certain areas. 

# Implementation details
- When you load, a copy of each chunk loaded is mirrored into a new world. This is fast and performant (not a fresh generation again).
- These mirrored worlds essentially track which blocks are naturally spawned and are a template for which blocks will regenerate.
- If a *configured* block in the mirror world matches a block in the world (overworld, nether, end) it regenerates.
- Player placed blocks won't regenerate (by design) unless the mirror world has the same block there already.
- Blocks are visibly broken while regenerating (cracks show, repairing fizzes). 
- Blocks sparkle when they regenerate (with config toggle)
- Simple configuration, a block name and a respawn time in seconds in regenerating_blocks.json config and they'll regen.
- Approach should support breaks of all non-entity blocks (including modded blocks).
- One single block on the bottom layer of chunks won't regenerate. In the regenerating mirror worlds it's used to track if a chunk has been mirrored.
- Safe to remove. No polluting world state is baked. Remove the mod, delete the mirror world save data.

# Caveats
- Blocks are still susceptible to conversions (ie stone -> moss, dirt -> farmland). This is not a grief protection plugin.
- A regenerating block destroyed/converted in the main world can be restored by placing the same block back there again.
  Because the mirror world still contains the natural block, it will continue to regenerate.
- I don't currently have plans to port it to later / earlier versions or other modding frameworks. 
  It will likely only ever target versions supported by the Create mod, and most likely only if/when I personally roll up to that version.
- Will increase disk usage. It's a necessary tradeoff to have what is essentially a single state voxel version control system.

# Major modifications from ancestor repository
- Pretty much an end to end rewrite to expand support and make it universally compatible.
- Previously the blocks were new blocks that tried to be other blocks, but now the regenerating blocks ARE the original blocks.
- Imported onto GitHub as preferred source management arrangement.
- Moved entire system to a dynamic resource pack which is runtime generated for ease of extension and compatibility.
- Now supports modded blocks from other namespaces.
- Breaks are not modified, respect tool properties and enchantments (all tools, not just pickaxes). 
- Breaks match source block drop experience behaviour.

# Bugs
- Much more likely to be addressed if you raise an issue.

# Performance Notes
- I don't like slow code. This has been implemented in a performance sensitive manner.
- Break status per block is stored in memory, break status resets when server is shutdown.
- State is failure safe, defaults to ready to be harvested state ensuring blocks never get locked regenerating.

# Dev notes
- IDE: IntelliJ & Gradle tab reload top right 
- Debugging: Run > Debug Client
- Terminal: .\gradlew clean build
- Artifacts: "\build\libs\regenerating_blocks-x.x.jar"

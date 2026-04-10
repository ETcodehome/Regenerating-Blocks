package me.jesuismister.regenerating_ores.blocks;

import me.jesuismister.regenerating_ores.Regenerable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class RegeneratingOreBlock extends Block {
    // Propriété pour indiquer si le bloc est en régénération
    public static final BooleanProperty REGENERATING = BooleanProperty.create("regenerating");
    public static final BooleanProperty DESTROYEDBYPLAYER = BooleanProperty.create("destroyedbyplayer");
    public Regenerable block;
    // Temporary storage to bridge the gap during the super() call
    private static StateDefinition<Block, BlockState> capturedSourceDefinition;

    public RegeneratingOreBlock(Regenerable block) {
        super(prepare(block.sourceBlock));
        this.block = block;

        // Register the default state
        BlockState defaultState = this.stateDefinition.any().setValue(REGENERATING, false).setValue(DESTROYEDBYPLAYER, false);

        // If the source had properties (like 'lit'), we should mirror its default
        // This ensures the light emission function doesn't crash on the first tick
        this.registerDefaultState(defaultState);
    }

    private static BlockBehaviour.Properties prepare(Block source) {
        capturedSourceDefinition = source.getStateDefinition();
        return BlockBehaviour.Properties.ofFullCopy(source).dropsLike(source);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 1. Add our custom property
        builder.add(REGENERATING);
        builder.add(DESTROYEDBYPLAYER);

        // 2. Add all properties from the source block we are copying
        if (capturedSourceDefinition != null) {
            for (Property<?> property : capturedSourceDefinition.getProperties()) {
                // Ensure we don't try to add REGENERATING twice if it somehow exists
                if (!property.getName().equals("regenerating")) {
                    builder.add(property);
                }
            }
            // Clear the static reference to avoid memory leaks
            capturedSourceDefinition = null;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Lorsqu'il est placé, le bloc n'est pas en régénération
        return this.defaultBlockState().setValue(REGENERATING, false);
    }

    // Called when a player successfully finishes breaking a block, but before the block is actually replaced with air.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {
        System.out.println("DEBUG: Checking playerWillDestroy:");

        boolean canDestroyRegeneratingBlock = player.isCreative();
        if (canDestroyRegeneratingBlock && state.getValue(REGENERATING)){
            // yields nothing deliberately
            level.setBlock(pos, state.setValue(DESTROYEDBYPLAYER, true), 3 | 16);
            return super.playerWillDestroy(level, pos, state, player);
        }

        if (!level.isClientSide && !state.getValue(REGENERATING))
        {
            level.setBlock(pos, state.setValue(DESTROYEDBYPLAYER, true), 3 | 16);

            ServerLevel serverLevel = (ServerLevel) level;
            ItemStack tool = player.getMainHandItem();
            BlockState sourceState = this.block.sourceBlock.defaultBlockState();

            // Check if the block even requires a tool (some blocks drop items with bare hands)
            boolean requiresTool = sourceState.requiresCorrectToolForDrops();

            // Check if the tool is actually the correct one
            // This is the NeoForge/Vanilla way to verify the tool's tier and type against the block
            boolean isCorrectTool = !requiresTool || tool.isCorrectToolForDrops(sourceState);
            if (isCorrectTool) {

                // Calculate XP using the precise breaker and tool context
                int xp = this.block.sourceBlock.getExpDrop(
                        this.block.sourceBlock.defaultBlockState(),
                        serverLevel,
                        pos,
                        level.getBlockEntity(pos),
                        player,
                        tool
                );

                // Spawn the experience if the check passes
                if (xp > 0) {
                    this.block.sourceBlock.popExperience(serverLevel, pos, xp);
                }
            }
        }

        System.out.println("DEBUG: playerWillDestroy returning old blockstate");
        return state;
    }

    // Specialized NeoForge/Forge hook that intercepts Explosive damage.
    // Called when an Explosion object includes this block’s coordinates in its "to-be-removed" list.
    // By default, it calls level.removeBlock, which then triggers the destroy hook.
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        System.out.println("DEBUG: Checking block exploded:");
        if (!state.getValue(REGENERATING)) {
            super.onBlockExploded(state, level, pos, explosion);
            destroy(level, pos, state);
        }
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion)
    {
        return !state.getValue(REGENERATING);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        System.out.println("DEBUG: tick handler fired at " + pos.toString());

        // Scenario A: The block is AIR (Drill just broke it, or cleanup failed)
        if (level.getBlockState(pos).isAir()) {
            System.out.println("DEBUG: Restoring AIR to REGENERATING");
            level.setBlock(pos, this.defaultBlockState().setValue(REGENERATING, true), 3);
            level.scheduleTick(pos, this, block.regenAfter * 20);
            return;
        }

        // Scenario B: Cooldown is over, return to NORMAL
        if (state.getValue(REGENERATING)) {
            System.out.println("DEBUG: Cooldown over. Returning to NORMAL.");
            // Use Flag 3 (Update + Re-render)
            level.setBlock(pos, this.defaultBlockState().setValue(REGENERATING, false), 3);
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        System.out.println("DEBUG: getpistonpushreaction:");
        return PushReaction.BLOCK;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        System.out.println("DEBUG: getexplosionresistance:");

        // 3,600,000.0F is the internal value for Bedrock.
        return state.getValue(REGENERATING) ? 3600000.0F : super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter world, BlockPos pos) {

        System.out.println("DEBUG: getdestroyprogress:");

        // never break a block while it is regenerating
        if (state.getValue(REGENERATING)){
            return 0.0F;
        }

        // Delegate to the source block. That way we respect all the source blocks respective tags.
        return this.block.sourceBlock.defaultBlockState().getDestroyProgress(player, world, pos);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.Entity entity) {

        System.out.println("DEBUG: canEntityDestroy:");

        // If it's regenerating, no entity (including contraptions) can destroy it.
        if (state.getValue(REGENERATING)) return false;
        return super.canEntityDestroy(state, level, pos, entity);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 1. Safety: If we are just changing properties (like REGENERATING changing), stop here.
        if (state.is(newState.getBlock())) {
            System.out.println("DEBUG: state is staying the same");
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        boolean playerBroke = state.getValue(DESTROYEDBYPLAYER);
        boolean blockRegenerating = state.getValue(REGENERATING);

        if (playerBroke && blockRegenerating)
        {
            // this was a creative break and the block should be removed.
            System.out.println("DEBUG: block was broken by player in creative. Do not restore.");
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        System.out.println("DEBUG: fell through");

        if (!playerBroke && blockRegenerating){
            level.setBlock(pos, state, 19);
            return;
        }

        // 2. The Logic: We want to restore the block if it's being removed (replaced by Air/Drills)
        if (!level.isClientSide) {
            System.out.println("DEBUG: Triggering restoration for " + (playerBroke ? "Player" : "Machine"));

            level.getServer().execute(() -> {
                if (level.getBlockState(pos).isAir()) {
                    // We create the new state, ensuring REGENERATING is true
                    // and resetting DESTROYEDBYPLAYER for the next cycle.
                    BlockState restored = state
                            .setValue(REGENERATING, true)
                            .setValue(DESTROYEDBYPLAYER, false);

                    super.onRemove(state, level, pos, newState, isMoving);
                    level.levelEvent(2001, pos, Block.getId(state));
                    level.setBlock(pos, restored, 3);
                    return;
                }
            });
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        if (state.getValue(REGENERATING)) {
            // Return a custom sound type with no volume
            return new SoundType(0.0f, 1.0f,
                    SoundEvents.EMPTY, // Break sound
                    SoundEvents.EMPTY, // Step sound
                    SoundEvents.EMPTY, // Place sound
                    SoundEvents.EMPTY, // Hit sound
                    SoundEvents.EMPTY  // Fall sound
            );
        }
        return super.getSoundType(state, level, pos, entity);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && state.getValue(REGENERATING)) {
            System.out.println("DEBUG: Block placed/restored. Scheduling regeneration tick.");
            level.scheduleTick(pos, this, block.regenAfter * 20);
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {

        if (state.getValue(REGENERATING)) {
            return false;
        }

        // Delegate to the source block's harvesting logic
        return this.block.sourceBlock.canHarvestBlock(this.block.sourceBlock.defaultBlockState(), world, pos, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // If the block is currently in its regenerating/indestructible state,
        // we return an empty list so no items are produced.
        if (state.getValue(REGENERATING)) {
            return Collections.emptyList();
        }

        // Otherwise, we delegate to the source block's loot table.
        return super.getDrops(state, params);
    }

    

}

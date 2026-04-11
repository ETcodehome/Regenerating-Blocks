package me.jesuismister.regenerating_ores.blocks;

import me.jesuismister.regenerating_ores.ConfigManager;
import me.jesuismister.regenerating_ores.Regenerable;
import me.jesuismister.regenerating_ores.RegeneratingOres;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
    public static final BooleanProperty REGENERATING = BooleanProperty.create(RegeneratingOres.MOD_ID + "_regenerating");
    public static final BooleanProperty DESTROYEDBYPLAYER = BooleanProperty.create(RegeneratingOres.MOD_ID + "_destroyedbyplayer");
    public Regenerable block;
    private static StateDefinition<Block, BlockState> sourceStateDefinition;


    public RegeneratingOreBlock(Regenerable block) {
        super(prepare(block.GetSourceBlock()));
        this.block = block;

        // Register the default state
        BlockState defaultState = this.stateDefinition.any().setValue(REGENERATING, false).setValue(DESTROYEDBYPLAYER, false);

        // If the source had properties (like 'lit'), we should mirror its default
        // This ensures the light emission function doesn't crash on the first tick
        this.registerDefaultState(defaultState);
    }

    private static BlockBehaviour.Properties prepare(Block source) {
        sourceStateDefinition = source.getStateDefinition();
        return BlockBehaviour.Properties.ofFullCopy(source).dropsLike(source);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        // Custom properties the new block type has
        builder.add(REGENERATING);
        builder.add(DESTROYEDBYPLAYER);

        // Add all properties from the source block we are copying
        if (sourceStateDefinition != null) {

            for (Property<?> prop : sourceStateDefinition.getProperties()) {
                // Ensure we don't try to add REGENERATING twice if it somehow exists
                if (!prop.getName().equals(REGENERATING.getName())) {
                    builder.add(prop);
                }
            }

            // Clear the static reference to avoid memory leaks
            sourceStateDefinition = null;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(REGENERATING, false);
    }

    // Called when a player successfully finishes breaking a block, but before the block is actually replaced with air.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {

        boolean canDestroyRegeneratingBlock = player.isCreative();
        if (canDestroyRegeneratingBlock && state.getValue(REGENERATING)){
            log("Allowed destruction of regenerating block because player is in creative gamemode.");
            level.setBlock(pos, state.setValue(DESTROYEDBYPLAYER, true), 3 | 16);
            return super.playerWillDestroy(level, pos, state, player);
        }

        if (!level.isClientSide && !state.getValue(REGENERATING))
        {
            log("Allowed destruction of fully regenerated block");
            level.setBlock(pos, state.setValue(DESTROYEDBYPLAYER, true), 3 | 16);

            ServerLevel serverLevel = (ServerLevel) level;
            ItemStack tool = player.getMainHandItem();
            BlockState sourceState = this.block.GetSourceBlock().defaultBlockState();

            // Check if the block even requires a tool (some blocks drop items with bare hands)
            boolean requiresTool = sourceState.requiresCorrectToolForDrops();

            // Check if the tool is actually the correct one
            // This is the NeoForge/Vanilla way to verify the tool's tier and type against the block
            boolean isCorrectTool = !requiresTool || tool.isCorrectToolForDrops(sourceState);
            if (isCorrectTool) {
                log("Allowed drops and experience because a correct tool was used.");

                // Calculate XP using the precise breaker and tool context
                int xp = this.block.GetSourceBlock().getExpDrop(
                        this.block.GetSourceBlock().defaultBlockState(),
                        serverLevel,
                        pos,
                        level.getBlockEntity(pos),
                        player,
                        tool
                );

                // Spawn the experience if the check passes
                if (xp > 0) {
                    this.block.GetSourceBlock().popExperience(serverLevel, pos, xp);
                }
            }
        }

        return state;
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion)
    {

        if (state.getValue(REGENERATING)){
            log("Denied block drops from explosion because block is regenerating.");
            return false;
        }

        return super.canDropFromExplosion(state, level, pos, explosion);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {

        // we tick first as it is state tied
        super.tick(state, level, pos, random);

        if (state.getValue(REGENERATING)) {
            log("Regen cooldown ended. Returning to regenerating=false state.");
            level.setBlock(pos, this.defaultBlockState().setValue(REGENERATING, false), 3);
            this.spawnSubtleEffect(level, pos);
        }

    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {

        if (ConfigManager.getSettings().disablePushing()){
            log("Prevented block being pushed (obeyed config value)");
            return PushReaction.BLOCK;
        }

        // Otherwise, default handling
        return super.getPistonPushReaction(state);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {

        if (state.getValue(REGENERATING)){
            log("Provided bedrock values because block should be unbreakable");
            return 3600000.0F;
        }

        // Otherwise, default handling
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter world, BlockPos pos) {

        // never break a block while it is regenerating
        if (state.getValue(REGENERATING)){
            log("Progress 0.0 because block is regenerating");
            return 0.0F;
        }

        // Delegate to the source block. That way we respect all the source blocks respective tags.
        return this.block.GetSourceBlock().defaultBlockState().getDestroyProgress(player, world, pos);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.Entity entity) {

        // If it's regenerating, no entity (including contraptions) can destroy it.
        if (state.getValue(REGENERATING)) {
            log("Destruction prevented because block is regenerating)");
            return false;
        }

        // Otherwise, default handling
        return super.canEntityDestroy(state, level, pos, entity);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {

        if (isMoving) {
            log("Block is moving, didn't interfere further.");
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        if (state.is(newState.getBlock())) {
            log("Properties change only, didn't interfere further.");
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        boolean playerBroke = state.getValue(DESTROYEDBYPLAYER);
        boolean blockRegenerating = state.getValue(REGENERATING);

        if (playerBroke && blockRegenerating)
        {
            log("Block broken by player in creative. Allowed permanent destruction.");
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        if (!playerBroke && blockRegenerating){
            log("Block broken by non-player while regenerating. Preventing destruction.");
            super.onRemove(state, level, pos, newState, isMoving);
            level.setBlock(pos, state, 19);
            return;
        }

        if (!level.isClientSide) {
            log("Block broken by " + (playerBroke ? "player" : "non-player") + ".");

            // Handles almost all conversions, moss, sculk spread, burning to ash etc
            boolean isAir = newState.isAir();
            boolean stateSwap = newState.is(state.getBlock());
            if (isAir || stateSwap){

                log("Block broken (expected path). Setting back to a regenerating block.");
                level.getServer().execute(() -> {

                    BlockState restored = state
                            .setValue(REGENERATING, true)
                            .setValue(DESTROYEDBYPLAYER, false);

                    super.onRemove(state, level, pos, newState, isMoving);
                    level.levelEvent(2001, pos, Block.getId(state));
                    level.setBlock(pos, restored, 3);
                });
                return;
            }

            boolean disableTransitions = ConfigManager.getSettings().disableTransitions();
            if (disableTransitions && !isAir) {
                log("Obeyed config setting. Prevented block transitioning to " + newState.getBlock().getName().toString());
                level.getServer().execute(() -> {
                    level.levelEvent(2001, pos, Block.getId(state));
                    level.setBlock(pos, state, 3);
                });
                return;
            }

        }

        log("(Probably bug) No intentional handling path defined, using default.");
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {

        if (state.getValue(REGENERATING)) {
            return new SoundType(0.0f, 1.0f,
                    SoundEvents.EMPTY, // Break sound
                    SoundEvents.EMPTY, // Step sound
                    SoundEvents.EMPTY, // Place sound
                    SoundEvents.EMPTY, // Hit sound
                    SoundEvents.EMPTY  // Fall sound
            );
        }

        // Otherwise, default handling
        return super.getSoundType(state, level, pos, entity);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {

        if (!level.isClientSide && state.getValue(REGENERATING)) {
            log("Regenerating block placed. Scheduling regeneration tick.");
            level.scheduleTick(pos, this, block.regenAfter * 20);
        }

        // Otherwise, default handling
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {

        if (state.getValue(REGENERATING)) {
            log("Harvest prevented because block is currently regenerating.");
            return false;
        }

        // Otherwise, delegate to actual source block harvesting behavior
        return this.block.GetSourceBlock().canHarvestBlock(this.block.GetSourceBlock().defaultBlockState(), world, pos, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {

        if (state.getValue(REGENERATING)) {
            log("Drops prevented because blocks currently regenerating drop nothing if somehow broken.");
            return Collections.emptyList();
        }

        // Otherwise, default handling
        return super.getDrops(state, params);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {

        if (state.getValue(REGENERATING)) {
            return ConfigManager.getSettings().getRegeneratingBlockLightEmission();
        }

        // Otherwise, default handling
        return super.getLightEmission(state, level, pos);
    }

    private void spawnSubtleEffect(ServerLevel level, BlockPos pos) {

        // Disable particles if user has opted out via config setting
        if (!ConfigManager.getSettings().showParticles()) { return; }

        double offset = 0.25;
        level.sendParticles(ParticleTypes.SCRAPE,
                pos.getX() -0.2, pos.getY() + 0.5, pos.getZ()+0.5,
                5, offset, offset, offset, 0.1);
        level.sendParticles(ParticleTypes.SCRAPE,
                pos.getX()  +1.2, pos.getY() + 0.5, pos.getZ()+0.5,
                5, offset, offset, offset, 0.1);
        level.sendParticles(ParticleTypes.SCRAPE,
                pos.getX() +0.5, pos.getY() + 0.5, pos.getZ() -0.2,
                5, offset, offset, offset, 0.1);
        level.sendParticles(ParticleTypes.SCRAPE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() +1.2,
                5, offset, offset, offset, 0.1);
    }

    private void log(String s){
        if (!ConfigManager.getSettings().verboseLogging()){ return; }

        String callerName = StackWalker.getInstance()
                .walk(frames -> frames
                        .skip(1) // Skip the current method
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse("unknown"));

        System.out.println("[Regenerating Ores][" + callerName +"] " + s);
    }
}

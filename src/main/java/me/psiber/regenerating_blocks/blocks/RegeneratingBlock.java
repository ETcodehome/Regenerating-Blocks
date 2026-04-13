package me.psiber.regenerating_blocks.blocks;

import me.psiber.regenerating_blocks.ConfigManager;
import me.psiber.regenerating_blocks.RegenManager;
import me.psiber.regenerating_blocks.Regenerable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class RegeneratingBlock extends Block {

    public Regenerable block;
    public int regenTicks;

    public RegeneratingBlock(Regenerable block, int regenAfter) {
        super(prepare(block.GetSourceBlock()));
        this.block = block;
        this.regenTicks = regenAfter * 20;
        this.registerDefaultState(this.stateDefinition.any());
    }

    private static BlockBehaviour.Properties prepare(Block source) {
        return BlockBehaviour.Properties.ofFullCopy(source)
                .dropsLike(source)
                .lightLevel(state -> 0);
    }

    // Called when a player successfully finishes breaking a block, but before the block is actually replaced with air.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {

        boolean regenerating = RegenManager.isRegenerating(pos);
        boolean canDestroyRegeneratingBlock = player.isCreative();
        if (canDestroyRegeneratingBlock && regenerating){
            log("Allowed destruction of regenerating block because player is in creative gamemode.");
            RegenManager.getData(pos).creativeBreak = true;
            return super.playerWillDestroy(level, pos, state, player);
        }

        if (!level.isClientSide && !regenerating)
        {
            log("Allowed destruction of fully regenerated block");
            level.setBlock(pos, state, 3 | 16);
            int uniqueBreakerId = pos.hashCode() + 5000;
            level.destroyBlockProgress(uniqueBreakerId, pos, 9);
            RegenManager.cacheBreakData(level, pos, true);

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
                        state,
                        serverLevel,
                        pos,
                        level.getBlockEntity(pos),
                        player,
                        tool
                );

                // Spawn the experience if the check passes
                boolean usingSilk = tool.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH)) > 0;
                if (xp > 0 && !usingSilk) {
                    this.popExperience(serverLevel, pos, xp);
                }

            }
        }

        return state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        int uniqueBreakerId = pos.hashCode() + 5000;
        RegenManager.RegenData breakData = RegenManager.getData(pos);

        if (breakData == null) {
            log("No data found (reboot or out-of-sync). Healing instantly.");
            level.destroyBlockProgress(uniqueBreakerId, pos, -1);
            return;
        }

        long elapsed = level.getGameTime() - breakData.startTime;
        long totalDuration = (long) this.regenTicks;

        if (elapsed >= totalDuration) {
            log("Regen cycle complete.");
            level.destroyBlockProgress(uniqueBreakerId, pos, -1);
            RegenManager.clearDataAt(pos);
            this.spawnSubtleEffect(level, pos);
            return;
        }

        float progress = (float) elapsed / (float) totalDuration;
        int currentVisualStage = 9 - (int) (progress * 10);
        if (currentVisualStage != breakData.lastVisualStage) {
            breakData.lastVisualStage = currentVisualStage;
            level.getServer().execute(() -> {
                level.levelEvent(2005, pos, 0);
            });
        }

        // Since we are counting down 9 -> 0, stage 8 triggers at 10% progress, stage 7 at 20%, etc.
        float nextProgressThreshold = (float) (10 - currentVisualStage) / 10.0f;
        long ticksUntilNextStage = (long) (nextProgressThreshold * totalDuration) - elapsed;

        int maxTimeWithoutUpdateInTicks = 380; // cracks disappear after 400 ticks
        int minimumTickStep = 1;
        long nextTickDelay = Math.max(minimumTickStep, Math.min(maxTimeWithoutUpdateInTicks, ticksUntilNextStage));

        level.getServer().execute(()-> {
            level.destroyBlockProgress(uniqueBreakerId, pos, currentVisualStage);
            log("Regen cycle wasn't completed."
                    + " | Elapsed: " + elapsed
                    + " | Total: " + totalDuration
                    + " | Stage: " + currentVisualStage
                    + " | TickDelay: " + nextTickDelay
            );
            level.scheduleTick(pos, this, (int) nextTickDelay);
        });
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

        if (RegenManager.isRegenerating(pos)){
            log("Provided bedrock values because block should be unbreakable");
            return 3600000.0F;
        }

        // Otherwise, default handling
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter world, BlockPos pos) {

        // never break a block while it is regenerating
        if (RegenManager.isRegenerating(pos)){
            log("Progress 0.0 because block is regenerating");
            return 0.0F;
        }

        // Delegate to the source block. That way we respect all the source blocks respective tags.
        return this.block.GetSourceBlock().defaultBlockState().getDestroyProgress(player, world, pos);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.Entity entity) {

        // If it's regenerating, no entity (including contraptions) can destroy it.
        if (RegenManager.isRegenerating(pos)){
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

        boolean regenerating = RegenManager.isRegenerating(pos);

        if (regenerating){
            RegenManager.RegenData breakData = RegenManager.getData(pos);

            if (breakData.creativeBreak) {
                log("Block broken by player in creative. Allowed permanent destruction.");
                RegenManager.clearDataAt(pos);
                int uniqueBreakerId = pos.hashCode() + 5000;
                level.destroyBlockProgress(uniqueBreakerId, pos, -1);
                super.onRemove(state, level, pos, newState, isMoving);
                return;
            }

        }

        boolean isAir = newState.isAir();
        boolean stateSwap = newState.is(state.getBlock());
        if ((isAir || stateSwap) && !regenerating){
            log("Block broken by non-player. Setting back to a regenerating block.");
            //super.onRemove(state, level, pos, newState, isMoving);
            makeRegenerating(level, pos, state);
            return;
        }

        // Handles almost all conversions, moss, sculk spread, burning to ash etc
        boolean disableTransitions = ConfigManager.getSettings().disableTransitions();
        if (disableTransitions && !isAir) {
            log("Obeyed config setting. Prevented block transitioning to " + newState.getBlock().getName().toString());
            resetBlockToState(level, pos, state);
            return;
        }

        if (!isAir && !disableTransitions) {
            log("Obeyed config setting. Allowed block to transition to " + newState.getBlock().getName().toString());
            // allow the transition
            return;
        }

        // Probably a direct removal
        // We prevent the block being removed from the world by reinstating the state
        log("Prevented permanent block destruction by restoring to previous state");
        resetBlockToState(level, pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {

        log("Drops table consulted");

        Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin == null) {
            log("Checking a drop table without a position. Drops nothing always.");
            return Collections.emptyList();
        }

        BlockPos pos = BlockPos.containing(origin);
        boolean regenerating = RegenManager.isRegenerating(pos);
        if (regenerating) {

            if (RegenManager.getData(pos).playerBreak){
                log("Returned default drops from player break");
                return super.getDrops(state, params);
            }

            log("Prevented drops from non-player break (block regenerating)");
            return Collections.emptyList();
        }

        log("returned default drops");
        return super.getDrops(state, params);
    }


    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {

        if (RegenManager.isRegenerating(pos)) {
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

        if (!level.isClientSide && RegenManager.isRegenerating(pos)) {
            log("Scheduling regeneration tick.");
            level.scheduleTick(pos, this, 1);
        }

        // Otherwise, default handling
        super.onPlace(state, level, pos, oldState, isMoving);
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

    @Override
    public MutableComponent getName() {
        return Component.translatable("text.regenerating_blocks.format",
                Component.translatable(this.block.GetSourceBlock().getDescriptionId()));
    }

    public void resetBlockToState(ServerLevel level, BlockPos pos, BlockState state) {

        level.levelEvent(2001, pos, Block.getId(state));

        level.getServer().execute(() -> {
            level.setBlock(pos, state, 3);
        });
    }
    public void resetBlockToState(Level level, BlockPos pos, BlockState state){
        if (level instanceof ServerLevel serverLevel) {
            resetBlockToState(serverLevel, pos, state);
        }
    }

    public void makeRegenerating(ServerLevel level, BlockPos pos, BlockState state) {

        // shows a block break effect to show the set occurred
        level.levelEvent(2001, pos, Block.getId(state));

        RegenManager.cacheBreakData(level, pos, false);

        level.getServer().execute(() -> {
            level.setBlock(pos, state, 3);
        });
    }
    public void makeRegenerating(Level level, BlockPos pos, BlockState state){
        if (level instanceof ServerLevel serverLevel) {
            makeRegenerating(serverLevel, pos, state);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        Block source = this.block.GetSourceBlock();
        source.stepOn(level, pos, source.defaultBlockState(), entity);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return this.block.GetSourceBlock().defaultBlockState().isSignalSource();
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // We check the source's default state for power output
        return this.block.GetSourceBlock().defaultBlockState().getSignal(level, pos, direction);
    }


    public static void log(String s){
        if (!ConfigManager.getSettings().verboseLogging()){ return; }

        String callerName = StackWalker.getInstance()
                .walk(frames -> frames
                        .skip(1) // Skip the current method
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse("unknown"));

        System.out.println("[Regenerating Blocks][" + callerName +"] " + s);
    }
}

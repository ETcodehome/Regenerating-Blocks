package me.jesuismister.regenerating_ores.blocks;

import me.jesuismister.regenerating_ores.Regenerable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class RegeneratingOreBlock extends Block {
    // Propriété pour indiquer si le bloc est en régénération
    public static final BooleanProperty REGENERATING = BooleanProperty.create("regenerating");
    public Regenerable block;
    // Temporary storage to bridge the gap during the super() call
    private static StateDefinition<Block, BlockState> capturedSourceDefinition;

    public RegeneratingOreBlock(Regenerable block) {
        super(prepare(block.sourceBlock));
        this.block = block;

        // Register the default state
        BlockState defaultState = this.stateDefinition.any().setValue(REGENERATING, false);

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

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {
        if (!level.isClientSide && !state.getValue(REGENERATING))
        {
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

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!state.getValue(REGENERATING)) {
            super.destroy(level, pos, state);
            
            // Change l'état du bloc en régénération
            level.setBlock(pos, this.defaultBlockState().setValue(REGENERATING, true), 3);

            // Planifie le retour à l'état normal après X secondes
            int ticksPerSecond = 20;
            level.scheduleTick(pos, this, block.regenAfter * ticksPerSecond);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, net.minecraft.util.RandomSource random) {
        // Après le délai, retourne à l'état normal
        if (state.getValue(REGENERATING)) {
            world.setBlock(pos, this.defaultBlockState().setValue(REGENERATING, false), 3);
        }
    }

    @Override
    public float getDestroyProgress(BlockState state, net.minecraft.world.entity.player.Player player, net.minecraft.world.level.BlockGetter world, BlockPos pos) {

        // never break a block while it is regenerating
        if (state.getValue(REGENERATING)){
            return 0.0F;
        }

        // Delegate to the source block. That way we respect all the source blocks respective tags.
        BlockState sourceState = this.block.sourceBlock.defaultBlockState();
        return sourceState.getDestroyProgress(player, world, pos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        // Delegate to the source block's harvesting logic
        return this.block.sourceBlock.canHarvestBlock(this.block.sourceBlock.defaultBlockState(), world, pos, player);
    }

}

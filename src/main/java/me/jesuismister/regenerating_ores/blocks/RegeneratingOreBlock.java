package me.jesuismister.regenerating_ores.blocks;

import me.jesuismister.regenerating_ores.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class RegeneratingOreBlock extends Block {
    // Propriété pour indiquer si le bloc est en régénération
    public static final BooleanProperty REGENERATING = BooleanProperty.create("regenerating");

    public RegeneratingOreBlock() {
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(3.0f, 3.0f));
        // Définit l'état initial du bloc
        this.registerDefaultState(this.defaultBlockState().setValue(REGENERATING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REGENERATING); // Ajoute la propriété "regenerating"
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Lorsqu'il est placé, le bloc n'est pas en régénération
        return this.defaultBlockState().setValue(REGENERATING, false);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!state.getValue(REGENERATING)) {
            // Change l'état du bloc en régénération
            level.setBlock(pos, this.defaultBlockState().setValue(REGENERATING, true), 3);

            // Planifie le retour à l'état normal après X secondes
            // TODO - Fix this back up using block state lookup
            level.scheduleTick(pos, this, 200);
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
        // Rend le bloc impossible à miner lorsqu'il est en régénération
        return state.getValue(REGENERATING) ? 0.0f : super.getDestroyProgress(state, player, world, pos);
    }
}

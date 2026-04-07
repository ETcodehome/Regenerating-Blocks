package me.jesuismister.regenerating_ores.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public class RegeneratingOreBlock extends Block {
    // Propriété pour indiquer si le bloc est en régénération
    public static final BooleanProperty REGENERATING = BooleanProperty.create("regenerating");

    public RegeneratingOreBlock() {
        // we explicitly do not require correct tool for drops here since this breaks the custom destroy progress check.
        // the custom destroy progress check is needed to ensure a regenerating block isn't broken
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                .strength(3.0f)
                .explosionResistance(3.0f)
        );
        // Définit l'état initial du bloc
        this.registerDefaultState(this.defaultBlockState()
                .setValue(REGENERATING, false));
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

        // never break a block while it is regenerating
        if (state.getValue(REGENERATING)){
            return 0.0F;
        }

        // run the default mining logic
        return super.getDestroyProgress(state, player, world, pos);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
    {
        // This avoids the need to define loot tables per block.
        // We just roll whatever loot table the original block the regenerating black was copied from uses.

        ServerLevel level = params.getLevel();
        String fullBlockName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        ResourceLocation originalMaterial = ResourceLocation.parse(ModBlocks.blockMap.get(fullBlockName));
        Block block = BuiltInRegistries.BLOCK.get(originalMaterial);
        BlockState targetState = block.defaultBlockState();
        ResourceKey<LootTable> resourcekey = targetState.getBlock().getLootTable();
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(resourcekey);

        // Generate the drops using the provided context parameters
        // This respects things like fortune and silk touch if they are in the 'params'
        return lootTable.getRandomItems(params.withParameter(LootContextParams.BLOCK_STATE, targetState).create(LootContextParamSets.BLOCK));
    }
}

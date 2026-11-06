package dev.windblown.client;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.SmallDripleafBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.TwistingVinesBlock;
import net.minecraft.world.level.block.TwistingVinesPlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WeepingVinesBlock;
import net.minecraft.world.level.block.WeepingVinesPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class Plants {
    private Plants() {
    }

    public static int info(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock) {
            return Foliage.info(Foliage.LEAVES, Foliage.FULL);
        }
        if (block instanceof FlowerBedBlock) {
            return Foliage.info(Foliage.GRASS, Foliage.FLAT);
        }
        if (block instanceof LeafLitterBlock) {
            return Foliage.info(Foliage.LEAVES, Foliage.FLAT);
        }
        if (block instanceof MushroomBlock || block instanceof SmallDripleafBlock) {
            return Foliage.NONE;
        }
        if (block instanceof KelpBlock || block instanceof KelpPlantBlock) {
            return Foliage.info(Foliage.WATER, Foliage.STACK_UP);
        }
        if (block instanceof TallSeagrassBlock) {
            return Foliage.info(Foliage.WATER, half(state));
        }
        if (block instanceof SeagrassBlock) {
            return Foliage.info(Foliage.WATER, Foliage.SINGLE);
        }
        if (block instanceof TwistingVinesBlock || block instanceof TwistingVinesPlantBlock) {
            return Foliage.info(Foliage.VINES, Foliage.STACK_UP);
        }
        if (block instanceof WeepingVinesBlock || block instanceof WeepingVinesPlantBlock
                || block instanceof CaveVinesBlock || block instanceof CaveVinesPlantBlock || block instanceof GrowingPlantBlock) {
            return Foliage.info(Foliage.VINES, Foliage.STACK_DOWN);
        }
        if (block instanceof PitcherCropBlock) {
            return Foliage.info(Foliage.CROPS, half(state));
        }
        if (block instanceof DoublePlantBlock) {
            return Foliage.info(Foliage.GRASS, half(state));
        }
        if (block instanceof CropBlock || block instanceof StemBlock || block instanceof NetherWartBlock) {
            return Foliage.info(Foliage.CROPS, Foliage.SINGLE);
        }
        if (block instanceof MangrovePropaguleBlock && state.getValue(MangrovePropaguleBlock.HANGING)) {
            return Foliage.info(Foliage.VINES, Foliage.HANG);
        }
        if (flowerBed(state)) {
            return Foliage.info(Foliage.GRASS, Foliage.FLAT);
        }
        if (block instanceof VegetationBlock) {
            return Foliage.info(Foliage.GRASS, Foliage.SINGLE);
        }
        if (block instanceof VineBlock) {
            return Foliage.info(Foliage.VINES, Foliage.WALL);
        }
        if (block instanceof HangingRootsBlock || block instanceof HangingMossBlock) {
            return Foliage.info(Foliage.VINES, Foliage.HANG);
        }
        return Foliage.NONE;
    }

    // Modded multi-flower blocks (Sweety's Garden: anemones, hepatica, wood sorrel...) built like pink petals.
    private static boolean flowerBed(BlockState state) {
        for (net.minecraft.world.level.block.state.properties.Property<?> p : state.getProperties()) {
            if (p.getName().equals("flower_amount")) {
                return true;
            }
        }
        return false;
    }

    private static int half(BlockState state) {
        return state.hasProperty(DoublePlantBlock.HALF) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER
                ? Foliage.UPPER : Foliage.LOWER;
    }
}

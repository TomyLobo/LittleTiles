package com.creativemd.littletiles.common.action.tool;

import com.creativemd.littletiles.common.action.LittleAction;
import com.creativemd.littletiles.common.action.LittleActionException;
import com.creativemd.littletiles.common.action.LittleActionInteract;
import com.creativemd.littletiles.common.action.block.LittleActionDestroyBoxes;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.tiles.LittleTile;
import com.creativemd.littletiles.common.tiles.preview.LittleTilePreview;
import com.creativemd.littletiles.common.tiles.vec.LittleBoxes;
import com.creativemd.littletiles.common.tiles.vec.LittleTileBox;
import com.creativemd.littletiles.common.tiles.vec.LittleTileIdentifierAbsolute;
import com.creativemd.littletiles.common.utils.grid.LittleGridContext;
import com.creativemd.littletiles.common.utils.ingredients.BlockIngredient;
import com.creativemd.littletiles.common.utils.ingredients.BlockIngredientEntry;
import com.creativemd.littletiles.common.utils.ingredients.ColorIngredient;
import com.creativemd.littletiles.common.utils.ingredients.LittleIngredients;
import com.creativemd.littletiles.common.utils.ingredients.LittleInventory;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class LittleActionSaw extends LittleActionInteract {
	
	public boolean toLimit;
	public LittleGridContext context;
	
	public LittleActionSaw(World world, BlockPos blockPos, EntityPlayer player, boolean toLimit, LittleGridContext context) {
		super(world, blockPos, player);
		this.toLimit = toLimit;
		this.context = context;
	}
	
	public LittleActionSaw() {
		super();
	}
	
	@Override
	protected boolean isRightClick() {
		return true;
	}
	
	public LittleTileBox oldBox = null;
	public LittleBoxes newBoxes;
	public LittleTileIdentifierAbsolute coord = null;
	public EnumFacing facing;
	
	@Override
	public RayTraceResult rayTrace(TileEntityLittleTiles te, LittleTile tile) {
		return new LittleTileBox(tile.box).calculateIntercept(te.getContext(), te.getPos(), pos, look);
	}
	
	@Override
	protected boolean action(World world, TileEntityLittleTiles te, LittleTile tile, ItemStack stack, EntityPlayer player, RayTraceResult moving, BlockPos pos, boolean secondMode) throws LittleActionException {
		
		facing = moving.sideHit;
		if (tile.canSawResizeTile(facing, player)) {
			LittleTileBox box;
			
			Axis axis = facing.getAxis();
			
			te.ensureMinContext(context);
			if (context != te.getContext())
				context = te.getContext();
			
			oldBox = tile.box.copy();
			
			boolean outside = false;
			
			if (secondMode)
				box = tile.box.shrink(facing, toLimit);
			else {
				box = tile.box.grow(facing);
				if (tile.box.isFaceAtEdge(te.getContext(), facing)) {
					BlockPos newPos = te.getPos().offset(facing);
					
					box = box.createOutsideBlockBox(te.getContext(), facing);
					if (box == null)
						return false;
					
					LittleGridContext context = LittleGridContext.get(box.getSmallestContext(te.getContext()));
					te = loadTe(player, world, newPos, false);
					
					if (te == null)
						return false;
					
					if (context != te.getContext()) {
						if (context.size > te.getContext().size)
							te.ensureMinContext(context);
						else
							box.convertTo(context, te.getContext());
					}
					
					outside = true;
				}
				
				if (toLimit) {
					LittleTileBox before = null;
					while (!box.isFaceAtEdge(te.getContext(), facing) && te.isSpaceForLittleTileIgnore(box, tile)) {
						before = box;
						box = box.grow(facing);
					}
					if (!te.isSpaceForLittleTileIgnore(box, tile))
						box = before;
				} else if (!te.isSpaceForLittleTileIgnore(box, tile))
					box = null;
			}
			
			if (box != null) {
				double amount = Math.abs(box.getPercentVolume(te.getContext()) - tile.box.getPercentVolume(te.getContext()));
				LittleIngredients ingredients = new LittleIngredients();
				LittleInventory inventory = new LittleInventory(player);
				BlockIngredient blocks = new BlockIngredient();
				LittleTilePreview preview = tile.getPreviewTile();
				BlockIngredientEntry block = preview.getBlockIngredient(te.getContext());
				if (block != null) {
					block.value = amount;
					blocks.add(block);
					ingredients.set(blocks.getClass(), blocks);
					
					ColorIngredient unit = null;
					if (preview.hasColor()) {
						unit = ColorIngredient.getColors(preview.getColor());
						unit.scaleLoose(amount);
						ingredients.set(unit.getClass(), unit);
					}
					
					if (secondMode)
						give(player, inventory, ingredients);
					else
						take(player, inventory, ingredients);
					
				}
				
				if (outside) {
					LittleTile newTile = tile.copy();
					newTile.box = box;
					newTile.te = te;
					te.updateTiles((x) -> newTile.place(x));
					te.updateBlock();
					newBoxes = new LittleBoxes(te.getPos(), te.getContext());
					newBoxes.addBox(newTile);
					te.convertToSmallest();
					return true;
				} else {
					tile.box = box;
					te.updateBlock();
					coord = new LittleTileIdentifierAbsolute(tile);
					te.convertToSmallest();
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean canBeReverted() {
		return true;
	}
	
	@Override
	public LittleAction revert() {
		if (newBoxes != null)
			return new LittleActionDestroyBoxes(newBoxes);
		return new LittleActionSawRevert(context, coord, oldBox, facing);
	}
	
	@Override
	public void writeBytes(ByteBuf buf) {
		super.writeBytes(buf);
		buf.writeBoolean(toLimit);
		writeContext(context, buf);
	}
	
	@Override
	public void readBytes(ByteBuf buf) {
		super.readBytes(buf);
		toLimit = buf.readBoolean();
		context = readContext(buf);
	}
	
	public static class LittleActionSawRevert extends LittleAction {
		
		public LittleTileBox oldBox;
		public LittleTileBox replacedBox;
		public LittleTileIdentifierAbsolute coord;
		public LittleTileIdentifierAbsolute newCoord;
		public EnumFacing facing;
		public LittleGridContext context;
		
		public LittleActionSawRevert(LittleGridContext context, LittleTileIdentifierAbsolute coord, LittleTileBox oldBox, EnumFacing facing) {
			this.coord = coord;
			this.oldBox = oldBox;
			this.facing = facing;
			this.context = context;
		}
		
		public LittleActionSawRevert() {
			
		}
		
		@Override
		public boolean canBeReverted() {
			return true;
		}
		
		@Override
		public LittleAction revert() throws LittleActionException {
			return new LittleActionSawRevert(context, newCoord, replacedBox, facing.getOpposite());
		}
		
		@Override
		protected boolean action(EntityPlayer player) throws LittleActionException {
			
			LittleTile tile = getTile(player.world, coord);
			
			if (tile.canSawResizeTile(facing, player)) {
				tile.te.ensureMinContext(context);
				
				if (tile.getContext() != context) {
					oldBox.convertTo(context, tile.getContext());
					context = tile.getContext();
				}
				
				double amount = Math.abs(oldBox.getPercentVolume(context) - tile.box.getPercentVolume(tile.getContext()));
				
				LittleTilePreview preview = tile.getPreviewTile();
				LittleIngredients ingredients = new LittleIngredients();
				BlockIngredient blocks = new BlockIngredient();
				BlockIngredientEntry block = preview.getBlockIngredient(tile.getContext());
				if (block != null) {
					LittleInventory inventory = new LittleInventory(player);
					block.value = amount;
					blocks.add(block);
					ingredients.set(blocks.getClass(), blocks);
					
					ColorIngredient unit = null;
					if (preview.hasColor()) {
						unit = ColorIngredient.getColors(preview.getColor());
						unit.scaleLoose(amount);
						ingredients.set(unit.getClass(), unit);
					}
					
					if (oldBox.getVolume() < tile.box.getVolume())
						give(player, inventory, ingredients);
					else
						take(player, inventory, ingredients);
				}
				
				replacedBox = tile.box.copy();
				// replacedBox.addOffset(tile.te.getPos());
				tile.box = oldBox.copy();
				
				tile.te.convertToSmallest();
				tile.te.updateBlock();
				
				newCoord = new LittleTileIdentifierAbsolute(tile);
				return true;
			}
			
			return false;
		}
		
		@Override
		public void writeBytes(ByteBuf buf) {
			writeAbsoluteCoord(coord, buf);
			writeLittleBox(oldBox, buf);
			writeFacing(buf, facing);
			writeContext(context, buf);
		}
		
		@Override
		public void readBytes(ByteBuf buf) {
			coord = readAbsoluteCoord(buf);
			oldBox = readLittleBox(buf);
			facing = readFacing(buf);
			context = readContext(buf);
		}
	}
}

package com.creativemd.littletiles.common.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.utils.math.Rotation;
import com.creativemd.creativecore.common.utils.mc.WorldUtils;
import com.creativemd.creativecore.common.utils.type.HashMapList;
import com.creativemd.creativecore.gui.container.GuiParent;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.action.block.LittleActionActivated;
import com.creativemd.littletiles.common.structure.attribute.LittleStructureAttribute;
import com.creativemd.littletiles.common.structure.connection.StructureLink;
import com.creativemd.littletiles.common.structure.connection.StructureLinkTile;
import com.creativemd.littletiles.common.structure.connection.StructureMainTile;
import com.creativemd.littletiles.common.structure.premade.LittleStructurePremade;
import com.creativemd.littletiles.common.structure.type.LittleBed;
import com.creativemd.littletiles.common.structure.type.LittleBed.LittleBedParser;
import com.creativemd.littletiles.common.structure.type.LittleChair;
import com.creativemd.littletiles.common.structure.type.LittleChair.LittleChairParser;
import com.creativemd.littletiles.common.structure.type.LittleDoor;
import com.creativemd.littletiles.common.structure.type.LittleDoor.LittleDoorParser;
import com.creativemd.littletiles.common.structure.type.LittleFixedStructure;
import com.creativemd.littletiles.common.structure.type.LittleFixedStructure.LittleFixedStructureParser;
import com.creativemd.littletiles.common.structure.type.LittleLadder;
import com.creativemd.littletiles.common.structure.type.LittleLadder.LittleLadderParser;
import com.creativemd.littletiles.common.structure.type.LittleNoClipStructure;
import com.creativemd.littletiles.common.structure.type.LittleNoClipStructure.LittleNoClipStructureParser;
import com.creativemd.littletiles.common.structure.type.LittleSlidingDoor;
import com.creativemd.littletiles.common.structure.type.LittleSlidingDoor.LittleSlidingDoorParser;
import com.creativemd.littletiles.common.structure.type.LittleStorage;
import com.creativemd.littletiles.common.structure.type.LittleStorage.LittleStorageParser;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.tiles.LittleTile;
import com.creativemd.littletiles.common.tiles.LittleTile.LittleTilePosition;
import com.creativemd.littletiles.common.tiles.place.PlacePreviewTile;
import com.creativemd.littletiles.common.tiles.preview.LittlePreviews;
import com.creativemd.littletiles.common.tiles.preview.LittleTilePreview;
import com.creativemd.littletiles.common.tiles.vec.LittleTileBox;
import com.creativemd.littletiles.common.tiles.vec.LittleTileIdentifierRelative;
import com.creativemd.littletiles.common.tiles.vec.LittleTileIdentifierStructure;
import com.creativemd.littletiles.common.tiles.vec.LittleTilePos;
import com.creativemd.littletiles.common.tiles.vec.LittleTileVec;
import com.creativemd.littletiles.common.tiles.vec.RelativeBlockPos;
import com.creativemd.littletiles.common.utils.grid.LittleGridContext;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public abstract class LittleStructure {
	
	private static HashMap<String, LittleStructureEntry> structuresID = new LinkedHashMap<String, LittleStructureEntry>();
	private static HashMap<Class<? extends LittleStructure>, LittleStructureEntry> structuresClass = new LinkedHashMap<Class<? extends LittleStructure>, LittleStructureEntry>();
	
	private static List<String> cachedNames = new ArrayList<>();
	
	public static List<String> getStructureTypeNames() {
		return new ArrayList<>(cachedNames);
	}
	
	public static void registerStructureType(String id, Class<? extends LittleStructure> classStructure, LittleStructureAttribute attribute, Class<? extends LittleStructureParser> parser) {
		LittleStructureEntry entry = new LittleStructureEntry(id, classStructure, parser, attribute);
		registerStructureType(id, entry);
		if (attribute != LittleStructureAttribute.PREMADE)
			cachedNames.add(id);
	}
	
	private static void registerStructureType(String id, LittleStructureEntry entry) {
		if (structuresID.containsKey(id))
			throw new RuntimeException("ID is already taken! id=" + id);
		else if (structuresID.containsValue(entry))
			throw new RuntimeException("Already registered class=" + entry);
		else {
			structuresID.put(id, entry);
			structuresClass.put(entry.structureClass, entry);
		}
	}
	
	public static String getIDByClass(Class<? extends LittleStructure> classStructure) {
		LittleStructureEntry entry = structuresClass.get(classStructure);
		if (entry != null)
			return entry.id;
		return null;
	}
	
	public static Class<? extends LittleStructure> getClassByID(String id) {
		LittleStructureEntry entry = structuresID.get(id);
		if (entry != null)
			return entry.structureClass;
		return null;
	}
	
	public static LittleStructureEntry getStructureEntryByID(String id) {
		return structuresID.get(id);
	}
	
	public static LittleStructureEntry getStructureEntryByClass(Class<? extends LittleStructure> classStructure) {
		return structuresClass.get(classStructure);
	}
	
	public static void initStructures() {
		registerStructureType("fixed", LittleFixedStructure.class, LittleStructureAttribute.NONE, LittleFixedStructureParser.class);
		registerStructureType("chair", LittleChair.class, LittleStructureAttribute.NONE, LittleChairParser.class);
		registerStructureType("door", LittleDoor.class, LittleStructureAttribute.NONE, LittleDoorParser.class);
		registerStructureType("slidingDoor", LittleSlidingDoor.class, LittleStructureAttribute.NONE, LittleSlidingDoorParser.class);
		registerStructureType("ladder", LittleLadder.class, LittleStructureAttribute.LADDER, LittleLadderParser.class);
		registerStructureType("bed", LittleBed.class, LittleStructureAttribute.NONE, LittleBedParser.class);
		registerStructureType("storage", LittleStorage.class, LittleStructureAttribute.NONE, LittleStorageParser.class);
		registerStructureType("noclip", LittleNoClipStructure.class, LittleStructureAttribute.COLLISION, LittleNoClipStructureParser.class);
		
		LittleStructurePremade.initPremadeStructures();
	}
	
	public static LittleStructure createAndLoadStructure(NBTTagCompound nbt, @Nullable LittleTile mainTile) {
		if (nbt == null)
			return null;
		String id = nbt.getString("id");
		LittleStructureEntry entry = getStructureEntryByID(id);
		if (entry != null) {
			Class<? extends LittleStructure> classStructure = entry.structureClass;
			if (classStructure != null) {
				LittleStructure structure = null;
				try {
					structure = classStructure.getConstructor().newInstance();
				} catch (Exception e) {
					System.out.println("Found invalid structureID=" + id);
				}
				structure.mainTile = mainTile;
				structure.loadFromNBT(nbt);
				return structure;
			}
		}
		return null;
	}
	
	public final LittleStructureAttribute attribute;
	public final String structureID;
	
	public String name;
	
	public StructureLink parent;
	public HashMap<Integer, StructureLink> children;
	
	public LittleStructure() {
		LittleStructureEntry entry = getStructureEntryByClass(this.getClass());
		this.attribute = entry.attribute;
		this.structureID = entry.id;
	}
	
	public void placedStructure(ItemStack stack) {
		NBTTagCompound nbt;
		if (stack != null && (nbt = stack.getSubCompound("display")) != null && nbt.hasKey("Name", 8))
			name = nbt.getString("Name");
	}
	
	/**
	 * This will notify every client that the structure has changed
	 */
	public void updateStructure() {
		mainTile.te.updateBlock();
	}
	
	public void setMainTile(LittleTile tile) {
		this.mainTile = tile;
		
		this.mainTile.connection = new StructureMainTile(mainTile, this);
		updateStructure();
		
		if (!containsTile(tile))
			addTile(tile);
		
		for (Iterator<Entry<TileEntityLittleTiles, ArrayList<LittleTile>>> iterator = tiles.entrySet().iterator(); iterator.hasNext();) {
			Entry<TileEntityLittleTiles, ArrayList<LittleTile>> entry = iterator.next();
			entry.getKey().getWorld().markChunkDirty(entry.getKey().getPos(), entry.getKey());
			
			for (Iterator iterator2 = entry.getValue().iterator(); iterator2.hasNext();) {
				LittleTile stTile = (LittleTile) iterator2.next();
				
				if (stTile != mainTile) {
					stTile.connection = getStructureLink(stTile);
					stTile.connection.setLoadedStructure(this, attribute);
				}
			}
		}
		
	}
	
	public LittleTileIdentifierStructure getMainTileCoord(BlockPos pos) {
		return new LittleTileIdentifierStructure(pos, mainTile.te.getPos(), mainTile.getContext(), mainTile.getIdentifier(), attribute);
	}
	
	public StructureLinkTile getStructureLink(LittleTile tile) {
		return new StructureLinkTile(tile.te, mainTile.te.getPos(), mainTile.getContext(), mainTile.getIdentifier(), attribute, tile);
	}
	
	public boolean hasMainTile() {
		return mainTile != null;
	}
	
	public void moveStructure(EnumFacing facing) {
		
	}
	
	public void combineTiles() {
		if (!hasLoaded())
			return;
		
		BlockPos pos = null;
		
		for (Iterator<TileEntityLittleTiles> iterator = tiles.keySet().iterator(); iterator.hasNext();) {
			iterator.next().combineTiles(this);
		}
	}
	
	public void selectMainTile() {
		if (hasLoaded()) {
			LittleTile first = tiles.getFirst();
			if (first != null)
				setMainTile(first);
		}
	}
	
	private LittleTile mainTile;
	
	/** The core of the structure. Handles saving & loading of the structures. All Tiles inside the structure are containing relative coordinates to this tile **/
	public LittleTile getMainTile() {
		return mainTile;
	}
	
	protected HashMapList<TileEntityLittleTiles, LittleTile> tiles = null;
	
	public void setTiles(HashMapList<TileEntityLittleTiles, LittleTile> tiles) {
		this.tiles = tiles;
	}
	
	public boolean LoadList() {
		if (tiles == null)
			return loadTiles();
		return true;
	}
	
	public boolean containsTile(LittleTile tile) {
		return tiles.contains(tile.te, tile);
	}
	
	public HashMapList<TileEntityLittleTiles, LittleTile> copyOfTiles() {
		if (tiles == null)
			if (!loadTiles())
				return new HashMapList<>();
		return new HashMapList<>(tiles);
	}
	
	public Iterator<LittleTile> getTiles() {
		if (tiles == null)
			if (!loadTiles())
				return new Iterator<LittleTile>() {
					
					@Override
					public boolean hasNext() {
						return false;
					}
					
					@Override
					public LittleTile next() {
						return null;
					}
					
				};
			
		return tiles.iterator();
	}
	
	public Set<Entry<TileEntityLittleTiles, ArrayList<LittleTile>>> entrySet() {
		if (tiles == null)
			if (!loadTiles())
				return Collections.EMPTY_SET;
		return tiles.entrySet();
	}
	
	public void removeTile(LittleTile tile) {
		if (tiles != null)
			tiles.removeValue(tile.te, tile);
	}
	
	public void addTile(LittleTile tile) {
		tiles.add(tile.te, tile);
	}
	
	public boolean hasLoaded() {
		loadTiles();
		return mainTile != null && tiles != null && (tilesToLoad == null || tilesToLoad.size() == 0);
	}
	
	public boolean loadTiles() {
		if (mainTile != null) {
			if (tiles == null) {
				tiles = new HashMapList<>();
				addTile(mainTile);
			}
			
			if (tilesToLoad == null)
				return true;
			
			for (Iterator<Entry<BlockPos, Integer>> iterator = tilesToLoad.entrySet().iterator(); iterator.hasNext();) {
				Entry<BlockPos, Integer> entry = iterator.next();
				if (checkForTiles(mainTile.te.getWorld(), entry.getKey(), entry.getValue()))
					iterator.remove();
			}
			
			if (!tiles.contains(mainTile))
				addTile(mainTile);
			
			if (tilesToLoad.size() == 0)
				tilesToLoad = null;
			return true;
		}
		return false;
	}
	
	public HashMap<BlockPos, Integer> tilesToLoad = null;
	
	public void loadStructure(LittleTile mainTile) {
		this.mainTile = mainTile;
		this.mainTile.connection = new StructureMainTile(mainTile, this);
		
		if (tiles != null && !containsTile(mainTile))
			addTile(mainTile);
	}
	
	public void loadFromNBT(NBTTagCompound nbt) {
		if (tiles != null)
			tiles = null;
		
		tilesToLoad = new HashMap<>();
		
		//LoadTiles
		if (nbt.hasKey("count")) //Old way
		{
			int count = nbt.getInteger("count");
			for (int i = 0; i < count; i++) {
				LittleTileIdentifierRelative coord = null;
				if (nbt.hasKey("i" + i + "coX")) {
					LittleTilePosition pos = new LittleTilePosition("i" + i, nbt);
					coord = new LittleTileIdentifierRelative(mainTile.te, pos.coord, LittleGridContext.get(), new int[] { pos.position.x, pos.position.y, pos.position.z });
				} else {
					coord = LittleTileIdentifierRelative.loadIdentifierOld("i" + i, nbt);
				}
				
				BlockPos pos = coord.getAbsolutePosition(mainTile.te);
				Integer insideBlock = tilesToLoad.get(pos);
				if (insideBlock == null)
					insideBlock = new Integer(1);
				else
					insideBlock = insideBlock + 1;
				tilesToLoad.put(pos, insideBlock);
			}
			
		} else if (nbt.hasKey("tiles")) { //new way
			NBTTagList list = nbt.getTagList("tiles", 11);
			for (int i = 0; i < list.tagCount(); i++) {
				int[] array = list.getIntArrayAt(i);
				if (array.length == 4) {
					RelativeBlockPos pos = new RelativeBlockPos(array);
					tilesToLoad.put(pos.getAbsolutePos(mainTile.te), array[3]);
				} else
					System.out.println("Found invalid array! " + nbt);
			}
		}
		
		if (nbt.hasKey("name"))
			name = nbt.getString("name");
		else
			name = null;
		
		// Load family (parent and children)		
		if (nbt.hasKey("parent", 10))
			parent = new StructureLink(nbt, this, true);
		else
			parent = null;
		
		if (nbt.hasKey("children", 10)) {
			children = new HashMap<>();
			NBTTagList list = nbt.getTagList("tiles", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				StructureLink child = new StructureLink(list.getCompoundTagAt(i), this, false);
				children.put(child.childID, child);
			}
		} else
			children = null;
		
		loadFromNBTExtra(nbt);
	}
	
	protected abstract void loadFromNBTExtra(NBTTagCompound nbt);
	
	public void writeToNBTPreview(NBTTagCompound nbt, BlockPos newCenter) {
		nbt.setString("id", getIDOfStructure());
		writeToNBTExtra(nbt);
	}
	
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setString("id", getIDOfStructure());
		if (name != null)
			nbt.setString("name", name);
		else
			nbt.removeTag("name");
		
		// Save family (parent and children)
		if (parent != null)
			nbt.setTag("parent", parent.writeToNBT(new NBTTagCompound()));
		
		if (children != null && !children.isEmpty()) {
			NBTTagList list = new NBTTagList();
			for (StructureLink child : children.values()) {
				list.appendTag(child.writeToNBT(new NBTTagCompound()));
			}
			nbt.setTag("children", list);
		}
		
		// SaveTiles
		HashMap<BlockPos, Integer> positions = new HashMap<>();
		if (tiles != null) {
			for (Iterator<Entry<TileEntityLittleTiles, ArrayList<LittleTile>>> iterator = tiles.entrySet().iterator(); iterator.hasNext();) {
				Entry<TileEntityLittleTiles, ArrayList<LittleTile>> entry = iterator.next();
				if (entry.getValue().size() > 0)
					positions.put(entry.getKey().getPos(), entry.getValue().size());
			}
		}
		
		if (tilesToLoad != null)
			positions.putAll(tilesToLoad);
		
		if (positions.size() > 0) {
			NBTTagList list = new NBTTagList();
			for (Iterator<Entry<BlockPos, Integer>> iterator = positions.entrySet().iterator(); iterator.hasNext();) {
				Entry<BlockPos, Integer> entry = iterator.next();
				RelativeBlockPos pos = new RelativeBlockPos(mainTile.te, entry.getKey());
				list.appendTag(new NBTTagIntArray(new int[] { pos.getRelativePos().getX(), pos.getRelativePos().getY(), pos.getRelativePos().getZ(), entry.getValue() }));
			}
			nbt.setTag("tiles", list);
		}
		
		writeToNBTExtra(nbt);
	}
	
	protected abstract void writeToNBTExtra(NBTTagCompound nbt);
	
	public boolean doesLinkToMainTile(LittleTile tile) {
		try {
			return tile == getMainTile() || (tile.connection.isLink() && tile.connection.getStructurePosition().equals(mainTile.te.getPos()) && tile.connection.is(mainTile));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean checkForTiles(World world, BlockPos pos, Integer expectedCount) {
		Chunk chunk = world.getChunkFromBlockCoords(pos);
		if (WorldUtils.checkIfChunkExists(chunk)) {
			//chunk.isChunkLoaded
			TileEntity tileEntity = world.getTileEntity(pos);
			if (tileEntity instanceof TileEntityLittleTiles) {
				if (!((TileEntityLittleTiles) tileEntity).hasLoaded())
					return false;
				int found = 0;
				
				if (tiles.keySet().contains(tileEntity))
					tiles.removeKey((TileEntityLittleTiles) tileEntity);
				
				for (Iterator iterator = ((TileEntityLittleTiles) tileEntity).getTiles().iterator(); iterator.hasNext();) {
					LittleTile tile = (LittleTile) iterator.next();
					if (tile.isChildOfStructure() && (tile.connection.getStructureWithoutLoading() == this || doesLinkToMainTile(tile))) {
						tiles.add((TileEntityLittleTiles) tileEntity, tile);
						tile.connection.setLoadedStructure(this, attribute);
						found++;
					}
				}
				
				if (found == expectedCount)
					return true;
			}
		}
		return false;
	}
	
	// ====================Placing====================
	
	public boolean shouldPlaceTile(LittleTile tile) {
		return true;
	}
	
	public LittleGridContext getMinContext() {
		return LittleGridContext.getMin();
	}
	
	public ArrayList<PlacePreviewTile> getSpecialTiles(LittleGridContext context) {
		return new ArrayList<>();
	}
	
	//====================LittleTile-Stuff====================
	
	public void onLittleTileDestroy() {
		if (hasLoaded()) {
			for (Entry<TileEntityLittleTiles, ArrayList<LittleTile>> entry : tiles.entrySet()) {
				entry.getKey().removeTiles(entry.getValue());
			}
		}
	}
	
	public ItemStack getStructureDrop() {
		if (hasLoaded()) {
			BlockPos pos = getMainTile().te.getPos();
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			
			for (Iterator<TileEntityLittleTiles> iterator = tiles.keySet().iterator(); iterator.hasNext();) {
				TileEntityLittleTiles te = iterator.next();
				x = Math.min(x, te.getPos().getX());
				y = Math.min(y, te.getPos().getY());
				z = Math.min(z, te.getPos().getZ());
			}
			
			pos = new BlockPos(x, y, z);
			
			ItemStack stack = new ItemStack(LittleTiles.multiTiles);
			
			LittlePreviews previews = new LittlePreviews(LittleGridContext.getMin());
			
			for (Iterator<LittleTile> iterator = getTiles(); iterator.hasNext();) {
				LittleTile tile = iterator.next();
				LittleTilePreview preview = previews.addTile(tile);
				preview.box.addOffset(new LittleTileVec(previews.context, tile.te.getPos().subtract(pos)));
			}
			
			previews.convertToSmallest();
			
			LittleTilePreview.savePreviewTiles(previews, stack);
			
			NBTTagCompound structureNBT = new NBTTagCompound();
			
			this.writeToNBTPreview(structureNBT, pos);
			stack.getTagCompound().setTag("structure", structureNBT);
			
			if (name != null) {
				NBTTagCompound display = new NBTTagCompound();
				display.setString("Name", name);
				stack.getTagCompound().setTag("display", display);
			}
			return stack;
		}
		return ItemStack.EMPTY;
	}
	
	public boolean onBlockActivated(World worldIn, LittleTile tile, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ, LittleActionActivated action) {
		return false;
	}
	
	//====================SORTING====================
	
	// ====================SORTING====================
	
	public void onFlip(World world, EntityPlayer player, ItemStack stack, LittleGridContext context, Axis axis, LittleTileVec doubledCenter) {
	}
	
	public void onRotate(World world, EntityPlayer player, ItemStack stack, LittleGridContext context, Rotation rotation, LittleTileVec doubledCenter) {
	}
	
	//====================Helpers====================
	
	public Vec3d getHighestCenterVec() {
		if (tiles == null)
			return null;
		
		int minYPos = Integer.MAX_VALUE;
		
		long minX = Long.MAX_VALUE;
		long minY = Long.MAX_VALUE;
		long minZ = Long.MAX_VALUE;
		
		int maxYPos = Integer.MIN_VALUE;
		
		long maxX = Long.MIN_VALUE;
		long maxY = Long.MIN_VALUE;
		long maxZ = Long.MIN_VALUE;
		
		LittleGridContext context = LittleGridContext.getMin();
		boolean first = true;
		
		HashMap<BlockPos, TileEntityLittleTiles> map = new HashMap<>();
		
		for (Entry<TileEntityLittleTiles, ArrayList<LittleTile>> entry : tiles.entrySet()) {
			if (context.size < entry.getKey().getContext().size) {
				if (!first) {
					if (context.size > entry.getKey().getContext().size) {
						int modifier = context.size / entry.getKey().getContext().size;
						minX /= modifier;
						minY /= modifier;
						minZ /= modifier;
						maxX /= modifier;
						maxY /= modifier;
						maxZ /= modifier;
					} else {
						int modifier = entry.getKey().getContext().size / context.size;
						minX *= modifier;
						minY *= modifier;
						minZ *= modifier;
						maxX *= modifier;
						maxY *= modifier;
						maxZ *= modifier;
					}
				}
				context = entry.getKey().getContext();
			}
			
			first = false;
			
			for (LittleTile tile : entry.getValue()) {
				LittleTileBox box = tile.getCompleteBox();
				minX = Math.min(minX, entry.getKey().getPos().getX() * context.size + box.minX);
				minY = Math.min(minY, entry.getKey().getPos().getY() * context.size + box.minY);
				minZ = Math.min(minZ, entry.getKey().getPos().getZ() * context.size + box.minZ);
				
				maxX = Math.max(maxX, entry.getKey().getPos().getX() * context.size + box.maxX);
				maxY = Math.max(maxY, entry.getKey().getPos().getY() * context.size + box.maxY);
				maxZ = Math.max(maxZ, entry.getKey().getPos().getZ() * context.size + box.maxZ);
				
				minYPos = Math.min(minYPos, entry.getKey().getPos().getY());
				maxYPos = Math.max(maxYPos, entry.getKey().getPos().getY());
			}
			
			map.put(entry.getKey().getPos(), entry.getKey());
		}
		
		//double test = Math.floor(((minX+maxX)/LittleTile.gridSize/2D));
		int centerX = (int) Math.floor((minX + maxX) / (double) context.size / 2D);
		int centerY = (int) Math.floor((minY + maxY) / (double) context.size / 2D);
		int centerZ = (int) Math.floor((minZ + maxZ) / (double) context.size / 2D);
		
		int centerTileX = (int) (Math.floor(minX + maxX) / 2D) - centerX * context.size;
		int centerTileY = (int) (Math.floor(minY + maxY) / 2D) - centerY * context.size;
		int centerTileZ = (int) (Math.floor(minZ + maxZ) / 2D) - centerZ * context.size;
		
		LittleTilePos pos = new LittleTilePos(new BlockPos(centerX, minYPos, centerZ), context, new LittleTileVec(centerTileX, 0, centerTileZ));
		
		for (int y = minYPos; y <= maxYPos; y++) {
			TileEntityLittleTiles te = map.get(new BlockPos(centerX, y, centerZ));
			ArrayList<LittleTile> tilesInCenter = tiles.getValues(te);
			if (tilesInCenter != null) {
				te.convertTo(context);
				LittleTileBox box = new LittleTileBox(centerTileX, context.minPos, centerTileZ, centerTileX + 1, context.maxPos, centerTileZ + 1);
				if (context.size >= centerTileX) {
					box.minX = context.size - 1;
					box.maxX = context.size;
				}
				
				if (context.size >= centerTileZ) {
					box.minZ = context.size - 1;
					box.maxZ = context.size;
				}
				
				//int highest = LittleTile.minPos;
				for (int i = 0; i < tilesInCenter.size(); i++) {
					List<LittleTileBox> collision = tilesInCenter.get(i).getCollisionBoxes();
					for (int j = 0; j < collision.size(); j++) {
						LittleTileBox littleBox = collision.get(j);
						if (LittleTileBox.intersectsWith(box, littleBox)) {
							pos.contextVec.context = te.getContext();
							pos.contextVec.vec.y = Math.max((y - minYPos) * context.size + littleBox.maxY, pos.contextVec.vec.y);
						}
					}
				}
				te.convertToSmallest();
			}
		}
		
		return new Vec3d(context.toVanillaGrid((minX + maxX) / 2D), pos.getPosY(), context.toVanillaGrid((minZ + maxZ) / 2D));
	}
	
	public LittleTilePos getHighestCenterPoint() {
		if (tiles == null)
			return null;
		
		int minYPos = Integer.MAX_VALUE;
		
		long minX = Long.MAX_VALUE;
		long minY = Long.MAX_VALUE;
		long minZ = Long.MAX_VALUE;
		
		int maxYPos = Integer.MIN_VALUE;
		
		long maxX = Long.MIN_VALUE;
		long maxY = Long.MIN_VALUE;
		long maxZ = Long.MIN_VALUE;
		
		LittleGridContext context = LittleGridContext.getMin();
		boolean first = true;
		
		HashMap<BlockPos, TileEntityLittleTiles> map = new HashMap<>();
		
		for (Entry<TileEntityLittleTiles, ArrayList<LittleTile>> entry : tiles.entrySet()) {
			if (context.size < entry.getKey().getContext().size) {
				if (!first) {
					if (context.size > entry.getKey().getContext().size) {
						int modifier = context.size / entry.getKey().getContext().size;
						minX /= modifier;
						minY /= modifier;
						minZ /= modifier;
						maxX /= modifier;
						maxY /= modifier;
						maxZ /= modifier;
					} else {
						int modifier = entry.getKey().getContext().size / context.size;
						minX *= modifier;
						minY *= modifier;
						minZ *= modifier;
						maxX *= modifier;
						maxY *= modifier;
						maxZ *= modifier;
					}
				}
				context = entry.getKey().getContext();
			}
			
			first = false;
			
			for (LittleTile tile : entry.getValue()) {
				LittleTileBox box = tile.getCompleteBox();
				minX = Math.min(minX, entry.getKey().getPos().getX() * context.size + box.minX);
				minY = Math.min(minY, entry.getKey().getPos().getY() * context.size + box.minY);
				minZ = Math.min(minZ, entry.getKey().getPos().getZ() * context.size + box.minZ);
				
				maxX = Math.max(maxX, entry.getKey().getPos().getX() * context.size + box.maxX);
				maxY = Math.max(maxY, entry.getKey().getPos().getY() * context.size + box.maxY);
				maxZ = Math.max(maxZ, entry.getKey().getPos().getZ() * context.size + box.maxZ);
				
				minYPos = Math.min(minYPos, entry.getKey().getPos().getY());
				maxYPos = Math.max(maxYPos, entry.getKey().getPos().getY());
			}
			
			map.put(entry.getKey().getPos(), entry.getKey());
		}
		
		//double test = Math.floor(((minX+maxX)/LittleTile.gridSize/2D));
		int centerX = (int) Math.floor((minX + maxX) / (double) context.size / 2D);
		int centerY = (int) Math.floor((minY + maxY) / (double) context.size / 2D);
		int centerZ = (int) Math.floor((minZ + maxZ) / (double) context.size / 2D);
		
		int centerTileX = (int) (Math.floor(minX + maxX) / 2D) - centerX * context.size;
		int centerTileY = (int) (Math.floor(minY + maxY) / 2D) - centerY * context.size;
		int centerTileZ = (int) (Math.floor(minZ + maxZ) / 2D) - centerZ * context.size;
		
		LittleTilePos pos = new LittleTilePos(new BlockPos(centerX, minYPos, centerZ), context, new LittleTileVec(centerTileX, 0, centerTileZ));
		
		for (int y = minYPos; y <= maxYPos; y++) {
			TileEntityLittleTiles te = map.get(new BlockPos(centerX, y, centerZ));
			ArrayList<LittleTile> tilesInCenter = tiles.getValues(te);
			if (tilesInCenter != null) {
				te.convertTo(context);
				LittleTileBox box = new LittleTileBox(centerTileX, context.minPos, centerTileZ, centerTileX + 1, context.maxPos, centerTileZ + 1);
				if (context.size <= centerTileX) {
					box.minX = context.size - 1;
					box.maxX = context.size;
				}
				
				if (context.size <= centerTileZ) {
					box.minZ = context.size - 1;
					box.maxZ = context.size;
				}
				
				//int highest = LittleTile.minPos;
				for (int i = 0; i < tilesInCenter.size(); i++) {
					List<LittleTileBox> collision = tilesInCenter.get(i).getCollisionBoxes();
					for (int j = 0; j < collision.size(); j++) {
						LittleTileBox littleBox = collision.get(j);
						if (LittleTileBox.intersectsWith(box, littleBox)) {
							pos.contextVec.context = te.getContext();
							pos.contextVec.vec.y = Math.max((y - minYPos) * context.size + littleBox.maxY, pos.contextVec.vec.y);
						}
					}
				}
				te.convertToSmallest();
			}
		}
		
		pos.removeInternalBlockOffset();
		pos.convertToSmallest();
		return pos;
	}
	
	//====================LittleStructure ID====================
	
	public String getIDOfStructure() {
		return getIDByClass(this.getClass());
	}
	
	public static class LittleStructureEntry {
		
		public final String id;
		public final Class<? extends LittleStructure> structureClass;
		public Class<? extends LittleStructureParser> parser;
		public final LittleStructureAttribute attribute;
		
		public LittleStructureEntry(String id, Class<? extends LittleStructure> structureClass, Class<? extends LittleStructureParser> parser, LittleStructureAttribute attribute) {
			this.id = id;
			this.structureClass = structureClass;
			this.parser = parser;
			this.attribute = attribute;
		}
		
		@Override
		public boolean equals(Object object) {
			return object instanceof LittleStructureEntry && ((LittleStructureEntry) object).structureClass == this.structureClass;
		}
		
		@Override
		public String toString() {
			return structureClass.toString();
		}
		
		public LittleStructureParser createParser(GuiParent parent) {
			try {
				return parser.getConstructor(String.class, GuiParent.class).newInstance(id, parent);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public boolean isBed(IBlockAccess world, BlockPos pos, EntityLivingBase player) {
		return false;
	}
	
	public boolean shouldCheckForCollision() {
		return false;
	}
	
	public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
		
	}
	
	public void onUpdatePacketReceived() {
		
	}
	
	public void removeWorldProperties() {
		mainTile = null;
		tiles = new HashMapList<>();
		tilesToLoad = null;
	}
	
	public boolean canOnlyBePlacedByItemStack() {
		return false;
	}
	
	/**
	 * Only important for structures which require to be placed by the given itemstack
	 * 
	 * @return
	 */
	public String getStructureDropIdentifier() {
		return null;
	}
	
}

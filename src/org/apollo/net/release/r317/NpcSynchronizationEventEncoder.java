package org.apollo.net.release.r317;

import org.apollo.game.event.impl.NpcSynchronizationEvent;
import org.apollo.game.model.Animation;
import org.apollo.game.model.Direction;
import org.apollo.game.model.Graphic;
import org.apollo.game.model.Position;
import org.apollo.game.sync.block.AnimationBlock;
import org.apollo.game.sync.block.ForceChatBlock;
import org.apollo.game.sync.block.GraphicBlock;
import org.apollo.game.sync.block.HitUpdateBlock;
import org.apollo.game.sync.block.InteractingMobBlock;
import org.apollo.game.sync.block.SecondaryHitUpdateBlock;
import org.apollo.game.sync.block.SynchronizationBlockSet;
import org.apollo.game.sync.block.TransformBlock;
import org.apollo.game.sync.block.TurnToPositionBlock;
import org.apollo.game.sync.seg.AddNpcSegment;
import org.apollo.game.sync.seg.MovementSegment;
import org.apollo.game.sync.seg.SegmentType;
import org.apollo.game.sync.seg.SynchronizationSegment;
import org.apollo.net.codec.game.DataOrder;
import org.apollo.net.codec.game.DataTransformation;
import org.apollo.net.codec.game.DataType;
import org.apollo.net.codec.game.GamePacket;
import org.apollo.net.codec.game.GamePacketBuilder;
import org.apollo.net.meta.PacketType;
import org.apollo.net.release.EventEncoder;

/**
 * An {@link EventEncoder} for the {@link NpcSynchronizationEvent}.
 * 
 * @author Major
 */
public final class NpcSynchronizationEventEncoder extends EventEncoder<NpcSynchronizationEvent> {

	@Override
	public GamePacket encode(NpcSynchronizationEvent event) {
		GamePacketBuilder builder = new GamePacketBuilder(65, PacketType.VARIABLE_SHORT);
		builder.switchToBitAccess();

		GamePacketBuilder blockBuilder = new GamePacketBuilder();
		builder.putBits(8, event.getLocalNpcCount());

		for (SynchronizationSegment segment : event.getSegments()) {
			SegmentType type = segment.getType();
			if (type == SegmentType.REMOVE_MOB) {
				putRemoveMobUpdate(builder);
			} else if (type == SegmentType.ADD_MOB) {
				putAddNpcUpdate((AddNpcSegment) segment, event, builder);
				putBlocks(segment, blockBuilder);
			} else {
				putMovementUpdate(segment, event, builder);
				putBlocks(segment, blockBuilder);
			}
		}

		if (blockBuilder.getLength() > 0) {
			builder.putBits(14, 16383);
			builder.switchToByteAccess();
			builder.putRawBuilder(blockBuilder);
		} else {
			builder.switchToByteAccess();
		}

		return builder.toGamePacket();
	}

	/**
	 * Puts an add npc update.
	 * 
	 * @param seg The segment.
	 * @param event The event.
	 * @param builder The builder.
	 */
	private void putAddNpcUpdate(AddNpcSegment seg, NpcSynchronizationEvent event, GamePacketBuilder builder) {
		boolean updateRequired = seg.getBlockSet().size() > 0;
		Position npc = event.getPosition();
		Position other = seg.getPosition();
		builder.putBits(14, seg.getIndex());
		builder.putBits(5, other.getY() - npc.getY());
		builder.putBits(5, other.getX() - npc.getX());
		builder.putBits(1, 0); // discard walking queue
		builder.putBits(12, seg.getNpcId());
		builder.putBits(1, updateRequired ? 1 : 0);
	}

	/**
	 * Puts an animation block into the specified builder.
	 * 
	 * @param block The block.
	 * @param builder The builder.
	 */
	private void putAnimationBlock(AnimationBlock block, GamePacketBuilder builder) {
		Animation animation = block.getAnimation();
		builder.put(DataType.SHORT, DataOrder.LITTLE, animation.getId());
		builder.put(DataType.BYTE, animation.getDelay());
	}

	/**
	 * Puts the blocks for the specified segment.
	 * 
	 * @param segment The segment.
	 * @param builder The block builder.
	 */
	private void putBlocks(SynchronizationSegment segment, GamePacketBuilder builder) {
		SynchronizationBlockSet blockSet = segment.getBlockSet();
		if (blockSet.size() > 0) {
			int mask = 0;

			if (blockSet.contains(AnimationBlock.class)) {
				mask |= 0x10;
			}

			if (blockSet.contains(HitUpdateBlock.class)) {
				mask |= 0x8;
			}

			if (blockSet.contains(GraphicBlock.class)) {
				mask |= 0x80;
			}

			if (blockSet.contains(InteractingMobBlock.class)) {
				mask |= 0x20;
			}

			if (blockSet.contains(ForceChatBlock.class)) {
				mask |= 0x1;
			}

			if (blockSet.contains(SecondaryHitUpdateBlock.class)) {
				mask |= 0x40;
			}

			if (blockSet.contains(TransformBlock.class)) {
				mask |= 0x2;
			}

			if (blockSet.contains(TurnToPositionBlock.class)) {
				mask |= 0x4;
			}

			builder.put(DataType.BYTE, mask);

			if (blockSet.contains(AnimationBlock.class)) {
				putAnimationBlock(blockSet.get(AnimationBlock.class), builder);
			}

			if (blockSet.contains(HitUpdateBlock.class)) {
				putHitUpdateBlock(blockSet.get(HitUpdateBlock.class), builder);
			}

			if (blockSet.contains(GraphicBlock.class)) {
				putGraphicBlock(blockSet.get(GraphicBlock.class), builder);
			}

			if (blockSet.contains(InteractingMobBlock.class)) {
				putInteractingMobBlock(blockSet.get(InteractingMobBlock.class), builder);
			}

			if (blockSet.contains(ForceChatBlock.class)) {
				putForceChatBlock(blockSet.get(ForceChatBlock.class), builder);
			}

			if (blockSet.contains(SecondaryHitUpdateBlock.class)) {
				putSecondHitUpdateBlock(blockSet.get(SecondaryHitUpdateBlock.class), builder);
			}

			if (blockSet.contains(TransformBlock.class)) {
				putTransformBlock(blockSet.get(TransformBlock.class), builder);
			}

			if (blockSet.contains(TurnToPositionBlock.class)) {
				putTurnToPositionBlock(blockSet.get(TurnToPositionBlock.class), builder);
			}
		}
	}

	/**
	 * Puts a force chat block into the specified builder.
	 * 
	 * @param block The block.
	 * @param builder The builder.
	 */
	private void putForceChatBlock(ForceChatBlock block, GamePacketBuilder builder) {
		builder.putString(block.getMessage());
	}

	/**
	 * Puts a graphic block into the specified builder.
	 * 
	 * @param block The block.
	 * @param builder The builder.
	 */
	private void putGraphicBlock(GraphicBlock block, GamePacketBuilder builder) {
		Graphic graphic = block.getGraphic();
		builder.put(DataType.SHORT, graphic.getId());
		builder.put(DataType.INT, graphic.getDelay());
	}

	/**
	 * Puts a hit update block into the specified builder.
	 * 
	 * @param block The block.
	 * @param builder The builder.
	 */
	private void putHitUpdateBlock(HitUpdateBlock block, GamePacketBuilder builder) {
		builder.put(DataType.BYTE, DataTransformation.ADD, block.getDamage());
		builder.put(DataType.BYTE, DataTransformation.NEGATE, block.getType());
		builder.put(DataType.BYTE, DataTransformation.ADD, block.getCurrentHealth());
		builder.put(DataType.BYTE, block.getMaximumHealth());
	}

	/**
	 * Puts an interacting mob block into the specified builder.
	 * 
	 * @param block The block.
	 * @param builder The builder.
	 */
	private void putInteractingMobBlock(InteractingMobBlock block, GamePacketBuilder builder) {
		builder.put(DataType.SHORT, block.getInteractingMobIndex());
	}

	/**
	 * Puts a movement update for the specified segment.
	 * 
	 * @param segment The segment.
	 * @param event The event.
	 * @param builder The builder.
	 */
	private void putMovementUpdate(SynchronizationSegment segment, NpcSynchronizationEvent event,
			GamePacketBuilder builder) {
		boolean updateRequired = segment.getBlockSet().size() > 0;
		if (segment.getType() == SegmentType.RUN) {
			Direction[] directions = ((MovementSegment) segment).getDirections();
			builder.putBits(1, 1);
			builder.putBits(2, 2);
			builder.putBits(3, directions[0].toInteger());
			builder.putBits(3, directions[1].toInteger());
			builder.putBits(1, updateRequired ? 1 : 0);
		} else if (segment.getType() == SegmentType.WALK) {
			Direction[] directions = ((MovementSegment) segment).getDirections();
			builder.putBits(1, 1);
			builder.putBits(2, 1);
			builder.putBits(3, directions[0].toInteger());
			builder.putBits(1, updateRequired ? 1 : 0);
		} else {
			if (updateRequired) {
				builder.putBits(1, 1);
				builder.putBits(2, 0);
			} else {
				builder.putBits(1, 0);
			}
		}
	}

	/**
	 * Puts a remove mob update.
	 * 
	 * @param builder The builder.
	 */
	private void putRemoveMobUpdate(GamePacketBuilder builder) {
		builder.putBits(1, 1);
		builder.putBits(2, 3);
	}

	/**
	 * Puts a second hit update block into the specified builder.
	 * 
	 * @param block The block.
	 * @param builder The builder.
	 */
	private void putSecondHitUpdateBlock(SecondaryHitUpdateBlock block, GamePacketBuilder builder) {
		builder.put(DataType.BYTE, DataTransformation.NEGATE, block.getDamage());
		builder.put(DataType.BYTE, DataTransformation.SUBTRACT, block.getType());
		builder.put(DataType.BYTE, DataTransformation.SUBTRACT, block.getCurrentHealth());
		builder.put(DataType.BYTE, DataTransformation.NEGATE, block.getMaximumHealth());
	}

	/**
	 * Puts a transform block into the specified builder.
	 * 
	 * @param block The block.
	 * @param builder The builder.
	 */
	private void putTransformBlock(TransformBlock block, GamePacketBuilder builder) {
		builder.put(DataType.SHORT, DataOrder.LITTLE, DataTransformation.ADD, block.getId());
	}

	/**
	 * Puts a turn to position block into the specified builder.
	 * 
	 * @param block The block.
	 * @param builder The builder.
	 */
	private void putTurnToPositionBlock(TurnToPositionBlock block, GamePacketBuilder builder) {
		Position position = block.getPosition();
		builder.put(DataType.SHORT, DataOrder.LITTLE, position.getX() * 2 + 1);
		builder.put(DataType.SHORT, DataOrder.LITTLE, position.getY() * 2 + 1);
	}

}
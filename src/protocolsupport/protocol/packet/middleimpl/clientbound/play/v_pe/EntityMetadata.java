package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe;

import io.netty.buffer.ByteBuf;
import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleEntityMetadata;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe.EntitySetAttributes.AttributeInfo;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.protocol.typeremapper.watchedentity.DataWatcherDataRemapper;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObject;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObjectIdRegistry;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObjectIndex;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectSVarLong;
import protocolsupport.protocol.utils.types.networkentity.NetworkEntity;
import protocolsupport.protocol.utils.types.networkentity.NetworkEntityItemDataCache;
import protocolsupport.protocol.utils.types.networkentity.NetworkEntityType;
import protocolsupport.utils.CollectionsUtils.ArrayMap;
import protocolsupport.utils.ObjectFloatTuple;
import protocolsupport.utils.recyclable.RecyclableArrayList;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.zplatform.ServerPlatform;
import protocolsupport.zplatform.itemstack.ItemStackWrapper;

public class EntityMetadata extends MiddleEntityMetadata {

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		RecyclableArrayList<ClientBoundPacketData> packets = RecyclableArrayList.create();
		ProtocolVersion version = connection.getVersion();
		String locale = cache.getAttributesCache().getLocale();
		NetworkEntity entity = cache.getWatchedEntityCache().getWatchedEntity(entityId);
		if (entity == null) {
			return packets;
		}
		switch (entity.getType()) {
			case ITEM: {
				DataWatcherObjectIndex.Item.ITEM.getValue(metadata.getOriginal()).ifPresent(itemWatcher -> {
					NetworkEntityItemDataCache itemDataCache = (NetworkEntityItemDataCache) entity.getDataCache();
					packets.addAll(itemDataCache.updateItem(version, entity.getId(), itemWatcher.getValue()));
				});
				break;
			}
			default: {
				if (entity.getType().isOfType(NetworkEntityType.LIVING)) {
					DataWatcherObjectIndex.EntityLiving.HEALTH.getValue(metadata.getOriginal()).ifPresent(healthWatcher -> {
						packets.add(EntitySetAttributes.create(version, entity, new ObjectFloatTuple<>(AttributeInfo.HEALTH, healthWatcher.getValue())));
					});
				}
				if (entity.getType().isOfType(NetworkEntityType.BATTLE_HORSE)) {
					DataWatcherObjectIndex.BattleHorse.ARMOR.getValue(metadata.getOriginal()).ifPresent(armorWatcher -> {
						int type = armorWatcher.getValue();
						packets.add(EntityEquipment.create(version, locale, entityId,
							ItemStackWrapper.NULL,
							type == 0 ? ItemStackWrapper.NULL : ServerPlatform.get().getWrapperFactory().createItemStack(416 + armorWatcher.getValue()),
							ItemStackWrapper.NULL,
							ItemStackWrapper.NULL
						));
					});
				}
				packets.add(create(entity, locale, metadata.getRemapped(), version));
			}
		}
		return packets;
	}

	public static ClientBoundPacketData createFaux(NetworkEntity entity, String locale, ArrayMap<DataWatcherObject<?>> fauxMeta, ProtocolVersion version) {
		return create(entity, locale, transform(entity, fauxMeta, version), version);
	}

	public static ClientBoundPacketData createFaux(NetworkEntity entity, String locale, ProtocolVersion version) {
		DataWatcherDataRemapper faux = new DataWatcherDataRemapper();
		faux.remap(version, entity);
		return create(entity, locale, transform(entity, faux.getRemapped(), version), version);
	}

	public static ArrayMap<DataWatcherObject<?>> transform(NetworkEntity entity, ArrayMap<DataWatcherObject<?>> peMetadata, ProtocolVersion version) {
		peMetadata.put(0, new DataWatcherObjectSVarLong(entity.getDataCache().getPeBaseFlags()));
		return peMetadata;
	}

	public static ClientBoundPacketData create(NetworkEntity entity, String locale, ArrayMap<DataWatcherObject<?>> peMetadata, ProtocolVersion version) {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PEPacketIDs.SET_ENTITY_DATA, version);
		VarNumberSerializer.writeVarLong(serializer, entity.getId());
		EntityMetadata.encodeMeta(serializer, version, locale, transform(entity, peMetadata, version));
		return serializer;
	}

	public static void encodeMeta(ByteBuf to, ProtocolVersion version, String locale, ArrayMap<DataWatcherObject<?>> peMetadata) {
		//For now. Iterate two times :P TODO: Fake varint, if that's possible.
		int entries = 0;
		for (int key = peMetadata.getMinKey(); key < peMetadata.getMaxKey(); key++) {
			DataWatcherObject<?> object = peMetadata.get(key);
			if (object != null) {
				entries++;
			}
		}
		//We stored that. Now write the length first and then go.
		VarNumberSerializer.writeVarInt(to, entries);
		for (int key = peMetadata.getMinKey(); key < peMetadata.getMaxKey(); key++) {
			DataWatcherObject<?> object = peMetadata.get(key);
			if (object != null) {
				VarNumberSerializer.writeVarInt(to, key);
				VarNumberSerializer.writeVarInt(to, DataWatcherObjectIdRegistry.getTypeId(object, version));
				object.writeToStream(to, version, locale);
				entries++;
			}
		}
	}

	public static class PeMetaBase {

		//PE's extra baseflags. TODO: Implement more flags (Easy Remapping)
		protected static int id = 1;
		protected static int takeNextId() {
			return id++;
		}

		public static final int FLAG_ON_FIRE = takeNextId(); //0
		public static final int FLAG_SNEAKING = takeNextId(); //1
		public static final int FLAG_RIDING = takeNextId();
		public static final int FLAG_SPRINTING = takeNextId();
		public static final int FLAG_USING_ITEM = takeNextId();
		public static final int FLAG_INVISIBLE = takeNextId();
		public static final int FLAG_TEMPTED = takeNextId();
		public static final int FLAG_IN_LOVE = takeNextId();
		public static final int FLAG_SADDLED = takeNextId();
		public static final int FLAG_POWERED = takeNextId();
		public static final int FLAG_IGNITED = takeNextId(); //10
		public static final int FLAG_BABY = takeNextId();
		public static final int FLAG_CONVERTING = takeNextId();
		public static final int FLAG_CRITICAL = takeNextId();
		public static final int FLAG_SHOW_NAMETAG = takeNextId();
		public static final int FLAG_ALWAYS_SHOW_NAMETAG = takeNextId();
		public static final int FLAG_NO_AI = takeNextId();
		public static final int FLAG_SILENT = takeNextId();
		public static final int FLAG_CLIMBING = takeNextId();
		public static final int FLAG_CAN_CLIMB = takeNextId();
		public static final int FLAG_CAN_SWIM = takeNextId(); //20
		public static final int FLAG_CAN_FLY = takeNextId();
		public static final int FLAG_RESTING = takeNextId();
		public static final int FLAG_SITTING = takeNextId();
		public static final int FLAG_ANGRY = takeNextId();
		public static final int FLAG_INTERESTED = takeNextId();
		public static final int FLAG_CHARGED = takeNextId();
		public static final int FLAG_TAMED = takeNextId();
		public static final int FLAG_LEASHED = takeNextId();
		public static final int FLAG_SHEARED = takeNextId();
		public static final int FLAG_GLIDING = takeNextId(); //30
		public static final int FLAG_ELDER = takeNextId();
		public static final int FLAG_MOVING = takeNextId();
		public static final int FLAG_BREATHING = takeNextId();
		public static final int FLAG_CHESTED = takeNextId();
		public static final int FLAG_STACKABLE = takeNextId();
		public static final int FLAG_SHOW_BASE = takeNextId();
		public static final int FLAG_REARING = takeNextId();
		public static final int FLAG_VIBRATING = takeNextId();
		public static final int FLAG_IDLING = takeNextId();
		public static final int FLAG_EVOKER_SPELL = takeNextId(); //40
		public static final int FLAG_CHARGE_ATTACK = takeNextId();
		public static final int FLAG_WASD_CONTROLLED = takeNextId();
		public static final int FLAG_CAN_POWER_JUMP = takeNextId();
		public static final int FLAG_LINGER = takeNextId();
		public static final int FLAG_COLLIDE = takeNextId();
		public static final int FLAG_GRAVITY = takeNextId();
		public static final int FLAG_FIRE_IMMUNE = takeNextId();
		public static final int FLAG_DANCING = takeNextId(); //48

	}
}
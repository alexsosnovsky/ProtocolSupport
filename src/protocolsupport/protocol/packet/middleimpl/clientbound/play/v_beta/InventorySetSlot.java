package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_beta;

import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.ClientBoundPacket;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleInventorySetSlot;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.serializer.ItemStackSerializer;
import protocolsupport.protocol.typeremapper.basic.WindowSlotsRemappingHelper;
import protocolsupport.protocol.utils.types.NetworkItemStack;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableEmptyList;
import protocolsupport.utils.recyclable.RecyclableSingletonList;

public class InventorySetSlot extends MiddleInventorySetSlot {

	public InventorySetSlot(ConnectionImpl connection) {
		super(connection);
	}

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		String locale = cache.getAttributesCache().getLocale();
		if (windowId == WINDOW_ID_PLAYER_CURSOR) {
			return RecyclableSingletonList.create(create(version, locale, windowId, slot, itemstack));
		}
		if (windowId == WINDOW_ID_PLAYER_INVENTORY) {
			//TODO: remap for versions that don't actually support this special window id
			return RecyclableSingletonList.create(create(version, locale, windowId, slot, itemstack));
		}
		if (windowId == WINDOW_ID_PLAYER_HOTBAR && slot >= 36 && slot < 45) {
			return RecyclableSingletonList.create(create(version, locale, windowId, slot, itemstack));
		}

		if (!cache.getWindowCache().isValidWindowId(windowId)) {
			return RecyclableEmptyList.get();
		}
		switch (cache.getWindowCache().getOpenedWindow()) {
			case PLAYER: {
				if (!WindowSlotsRemappingHelper.hasPlayerOffhandSlot(version)) {
					if (slot == WindowSlotsRemappingHelper.PLAYER_OFF_HAND_SLOT) {
						return RecyclableEmptyList.get();
					}
				}
				break;
			}
			default: {
				break;
			}
		}
		return RecyclableSingletonList.create(create(version, locale, windowId, slot, itemstack));
	}

	protected static ClientBoundPacketData create(ProtocolVersion version, String locale, int windowId, int slot, NetworkItemStack itemstack) {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(ClientBoundPacket.PLAY_WINDOW_SET_SLOT_ID);
		serializer.writeByte(windowId);
		serializer.writeShort(slot);
		ItemStackSerializer.writeItemStack(serializer, version, locale, itemstack);
		return serializer;
	}

}

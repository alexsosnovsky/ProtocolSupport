package protocolsupport.protocol.packet.middle.serverbound.play;

import protocolsupport.protocol.packet.PacketType;
import protocolsupport.protocol.packet.middle.ServerBoundMiddlePacket;
import protocolsupport.protocol.packet.middleimpl.ServerBoundPacketData;
import protocolsupport.protocol.serializer.VarNumberSerializer;

public abstract class MiddleSetBeaconEffect extends ServerBoundMiddlePacket {

	public MiddleSetBeaconEffect(MiddlePacketInit init) {
		super(init);
	}

	protected int primary;
	protected int secondary;

	@Override
	protected void write() {
		codec.writeServerbound(create(primary, secondary));
	}

	public static ServerBoundPacketData create(int primary, int secondary) {
		ServerBoundPacketData serializer = ServerBoundPacketData.create(PacketType.SERVERBOUND_PLAY_SET_BEACON_EFFECT);
		VarNumberSerializer.writeVarInt(serializer, primary);
		VarNumberSerializer.writeVarInt(serializer, secondary);
		return serializer;
	}

}

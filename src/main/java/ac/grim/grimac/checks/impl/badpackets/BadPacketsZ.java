package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import static ac.grim.grimac.events.packets.patch.ResyncWorldUtil.resyncPosition;
import static ac.grim.grimac.utils.nmsutil.BlockBreakSpeed.getBlockDamage;

@CheckData(name = "BadPacketsZ", experimental = true)
public class BadPacketsZ extends Check implements PacketCheck {
    public BadPacketsZ(final GrimPlayer player) {
        super(player);
    }

    private boolean exemptNextFinish = false;
    private Vector3i lastBlock, lastLastBlock = null;
    private final int exemptedY = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? 4095 : 255;

    // The client sometimes sends a wierd cancel packet
    private boolean shouldExempt(final Vector3i pos) {
        // lastLastBlock is always null when this happens, and lastBlock isn't
        if (lastLastBlock != null || lastBlock == null)
            return false;

        // on pre 1.14.4 clients, the YPos of this packet is always the same
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) && pos.y != exemptedY)
            return false;

        // the client only sends this packet if the last block was an instant break
        if (getBlockDamage(player, lastBlock) < 1)
            return false;

        // and if this block is not an instant break
        return player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || getBlockDamage(player, pos) < 1;
    }

    private String formatted(Vector3i vec) {
        return vec == null ? "null" : vec.x + ", " + vec.y + ", " + vec.z;
    }

    public void handle(PacketReceiveEvent event, WrapperPlayClientPlayerDigging dig) {
        if (dig.getAction() == DiggingAction.START_DIGGING) {
            lastLastBlock = lastBlock;
            lastBlock = dig.getBlockPosition();

            exemptNextFinish = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14_4) && getBlockDamage(player, lastBlock) >= 1;
            return;
        }

        if (dig.getAction() == DiggingAction.CANCELLED_DIGGING) {
            if (shouldExempt(dig.getBlockPosition())) {
                lastLastBlock = null;
                lastBlock = null;
                return;
            }

            exemptNextFinish = false;

            if ((lastBlock == null || !lastBlock.equals(dig.getBlockPosition())) && (lastLastBlock == null || !lastLastBlock.equals(dig.getBlockPosition()))) {
                if (flagAndAlert("action=CANCELLED_DIGGING, last=" + formatted(lastBlock) + "/" + formatted(lastLastBlock) + ", pos=" + formatted(dig.getBlockPosition()))) {
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }
            }

            lastLastBlock = null;
            lastBlock = null;
        }

        if (dig.getAction() == DiggingAction.FINISHED_DIGGING) {
            if (exemptNextFinish) {
                exemptNextFinish = false;
                return;
            }

            if ((lastBlock == null || !lastBlock.equals(dig.getBlockPosition())) && (lastLastBlock == null || !lastLastBlock.equals(dig.getBlockPosition()))) {
                if (flagAndAlert("action=FINISHED_DIGGING, last=" + formatted(lastBlock) + "/" + formatted(lastLastBlock) + ", pos=" + formatted(dig.getBlockPosition()))) {
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        resyncPosition(player, dig.getBlockPosition());
                    }
                }
            }

            // 1.14.4+ clients don't send another start break in protected regions
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) {
                lastLastBlock = null;
                lastBlock = null;
            }
        }
    }
}

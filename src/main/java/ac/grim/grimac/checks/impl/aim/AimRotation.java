package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.HeadRotation;
import com.github.retrooper.packetevents.util.Vector3d;

@CheckData(name = "AimRotation")
public class AimRotation extends Check implements RotationCheck, PositionCheck {

    private double checkViolations = 0;
    private double flagVL = 3;
    private double deltaXZ = 0;
    
    public AimRotation(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        HeadRotation to = update.getTo();
        HeadRotation from = update.getFrom();
        float deltaYaw = Math.abs(to.getYaw() - from.getYaw());
        if (player.actionManager.hasAttackedSince(20 * 50) && !player.packetStateData.lastPacketWasTeleport) {
            if (to.getPitch() != 0.0F || from.getPitch() != 0.0F || !(deltaYaw > 2.0F) && !(this.deltaXZ > 0.08)) {
                this.checkViolations *= 0.8;
            } else if (++this.checkViolations > flagVL) {
                flagAndAlert("p=" + to.getPitch() + " lp=" + from.getPitch());
            }
        }
    }

    @Override
    public void onPositionUpdate(PositionUpdate positionUpdate) {
        Vector3d dv = positionUpdate.getTo().subtract(positionUpdate.getFrom());
        deltaXZ = Math.hypot(dv.x, dv.z);
    }

    @Override
    public void reload() {
        super.reload();
        flagVL = getConfig().getDoubleElse(getConfigName() + ".flagVL", 5);
    }

}

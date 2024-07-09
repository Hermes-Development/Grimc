package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.HeadRotation;

@CheckData(name = "AimZeroDelta")
public class AimZeroDelta extends Check implements RotationCheck {
    private int zeroDeltaTicks;
    private double checkViolations = 0;
    private double flagVL = 5;
    public AimZeroDelta(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        HeadRotation to = update.getTo();
        HeadRotation from = update.getFrom();
        float deltaYaw = Math.abs(to.getYaw() - from.getYaw());
        float deltaPitch = Math.abs(to.getPitch() - from.getPitch());
        if (player.actionManager.hasAttackedSince(3 * 50) && !player.packetStateData.lastPacketWasTeleport) {
            if (deltaPitch == 0.0F) {
                ++this.zeroDeltaTicks;
            } else {
                this.zeroDeltaTicks = 0;
            }

            if (this.zeroDeltaTicks <= 40 || !(deltaYaw > 3.0F) || !(Math.abs(to.getPitch()) < 45.0F)) {
                this.checkViolations *= 0.75;
            } else if (++this.checkViolations > flagVL) {
                this.flagAndAlert("vl=" + checkViolations + " p=" + to.getPitch() + " lp=" + from.getPitch());
            }
        }
    }

    @Override
    public void reload() {
        super.reload();
        flagVL = getConfig().getDoubleElse(getConfigName() + ".flagVL", 5);
    }


}

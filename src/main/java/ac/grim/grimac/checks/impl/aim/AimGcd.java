package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.HeadRotation;
import ac.grim.grimac.utils.math.GrimMath;

@CheckData(name = "AimGcd")
public class AimGcd extends Check implements RotationCheck, PositionCheck {

    double pitch;
    double yaw;
    double thresholdP;
    double thresholdY;
    double flagThresholdP = 8;
    double flagThresholdY = 4;
    public AimGcd(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        HeadRotation to = update.getTo();
        HeadRotation from = update.getFrom();
        float pitch = Math.abs(to.getPitch() - from.getPitch());
        float yaw = Math.abs(to.getYaw() - from.getYaw());
        if (!this.player.packetStateData.lastPacketWasTeleport && !this.player.isRiptidePose && (this.player.compensatedEntities.serverPlayerVehicle != null) && this.player.getHorizontalSensitivity() < 150) {
            double gcdP = GrimMath.gcd(pitch, this.pitch);
            double gcdY = GrimMath.gcd(yaw, this.yaw);
            if (gcdP > 0.7 && ((double)pitch % this.pitch == 0.0 || Double.isNaN((double)pitch % this.pitch)) && pitch <= 10.0F) {
                this.thresholdP = Math.min(10.0, this.thresholdP + 0.5);
                if (this.thresholdP > flagThresholdP) {
                    this.flagAndAlert("GCD=" + gcdP + " COUNT=" + this.thresholdP);
                }
            } else {
                this.thresholdP = Math.max(0.0, this.thresholdP - 1.25);
            }

            if (gcdY > 0.7 && ((double)yaw % this.yaw == 0.0 || Double.isNaN((double)yaw % this.yaw)) && yaw <= 10.0F) {
                this.thresholdY = Math.min(10.0, this.thresholdY + 0.5);
                if (this.thresholdY > flagThresholdY) {
                    this.flagAndAlert("GCD=" + gcdY + " COUNT=" + this.thresholdY);
                }
            } else {
                this.thresholdY = Math.max(0.0, this.thresholdY - 1.5);
            }
        }

        this.pitch = pitch;
        this.yaw = yaw;
    }

    @Override
    public void reload() {
        super.reload();
        flagThresholdP = getConfig().getDoubleElse(getConfigName() + ".flagThresholdP", 8);
        flagThresholdY = getConfig().getDoubleElse(getConfigName() + ".flagThresholdY", 4);
    }
}

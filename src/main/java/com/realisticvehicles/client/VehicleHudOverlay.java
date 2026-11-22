package com.realisticvehicles.client;

import com.realisticvehicles.minecraft.VehicleClientPose;
import com.realisticvehicles.minecraft.VehicleEntity;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders an automotive HUD (speed km/h, RPM bar, gear, lights, indicators, camera mode) from synced simulation data.
 */
public final class VehicleHudOverlay {
    private static final float MAX_DISPLAY_RPM = 7500.0F;

    private VehicleHudOverlay() {
    }

    public static void render(GuiGraphics gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }

        VehicleClientPose pose = vehicle.clientPose();
        int width = gui.guiWidth();
        int height = gui.guiHeight();
        int boxW = 160;
        int boxH = 92;
        int x = width - boxW - 12;
        int y = height - boxH - 12;

        gui.fill(x, y, x + boxW, y + boxH, 0xB810141A);
        gui.renderOutline(x, y, boxW, boxH, 0xFF3A4656);

        Font font = mc.font;
        String speedText = String.format(Locale.ROOT, "%3.0f km/h", pose.speedKmh());
        gui.drawString(font, speedText, x + 8, y + 7, 0xFFFFFFFF, false);

        String gearLabel = pose.gear() < 0 ? "R" : (pose.gear() == 0 ? "N" : String.valueOf(pose.gear()));
        int gearColor = pose.gear() < 0 ? 0xFFFFB74D : 0xFF64B5F6;
        gui.drawString(font, "G:" + gearLabel, x + boxW - 34, y + 7, gearColor, false);

        float rpm = Math.max(0.0F, Math.min(MAX_DISPLAY_RPM, pose.engineRpm()));
        float rpmFrac = rpm / MAX_DISPLAY_RPM;
        int barX = x + 8, barY = y + 21, barW = boxW - 16;
        gui.fill(barX, barY, barX + barW, barY + 5, 0xFF222832);
        int fillColor = rpmFrac > 0.85F ? 0xFFE53935 : (rpmFrac > 0.65F ? 0xFFFFA726 : 0xFF42A5F5);
        gui.fill(barX, barY, barX + Math.round(barW * rpmFrac), barY + 5, fillColor);

        String rpmText = String.format(Locale.ROOT, "%.0f RPM", rpm);
        gui.drawString(font, rpmText, x + 8, y + 30, 0xFFB0BEC5, false);

        String status = pose.braking() ? "[BRK]" : (pose.reversing() ? "[REV]" : "[OK]");
        int statusColor = pose.braking() ? 0xFFEF5350 : 0xFF81C784;
        gui.drawString(font, status, x + boxW - 38, y + 30, statusColor, false);

        // Fuel gauge bar + liters
        float fuelFrac = Math.max(0.0F, Math.min(1.0F, pose.fuelLiters() / 70.0F));
        String fuelText = pose.isMisfueled()
            ? "⛽ ERREUR DIESEL!"
            : String.format(Locale.ROOT, "⛽ %.1f / 70 L", pose.fuelLiters());
        int fuelColor = (pose.isMisfueled() || fuelFrac < 0.15F) ? 0xFFEF5350 : 0xFF66BB6A;
        gui.drawString(font, fuelText, x + 8, y + 43, fuelColor, false);

        boolean blink = ((System.currentTimeMillis() / 280L) & 1L) == 0L;
        byte mask = pose.lightsMask();
        boolean leftInd = (mask & 4) != 0 && blink;
        boolean rightInd = (mask & 8) != 0 && blink;
        boolean highBeam = (mask & 2) != 0;
        boolean lowBeam = (mask & 1) != 0;

        gui.drawString(font, "<", x + 8, y + 57, leftInd ? 0xFFFFA726 : 0xFF455A64, false);
        String beamText = highBeam ? "[FEUX ROUTE]" : (lowBeam ? "[CROISEMENT]" : "[FEUX OFF]");
        int beamColor = highBeam ? 0xFF42A5F5 : (lowBeam ? 0xFF81C784 : 0xFF607D8B);
        gui.drawString(font, beamText, x + 22, y + 57, beamColor, false);
        gui.drawString(font, ">", x + boxW - 16, y + 57, rightInd ? 0xFFFFA726 : 0xFF455A64, false);

        String camText = "[C] " + VehicleCameraController.activeMode().label() + " [L/G/H/K/B]";
        gui.drawString(font, camText, x + 8, y + 72, 0xFF90A4AE, false);
    }
}

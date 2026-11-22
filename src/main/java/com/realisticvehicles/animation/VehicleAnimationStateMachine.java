package com.realisticvehicles.animation;

import com.realisticvehicles.core.math.MathUtil;

/**
 * State machine managing smooth door, hood, trunk, wiper, window, and entry/exit animations.
 */
public final class VehicleAnimationStateMachine {
    private VehicleAnimationState state = VehicleAnimationState.IDLE;
    private double stateProgress;
    private float leftDoorOpen;
    private float rightDoorOpen;
    private float hoodOpen;
    private float trunkOpen;
    private float windowOpen;
    private float wiperPhase;
    private boolean wipersEnabled;
    private float steeringWheelRad;

    public VehicleAnimationState state() {
        return state;
    }

    public double stateProgress() {
        return stateProgress;
    }

    public float leftDoorOpen() {
        return leftDoorOpen;
    }

    public float rightDoorOpen() {
        return rightDoorOpen;
    }

    public float hoodOpen() {
        return hoodOpen;
    }

    public float trunkOpen() {
        return trunkOpen;
    }

    public float windowOpen() {
        return windowOpen;
    }

    public float wiperAngleRad() {
        return (float) (Math.sin(wiperPhase) * 0.65 + 0.65) * 0.5F;
    }

    public float steeringWheelRad() {
        return steeringWheelRad;
    }

    public void beginEntry() {
        if (state == VehicleAnimationState.IDLE || state == VehicleAnimationState.EXITING) {
            state = VehicleAnimationState.APPROACHING;
            stateProgress = 0.0;
        }
    }

    public void beginExit() {
        if (state == VehicleAnimationState.SEATED || state == VehicleAnimationState.ENTERING) {
            state = VehicleAnimationState.EXITING;
            stateProgress = 0.0;
        }
    }

    public void interruptToIdle() {
        state = VehicleAnimationState.IDLE;
        stateProgress = 0.0;
    }

    public void toggleLeftDoor(boolean jammed) {
        if (!jammed) {
            leftDoorOpen = leftDoorOpen > 0.5F ? 0.0F : 1.0F;
        }
    }

    public void toggleHood() {
        hoodOpen = hoodOpen > 0.5F ? 0.0F : 1.0F;
    }

    public void toggleTrunk() {
        trunkOpen = trunkOpen > 0.5F ? 0.0F : 1.0F;
    }

    public void toggleWipers() {
        wipersEnabled = !wipersEnabled;
    }

    public void toggleWindow() {
        windowOpen = windowOpen > 0.5F ? 0.0F : 1.0F;
    }

    public void step(double dt, double frontSteerRad, boolean hasDriver) {
        if (dt <= 0.0) {
            return;
        }
        float targetSteerWheel = (float) (frontSteerRad * 7.5);
        steeringWheelRad = (float) MathUtil.lerp(steeringWheelRad, targetSteerWheel, MathUtil.clamp(dt * 12.0, 0.0, 1.0));

        if (wipersEnabled) {
            wiperPhase = (float) ((wiperPhase + dt * 4.2) % (Math.PI * 2.0));
        } else {
            wiperPhase = (float) MathUtil.moveTowards(wiperPhase, 0.0, dt * 4.0);
        }

        switch (state) {
            case APPROACHING -> advanceState(dt, 3.0, VehicleAnimationState.OPENING_DOOR);
            case OPENING_DOOR -> {
                leftDoorOpen = (float) MathUtil.moveTowards(leftDoorOpen, 1.0, dt * 3.0);
                advanceState(dt, 2.5, VehicleAnimationState.ENTERING);
            }
            case ENTERING -> advanceState(dt, 2.5, VehicleAnimationState.SEATED);
            case SEATED -> {
                leftDoorOpen = (float) MathUtil.moveTowards(leftDoorOpen, 0.0, dt * 2.8);
                if (!hasDriver) {
                    beginExit();
                }
            }
            case EXITING -> {
                leftDoorOpen = (float) MathUtil.moveTowards(leftDoorOpen, stateProgress < 0.6 ? 1.0 : 0.0, dt * 3.0);
                advanceState(dt, 2.0, VehicleAnimationState.IDLE);
            }
            case IDLE -> {
                if (hasDriver) {
                    beginEntry();
                }
            }
        }
    }

    private void advanceState(double dt, double rate, VehicleAnimationState next) {
        stateProgress += dt * rate;
        if (stateProgress >= 1.0) {
            state = next;
            stateProgress = 0.0;
        }
    }
}

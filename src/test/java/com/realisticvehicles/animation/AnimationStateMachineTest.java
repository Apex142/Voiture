package com.realisticvehicles.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class AnimationStateMachineTest {

    @Test
    void entryAndExitProgressSmoothlyWithoutTeleporting() {
        VehicleAnimationStateMachine sm = new VehicleAnimationStateMachine();
        assertEquals(VehicleAnimationState.IDLE, sm.state());

        sm.beginEntry();
        assertEquals(VehicleAnimationState.APPROACHING, sm.state());

        for (int i = 0; i < 60; i++) {
            sm.step(0.05, 0.25, true);
        }
        assertEquals(VehicleAnimationState.SEATED, sm.state());
        assertEquals(0.0F, sm.leftDoorOpen(), 0.05F, "Door closes once seated");
        assertTrue(sm.steeringWheelRad() > 1.0F, "Steering wheel rotates proportionally to front steer");

        sm.step(0.05, 0.0, false);
        assertEquals(VehicleAnimationState.EXITING, sm.state());
        for (int i = 0; i < 30; i++) {
            sm.step(0.05, 0.0, false);
        }
        assertEquals(VehicleAnimationState.IDLE, sm.state());
    }

    @Test
    void jammedDoorCannotOpenWhileHealthyDoorAndHoodToggle() {
        VehicleAnimationStateMachine sm = new VehicleAnimationStateMachine();
        sm.toggleLeftDoor(true);
        assertEquals(0.0F, sm.leftDoorOpen(), 1e-6);

        sm.toggleLeftDoor(false);
        assertEquals(1.0F, sm.leftDoorOpen(), 1e-6);

        sm.toggleHood();
        sm.toggleTrunk();
        assertEquals(1.0F, sm.hoodOpen(), 1e-6);
        assertEquals(1.0F, sm.trunkOpen(), 1e-6);
    }

    @Test
    void entryCanBeSafelyInterruptedToIdle() {
        VehicleAnimationStateMachine sm = new VehicleAnimationStateMachine();
        sm.beginEntry();
        sm.step(0.2, 0.0, true);
        sm.interruptToIdle();
        assertEquals(VehicleAnimationState.IDLE, sm.state());
        assertEquals(0.0, sm.stateProgress(), 1e-6);
    }
}

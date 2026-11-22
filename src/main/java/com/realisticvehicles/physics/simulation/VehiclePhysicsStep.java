package com.realisticvehicles.physics.simulation;

import com.realisticvehicles.core.event.EventBus;
import com.realisticvehicles.core.math.Quat4d;
import com.realisticvehicles.core.math.Vec3d;
import com.realisticvehicles.physics.body.RigidBodyIntegrator;
import com.realisticvehicles.physics.body.RigidBodyState;
import com.realisticvehicles.physics.collision.ChassisCollider;
import com.realisticvehicles.physics.collision.CollisionResolver;
import com.realisticvehicles.physics.drivetrain.AbsTcsController;
import com.realisticvehicles.physics.drivetrain.DrivetrainOutput;
import com.realisticvehicles.physics.drivetrain.DrivetrainSolver;
import com.realisticvehicles.physics.drivetrain.VehicleEngine;
import com.realisticvehicles.physics.drivetrain.VehicleTransmission;
import com.realisticvehicles.physics.steering.AckermannSteeringSolver;
import com.realisticvehicles.physics.suspension.AntiRollBarSolver;
import com.realisticvehicles.physics.suspension.SuspensionSolver;
import com.realisticvehicles.physics.suspension.WheelState;
import com.realisticvehicles.physics.tires.TireContext;
import com.realisticvehicles.physics.tires.TireForces;
import com.realisticvehicles.physics.tires.TireModel;
import com.realisticvehicles.physics.world.CollisionWorld;
import java.util.List;

/**
 * Executes a single fixed-timestep vehicle physics substep.
 */
public final class VehiclePhysicsStep {
    public static final double GRAVITY = 9.81;

    private final RigidBodyIntegrator integrator;
    private final SuspensionSolver suspensionSolver;
    private final AntiRollBarSolver antiRollBarSolver;
    private final AckermannSteeringSolver steeringSolver;
    private final DrivetrainSolver drivetrainSolver;
    private final TireModel tireModel;
    private final ChassisCollider chassisCollider;
    private final CollisionResolver collisionResolver;

    public VehiclePhysicsStep(
        VehicleConfig config,
        TireModel tireModel,
        AckermannSteeringSolver steeringSolver
    ) {
        this.integrator = new RigidBodyIntegrator();
        this.suspensionSolver = new SuspensionSolver();
        this.antiRollBarSolver = new AntiRollBarSolver();
        this.steeringSolver = steeringSolver;
        this.drivetrainSolver = new DrivetrainSolver(
            config.maxServiceBrakeTorqueNm(),
            config.handbrakeTorqueNm(),
            config.differential(),
            new AbsTcsController(config.absEnabled(), config.tcsEnabled())
        );
        this.tireModel = tireModel;
        this.chassisCollider = new ChassisCollider(config.body());
        this.collisionResolver = new CollisionResolver();
    }

    public DrivetrainOutput execute(
        VehicleConfig config,
        RigidBodyState body,
        List<WheelState> wheels,
        VehicleEngine engine,
        VehicleTransmission transmission,
        VehicleInput input,
        CollisionWorld world,
        EventBus eventBus,
        double dt
    ) {
        double weight = config.body().mass() * GRAVITY;
        body.addForce(new Vec3d(0.0, -weight, 0.0));

        double forwardSpeed = body.forwardSpeed();
        steeringSolver.updateSteering(input.steer(), forwardSpeed, config.steering(), wheels, dt);

        for (WheelState wheel : wheels) {
            suspensionSolver.solveWheel(body, wheel, world, weight, dt);
        }
        antiRollBarSolver.applyAxleAntiRoll(
            body, wheels, config.frontAntiRollStiffness(), config.rearAntiRollStiffness()
        );

        DrivetrainOutput drivetrain = drivetrainSolver.solve(
            engine,
            transmission,
            wheels,
            forwardSpeed,
            input.throttle(),
            input.brake(),
            input.handbrake(),
            dt
        );

        applyTireForces(body, wheels, drivetrain, dt);
        if (body.orientation().up().y() <= 0.25) {
            // Overturned on roof/side: slide with metal friction and allow GTA V steer-to-upright roll torque
            body.setLinearVelocity(body.linearVelocity().scale(Math.max(0.0, 1.0 - 1.8 * dt)));
            if (Math.abs(input.steer()) > 0.05) {
                Vec3d fwd = body.orientation().forward();
                body.addTorque(fwd.scale(-input.steer() * config.body().mass() * 8.5));
            }
        }
        integrator.applyAerodynamicDrag(body, config.body());
        integrator.integrate(body, config.body(), dt);
        collisionResolver.resolve(body, config.body(), chassisCollider, world, eventBus);
        stabilizeAtRest(body, wheels, input);
        return drivetrain;
    }

    private void applyTireForces(
        RigidBodyState body,
        List<WheelState> wheels,
        DrivetrainOutput drivetrain,
        double dt
    ) {
        Vec3d chassisUp = body.orientation().up();
        Vec3d chassisForward = body.orientation().forward();

        for (int i = 0; i < wheels.size(); i++) {
            WheelState wheel = wheels.get(i);
            Quat4d steerRot = Quat4d.fromAxisAngle(chassisUp, wheel.steerAngle());
            Vec3d steeredForward = steerRot.rotate(chassisForward);

            Vec3d groundNormal = wheel.grounded() ? wheel.contactNormal() : chassisUp;
            Vec3d wheelForward = projectOntoPlane(steeredForward, groundNormal);
            Vec3d wheelLeft = groundNormal.cross(wheelForward).normalize();

            Vec3d contactVel = body.velocityAtPoint(wheel.contactPoint());
            double vLong = contactVel.dot(wheelForward);
            double vLat = contactVel.dot(wheelLeft);

            TireContext ctx = new TireContext(
                wheel.normalForce(),
                vLong,
                vLat,
                wheel.angularVelocity(),
                wheel.config().radius(),
                wheel.config().wheelInertia(),
                drivetrain.wheelDriveTorques()[i],
                drivetrain.wheelBrakeTorques()[i],
                wheel.config().gripFactor(),
                wheel.surface(),
                dt
            );

            TireForces forces = tireModel.calculate(ctx);
            wheel.setAngularVelocity(forces.nextWheelAngularVelocity());
            wheel.advanceSpin(dt);
            wheel.updateSlip(forces.slipRatio(), forces.slipAngle());

            if (wheel.grounded() && chassisUp.y() > 0.25) {
                Vec3d totalTireForce = wheelForward.scale(forces.longitudinalForce())
                    .addScaled(wheelLeft, forces.lateralForce());
                // GTA V vecCentreOfMassOffset: apply lateral & longitudinal tire forces near CoM height so acceleration/braking/cornering never flip the car
                Vec3d comProj = new Vec3d(wheel.contactPoint().x(), body.position().y() - 0.12, wheel.contactPoint().z());
                body.addForceAtPoint(totalTireForce, comProj);
            }
        }
        // GTA V RAGE handling: high-speed downforce + yaw stability + pitch/roll gyroscopic anti-flip damping
        Vec3d angVel = body.angularVelocity();
        if (chassisUp.y() > 0.35) {
            Vec3d chassisLeft = body.orientation().left();
            body.addTorque(chassisLeft.scale(-angVel.dot(chassisLeft) * 1100.0));
            body.addTorque(chassisForward.scale(-angVel.dot(chassisForward) * 1400.0));
            double pitch = body.orientation().pitchRadians(), roll = body.orientation().rollRadians();
            if (Math.abs(pitch) > 0.42) body.addTorque(chassisLeft.scale(-pitch * 4500.0));
            if (Math.abs(roll) > 0.38) body.addTorque(chassisForward.scale(-roll * 5200.0));
            double speedSq = body.linearVelocity().lengthSquared();
            if (speedSq > 4.0) {
                body.addForce(chassisUp.scale(-1.55 * speedSq));
                body.addTorque(chassisUp.scale(-angVel.dot(chassisUp) * 1650.0));
            }
        }
    }

    private Vec3d projectOntoPlane(Vec3d vector, Vec3d normal) {
        Vec3d projected = vector.sub(normal.scale(vector.dot(normal)));
        return projected.lengthSquared() > 1e-6 ? projected.normalize() : vector.normalize();
    }

    private void stabilizeAtRest(RigidBodyState body, List<WheelState> wheels, VehicleInput input) {
        if (input.throttle() > 0.01 || input.brake() > 0.01) return;
        int grounded = 0;
        for (WheelState wheel : wheels) if (wheel.grounded()) grounded++;
        if (grounded < 3) return;
        if (body.linearVelocity().length() < 0.03 && body.angularVelocity().length() < 0.03) {
            body.setLinearVelocity(body.linearVelocity().scale(0.85));
            body.setAngularVelocity(body.angularVelocity().scale(0.85));
            if (body.linearVelocity().length() < 0.005) {
                body.setLinearVelocity(Vec3d.ZERO);
                body.setAngularVelocity(Vec3d.ZERO);
            }
        }
    }
}

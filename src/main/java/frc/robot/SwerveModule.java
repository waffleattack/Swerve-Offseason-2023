package frc.robot;

import static frc.robot.constants.Constants.*;
import static java.lang.Math.*;

import com.ctre.phoenix.sensors.CANCoder;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.trajectory.TrapezoidProfile.State;
import edu.wpi.first.wpilibj.Timer;

public class SwerveModule {
	public PIDController pidTurn = new PIDController(0, 0, 0);
	public PIDController pidSpeed = new PIDController(0, 0, 0);

	public static final State zeroState = new State(0, 0);
	TrapezoidProfile profile = new TrapezoidProfile(SWERVE_TURN_TRAPEZOID, zeroState, zeroState);
	double profile_t0 = Timer.getFPGATimestamp();

	CANCoder encoder;
	CANSparkMax turn_motor, drive_motor;

	SwerveModuleState target_state;

	public SwerveModule(int port_cancoder, int port_turn_motor, int port_drive_motor) {
		encoder = new CANCoder(port_cancoder);
		turn_motor = new CANSparkMax(port_turn_motor, MotorType.kBrushless);
		drive_motor = new CANSparkMax(port_drive_motor, MotorType.kBrushless);
	}

	public double getDirection() {
		return encoder.getAbsolutePosition() / 180 * PI;
	}

	public double getAngularVelocity() { // in rad/s
		return turn_motor.getEncoder().getVelocity() * 2 * PI / 60 * L2_TURN_RATIO;
	}

	public double getVelocity() { // in m/s
		return drive_motor.getEncoder().getVelocity() * 2 * PI / 60 * L2_DRIVE_RATIO * SWERVE_WHEEL_RAD;
	}

	public void setState(SwerveModuleState state) {
		state = SwerveModuleState.optimize(state, new Rotation2d(getDirection()));
		target_state = state;
		double delta = getDirection() - state.angle.getRadians();
		delta = MathUtil.angleModulus(delta);
		profile = new TrapezoidProfile(SWERVE_TURN_TRAPEZOID, new State(delta, getAngularVelocity()), zeroState);
		profile_t0 = Timer.getFPGATimestamp();
	}

	public void periodic() {
		State turn_ref = profile.calculate(Timer.getFPGATimestamp() - profile_t0);

		drive_motor.setVoltage(DRIVE_KS * signum(target_state.speedMetersPerSecond)
				+ DRIVE_KV * target_state.speedMetersPerSecond
				+ pidSpeed.calculate(getVelocity(), target_state.speedMetersPerSecond));

		turn_motor.setVoltage(TURN_KS * signum(turn_ref.velocity)
				+ TURN_KV * turn_ref.velocity
				+ pidTurn.calculate(getDirection(), turn_ref.position + target_state.angle.getRadians()));
	}
}

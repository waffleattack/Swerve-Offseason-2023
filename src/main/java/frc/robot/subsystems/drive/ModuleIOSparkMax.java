package frc.robot.subsystems.drive;

import static frc.robot.constants.Constants.*;
import static frc.robot.constants.Ports.*;
import static java.lang.Math.*;

import com.ctre.phoenix6.hardware.CANcoder;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.RelativeEncoder;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;

/**
 * Module IO implementation for SparkMax drive motor controller, SparkMax turn motor controller (NEO
 * or NEO 550), and analog absolute encoder connected to the RIO
 *
 * <p>NOTE: This implementation should be used as a starting point and adapted to different hardware
 * configurations (e.g. If using a CANcoder, copy from "ModuleIOTalonFX")
 *
 * <p>To calibrate the absolute encoder offsets, point the modules straight (such that forward
 * motion on the drive motor will propel the robot forward) and copy the reported values from the
 * absolute encoders using AdvantageScope. These values are logged under
 * "/Drive/ModuleX/TurnAbsolutePositionRad"
 */
public class ModuleIOSparkMax implements ModuleIO {
	// Gear ratios for SDS MK4i L2, adjust as necessary
	private static final double DRIVE_GEAR_RATIO = 1/L2_DRIVE_RATIO;
	private static final double TURN_GEAR_RATIO = 1/L2_TURN_RATIO;

	private final CANSparkMax driveSparkMax;
	private final CANSparkMax turnSparkMax;

	private final RelativeEncoder driveEncoder;
	private final RelativeEncoder turnRelativeEncoder;
	private final CANcoder turnAbsoluteEncoder;

	private final boolean isTurnMotorInverted = true;
	private final Rotation2d absoluteEncoderOffset;

	public ModuleIOSparkMax(int index) {
		switch (index) {
			case 0:
				driveSparkMax = new CANSparkMax(NW_DRIVE_MOTOR, MotorType.kBrushless);
				turnSparkMax = new CANSparkMax(NW_TURN_MOTOR, MotorType.kBrushless);
				turnAbsoluteEncoder = new CANcoder(NW_ENCODER);
				absoluteEncoderOffset = NW_ENCODER_OFFSET; // MUST BE CALIBRATED
				break;
			case 1:
				driveSparkMax = new CANSparkMax(NE_DRIVE_MOTOR, MotorType.kBrushless);
				turnSparkMax = new CANSparkMax(NE_TURN_MOTOR, MotorType.kBrushless);
				turnAbsoluteEncoder = new CANcoder(NE_ENCODER);
				absoluteEncoderOffset = NE_ENCODER_OFFSET; // MUST BE CALIBRATED
				break;
			case 2:
				driveSparkMax = new CANSparkMax(SW_DRIVE_MOTOR, MotorType.kBrushless);
				turnSparkMax = new CANSparkMax(SW_TURN_MOTOR, MotorType.kBrushless);
				turnAbsoluteEncoder = new CANcoder(SW_ENCODER);
				absoluteEncoderOffset = NE_ENCODER_OFFSET; // MUST BE CALIBRATED
				break;
			case 3:
				driveSparkMax = new CANSparkMax(SE_DRIVE_MOTOR, MotorType.kBrushless);
				turnSparkMax = new CANSparkMax(SE_TURN_MOTOR, MotorType.kBrushless);
				turnAbsoluteEncoder = new CANcoder(SE_ENCODER);
				absoluteEncoderOffset = SE_ENCODER_OFFSET; // MUST BE CALIBRATED
				break;
			default:
				throw new RuntimeException("Invalid module index");
		}

		driveSparkMax.restoreFactoryDefaults();
		turnSparkMax.restoreFactoryDefaults();

		driveSparkMax.setCANTimeout(250);
		turnSparkMax.setCANTimeout(250);

		driveEncoder = driveSparkMax.getEncoder();
		driveEncoder.setPosition(0.0);

		turnRelativeEncoder = turnSparkMax.getEncoder();

		turnSparkMax.setInverted(isTurnMotorInverted);
		driveSparkMax.setSmartCurrentLimit(40);
		turnSparkMax.setSmartCurrentLimit(30);
		driveSparkMax.enableVoltageCompensation(12.0);
		turnSparkMax.enableVoltageCompensation(12.0);

		turnRelativeEncoder.setPosition(0.0);
		turnRelativeEncoder.setMeasurementPeriod(10);
		turnRelativeEncoder.setAverageDepth(2);

		driveSparkMax.setCANTimeout(0);
		turnSparkMax.setCANTimeout(0);

		driveSparkMax.burnFlash();
		turnSparkMax.burnFlash();
	}

	@Override
	public void updateInputs(ModuleIOInputs inputs) {
		inputs.drivePositionRad = driveEncoder.getPosition() * 2 * PI / DRIVE_GEAR_RATIO;
		inputs.driveVelocityRadPerSec =
				Units.rotationsPerMinuteToRadiansPerSecond(driveEncoder.getVelocity()) / DRIVE_GEAR_RATIO;
		inputs.driveAppliedVolts = driveSparkMax.getAppliedOutput() * driveSparkMax.getBusVoltage();
		inputs.driveCurrentAmps = new double[] {driveSparkMax.getOutputCurrent()};

		inputs.turnAbsolutePosition = new Rotation2d(Units.rotationsToRadians(
						turnAbsoluteEncoder.getAbsolutePosition().getValueAsDouble()))
				.minus(absoluteEncoderOffset);
		inputs.turnAbsolutePositionRad = inputs.turnAbsolutePosition.getRadians();
		inputs.turnPosition = Rotation2d.fromRotations(turnRelativeEncoder.getPosition() / TURN_GEAR_RATIO);
		inputs.turnVelocityRadPerSec =
				Units.rotationsPerMinuteToRadiansPerSecond(turnRelativeEncoder.getVelocity()) / TURN_GEAR_RATIO;
		inputs.turnAppliedVolts = turnSparkMax.getAppliedOutput() * turnSparkMax.getBusVoltage();
		inputs.turnCurrentAmps = new double[] {turnSparkMax.getOutputCurrent()};
	}

	@Override
	public void setDriveVoltage(double volts) {
		driveSparkMax.setVoltage(volts);
	}

	@Override
	public void setTurnVoltage(double volts) {
		turnSparkMax.setVoltage(volts);
	}

	@Override
	public void setDriveBrakeMode(boolean enable) {
		driveSparkMax.setIdleMode(enable ? IdleMode.kBrake : IdleMode.kCoast);
	}

	@Override
	public void setTurnBrakeMode(boolean enable) {
		turnSparkMax.setIdleMode(enable ? IdleMode.kBrake : IdleMode.kCoast);
	}
}

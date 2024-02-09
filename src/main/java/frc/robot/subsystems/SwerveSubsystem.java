package frc.robot.subsystems;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.SwerveDrivetrainConstants;
import swervelib.SwerveController;
import swervelib.SwerveDrive;
import swervelib.parser.SwerveDriveConfiguration;
import swervelib.parser.SwerveParser;
import swervelib.telemetry.SwerveDriveTelemetry;

import java.io.File;
import java.io.IOException;
import java.util.function.DoubleSupplier;

public class SwerveSubsystem extends SubsystemBase {
    /**
     * Swerve Drive object
     */
    private final SwerveDrive swerveDrive;

    /**
     * Initializes {@link SwerveDrive} from the directory path provided
     *
     * @param directory The directory to retrieve the configuration from
     */
    public SwerveSubsystem(File directory) {
        // Configuring telemetry before creating the SwerveDrive avoids unnecessary object creation
        SwerveDriveTelemetry.verbosity = SwerveDriveTelemetry.TelemetryVerbosity.HIGH;

        try {
            swerveDrive = new SwerveParser(directory)
                    .createSwerveDrive(SwerveDrivetrainConstants.maximumSpeed);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read swerve configuration", e);
        }
    }

    /**
     * Gets the current velocity (x, y and omega) of the robot
     *
     * @return A {@link ChassisSpeeds} object of the current velocity
     */
    public ChassisSpeeds getRobotVelocity() {
        var actualVelocity = swerveDrive.getRobotVelocity();
        System.out.println("Requested robot velocity: " + actualVelocity);
        var inverted = new ChassisSpeeds(
                -actualVelocity.vxMetersPerSecond,
                -actualVelocity.vyMetersPerSecond,
                actualVelocity.omegaRadiansPerSecond);
        System.out.println("Inverting to: " + inverted);
        return inverted;
    }

    /**
     * Set chassis speeds with closed-loop velocity control.
     *
     * @param chassisSpeeds Chassis Speeds to set.
     */
    public void setChassisSpeeds(ChassisSpeeds chassisSpeeds) {
        System.out.println("Requested to set ChassisSpeeds to: " + chassisSpeeds);
        var inverted = new ChassisSpeeds(
                -chassisSpeeds.vxMetersPerSecond,
                -chassisSpeeds.vyMetersPerSecond,
                chassisSpeeds.omegaRadiansPerSecond);
        System.out.println("Inverting to set ChassisSpeeds to: " + chassisSpeeds);
        swerveDrive.setChassisSpeeds(inverted);
    }

    /**
     * Command to drive the robot using translative values and heading as a setpoint.
     *
     * @param translationX Translation in the X direction. Cubed for smoother controls.
     * @param translationY Translation in the Y direction. Cubed for smoother controls.
     * @param headingX     Heading X to calculate angle of the joystick.
     * @param headingY     Heading Y to calculate angle of the joystick.
     * @return Drive command.
     */
    public Command driveCommand(DoubleSupplier translationX, DoubleSupplier translationY, DoubleSupplier headingX,
                                DoubleSupplier headingY) {
        swerveDrive.setHeadingCorrection(true); // Normally you would want heading correction for this kind of control.
        return run(() -> {
            double xInput = Math.pow(translationX.getAsDouble(), 3); // Smooth control out
            double yInput = Math.pow(translationY.getAsDouble(), 3); // Smooth control out
            // Make the robot move
            driveFieldOriented(swerveDrive.swerveController.getTargetSpeeds(xInput, yInput,
                    headingX.getAsDouble(),
                    headingY.getAsDouble(),
                    getHeading().getRadians(),
                    swerveDrive.getMaximumVelocity()));
        }).finallyDo(() -> swerveDrive.setHeadingCorrection(false));
    }

    /**
     * Command to drive the robot using translative values and heading as a setpoint.
     *
     * @param translationX Translation in the X direction.
     * @param translationY Translation in the Y direction.
     * @param rotation     Rotation as a value between [-1, 1] converted to radians.
     * @return Drive command.
     */
    public Command simDriveCommand(DoubleSupplier translationX, DoubleSupplier translationY, DoubleSupplier rotation) {
        // swerveDrive.setHeadingCorrection(true); // Normally you would want heading correction for this kind of control.
        return run(() -> {
            // Make the robot move
            driveFieldOriented(swerveDrive.swerveController.getTargetSpeeds(translationX.getAsDouble(),
                    translationY.getAsDouble(),
                    rotation.getAsDouble() * Math.PI,
                    getHeading().getRadians(),
                    swerveDrive.getMaximumVelocity()));
        });
    }

    /**
     * Command to drive the robot using translative values and heading as angular velocity.
     *
     * @param translationX     Translation in the X direction. Cubed for smoother controls.
     * @param translationY     Translation in the Y direction. Cubed for smoother controls.
     * @param angularRotationX Angular velocity of the robot to set. Cubed for smoother controls.
     * @return Drive command.
     */
    public Command driveCommand(DoubleSupplier translationX, DoubleSupplier translationY, DoubleSupplier angularRotationX) {
        return run(() -> {
            // Make the robot move
            swerveDrive.drive(new Translation2d(Math.pow(translationX.getAsDouble(), 3) * swerveDrive.getMaximumVelocity(),
                            Math.pow(translationY.getAsDouble(), 3) * swerveDrive.getMaximumVelocity()),
                    Math.pow(angularRotationX.getAsDouble(), 3) * swerveDrive.getMaximumAngularVelocity(),
                    true,
                    false);
        });
    }

    /**
     * The primary method for controlling the drivebase. Takes a {@link Translation2d} and a rotation rate.
     * Can also be robot- or field-relative, affecting how the translation vector is applied.
     *
     * @param translation {@link Translation2d} that is the commanded linear velocity of the robot, in meters per
     *    *                      second. In robot-relative mode, positive x is towards the bow (front) and positive y is
     *    *                      towards port (left). In field-relative mode, positive x is away from the alliance wall
     *    *                      (field North) and positive y is towards the left wall when looking through the driver station
     *    *                      glass (field West).
     * @param rotation The robot's angular rotation rate, in radians (degrees * (π/180)) per second. Unaffected by
     *                 robot/field relativity.
     * @param fieldRelative Drive mode. True for field-relative, false for robot-relative.
     */
    public void drive(Translation2d translation, double rotation, boolean fieldRelative) {
        swerveDrive.drive(
                translation,
                rotation,
                fieldRelative,
                false);
    }

    /**
     * Drives the robot given a chassis field-oriented velocity
     *
     * @param velocity Velocity according to the field
     */
    public void driveFieldOriented(ChassisSpeeds velocity) {
        swerveDrive.driveFieldOriented(velocity);
    }

    /**
     * Drive according to the chassis robot oriented velocity.
     *
     * @param velocity Robot oriented {@link ChassisSpeeds}
     */
    public void drive(ChassisSpeeds velocity) {
        swerveDrive.drive(velocity);
    }

    /**
     * Gets the heading (yaw) from the IMU
     * @return The heading, as a {@link Rotation2d}
     */
    public Rotation2d getHeading() {
        return swerveDrive.getOdometryHeading();
    }

    /**
     * Gets the current pose (position and rotation) of the robot, as reported by odometry.
     *
     * @return The robot's pose
     */
    public Pose2d getPose() {
        var internalPose = swerveDrive.getPose();
        System.out.println("Requested pose is " + internalPose);
        var inverted = new Pose2d(internalPose.getTranslation().unaryMinus(), internalPose.getRotation());
        System.out.println("Inverting requested pose to: " + inverted);
        return inverted;
    }

    /**
     * Get the chassis speeds based on controller input of 1 joystick and one angle. Control the robot at an offset of
     * 90deg.
     *
     * @param xInput X joystick input for the robot to move in the X direction.
     * @param yInput Y joystick input for the robot to move in the Y direction.
     * @param angle  The angle in as a {@link Rotation2d}.
     * @return {@link ChassisSpeeds} which can be sent to th Swerve Drive.
     */
    public ChassisSpeeds getTargetSpeeds(double xInput, double yInput, Rotation2d angle) {
        xInput = Math.pow(xInput, 3);
        yInput = Math.pow(yInput, 3);
        return swerveDrive.swerveController.getTargetSpeeds(xInput,
                yInput,
                angle.getRadians(),
                getHeading().getRadians(),
                SwerveDrivetrainConstants.maximumSpeed);
    }

    /**
     * Gets the current field-relative velocity (x, y and omega) of the robot
     *
     * @return A ChassisSpeeds object of the current field-relative velocity
     */
    public ChassisSpeeds getFieldVelocity() {
        return swerveDrive.getFieldVelocity();
    }

    /**
     * Resets the gyro angle to zero and resets odometry to the same position, but facing toward 0.
     */
    public void zeroGyro() {
        swerveDrive.zeroGyro();
    }

    /**
     * Resets odometry to the given pose. Gyro angle and module positions do not need to be reset when calling this
     * method.  However, if either gyro angle or module position is reset, this must be called in order for odometry to
     * keep working.
     *
     * @param initialHolonomicPose The pose to set the odometry to
     */
    public void resetOdometry(Pose2d initialHolonomicPose) {
        System.out.println("Resetting Odometry: " + initialHolonomicPose.toString());
        var inverted = new Pose2d(initialHolonomicPose.getTranslation().unaryMinus(), initialHolonomicPose.getRotation());
        System.out.println("Inverting Odometry reset request to: " + inverted);
        swerveDrive.resetOdometry(inverted);
    }

    /**
     * Gets the {@link SwerveController} used by the swerve drive
     * @return The {@link SwerveController} used by the swerve drive
     */
    public SwerveController getSwerveController() {
        return swerveDrive.getSwerveController();
    }

    /**
     * Gets the maximum velocity achievable by the robot
     * @return The maximum velocity, in meters per second
     */
    public double getMaximumVelocity() {
        return swerveDrive.getMaximumVelocity();
    }

    public double getMaximumAngularVelocity() {
        return swerveDrive.getMaximumAngularVelocity();
    }

    /**
     * Get the {@link SwerveDriveConfiguration} object.
     *
     * @return The {@link SwerveDriveConfiguration} fpr the current drive.
     */
    public SwerveDriveConfiguration getSwerveDriveConfiguration() {
        return swerveDrive.swerveDriveConfiguration;
    }

    public SwerveDrive getSwerveDrive() {
        return swerveDrive;
    }

    /**
     * Get the chassis speeds based on controller input of 2 joysticks. One for speeds in which direction. The other for
     * the angle of the robot.
     *
     * @param xInput   X joystick input for the robot to move in the X direction.
     * @param yInput   Y joystick input for the robot to move in the Y direction.
     * @param headingX X joystick which controls the angle of the robot.
     * @param headingY Y joystick which controls the angle of the robot.
     * @return {@link ChassisSpeeds} which can be sent to th Swerve Drive.
     */
    public ChassisSpeeds getTargetSpeeds(double xInput, double yInput, double headingX, double headingY) {
        xInput = Math.pow(xInput, 3);
        yInput = Math.pow(yInput, 3);
        return swerveDrive.swerveController.getTargetSpeeds(xInput,
                yInput,
                headingX,
                headingY,
                getHeading().getRadians(),
                SwerveDrivetrainConstants.maximumSpeed);
    }
}

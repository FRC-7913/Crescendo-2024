package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.SwerveDrivetrainConstants;
import swervelib.SwerveDrive;
import swervelib.parser.SwerveParser;
import swervelib.telemetry.SwerveDriveTelemetry;

import java.io.File;
import java.io.IOException;

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
}

package frc.robot.subsystems;

import com.revrobotics.CANSparkLowLevel;
import com.revrobotics.CANSparkMax;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class IntakeSubsystem extends SubsystemBase {
    private final CANSparkMax intake;
    private final CANSparkMax shooterTop;
    private final CANSparkMax shooterBottom;

    private double intakeTargetSpeed;
    private double shooterTargetSpeed;

    public IntakeSubsystem() {
        intake = new CANSparkMax(13, CANSparkLowLevel.MotorType.kBrushless);
        intake.setInverted(false);
        intake.setSmartCurrentLimit(30);

        shooterTop = new CANSparkMax(14, CANSparkLowLevel.MotorType.kBrushless);
        intake.setInverted(false);
        intake.setSmartCurrentLimit(50);

        shooterBottom = new CANSparkMax(15, CANSparkLowLevel.MotorType.kBrushless);
        intake.setInverted(false);
        intake.setSmartCurrentLimit(50);

        intake.burnFlash();
        shooterTop.burnFlash();
        shooterBottom.burnFlash();
    }

    public void setIntakeSpeed(double speed) {
        intakeTargetSpeed = speed;
    }

    public void setShooterSpeed(double speed) {
        shooterTargetSpeed = speed;
    }

    @Override
    public void periodic() {
        intake.set(intakeTargetSpeed);

        shooterTop.set(shooterTargetSpeed);
        shooterBottom.set(shooterTargetSpeed);
    }
}

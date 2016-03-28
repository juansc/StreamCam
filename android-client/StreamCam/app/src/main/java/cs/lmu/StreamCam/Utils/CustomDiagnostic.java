package cs.lmu.StreamCam.Utils;

/**
 * Created by juanscarrillo on 3/4/16.
 */
public class CustomDiagnostic {

    private boolean passed;
    private String message;

    public CustomDiagnostic() {

    }

    public CustomDiagnostic(boolean passed, String message) {
        this.passed = passed;
        this.message = message;
    }

    public boolean hasPassed() {
        return this.passed;
    }

    public String getMessage() {
        return this.message;
    }
}

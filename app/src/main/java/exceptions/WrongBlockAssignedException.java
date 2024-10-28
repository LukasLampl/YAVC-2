package exceptions;

public class WrongBlockAssignedException extends Exception {
	private static final long serialVersionUID = -997523861183991250L;

	public WrongBlockAssignedException(String message) {
		super(message);
	}
}

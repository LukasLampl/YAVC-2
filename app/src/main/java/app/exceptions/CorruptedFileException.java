package app.exceptions;

public class CorruptedFileException extends Exception {
	private static final long serialVersionUID = 4119688490860014192L;

	public CorruptedFileException(String message) {
		super(message);
	}
}

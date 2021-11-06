package se.kth.nada.bastianf.eyephone; 

public class IllegalPasswordException extends Exception {
	private static final long serialVersionUID = -603642438772582000L;

	public IllegalPasswordException() {
	}

	public IllegalPasswordException(String msg) {
		super(msg);
	}
}
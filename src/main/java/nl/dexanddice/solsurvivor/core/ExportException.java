package nl.dexanddice.solsurvivor.core;

/** Why the arena cannot be exported now: the message says so in words the player can act on. */
public class ExportException extends Exception
{
	public ExportException(String message)
	{
		super(message);
	}
}

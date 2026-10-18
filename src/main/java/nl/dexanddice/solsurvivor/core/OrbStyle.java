package nl.dexanddice.solsurvivor.core;

/** A Manticore orb's attack style, with its letter in the LoS link's orb orders ("r", "m", "Mrm" and so on). */
public enum OrbStyle
{
	MAGIC('m'),
	RANGED('r'),
	MELEE('M');

	private final char code;

	OrbStyle(char code)
	{
		this.code = code;
	}

	public char getCode()
	{
		return code;
	}
}

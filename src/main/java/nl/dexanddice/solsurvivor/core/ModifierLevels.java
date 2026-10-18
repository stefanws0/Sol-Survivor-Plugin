package nl.dexanddice.solsurvivor.core;

/** Reads a modifier's tier from the game: the value of its level varbit, 0 while it is not taken. */
@FunctionalInterface
public interface ModifierLevels
{
	int level(Modifier modifier);
}

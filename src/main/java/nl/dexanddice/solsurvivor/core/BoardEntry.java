package nl.dexanddice.solsurvivor.core;

import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * The site's speed leaderboard (its src/edge/board.ts and docs/leaderboard.md), with "Leaderboard" and "Share wave logs"
 * on: once the site has taken a wave's log, the plugin asks for that wave to go on the boards under the player's
 * RuneScape name, or the Leaderboard name they typed, by the game's own wave clock, which the log carries. The request names the run's id, which only the plugin knows, so
 * that nobody else can add waves to the player's run, and the player's key: a random secret made once and kept in the
 * settings, which holds the name and the times as the player's and lets "Remove my times" take them all off. The
 * RuneLite layer sends the requests; this class makes their bodies and says the site's answers in words. It holds no
 * RuneLite import, as the rest of the core.
 */
public final class BoardEntry
{
	/** The bodies' type: JSON. */
	public static final String TYPE = "application/json";

	/**
	 * A name the site takes (its NAME): 1 to 12 letters, digits, spaces, hyphens or underscores, beginning and ending with
	 * a letter or a digit, as RuneScape's names are.
	 */
	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9 _-]{0,10}[A-Za-z0-9])?");

	/** A run's id or a key: 32 hexadecimal digits (the site's SECRET). */
	private static final Pattern SECRET = Pattern.compile("[a-f0-9]{32}");

	private BoardEntry()
	{
	}

	/** The site's endpoint that puts a wave on the boards. */
	public static String endpoint(String site)
	{
		return ReplayLink.origin(site) + "/api/board/waves";
	}

	/** The site's endpoint that takes all of a player's times off. */
	public static String removeEndpoint(String site)
	{
		return ReplayLink.origin(site) + "/api/board/remove";
	}

	/** The leaderboard's page. */
	public static String page(String site)
	{
		return ReplayLink.origin(site) + "/leaderboard";
	}

	/**
	 * The name as the site takes it, `typed` with the spaces at its ends dropped, or null when it is blank or not a name
	 * the site takes.
	 */
	public static String name(String typed)
	{
		String name = typed == null ? "" : typed.trim();
		return NAME.matcher(name).matches() ? name : null;
	}

	/**
	 * The player's RuneScape name as the site takes it, or null for one it does not: the game writes the spaces in a name
	 * as no-break spaces, which go up as plain ones.
	 */
	public static String fromPlayer(String rsn)
	{
		return rsn == null ? null : name(rsn.replace('\u00A0', ' '));
	}

	/** Whether `typed` is no name at all, which goes up as the RuneScape name, rather than one the site does not take. */
	public static boolean isBlank(String typed)
	{
		return typed == null || typed.trim().isEmpty();
	}

	/** Whether `key` is a key the plugin made. */
	public static boolean isKey(String key)
	{
		return key != null && SECRET.matcher(key).matches();
	}

	/** A new key: 32 random hexadecimal digits. */
	public static String newKey(Random random)
	{
		byte[] bytes = new byte[16];
		random.nextBytes(bytes);
		StringBuilder key = new StringBuilder(32);
		for (byte b : bytes)
		{
			key.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
		}
		return key.toString();
	}

	/**
	 * The body that asks for wave `wave` of the run `run` to go on the boards under `name` (a name() that is not null) for
	 * the player whose key is `key`. Each is a run's id, a wave, a name or a key, which hold no character JSON escapes.
	 */
	public static String body(String run, int wave, String name, String key)
	{
		return "{\"run\":\"" + run + "\",\"wave\":" + wave + ",\"name\":\"" + name + "\",\"key\":\"" + key + "\"}";
	}

	/** The body that takes all of the times of the player whose key is `key` off the boards. */
	public static String removeBody(String key)
	{
		return "{\"key\":\"" + key + "\"}";
	}

	/** A time in ticks as the game's clock shows it, minutes, seconds and hundredths: a tick is 0.6 seconds. */
	public static String gameTime(int ticks)
	{
		int hundredths = ticks * 60;
		int seconds = hundredths / 100;
		return String.format(Locale.ROOT, "%d:%02d.%02d", seconds / 60, seconds % 60, hundredths % 100);
	}

	/** A place as it is said: 1st, 2nd, 3rd, 4th, 11th, 12th, 13th, 21st. */
	public static String place(int place)
	{
		int tens = place % 100;
		String suffix = tens >= 11 && tens <= 13 ? "th"
			: place % 10 == 1 ? "st"
			: place % 10 == 2 ? "nd"
			: place % 10 == 3 ? "rd"
			: "th";
		return place + suffix;
	}

	/** The site's answer to a wave, as its JSON holds it (the site's Entered); a figure the answer leaves out is null. */
	public static final class Answer
	{
		/** "entered", "held", "not counted" or "taken". */
		public String board;
		/** The name the player's times show under, or null when they have none yet. */
		public String name;
		public Integer wave;
		public Integer time;
		public Integer place;
		public Integer runTime;
		public Integer runPlace;
	}

	/** What the site's answer means for the player, in words for the panel, or null for one this plugin does not know. */
	public static String said(Answer answer, String asked)
	{
		if (answer == null || answer.board == null || answer.wave == null)
		{
			return null;
		}
		String wave = "wave " + answer.wave;
		switch (answer.board)
		{
			case "entered":
				return "Leaderboard: " + wave + (answer.time == null ? "" : " in " + gameTime(answer.time))
					+ (answer.place == null ? "" : ", " + place(answer.place)) + "."
					+ (answer.runTime == null ? "" : " Your full run: " + gameTime(answer.runTime)
					+ (answer.runPlace == null ? "" : ", " + place(answer.runPlace)) + ".");
			case "held":
				return "Leaderboard: " + wave + (answer.time == null ? "" : " in " + gameTime(answer.time))
					+ " is held for a look before it shows.";
			case "not counted":
				return "Leaderboard: " + wave + " is not on it, as the game's wave clock did not time it to its end.";
			case "taken":
				return answer.name == null
					? "Leaderboard: the name " + asked + " is another player's. Pick another in the plugin's settings."
					: "Leaderboard: the name " + asked + " is another player's, so " + wave + " went up as " + answer.name
					+ (answer.place == null ? "" : ", " + place(answer.place)) + ".";
			default:
				return null;
		}
	}
}

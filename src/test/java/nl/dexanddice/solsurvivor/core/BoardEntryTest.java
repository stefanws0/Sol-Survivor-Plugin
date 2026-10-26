package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Random;
import org.junit.Test;

/**
 * The leaderboard's requests and answers (the site's src/edge/board.ts): a name the site takes, a key made once, the
 * bodies it reads, and its answers said in words, with times as the game's clock shows them.
 */
public class BoardEntryTest
{
	private static final String RUN = "0123456789abcdef0123456789abcdef";
	private static final String KEY = "fedcba9876543210fedcba9876543210";

	@Test
	public void aNameIsOneTheSiteTakesWithTheSpacesAtItsEndsDropped()
	{
		assertEquals("Tile Hugger", BoardEntry.name("  Tile Hugger "));
		assertEquals("Zuk_2-go", BoardEntry.name("Zuk_2-go"));
		assertEquals("a", BoardEntry.name("a"));
		// Blank leaves the leaderboard off; 13 letters, other characters, or a hyphen at an end are refused.
		assertNull(BoardEntry.name(""));
		assertNull(BoardEntry.name(null));
		assertTrue(BoardEntry.isBlank("   "));
		assertNull(BoardEntry.name("Pillar Hugger"));
		assertFalse(BoardEntry.isBlank("Pillar Hugger"));
		assertNull(BoardEntry.name("Tile\"Hugger"));
		assertNull(BoardEntry.name("-Tile"));
		assertNull(BoardEntry.name("Sol Heredit!"));
	}

	@Test
	public void aRuneScapeNameGoesUpWithItsNoBreakSpacesPlain()
	{
		assertEquals("Sir Cullzaag", BoardEntry.fromPlayer("Sir\u00A0Cullzaag"));
		assertEquals("Zezima", BoardEntry.fromPlayer("Zezima"));
		assertNull(BoardEntry.fromPlayer(null));
		assertNull(BoardEntry.fromPlayer(""));
	}

	@Test
	public void aKeyIs32RandomHexDigitsAndEachIsNew()
	{
		Random random = new Random(7);
		String key = BoardEntry.newKey(random);
		assertTrue(key, BoardEntry.isKey(key));
		assertNotEquals(key, BoardEntry.newKey(random));
		assertFalse(BoardEntry.isKey(""));
		assertFalse(BoardEntry.isKey(KEY.toUpperCase()));
	}

	@Test
	public void theBodiesAreTheJsonTheSiteReads()
	{
		assertEquals("{\"run\":\"" + RUN + "\",\"wave\":7,\"name\":\"Tile Hugger\",\"key\":\"" + KEY + "\"}",
			BoardEntry.body(RUN, 7, "Tile Hugger", KEY));
		assertEquals("{\"key\":\"" + KEY + "\"}", BoardEntry.removeBody(KEY));
		assertEquals("https://solsurvivor.app/api/board/waves", BoardEntry.endpoint("https://solsurvivor.app/"));
		assertEquals("https://solsurvivor.app/api/board/remove", BoardEntry.removeEndpoint("https://solsurvivor.app/"));
		assertEquals("https://solsurvivor.app/leaderboard", BoardEntry.page("https://solsurvivor.app/"));
	}

	@Test
	public void timesAreTheGameClocksAndPlacesAreSaidAsWords()
	{
		assertEquals("0:00.00", BoardEntry.gameTime(0));
		assertEquals("1:00.60", BoardEntry.gameTime(101));
		assertEquals("13:30.60", BoardEntry.gameTime(1351));
		assertEquals("1st 2nd 3rd 4th 11th 12th 13th 21st 22nd 101st 111th",
			String.join(" ", BoardEntry.place(1), BoardEntry.place(2), BoardEntry.place(3), BoardEntry.place(4), BoardEntry.place(11),
				BoardEntry.place(12), BoardEntry.place(13), BoardEntry.place(21), BoardEntry.place(22), BoardEntry.place(101), BoardEntry.place(111)));
	}

	private static BoardEntry.Answer answer(String board, String name, int wave, Integer time, Integer place)
	{
		BoardEntry.Answer a = new BoardEntry.Answer();
		a.board = board;
		a.name = name;
		a.wave = wave;
		a.time = time;
		a.place = place;
		return a;
	}

	@Test
	public void theSitesAnswersAreSaidInWords()
	{
		assertEquals("Leaderboard: wave 7 in 15:00.00, 3rd.", BoardEntry.said(answer("entered", "Tile Hugger", 7, 1500, 3), "Tile Hugger"));
		BoardEntry.Answer whole = answer("entered", "Tile Hugger", 12, 300, 1);
		whole.runTime = 1500;
		whole.runPlace = 2;
		assertEquals("Leaderboard: wave 12 in 3:00.00, 1st. Your full run: 15:00.00, 2nd.", BoardEntry.said(whole, "Tile Hugger"));
		assertEquals("Leaderboard: wave 4 in 1:00.60 is held for a look before it shows.", BoardEntry.said(answer("held", "Tile Hugger", 4, 101, null), "Tile Hugger"));
		assertEquals("Leaderboard: wave 3 is not on it, as the game's wave clock did not time it to its end.",
			BoardEntry.said(answer("not counted", "Tile Hugger", 3, null, null), "Tile Hugger"));
		assertEquals("Leaderboard: the name Speedy is another player's. Pick another in the plugin's settings.",
			BoardEntry.said(answer("taken", null, 2, null, null), "Speedy"));
		assertEquals("Leaderboard: the name Speedy is another player's, so wave 2 went up as Tile Hugger, 5th.",
			BoardEntry.said(answer("taken", "Tile Hugger", 2, 80, 5), "Speedy"));
		// An answer this plugin does not know says nothing.
		assertNull(BoardEntry.said(answer("someday", "Tile Hugger", 2, 80, 5), "Tile Hugger"));
		assertNull(BoardEntry.said(new BoardEntry.Answer(), "Tile Hugger"));
		assertNull(BoardEntry.said(null, "Tile Hugger"));
	}
}

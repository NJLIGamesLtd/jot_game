//**************************************************************************************************
// Author: Jot Russell (copyright 2017, all rights reserved)

package com.jot.crossword.test;

import com.jot.crossword.BestWord;
import com.jot.crossword.Game;
import com.jot.crossword.Jot;
import com.jot.crossword.Scrabble;
import com.jot.decomp.TreeWords;
import com.jot.decomp.Words;

import org.junit.Test;
import static org.junit.Assert.*;

//**************************************************************************************************

public class BestWordTest
{
	private static String s_strTreeFile = new String("data/crosswords.jot");

	//**********************************************************************************************
	@Test
	public void
	testBestWordJot()
	{
		BestWord bestWord;
		Game game;
		Words words;

		try
		{
			words = new TreeWords(s_strTreeFile);
			game = new Jot(words);
			
			if (game.getBestWord(0) != 0)
			{
				System.out.println(game.toString());
			}
			else
			{
				System.out.println("** No words found **");
			}
		}
		catch (Exception e)
		{
			fail("Unable to create objects: " + e.getMessage());
		}
	}

	//**********************************************************************************************
	@Test
	public void
	testBestWordScrabble()
	{
		BestWord bestWord;
		Game game;
		Words words;

		try
		{
			words = new TreeWords(s_strTreeFile);
			game = new Scrabble(words);

			if (game.getBestWord(0) != 0)
			{
				System.out.println(game.toString());
			}
			else
			{
				System.out.println("** No words found **");
			}
		}
		catch (Exception e)
		{
			fail("Unable to create objects: " + e.getMessage());
		}
	}
}

//**************************************************************************************************

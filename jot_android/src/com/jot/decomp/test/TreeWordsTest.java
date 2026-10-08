//**************************************************************************************************
// Author: Jot Russell (copyright 2014, all rights reserved)

package com.jot.decomp.test;

import com.jot.decomp.TreeWords;
import com.jot.decomp.Words;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.Test;
import static org.junit.Assert.*;

//**************************************************************************************************

public class TreeWordsTest
{
	private static String[] s_astrTreeFiles =
	{
		"data/test1.jot",
		"data/test2.jot",
		"data/test3.jot",
	};
	private static String[] s_astrWordFiles =
	{
		"data/test1.words",
		"data/test2.words",
		"data/test3.words",
	};

	private int m_iTest;
	private Words m_words;

	//**********************************************************************************************
	private void
	testTreeAccess()
	{
		try
		{
			m_words = new TreeWords(s_astrTreeFiles[m_iTest]);
		}
		catch (Exception e)
		{
			fail("Unable to create tree object: " + e.getMessage());
			m_words = null;
		}
	}

	//**********************************************************************************************
	@Test
	public void
	testTreeAccess0()
	{
		m_iTest = 0;
		testTreeAccess();
	}

	//**********************************************************************************************
	@Test
	public void
	testTreeAccess1()
	{
		m_iTest = 1;
		testTreeAccess();
	}

	//**********************************************************************************************
	@Test
	public void
	testTreeAccess2()
	{
		m_iTest = 2;
		testTreeAccess();
	}

	//**********************************************************************************************
	private String
	readNextWord(InputStream isFile) throws IOException
	{
		ArrayList<Byte> alChars = new ArrayList<>();
		int iChar;
		while ((iChar = isFile.read()) >= 0)
		{
			if (iChar == 0 ||
				iChar == '\n' ||
				iChar == '\r')
			{
				if (alChars.size() > 0)
				{
					break;
				}
				continue;
			}
			alChars.add((byte)(iChar & 0xFF));
		}
		
		int nChars;
		if ((nChars = alChars.size()) == 0)
		{
			return null;
		}
		
		char[] aChars = new char[nChars];
		for (int i = 0; i < nChars; ++i)
		{
			iChar = alChars.get(i) & 0xFF;
			aChars[i] = (char)iChar;
		}
		return new String(aChars);
	}
	
	//**********************************************************************************************
	private void
	testTreeDump()
	{
		int iWord = 0;
		
		try
		{
			// Gain access to the tree data.
			testTreeAccess();
			if (m_words == null)
			{
				return;
			}
			
			// Open read access to the expected words.
			InputStream isWords = new FileInputStream(s_astrWordFiles[m_iTest]);
			String strFileWord = readNextWord(isWords);
			
			int nWords = m_words.getWordCount();
			while (++iWord <= nWords)
			{
				String strTreeWord = m_words.getWord(iWord, false);

				// Make sure we have each.
				if (strTreeWord == null)
				{
					fail("No " + Integer.toString(iWord) + "th word found within tree.");
				}
				if (strFileWord == null)
				{
					fail("No " + Integer.toString(iWord) + "th word found within test file.");
				}
				if (!strTreeWord.equals(strFileWord))
				{
					fail(Integer.toString(iWord) + "th word does not match: " +
						strTreeWord + " != " + strFileWord);
				}

				strFileWord = readNextWord(isWords);
			}

			if (strFileWord != null)
			{
				fail("No " + Integer.toString(iWord) + "th word found within tree.");
			}
			
			isWords.close();
		}
		catch (Exception e)
		{
			System.out.println("Exception triggered on word " + Integer.toString(iWord) +
				": " + e);
			fail("Failed to dump tree: " + e.getMessage());
		}
	}

	//**********************************************************************************************
	@Test
	public void
	testTreeDump0()
	{
		m_iTest = 0;
		testTreeDump();
	}

	//**********************************************************************************************
	@Test
	public void
	testTreeDump1()
	{
		m_iTest = 1;
		testTreeDump();
	}
	
	//**********************************************************************************************
	@Test
	public void
	testTreeDump2()
	{
		m_iTest = 2;
		testTreeDump();
	}
}

//**************************************************************************************************

/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Tree is a word list decompression library.
 */

package com.jot.decomp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

//**************************************************************************************************

public class TreeWords implements Words
{
	private static int MAXCHARS = 0xE0 + 1;

	private static int OFFSETS_INDEPENDENT = 0;

	private static byte s_aaVersions[][] =
	{
		{ 'J', 'O', 'T', '1' },
	};

	private static final int FLAG_VALID = 0x80;		// This node marks the end of a word.
	private static final int FLAG_ENUM = 0x40;		// This node contains enumeration info.
	private static final int FLAG_TAG = 0x20;		// This node contains tagged information.
	private static final int FLAG_ALTERNATE = 0x18;	// This node has an alternate node.
	private static final int FLAG_EXTEND = 0x07;	// This node has an extension node.

	private static final int FLAG_ALT_NONE = 0x00;	// No alternate node.
	private static final int FLAG_ALT_INLINE = 0x08;// The node is inlined after this node.
	private static final int FLAG_ALT_TABLE = 0x10;	// There's a table pointer to the node.
	private static final int FLAG_ALT_IMMED = 0x18;	// The pointer is stored as immediate data.

	private static final int FLAG_EXT_NONE = 0x00;	// No extension node.
	private static final int FLAG_EXT_INLINE = 0x01;// The node is inlined after this state.
	private static final int FLAG_EXT_OFFSET = 0x02;// There's an offset pointer to the node.
	private static final int FLAG_EXT_TABLE = 0x03;	// There's a table pointer to the node.
	private static final int FLAG_EXT_IMMED = 0x04;	// The pointer is stored as immediate data.

	private RandomAccessFile m_rafData;

	private int m_iDataNext;		// Index to the next byte to read.
	private int m_iNodeRoot;		// Index to the root node.
	private int m_iNodeExtend;		// Index to the root node.

	private byte[] m_aTabNodes;		// The node data.

	// Tables needed to decompression the data.
	private short[] m_aTabCharFlags;// Character and flag combinations.
	private short[] m_aTabTags;		// Tag data indexes.
	private short[] m_aTabEnums;	// Word enumerations.
	private short[] m_aTabIndices;	// Multiref indexes.
	private short[] m_aTabOffsets;	// Offset pointers.

	private byte[] m_aTabTagData;	// Tag data indexes.
	private short[] m_aTabTagIndices;// Tag data indexes.

	private byte[] m_aTabAccelChars;// Compressed prefixes.
	private byte[] m_aTabAccelData;	// Acceleration table.
	private int	m_nAccelSegShift;		// Bit shift size of wnum to index m_bAccelerateddata.

	private int m_nTagBits;
	private int m_nTagMask;

	private int	m_iLevel;			// Extension level into the tree.
	private int	m_bmFlags;			// Control flags for this node.
	private char m_cNode;			// Character value stored in current node.
	private int m_nTags;			// Tagged number stored in this node.
	private int m_nTagInfo;
	private int m_nEnum;			// Enumeration value in this node.
	private boolean	m_bCounting;	// True when counting word numbers.
	private boolean m_bRetreating;	// True when reverse extending.
	private int	m_nWords;			// Total number of words in the tree.
	private int	m_nMaxWord;			// Maximum length including null.
	private int m_iWord;			// Word number of active location.

	private char m_cRoot;			// Root node character.

	private int[] m_aIndices;		// Index to next node at each level.
	private int[] m_aOffsets;		// Collective offsets at each level.
	private int[] m_aWordNums;		// Current word number at each level.
	private char[] m_acWord;

	//**********************************************************************************************
	// Disable the default constructor. Data stream must be provided.
	@SuppressWarnings("unused")
	private
	TreeWords()
	{
	}

	/***********************************************************************************************
	 * Tree constructor based on a compared binary file.
	 *
	 * @param strFile
	 *		Name of the file containing our compressed binary data.
	 * @throws Exception
	 *		For file access failure or data format issue.
	 */
	public
	TreeWords(String strFile) throws Exception
	{
		this(new FileInputStream(strFile));
	}
	
//	/***********************************************************************************************
//	 * Tree constructor based on a compared binary file.
//	 *
//	 * @param strFile
//	 *		Name of the file containing our compressed binary data.
//	 * @throws Exception
//	 *		For file access failure or data format issue.
//	 */
//	public
//	TreeWords(String strFile) throws Exception
//	{
//		this(new RandomAccessFile(strFile, "r"), 0);
//	}
	
	/***********************************************************************************************
	 * Copy constructor.
	 *
	 * @param words
	 *		Another word list object.
	 */
	private
	TreeWords(TreeWords words)
	{
		// Copy a reference to the file access.
		m_rafData = words.m_rafData;

		// Get the number of words in the tree.
		m_nWords = words.m_nWords;

		// Get the maximum length word.
		m_nMaxWord = words.m_nMaxWord;

		// Allocate level-access arrays based on this length.
		m_acWord = new char[m_nMaxWord];
		m_aIndices = new int[m_nMaxWord];
		m_aOffsets = new int[m_nMaxWord];
		m_aWordNums = new int[m_nMaxWord];

		// Decode some other information.
		m_nAccelSegShift = words.m_nAccelSegShift;
		m_nTagBits = words.m_nTagBits;
		m_nTagMask = words.m_nTagMask;

		// Get a pointer to the start of the tree and the root node.
		m_iNodeRoot = words.m_iNodeRoot;

		// Read in our required data tables.
		m_aTabCharFlags = words.m_aTabCharFlags;
		m_aTabTags = words.m_aTabTags;
		m_aTabEnums = words.m_aTabEnums;
		m_aTabOffsets = words.m_aTabOffsets;
		m_aTabIndices = words.m_aTabIndices;

		// Optional data tables.
		m_aTabAccelChars = words.m_aTabAccelChars;
		m_aTabAccelData = words.m_aTabAccelData;
		m_aTabTagData = words.m_aTabTagData;
		m_aTabTagIndices = words.m_aTabTagIndices;

		// Copy a reference to the node data.
		m_aTabNodes = words.m_aTabNodes;
	}
	
	/***********************************************************************************************
	 * Tree constructor based on a provided data reader.
	 *
	 * @param rafData
	 *		RandomAccessFile reader to the file containing the compressed binary data.
	 * @param iOffset
	 *		Offset to the start of the tree data, if part of a larger file.
	 * @throws Exception
	 *		For file access failure or data format issue.
	 */
	public
	TreeWords(RandomAccessFile rafData, int iOffset) throws Exception
	{
		m_rafData = rafData;
		rafData.seek(iOffset);

		// Decode the file type.
		byte[] aWord = new byte[4];
		rafData.read(aWord);
		int iVersion = -1;
		while (true)
		{
			if (++iVersion >= s_aaVersions.length)
			{
				throw new Exception("Unsupported file format.");
			}
			if (Arrays.equals(s_aaVersions[iVersion], aWord))
			{
				++iVersion;
				break;
			}
		}

		// Get the size of the binary image.
		int nDataSize = rafData.readInt();

		// Get the number of words in the tree.
		m_nWords = rafData.readInt();

		// Get the maximum length word.
		m_nMaxWord = rafData.readUnsignedShort();

		// Allocate arrays based on this length.
		m_acWord = new char[m_nMaxWord];
		m_aIndices = new int[m_nMaxWord];
		m_aOffsets = new int[m_nMaxWord];
		m_aWordNums = new int[m_nMaxWord];

		// Make sure some other stuff is supported by this code.
		if (rafData.readUnsignedByte() != OFFSETS_INDEPENDENT)
		{
			throw new Exception("Indepdent offsets not suppoorted.");
		}
		if (rafData.readUnsignedByte() != 0)
		{
			throw new Exception("Double inlining not suppoorted.");
		}

		// Decode some other information.
		m_nAccelSegShift = rafData.readUnsignedByte();
		m_nTagBits = rafData.readUnsignedByte();
		m_nTagMask = rafData.readUnsignedByte();
		rafData.readUnsignedByte();

		// Decode pointers to the tree tables.
		int iOffsetCharFlags = rafData.readInt();
		int iOffsetTags = rafData.readInt();
		int iOffsetEnums = rafData.readInt();
		int iOffsetOffsets = rafData.readInt();
		int iOffsetIndices = rafData.readInt();

		// Decode pointers to the accelerator tables, if available.
		int iOffsetAccelChars = rafData.readInt();
		int iOffsetAccelData = rafData.readInt();

		// Decode pointers to the word tag tables, if available.
		int iOffsetTagData = rafData.readInt();
		int iOffsetTagIndices = rafData.readInt();

		// Get a pointer to the start of the tree and the root node.
		int iOffsetStart = rafData.readInt();
		m_iNodeRoot = rafData.readInt() - iOffsetStart;

		// Read in our required data tables.
		m_aTabCharFlags = new short[(iOffsetTags - iOffsetCharFlags) >> 1];
		readTable(m_aTabCharFlags, iOffsetCharFlags + iOffset);
		m_aTabTags = new short[(iOffsetEnums - iOffsetTags) >> 1];
		readTable(m_aTabTags, iOffsetTags + iOffset);
		m_aTabEnums = new short[(iOffsetOffsets - iOffsetEnums) >> 1];
		readTable(m_aTabEnums, iOffsetEnums + iOffset);
		m_aTabOffsets = new short[(iOffsetIndices - iOffsetOffsets) >> 1];
		readTable(m_aTabOffsets, iOffsetOffsets + iOffset);
		m_aTabIndices = new short[0x100];
		readTable(m_aTabIndices, iOffsetIndices + iOffset);

		// Optional data tables.
		if (iOffsetAccelChars > 0)
		{
			m_aTabAccelChars = new byte[iOffsetAccelData - iOffsetAccelChars];
			readTable(m_aTabAccelChars, iOffsetAccelChars + iOffset);
			m_aTabAccelData = new byte[6 * (m_nWords >> m_nAccelSegShift)];
			readTable(m_aTabAccelData, iOffsetAccelData + iOffset);
		}
		if (iOffsetTagData > 0)
		{
			m_aTabTagData = new byte[iOffsetTagIndices - iOffsetTagData];
			readTable(m_aTabTagData, iOffsetTagData + iOffset);
			m_aTabTagIndices = new short[(iOffsetStart - iOffsetTagIndices) >> 1];
			readTable(m_aTabTagIndices, iOffsetTagIndices + iOffset);
		}

		// Read in the node data.
		m_aTabNodes = new byte[nDataSize - iOffsetStart];
		readTable(m_aTabNodes, iOffsetStart + iOffset);
	}

	/***********************************************************************************************
	 * Tree constructor based on a provided data reader.
	 *
	 * @param isData
	 *		Input stream to the file containing the compressed binary data.
	 * @throws Exception
	 *		For file access failure or data format issue.
	 */
	public
	TreeWords(InputStream isData) throws Exception
	{
		m_rafData = null;

		// Decode the file type.
		byte[] aWord = new byte[4];
		isData.read(aWord);
		int iVersion = -1;
		while (true)
		{
			if (++iVersion >= s_aaVersions.length)
			{
				throw new Exception("Unsupported file format.");
			}
			if (Arrays.equals(s_aaVersions[iVersion], aWord))
			{
				++iVersion;
				break;
			}
		}

		// Get the size of the binary image.
		int nDataSize = readU32(isData);

		// Get the number of words in the tree.
		m_nWords = readU32(isData);

		// Get the maximum length word.
		m_nMaxWord = readU16(isData);

		// Allocate arrays based on this length.
		m_acWord = new char[m_nMaxWord];
		m_aIndices = new int[m_nMaxWord];
		m_aOffsets = new int[m_nMaxWord];
		m_aWordNums = new int[m_nMaxWord];

		// Make sure some other stuff is supported by this code.
		if (isData.read() != OFFSETS_INDEPENDENT)
		{
			throw new Exception("Indepdent offsets not suppoorted.");
		}
		if (isData.read() != 0)
		{
			throw new Exception("Double inlining not suppoorted.");
		}

		// Decode some other information.
		m_nAccelSegShift = isData.read();
		m_nTagBits = isData.read();
		m_nTagMask = isData.read();
	    isData.read();

		// Decode pointers to the tree tables.
		int iOffsetCharFlags = readU32(isData);
		int iOffsetTags = readU32(isData);
		int iOffsetEnums = readU32(isData);
		int iOffsetOffsets = readU32(isData);
		int iOffsetIndices = readU32(isData);

		// Decode pointers to the accelerator tables, if available.
		int iOffsetAccelChars = readU32(isData);
		int iOffsetAccelData = readU32(isData);

		// Decode pointers to the word tag tables, if available.
		int iOffsetTagData = readU32(isData);
		int iOffsetTagIndices = readU32(isData);

		// Get a pointer to the start of the tree and the root node.
		int iOffsetStart = readU32(isData);
		m_iNodeRoot = readU32(isData) - iOffsetStart;

		// Read in our required data tables.
		m_aTabCharFlags = new short[(iOffsetTags - iOffsetCharFlags) >> 1];
		readTable(isData, m_aTabCharFlags);
		m_aTabTags = new short[(iOffsetEnums - iOffsetTags) >> 1];
		readTable(isData, m_aTabTags);
		m_aTabEnums = new short[(iOffsetOffsets - iOffsetEnums) >> 1];
		readTable(isData, m_aTabEnums);
		m_aTabOffsets = new short[(iOffsetIndices - iOffsetOffsets) >> 1];
		readTable(isData, m_aTabOffsets);
		m_aTabIndices = new short[0x100];
		readTable(isData, m_aTabIndices);

		// Optional data tables.
		if (iOffsetAccelChars > 0)
		{
			m_aTabAccelChars = new byte[iOffsetAccelData - iOffsetAccelChars];
			isData.read(m_aTabAccelChars);
			m_aTabAccelData = new byte[6 * (m_nWords >> m_nAccelSegShift)];
			isData.read(m_aTabAccelData);
		}
		if (iOffsetTagData > 0)
		{
			m_aTabTagData = new byte[iOffsetTagIndices - iOffsetTagData];
			isData.read(m_aTabTagData);
			m_aTabTagIndices = new short[(iOffsetStart - iOffsetTagIndices) >> 1];
			readTable(isData, m_aTabTagIndices);
		}

		// Read in the node data.
		m_aTabNodes = new byte[nDataSize - iOffsetStart];
		isData.read(m_aTabNodes);
	}

	/***********************************************************************************************
	 * Return a clone of this object.
	 *
	 * @return
	 *		A clone.
	 */
	public Words
	clone()
	{
		return new TreeWords(this);
	}

	//**********************************************************************************************
	// Decode a 16/24 bit immediate value from the binary data.
	private int
	decodeValue()
	{
		int	value;

		if ((value = m_aTabNodes[m_iDataNext++] & 0xFF) >= 0xF0) {
			value = (value & 0xF) << 8;
			value |= m_aTabNodes[m_iDataNext++] & 0xFF;
		}
		value <<= 8;
		value |= m_aTabNodes[m_iDataNext++] & 0xFF;
		return (value);
	}

	//**********************************************************************************************
	// Decode a value from a tree table.
	private int
	decodeTable(short[] aTable)
	{
		int	value;
		int	index;

		if ((index = m_aTabNodes[m_iDataNext++] & 0xFF) >= 0xF0)
		{
			index = (index & 0x0F) << 8;
			index |= m_aTabNodes[m_iDataNext++] & 0xFF;
		}
		if ((value = aTable[index] & 0xFFFF) < 0xFF00)
		{
			return (value);
		}
		return (((value & 0xFF) << 16) | (aTable[index + 1] & 0xFFFF));
	}

	//**********************************************************************************************
	// Decode a node from the current tree location.
	private boolean
	decodeNode()
	{
		boolean	alt_inline;	// True if alternate node is inline.
		int	value;			// Value decoded from binary data.
		int	index;

		// Decode the character and flags part of the node.
		value = decodeTable(m_aTabCharFlags);
		m_bmFlags = value >> 8;
		m_cNode = (char)(value & 0xFF);
		m_acWord[m_iLevel] = m_cNode;

		if (m_iLevel + 1 >= m_nMaxWord)
		{
			System.out.println("Array overflow.");
		}
		m_acWord[m_iLevel + 1] = 0;

		// Decode the possible tag value at this node.
		m_nTags = 0;
		m_nTagInfo = 0;
		if ((m_bmFlags & FLAG_VALID) != 0)
		{
			if ((m_bmFlags & FLAG_TAG) != 0)
			{
				m_nTagInfo = decodeTable(m_aTabTags);
				if (m_nTagBits != 0)
				{
					m_nTags = m_nTagInfo & m_nTagMask;
				}
				else {
					m_nTags = 1;
				}
			}

			// Update the current word number.
			if (m_bCounting)
			{
				++m_iWord;
			}
		}

		// Decode the possible enumeration information at this node.

		if ((m_bmFlags & FLAG_ENUM) != 0)
		{
			m_nEnum = decodeTable(m_aTabEnums);
			m_aWordNums[m_iLevel] = m_iWord + m_nEnum;
		}
		else
		{
			m_aWordNums[m_iLevel] = 0;
			m_nEnum = 0;
		}

		// Decode the pointer to the next node in the state.
		alt_inline = false;
		switch (m_bmFlags & FLAG_ALTERNATE)
		{
			case FLAG_ALT_NONE:		// No alternate node.
				m_aIndices[m_iLevel] = -1;
				break;

			case FLAG_ALT_INLINE:	// The alternate node is next.
				alt_inline = true;
				break;

			case FLAG_ALT_TABLE:	// Index is stored in a table.
				value = m_aTabIndices[m_aTabNodes[m_iDataNext++] & 0xFF] & 0xFFFF;
				m_aIndices[m_iLevel] = value;
				break;

			case FLAG_ALT_IMMED:	// Index stored as immediate data.
				value = decodeValue();
				m_aIndices[m_iLevel] = value;
				break;
		}	

		// Decode the pointer the extension node.
		switch (m_bmFlags & FLAG_EXTEND)
		{
			case FLAG_EXT_NONE:		// No extend node.
				m_iNodeExtend = -1;
				break;

			case FLAG_EXT_INLINE:	// The extend node is next.
				m_aOffsets[m_iLevel + 1] = m_aOffsets[m_iLevel];
				m_iNodeExtend = m_iDataNext;
				break;

			case FLAG_EXT_OFFSET:	// Have an offset pointer.
				value = decodeTable(m_aTabOffsets);
				if (m_aOffsets[m_iLevel] == -1)
				{
					m_aOffsets[m_iLevel] = m_iDataNext + value;
				}
				else
				{
					m_aOffsets[m_iLevel] += value;
				}
				m_aOffsets[m_iLevel + 1] = -1;
				m_iNodeExtend = m_aOffsets[m_iLevel];
				break;

			case FLAG_EXT_TABLE:	// Index stored in a table.
				value = m_aTabIndices[m_aTabNodes[m_iDataNext++] & 0xFF] & 0xFFFF;
				m_iNodeExtend = value;
				m_aOffsets[m_iLevel + 1] = -1;
				break;

			case FLAG_EXT_IMMED:	// Index stored as immediate data.
				value = decodeValue();
				m_iNodeExtend = value;
				m_aOffsets[m_iLevel + 1] = -1;
				break;
		}

		// We now know where the end of the node is.  If we have an alternate
		// pointer inline to the next node, then set the pointer.
		if (alt_inline)
		{
			m_aIndices[m_iLevel] = m_iDataNext;
		}
		return (true);
	}

	/***********************************************************************************************
	 * Return the word indexed by the given number.  If traverse is set, make sure
	 * we can continue traversing the tree from this point.
	 */
	public String
	getWord(
		int nWord,
		boolean bTraverse)
	{
		int iData;
		int value;
		int i;

		// Word numbers start at one.  Make sure we have atleast that.
		if (nWord == 0)
		{
			return null;
		}

		// If we have acceleration data, use it.
		if (!bTraverse &&
			m_aTabAccelData != null)
		{
			// m_iWord doesn't need to be updated.
			m_bCounting = false;

			// Position us at the table entry.
			--nWord;
			value = nWord >> m_nAccelSegShift;
			nWord &= (1 << m_nAccelSegShift) - 1;
			iData = (value << 2) + (value << 1);

			// Decode the number of we have to skip to get the first in
			// our segment.
			value = m_aTabAccelData[iData++] & 0xFF;
			value |= (m_aTabAccelData[iData++] & 0xFF) << 8;
			nWord += value + 1;

			// Decode and set the jump position into the tree.
			value = m_aTabAccelData[iData++] & 0xFF;
			value |= (m_aTabAccelData[iData++] & 0xFF) << 8;
			value |= (m_aTabAccelData[iData] & 0x7) << 16;
			m_iDataNext = value;

			// Decode the prefix we skipped over.
			value = (m_aTabAccelData[iData++] & 0xFF) >> 3;
			value |= (m_aTabAccelData[iData] & 0x3F) << 5;

			// Decode the word starting at this index.
			for (i = 0; i < m_iLevel; ++i)
			{
				m_acWord[i] = (char)(m_aTabAccelChars[value++] & 0xFF);
			}
			m_acWord[i] = 0;

			// Decode the level we're jumping into the tree.
			m_iLevel = (m_aTabAccelData[iData] & 0xFF) >> 6;

			// Decode this first node.
			decodeNode();
		}
		else
		{
			// Starting from the root node, traverse through the tree
			// counting up each word skipped.  When we get to the Nth
			// word, return it.
			m_bCounting = true;
			scanRoot();
		}

		while (true)
		{
			// If we're at the end of a word, the see if it's the one we
			// want.
			if ((m_bmFlags & FLAG_VALID) != 0)
			{
				if (--nWord == 0)
				{
					return new String(m_acWord, 0, m_iLevel + 1);
				}
			}

			// If this node has enumeration, then try to skip over its
			// sub-tree.  Otherwise we have to traverse it and count it
			// by hand.
			if (m_nEnum != 0 && m_nEnum < nWord)
			{
				nWord -= m_nEnum;
			}
			else if (scanExtend())
			{
				continue;
			}

			// At the bottom of the tree or skipping over a section.  Try
			// to get an alternate node at this level or above.
			while (!scanAlternate())
			{
				if (!scanRetreat())
				{
					return null;
				}
			}
		}
	}

	/***********************************************************************************************
	 * Return the number of words in our tree.
	 *
	 * @return
	 *		The number of words in our tree.
	 */
	public int
	getWordCount()
	{
		return m_nWords;
	}

	/***********************************************************************************************
	 * Assuming a two byte tag encoding, decode the tag and return it.
	 */
	public int
	getTagValue(int iTag)
	{
		if (iTag >= m_nTags)
		{
			return (-1);
		}
		if (m_nTagBits != 0)
		{
			iTag = m_aTabTagIndices[(m_nTagInfo >> m_nTagBits) + iTag] & 0xFFFF;
			iTag = ((m_aTabTagData[iTag] & 0xFF) << 8) | (m_aTabTagData[iTag] & 0xFF);
			return (iTag);
		}
		return (m_nTagInfo);
	}

	/***********************************************************************************************
	 * Return true if the given string forms a valid word.
	 *
	 * @param strWord
	 *		A string of letters.
	 * @return
	 *		True if these letters form a word.
	 */
	public boolean
	isWord(String strWord)
	{
		m_bCounting = false;
		if (scanRoot())
		{
			int iWord = 0;
			do
			{
				char cWord = strWord.charAt(iWord);
				while (cWord != m_cNode)
				{
					if (!scanAlternate())
					{
						return false;
					}
				}
				if (++iWord >= strWord.length())
				{
					return (m_bmFlags & FLAG_VALID) != 0;
				}
			} while (scanExtend());
		}
		return false;
	}

	/***********************************************************************************************
	 * Return true if the given string forms a valid word.
	 *
	 * @param aWord
	 *		An array of letters.
	 * @return
	 *		True if these letters form a word.
	 */
	public boolean
	isWord(char[] aWord)
	{
		m_bCounting = false;
		if (scanRoot())
		{
			int iWord = 0;
			char cWord = aWord[0];
			do
			{
				while (cWord != m_cNode)
				{
					if (!scanAlternate())
					{
						return false;
					}
				}
				if ((cWord = aWord[++iWord]) == 0)
				{
					return (m_bmFlags & FLAG_VALID) != 0;
				}
			} while (scanExtend());
		}
		return false;
	}

	//**********************************************************************************************
	// Read in a data table.
	private void
	readTable(
		byte[] aTable,
		int iOffset) throws Exception
	{
		m_rafData.seek(iOffset);
		m_rafData.read(aTable);
	}
	
	//**********************************************************************************************
	// Read in a data table.
	private void
	readTable(
		InputStream isData,
		short[] aTable
	) throws Exception
	{
		for (int i = 0; i < aTable.length; ++i)
		{
			aTable[i] = (short)readU16(isData);
		}
	}

	//**********************************************************************************************
	// Read in a data table.
	private void
	readTable(
		InputStream isData,
		int[] aTable
	) throws Exception
	{
		for (int i = 0; i < aTable.length; ++i)
		{
			aTable[i] = readU32(isData);
		}
	}

	//**********************************************************************************************
	// Read in a data table.
	private void
	readTable(
		short[] aTable,
		int iOffset) throws Exception
	{
		m_rafData.seek(iOffset);
		for (int i = 0; i < aTable.length; ++i)
		{
			aTable[i] = m_rafData.readShort();
		}
	}

	//**********************************************************************************************
	// Read in a data table.
	private void
	readTable(
		int[] aTable,
		int iOffset) throws Exception
	{
		m_rafData.seek(iOffset);
		for (int i = 0; i < aTable.length; ++i)
		{
			aTable[i] = m_rafData.readShort() & 0xFFFF;
		}
	}

	//**********************************************************************************************
	// Decode a 32 bit immediate value from the binary data.
	private int
	readU16(InputStream isData) throws Exception
	{
		int	value;

		value =
			(isData.read() << 8) |
			isData.read();

		return (value);
	}

	//**********************************************************************************************
	// Decode a 32 bit immediate value from the binary data.
	private int
	readU32(InputStream isData) throws Exception
	{
		int	value;

		value =
			(isData.read() << 24) |
			(isData.read() << 16) |
			(isData.read() << 8) |
			isData.read();

		return (value);
	}

	/***********************************************************************************************
	 * Provide access to the word list data base.  The caller passes a function
	 * pointer to the call-back routine "prune()."  The routine can be used to
	 * search for a word, add them to a list, and prune over unwanted parts of
	 * the data base (by returning a non-zero level).
	 */
	public void
	scan(
		ScanWords scanWords,
		int bmScanFlags
	) throws Exception
	{
		boolean bRandomized;
		int iLevel;

		// Check to see if we're starting from a random location within the
		// word list.  If so, allow the search to loop around at the end of
		// the word list.

		bRandomized = false;
		if ((bmScanFlags & ScanWords.SCAN_FLAG_RANDOM) != 0)
		{
			bRandomized = true;
			getWord((int)(Math.random() * getWordCount()) + 1, true);
		}

		// For trees, ignoring the word numbers makes traversal faster.
		m_bCounting = (bmScanFlags & ScanWords.SCAN_FLAG_QUICK) == 0;

		// If we're search for the next word in a list, we have to jump past
		// the current word.
		boolean bGetNextWord = false;
		if ((bmScanFlags & ScanWords.SCAN_FLAG_NEXT) != 0)
		{
			bGetNextWord = true;
		}
		// If all these position bits are off, then search from the start of
		// the word list.
		else if (
			(bmScanFlags & (ScanWords.SCAN_FLAG_CONTINUE | ScanWords.SCAN_FLAG_RANDOM)) == 0)
		{
			scanRoot();
		}

		while (true)
		{
			do
			{
				// If needed, skip to the next word.
				if (bGetNextWord)
				{
					bGetNextWord = false;
					continue;
				}
				
				// Keep looping down the tree until we get a word.
				if ((m_bmFlags & FLAG_VALID) == 0)
				{
					continue;
				}

				// Have a word.  Call the prune function.
				switch ((iLevel = scanWords.prune(m_acWord, m_iWord)))
				{
					case ScanWords.SCAN_LEVEL_QUIT:		// Exit the scan.
						return;

					case ScanWords.SCAN_LEVEL_RESET:	// Reset to the start.
						scanRoot();
						continue;

					case ScanWords.SCAN_LEVEL_RANDOM:	// Randomize our position.
						bRandomized = true;
						getWord((int)(Math.random() * getWordCount()) + 1, true);
						continue;

					case ScanWords.SCAN_LEVEL_CONTINUE:	// No pruning, just continue.
						continue;

					default:			// Prune at this level in the tree.
						if (iLevel > m_iLevel)
						{
							continue;
						}
						setLevel(iLevel);
				}
				break;
			} while (scanExtend());

			// Alternate to the next word off the given level.
			while (!scanAlternate())
			{
				if (!scanRetreat())
				{
					// At the end of the tree.  If the prune
					// function asks for a random placement,
					// then reset to the start.
					if (bRandomized)
					{
						scanRoot();
						break;
					}
					return;
				}
			}
		}
	}

	/***********************************************************************************************
	 * Traverse to an alternate letter within our word.  Return false if there is
	 * no such letter.
	 */
	public boolean
	scanAlternate()
	{
		if (m_bCounting && !m_bRetreating)
		{
			if (m_nEnum != 0)
			{
				m_iWord += m_nEnum;
			}
			else if (scanExtend())
			{
				// No count information. Recurse into the tree and
				// count up all the valid nodes by hand.
				while (scanAlternate())
				{
				}
				scanRetreat();
			}
		}

		if ((m_iDataNext = m_aIndices[m_iLevel]) == -1)
		{
			return (false);
		}
		m_bRetreating = false;
		return (decodeNode());
	}

	/***********************************************************************************************
	 * Extend to the next letter in the current word.  Return false if there is no
	 * such letter.
	 */
	public boolean
	scanExtend()
	{
		if ((m_iDataNext = m_iNodeExtend) == -1)
		{
			return (false);
		}

		++m_iLevel;
		m_bRetreating = false;
		return (decodeNode());
	}

	/***********************************************************************************************
	 * Decrease the depth or length into the current tree word.  This is used to
	 * traverse out of the end of a sub-tree.  Returns false if we are at the end
	 * of the tree.
	 */
	public boolean
	scanRetreat()
	{
		if (m_iLevel == 0) {
			return (false);
		}

		m_bRetreating = true;
		m_acWord[m_iLevel--] = 0;
		return (true);
	}

	/***********************************************************************************************
	 * Reset to the first node in the tree.
	 */
	public boolean
	scanRoot()
	{
		m_iWord = 0;
		m_iLevel = -1;
		m_aOffsets[0] = -1;
		m_iNodeExtend = m_iNodeRoot;
		m_cRoot = 0;

		if (scanExtend())
		{
			// If we have a group of trees and the user only wants one of them,
			// then reset to this root.
			if (m_cRoot != 0)
			{
				while (m_cNode != m_cRoot)
				{
					if (!scanAlternate())
					{
						m_iNodeExtend = -1;
						return false;
					}
				}
				m_iWord = 0;
				m_iLevel = -1;
				m_aOffsets[0] = m_aOffsets[1];
				
				return scanExtend();
			}
		}
		return true;
	}

	/***********************************************************************************************
	 * Jump back up to this level.  The main problem is our knowledge of the word
	 * number at this level.  If it's not known, we'll have to scan for it.
	 */
	public void
	setLevel(int iLevel)
	{
		if (iLevel < m_iLevel)
		{
			if (!m_bCounting) {
				m_iLevel = iLevel + 1;
				scanRetreat();
			}
			else if (m_aWordNums[iLevel] != 0)
			{
				m_iWord = m_aWordNums[iLevel];
				m_iLevel = iLevel + 1;
				scanRetreat();
			}
			else
			{
				do
				{
					if (!scanAlternate())
					{
						if (!scanRetreat())
						{
							// Should never happen.
							return;
						}
					}
				} while (iLevel < m_iLevel);
			}
		}
	}
}

//**************************************************************************************************

//**************************************************************************************************
//
//  TreeWords.swift
//  
//
//  Created by Stephen Kennedy on 5/7/20.
//
//**************************************************************************************************

import Foundation

//**************************************************************************************************

public class TreeWords: Words
{
	enum TreeWordsError: Error
	{
		case ERROR(String)
	}

	let MAXCHARS = 0xE0 + 1
	let OFFSETS_INDEPENDENT = 0

	let s_aaVersions: [[UInt8]] =
	[
		[ 0x4A, 0x4F, 0x54, 0x31 ]
	]

	let FLAG_VALID = 0x80			// This node marks the end of a word.
	let FLAG_ENUM = 0x40			// This node contains enumeration info.
	let FLAG_TAG = 0x20				// This node contains tagged information.
	let FLAG_ALTERNATE = 0x18		// This node has an alternate node.
	let FLAG_EXTEND = 0x07			// This node has an extension node.

	let FLAG_ALT_NONE = 0x00		// No alternate node.
	let FLAG_ALT_INLINE = 0x08		// The node is inlined after this node.
	let FLAG_ALT_TABLE = 0x10		// There's a table pointer to the node.
	let FLAG_ALT_IMMED = 0x18		// The pointer is stored as immediate data.

	let FLAG_EXT_NONE = 0x00		// No extension node.
	let FLAG_EXT_INLINE = 0x01		// The node is inlined after this state.
	let FLAG_EXT_OFFSET = 0x02		// There's an offset pointer to the node.
	let FLAG_EXT_TABLE = 0x03		// There's a table pointer to the node.
	let FLAG_EXT_IMMED = 0x04		// The pointer is stored as immediate data.

	var m_fHandle: FileHandle

	var m_iDataNext = 0				// Index to the next byte to read.
	var m_iNodeRoot = 0				// Index to the root node.
	var m_iNodeExtend = 0			// Index to the root nod\e.

	var m_aTabNodes: [UInt8]		// The node data.

	// Tables needed to decompression the data.
	var m_aTabCharFlags: [UInt16]	// Character and flag combinations.
	var m_aTabTags: [UInt16]		// Tag data indexes.
	var m_aTabEnums: [UInt16]		// Word enumerations.
	var m_aTabIndices: [UInt16]		// Multiref indexes.
	var m_aTabOffsets: [UInt16]		// Offset pointers.

	var m_aTabTagData: [UInt8]? = nil		// Tag data indexes.
	var m_aTabTagIndices: [UInt16]? = nil	// Tag data indexes.

	var m_aTabAccelChars: [UInt8]? = nil	// Compressed prefixes.
	var m_aTabAccelData: [UInt8]? = nil		// Acceleration table.
	var m_nAccelSegShift = 0		// Bit shift size of wnum to index m_bAccelerateddata.

	var m_nTagBits = 0
	var m_nTagMask = 0

	var m_iLevel = 0				// Extension level into the tree.
	var m_bmFlags =  0				// Control flags for this node.
	var m_cNode: UInt8 = 0			// Character value stored in current node.
	var m_nTags = 0					// Tagged number stored in this node.
	var m_nTagInfo = 0
	var m_nEnum = 0					// Enumeration value in this node.
	var m_bCounting = false			// True when counting word numbers.
	var m_bRetreating = false		// True when reverse extending.
	var m_nWords = 0				// Total number of words in the tree.
	var m_nMaxWord = 0				// Maximum length including nil.
	var m_iWord = 0					// Word number of active location.

	var m_cRoot: UInt8 = 0			// Root node character.

	var m_aIndices: [Int]			// Index to next node at each level.
	var m_aOffsets: [Int]			// Collective offsets at each level.
	var m_aWordNums: [Int]			// Current word number at each level.
	var m_acWord: [UInt8]

	/***********************************************************************************************
	 * Tree constructor based on a compared binary file.
	 *
	 * @param strFile
	 *		Name of the file containing our compressed binary data.
	 * @throws Exception
	 *		For file access failure or data format issue.
	 */
	convenience
	init(strFile: String) throws
	{
        try self.init(fHandle: FileHandle(forReadingAtPath: strFile)!, iOffset: 0)
	}

	/***********************************************************************************************
	 * Copy constructor.
	 *
	 * @param words
	 *		Another word list object.
	 */
	init(words: TreeWords)
	{
		// Copy a reference to the file access.
		m_fHandle = words.m_fHandle

		// Get the number of words in the tree.
		m_nWords = words.m_nWords

		// Get the maximum length word.
		m_nMaxWord = words.m_nMaxWord

		// Allocate level-access arrays based on this length.
		m_acWord = Array(repeating: 0, count: m_nMaxWord)
		m_aIndices = Array(repeating: 0, count: m_nMaxWord)
		m_aOffsets = Array(repeating: 0, count: m_nMaxWord)
		m_aWordNums = Array(repeating: 0, count: m_nMaxWord)

		// Decode some other information.
		m_nAccelSegShift = words.m_nAccelSegShift
		m_nTagBits = words.m_nTagBits
		m_nTagMask = words.m_nTagMask

		// Get a pointer to the start of the tree and the root node.
		m_iNodeRoot = words.m_iNodeRoot

		// Read in our required data tables.
		m_aTabCharFlags = words.m_aTabCharFlags
		m_aTabTags = words.m_aTabTags
		m_aTabEnums = words.m_aTabEnums
		m_aTabOffsets = words.m_aTabOffsets
		m_aTabIndices = words.m_aTabIndices

		// Optional data tables.
		m_aTabAccelChars = words.m_aTabAccelChars
		m_aTabAccelData = words.m_aTabAccelData
		m_aTabTagData = words.m_aTabTagData
		m_aTabTagIndices = words.m_aTabTagIndices

		// Copy a reference to the node data.
		m_aTabNodes = words.m_aTabNodes
	}
	
	/***********************************************************************************************
	 * Tree constructor based on a provided data reader.
	 *
	 * @param fHandle
	 *		RandomAccessFile reader to the file containing the compressed binary data.
	 * @param iOffset
	 *		Offset to the start of the tree data, if part of a larger file.
	 * @throws Exception
	 *		For file access failure or data format issue.
	 */
	init(fHandle: FileHandle, iOffset: Int) throws
	{
		m_fHandle = fHandle
        fHandle.seek(toFileOffset: UInt64(iOffset))

		// Decode the file type.
        if #available(iOS 13.4, *)
		{
            guard let data = try fHandle.read(upToCount: 4) else
            {
                throw TreeWordsError.ERROR("TreeWords: read access failure")
            }

			var iVersion = -1
			while (true)
			{
				iVersion += 1
				if (iVersion >= s_aaVersions.count)
				{
					throw TreeWordsError.ERROR("Unsupported file format.")
				}
				if (data.elementsEqual(s_aaVersions[iVersion]))
				{
					iVersion += 1
					break
				}
			}

			// Avoid swift compiler bug.
			m_aTabNodes = Array(repeating: 0, count: 1)
			m_acWord = m_aTabNodes
			m_aTabCharFlags = Array(repeating: 0, count: 1)
			m_aTabTags = m_aTabCharFlags
			m_aTabEnums = m_aTabCharFlags
			m_aTabOffsets = m_aTabCharFlags
			m_aTabIndices = m_aTabCharFlags
			m_aIndices = Array(repeating: 0, count: 1)
			m_aOffsets = m_aIndices
			m_aWordNums = m_aIndices

			// Get the size of the binary image.
			let nDataSize = Int(try readUInt32())

			// Get the number of words in the tree.
			m_nWords = Int(try readUInt32())

			// Get the maximum length word.
			m_nMaxWord = Int(try readUInt16())

			// Allocate arrays based on this length.
			m_acWord = Array(repeating: 0, count: m_nMaxWord)
			m_aIndices = Array(repeating: 0, count: m_nMaxWord)
			m_aOffsets = Array(repeating: 0, count: m_nMaxWord)
			m_aWordNums = Array(repeating: 0, count: m_nMaxWord)

			// Make sure some other stuff is supported by this code.
			if (try readUInt8() != OFFSETS_INDEPENDENT)
			{
				throw TreeWordsError.ERROR("Independent offsets not suppoorted.")
			}
			if (try readUInt8() != 0)
			{
				throw TreeWordsError.ERROR("Double inlining not suppoorted.")
			}

			// Decode some other information.
			m_nAccelSegShift = Int(try readUInt8())
			m_nTagBits = Int(try readUInt8())
			m_nTagMask = Int(try readUInt8())
			_ = try readUInt8()

			// Decode pointers to the tree tables.
			let iOffsetCharFlags = Int(try readUInt32())
			let iOffsetTags = Int(try readUInt32())
			let iOffsetEnums = Int(try readUInt32())
			let iOffsetOffsets = Int(try readUInt32())
			let iOffsetIndices = Int(try readUInt32())

			// Decode pointers to the accelerator tables, if available.
			let iOffsetAccelChars = Int(try readUInt32())
			let iOffsetAccelData = Int(try readUInt32())

			// Decode pointers to the word tag tables, if available.
			let iOffsetTagData = Int(try readUInt32())
			let iOffsetTagIndices = Int(try readUInt32())

			// Get a pointer to the start of the tree and the root node.
			let iOffsetStart = Int(try readUInt32())
			m_iNodeRoot = Int(try readUInt32()) - iOffsetStart

			// Read in our required data tables.
			m_aTabCharFlags = Array(
				repeating: 0, count: (iOffsetTags - iOffsetCharFlags) >> 1)
			try readTable(aTable: &m_aTabCharFlags, iOffset: iOffsetCharFlags + iOffset)
			m_aTabTags = Array(
				repeating: 0, count: (iOffsetEnums - iOffsetTags) >> 1)
			try readTable(aTable: &m_aTabTags, iOffset: iOffsetTags + iOffset)
			m_aTabEnums = Array(
				repeating: 0, count: (iOffsetOffsets - iOffsetEnums) >> 1)
			try readTable(aTable: &m_aTabEnums, iOffset: iOffsetEnums + iOffset)
			m_aTabOffsets = Array(
				repeating: 0, count: (iOffsetIndices - iOffsetOffsets) >> 1)
			try readTable(aTable: &m_aTabOffsets, iOffset: iOffsetOffsets + iOffset)
			m_aTabIndices = Array(
				repeating: 0, count: 0x100)
			try readTable(aTable: &m_aTabIndices, iOffset: iOffsetIndices + iOffset)

			// Optional data tables.
			if (iOffsetAccelChars > 0)
			{
				var aTabAccelChars: [UInt8] = Array(
					repeating: 0, count: iOffsetAccelData - iOffsetAccelChars)
				m_aTabAccelChars = aTabAccelChars
				try readTable8(aTable: &aTabAccelChars, iOffset: iOffsetAccelChars + iOffset)

				var aTabAccelData: [UInt8] = Array(
					repeating: 0, count: 6 * (m_nWords >> m_nAccelSegShift))
				m_aTabAccelData = aTabAccelData
				try readTable8(aTable: &aTabAccelData, iOffset: iOffsetAccelData + iOffset)
			}
			if (iOffsetTagData > 0)
			{
				var aTabTagData: [UInt8] = Array(
					repeating: 0, count: iOffsetTagIndices - iOffsetTagData)
				m_aTabTagData = aTabTagData
				try readTable8(aTable: &aTabTagData, iOffset: iOffsetTagData + iOffset)

				var aTabTagIndices: [UInt16] = Array(
					repeating: 0, count: (iOffsetStart - iOffsetTagIndices) >> 1)
				m_aTabTagIndices = aTabTagIndices
				try readTable(aTable: &aTabTagIndices, iOffset: iOffsetTagIndices + iOffset)
			}

			// Read in the node data.
			m_aTabNodes = Array(
				repeating: 0, count: nDataSize - iOffsetStart)
			try readTable8(aTable: &m_aTabNodes, iOffset: iOffsetStart + iOffset)
        }
		else
		{
            throw TreeWordsError.ERROR("TreeWords: IOS 13.4 or later required.")
        }
	}

	/***********************************************************************************************
	 * Return a clone of this object.
	 *
	 * @return
	 *		A clone.
	 */
	func
	clone() -> Words
	{
		return TreeWords(words: self)
	}

	//**********************************************************************************************
	// Decode a 16/24 bit immediate value from the binary data.
	func
	decodeValue() -> Int
	{
		var value: Int

		value = Int(m_aTabNodes[m_iDataNext]) & 0xFF
		if (value >= 0xF0)
		{
			m_iDataNext += 1
			value = (value & 0xF) << 8
			value |= Int(m_aTabNodes[m_iDataNext]) & 0xFF
		}
		m_iDataNext += 1
		value <<= 8
		value |= Int(m_aTabNodes[m_iDataNext]) & 0xFF
		m_iDataNext += 1

		return value
	}

	//**********************************************************************************************
	// Decode a value from a tree table.
	func
	decodeTable(aTable: [UInt16]) -> Int
	{
		var	value: Int
		var index: Int

		index = Int(m_aTabNodes[m_iDataNext]) & 0xFF
		if (index >= 0xF0)
		{
			m_iDataNext += 1
			index = (index & 0x0F) << 8
			index |= Int(m_aTabNodes[m_iDataNext]) & 0xFF
		}
		m_iDataNext += 1
		value = Int(aTable[index]) & 0xFFFF
		if (value < 0xFF00)
		{
			return (value)
		}
		return (((value & 0xFF) << 16) | (Int(aTable[index + 1]) & 0xFFFF))
	}

	//**********************************************************************************************
	// Decode a node from the current tree location.
	func
	decodeNode() throws -> Bool
	{
		var alt_inline: Bool	// True if alternate node is inline.
		var value: Int 			// Value decoded from binary data.

		// Decode the character and flags part of the node.
		value = decodeTable(aTable: m_aTabCharFlags)
		m_bmFlags = value >> 8
		m_cNode = UInt8(value & 0xFF)
		m_acWord[m_iLevel] = m_cNode

		if (m_iLevel + 1 >= m_nMaxWord)
		{
			throw TreeWordsError.ERROR("TreeWords: Array maxword exceeded.")
		}
		m_acWord[m_iLevel + 1] = 0

		// Decode the possible tag value at this node.
		m_nTags = 0
		m_nTagInfo = 0
		if ((m_bmFlags & FLAG_VALID) != 0)
		{
			if ((m_bmFlags & FLAG_TAG) != 0)
			{
				m_nTagInfo = decodeTable(aTable: m_aTabTags)
				if (m_nTagBits != 0)
				{
					m_nTags = m_nTagInfo & m_nTagMask
				}
				else {
					m_nTags = 1
				}
			}

			// Update the current word number.
			if (m_bCounting)
			{
				m_iWord += 1
			}
		}

		// Decode the possible enumeration information at this node.
		if ((m_bmFlags & FLAG_ENUM) != 0)
		{
			m_nEnum = decodeTable(aTable: m_aTabEnums)
			m_aWordNums[m_iLevel] = m_iWord + m_nEnum
		}
		else
		{
			m_aWordNums[m_iLevel] = 0
			m_nEnum = 0
		}

		// Decode the pointer to the next node in the state.
		alt_inline = false
		switch (m_bmFlags & FLAG_ALTERNATE)
		{
			case FLAG_ALT_NONE:		// No alternate node.
				m_aIndices[m_iLevel] = -1
				break

			case FLAG_ALT_INLINE:	// The alternate node is next.
				alt_inline = true
				break

			case FLAG_ALT_TABLE:	// Index is stored in a table.
				value = Int(m_aTabIndices[Int(m_aTabNodes[m_iDataNext]) & 0xFF]) & 0xFFFF
				m_iDataNext += 1
				m_aIndices[m_iLevel] = value
				break

			case FLAG_ALT_IMMED:	// Index stored as immediate data.
				value = decodeValue()
				m_aIndices[m_iLevel] = value
				break
				
			default:
				break
		}	

		// Decode the pointer the extension node.
		switch (m_bmFlags & FLAG_EXTEND)
		{
			case FLAG_EXT_NONE:		// No extend node.
				m_iNodeExtend = -1
				break

			case FLAG_EXT_INLINE:	// The extend node is next.
				m_aOffsets[m_iLevel + 1] = m_aOffsets[m_iLevel]
				m_iNodeExtend = m_iDataNext
				break

			case FLAG_EXT_OFFSET:	// Have an offset pointer.
				value = decodeTable(aTable: m_aTabOffsets)
				if (m_aOffsets[m_iLevel] == -1)
				{
					m_aOffsets[m_iLevel] = m_iDataNext + value
				}
				else
				{
					m_aOffsets[m_iLevel] += value
				}
				m_aOffsets[m_iLevel + 1] = -1
				m_iNodeExtend = m_aOffsets[m_iLevel]
				break

			case FLAG_EXT_TABLE:	// Index stored in a table.
				value = Int(m_aTabIndices[Int(m_aTabNodes[m_iDataNext]) & 0xFF]) & 0xFFFF
				m_iDataNext += 1
				m_iNodeExtend = value
				m_aOffsets[m_iLevel + 1] = -1
				break

			case FLAG_EXT_IMMED:	// Index stored as immediate data.
				value = decodeValue()
				m_iNodeExtend = value
				m_aOffsets[m_iLevel + 1] = -1
				break
				
			default:
				break
		}

		// We now know where the end of the node is.  If we have an alternate
		// pointer inline to the next node, then set the pointer.
		if (alt_inline)
		{
			m_aIndices[m_iLevel] = m_iDataNext
		}
		return true
	}

	/***********************************************************************************************
	 * Return the word indexed by the given number.  If traverse is set, make sure
	 * we can continue traversing the tree from this point.
	 */
	func
	getWord(
		nWord: Int,
		bTraverse: Bool
	) throws -> String?
	{
		var nWord = nWord
		var iData: Int
		var value: Int

		// Word numbers start at one.  Make sure we have atleast that.
		if (nWord == 0)
		{
			return nil
		}

		// If we have acceleration data, use it.
		if let aTabAccelData = m_aTabAccelData
		{
			if (!bTraverse)
			{
				// m_iWord doesn't need to be updated.
				m_bCounting = false

				// Position us at the table entry.
				nWord -= 1
				value = nWord >> m_nAccelSegShift
				nWord &= (1 << m_nAccelSegShift) - 1
				iData = (value << 2) + (value << 1)

				// Decode the number of we have to skip to get the first in
				// our segment.
				value = Int(aTabAccelData[iData]) & 0xFF
				iData += 1
				value |= (Int(aTabAccelData[iData]) & 0xFF) << 8
				iData += 1
				nWord += value + 1

				// Decode and set the jump position into the tree.
				value = Int(aTabAccelData[iData]) & 0xFF
				iData += 1
				value |= (Int(aTabAccelData[iData]) & 0xFF) << 8
				iData += 1
				value |= (Int(aTabAccelData[iData]) & 0x7) << 16
				m_iDataNext = value

				// Decode the prefix we skipped over.
				value = (Int(aTabAccelData[iData]) & 0xFF) >> 3
				iData += 1
				value |= (Int(aTabAccelData[iData]) & 0x3F) << 5

				// Decode the word starting at this index.
				for i in 0..<m_iLevel
				{
					if let aTabAccelChars = m_aTabAccelChars
					{
						m_acWord[i] = aTabAccelChars[value] & 0xFF
					}
					value += 1
				}
				m_acWord[m_iLevel] = 0

				// Decode the level we're jumping into the tree.
				m_iLevel = (Int(aTabAccelData[iData]) & 0xFF) >> 6

				// Decode this first node.
				_ = try decodeNode()
			}
			else
			{
				// Starting from the root node, traverse through the tree
				// counting up each word skipped.  When we get to the Nth
				// word, return it.
				m_bCounting = true
				_ = try scanRoot()
			}
		}
		else
		{
			// Starting from the root node, traverse through the tree
			// counting up each word skipped.  When we get to the Nth
			// word, return it.
			m_bCounting = true
			_ = try scanRoot()
		}

		while (true)
		{
			// If we're at the end of a word, the see if it's the one we
			// want.
			if ((m_bmFlags & FLAG_VALID) != 0)
			{
				nWord -= 1
				if (nWord == 0)
				{
					m_acWord[m_iLevel + 1] = 0
                    return String(bytes: m_acWord, encoding: .isoLatin1)
				}
			}

			// If this node has enumeration, then try to skip over its
			// sub-tree.  Otherwise we have to traverse it and count it
			// by hand.
			if (m_nEnum != 0 && m_nEnum < nWord)
			{
				nWord -= m_nEnum
			}
			else if (try scanExtend())
			{
				continue
			}

			// At the bottom of the tree or skipping over a section.  Try
			// to get an alternate node at this level or above.
			while (try !scanAlternate())
			{
				if (!scanRetreat())
				{
					return nil
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
	func
	getWordCount() -> Int
	{
		return m_nWords
	}

	/***********************************************************************************************
	 * Assuming a two byte tag encoding, decode the tag and return it.
	 */
	func
	getTagValue(iTag: Int) -> Int
	{
		if (iTag >= m_nTags)
		{
			return -1
		}
		if (m_nTagBits != 0)
		{
			var iTag = 0
			if let aTabTagIndices = m_aTabTagIndices
			{
				iTag = Int(aTabTagIndices[(m_nTagInfo >> m_nTagBits) + iTag] & 0xFFFF)
			}
			if let aTabTagData = m_aTabTagData
			{
				iTag = Int(((aTabTagData[iTag] & 0xFF) << 8) | (aTabTagData[iTag] & 0xFF))
			}
			return iTag
		}
		return m_nTagInfo
	}

	/***********************************************************************************************
	 * Return true if the given string forms a valid word.
	 *
	 * @param strWord
	 *		A string of letters.
	 * @return
	 *		True if these letters form a word.
	 */
	func
	isWord(strWord: String) throws -> Bool
	{
		m_bCounting = false
		if (try scanRoot())
		{
			for (iWord, chWord) in strWord.enumerated()
			{
				let cWord = chWord.asciiValue
				while (cWord != m_cNode)
				{
					if (try !scanAlternate())
					{
						return false
					}
				}

				if (iWord + 1 >= strWord.count)
				{
					return (m_bmFlags & FLAG_VALID) != 0
				}

				if (try !scanExtend())
				{
					return false
				}
			}
		}
		return false
	}

	/***********************************************************************************************
	 * Return true if the given string forms a valid word.
	 *
	 * @param aWord
	 *		An array of letters.
	 * @return
	 *		True if these letters form a word.
	 */
	func
	isWord(aWord: [UInt8]) throws -> Bool
	{
		m_bCounting = false
		if (try scanRoot())
		{
			var iWord = 0
			var cWord = aWord[0]
			repeat
			{
				while (cWord != m_cNode)
				{
					if (try !scanAlternate())
					{
						return false
					}
				}
				iWord += 1
				cWord = aWord[iWord]
				if (cWord == 0)
				{
					return (m_bmFlags & FLAG_VALID) != 0
				}
			} while (try scanExtend())
		}
		return false
	}

	//**********************************************************************************************
	// Read in a data table.
	func
	readTable(
		aTable: inout [UInt16],
		iOffset: Int) throws
	{
		m_fHandle.seek(toFileOffset: UInt64(iOffset))
		for i in 0..<aTable.count
		{
			aTable[i] = try readUInt16()
		}
	}

	//**********************************************************************************************
	// Read in a data table.
	func
	readTable8(
		aTable: inout [UInt8],
		iOffset: Int) throws
	{
		m_fHandle.seek(toFileOffset: UInt64(iOffset))
		for i in 0..<aTable.count
		{
			aTable[i] = try readUInt8()
		}
	}

	/***********************************************************************************************
	 * Read in a number.
	 */
	func
	readUInt8() throws -> UInt8
	{
        if #available(iOS 13.4, *)
		{
	        guard let data = try m_fHandle.read(upToCount: 1) else
			{
				throw TreeWordsError.ERROR("TreeWords: read access failure")
			}
	        return data.first!
        }
		else
		{
            throw TreeWordsError.ERROR("TreeWords: IOS 13.4 or later required.")
        }
	}

	/***********************************************************************************************
	 * Read in a number.
	 */
	func
	readUInt16() throws -> UInt16
	{
        return (UInt16(try readUInt8()) << 8) | UInt16(try readUInt8())
	}

	/***********************************************************************************************
	 * Read in a number.
	 */
	func
	readUInt32() throws -> UInt32
	{
        return
			(UInt32(try readUInt8()) << 24) |
			(UInt32(try readUInt8()) << 16) |
			(UInt32(try readUInt8()) << 8) |
			UInt32(try readUInt8())
	}

	/***********************************************************************************************
	 * Provide access to the word list data base.  The caller passes a function
	 * pointer to the call-back routine "prune()."  The routine can be used to
	 * search for a word, add them to a list, and prune over unwanted parts of
	 * the data base (by returning a non-zero level).
	 */
	func
	scan(
		scanWords: ScanWords,
		bmScanFlags: Int) throws
	{
		// Check to see if we're starting from a random location within the
		// word list.  If so, allow the search to loop around at the end of
		// the word list.

		var bRandomized = false
		if ((bmScanFlags & ScanWordsConstants.SCAN_FLAG_RANDOM) != 0)
		{
			bRandomized = true
			let nWords = getWordCount() + 1
			_ = try getWord(nWord: Int.random(in: 1..<nWords), bTraverse: true)
		}

		// For trees, ignoring the word numbers makes traversal faster.
		m_bCounting = (bmScanFlags & ScanWordsConstants.SCAN_FLAG_QUICK) == 0

		// If we're search for the next word in a list, we have to jump past
		// the current word.
		var bGetNextWord = false
		if ((bmScanFlags & ScanWordsConstants.SCAN_FLAG_NEXT) != 0)
		{
			bGetNextWord = true
		}
		// If all these position bits are off, then search from the start of
		// the word list.
		else if (
			(bmScanFlags &
			(ScanWordsConstants.SCAN_FLAG_CONTINUE | ScanWordsConstants.SCAN_FLAG_RANDOM)) == 0)
		{
			_ = try scanRoot()
		}

		while (true)
		{
			repeat
			{
				// If needed, skip to the next word.
				if (bGetNextWord)
				{
					bGetNextWord = false
					continue
				}
				
				// Keep looping down the tree until we get a word.
				if ((m_bmFlags & FLAG_VALID) == 0)
				{
					continue
				}

				// Have a word.  Call the prune function.
                let iLevel = try scanWords.prune(aWord: m_acWord, iWord: m_iWord)
				switch (iLevel)
				{
					case ScanWordsConstants.SCAN_LEVEL_QUIT:		// Exit the scan.
						return

					case ScanWordsConstants.SCAN_LEVEL_RESET:	// Reset to the start.
						_ = try scanRoot()
						continue

					case ScanWordsConstants.SCAN_LEVEL_RANDOM:	// Randomize our position.
						bRandomized = true
						let nWords = getWordCount() + 1
						_ = try getWord(nWord: Int.random(in: 1..<nWords), bTraverse: true)
						continue

					case ScanWordsConstants.SCAN_LEVEL_CONTINUE:	// No pruning, just continue.
						continue

					default:			// Prune at this level in the tree.
						if (iLevel > m_iLevel)
						{
							continue
						}
						try setLevel(iLevel: iLevel)
				}
				break
			} while (try scanExtend())

			// Alternate to the next word off the given level.
			while (try !scanAlternate())
			{
				if (!scanRetreat())
				{
					// At the end of the tree.  If the prune
					// function asks for a random placement,
					// then reset to the start.
					if (bRandomized)
					{
						_ = try scanRoot()
						break
					}
					return
				}
			}
		}
	}

	/***********************************************************************************************
	 * Traverse to an alternate letter within our word.  Return false if there is
	 * no such letter.
	 */
	func
	scanAlternate() throws -> Bool
	{
		if (m_bCounting && !m_bRetreating)
		{
			if (m_nEnum != 0)
			{
				m_iWord += m_nEnum
			}
			else if (try scanExtend())
			{
				// No count information. Recurse into the tree and
				// count up all the valid nodes by hand.
				while (try scanAlternate())
				{
				}
				_ = scanRetreat()
			}
		}

		m_iDataNext = m_aIndices[m_iLevel]
		if (m_iDataNext == -1)
		{
			return false
		}
		m_bRetreating = false
		return try decodeNode()
	}

	/***********************************************************************************************
	 * Extend to the next letter in the current word.  Return false if there is no
	 * such letter.
	 */
	func
	scanExtend() throws -> Bool
	{
		m_iDataNext = m_iNodeExtend
		if (m_iDataNext == -1)
		{
			return false
		}

		m_iLevel += 1
		m_bRetreating = false
		return try decodeNode()
	}

	/***********************************************************************************************
	 * Decrease the depth or length into the current tree word.  This is used to
	 * traverse out of the end of a sub-tree.  Returns false if we are at the end
	 * of the tree.
	 */
	func
	scanRetreat() -> Bool
	{
		if (m_iLevel == 0)
		{
			return false
		}

		m_bRetreating = true
		m_acWord[m_iLevel] = 0
		m_iLevel -= 1
		return true
	}

	/***********************************************************************************************
	 * Reset to the first node in the tree.
	 */
	func
	scanRoot() throws -> Bool
	{
		m_iWord = 0
		m_iLevel = -1
		m_aOffsets[0] = -1
		m_iNodeExtend = m_iNodeRoot
		m_cRoot = 0

		if (try scanExtend())
		{
			// If we have a group of trees and the user only wants one of them,
			// then reset to this root.
			if (m_cRoot != 0)
			{
				while (m_cNode != m_cRoot)
				{
					if (try !scanAlternate())
					{
						m_iNodeExtend = -1
						return false
					}
				}
				m_iWord = 0
				m_iLevel = -1
				m_aOffsets[0] = m_aOffsets[1]
				
				return try scanExtend()
			}
		}
		return true
	}

	/***********************************************************************************************
	 * Jump back up to this level.  The main problem is our knowledge of the word
	 * number at this level.  If it's not known, we'll have to scan for it.
	 */
	func
	setLevel(iLevel: Int) throws
	{
		if (iLevel < m_iLevel)
		{
			if (!m_bCounting) {
				m_iLevel = iLevel + 1
				_ = scanRetreat()
			}
			else if (m_aWordNums[iLevel] != 0)
			{
				m_iWord = m_aWordNums[iLevel]
				m_iLevel = iLevel + 1
				_ = scanRetreat()
			}
			else
			{
				repeat
				{
					if (try !scanAlternate())
					{
						if (!scanRetreat())
						{
							// Should never happen.
							return
						}
					}
				} while (iLevel < m_iLevel)
			}
		}
	}
}

//**************************************************************************************************

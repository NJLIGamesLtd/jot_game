/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2017-11-30
 *
 * Words is a word list decompression interface.
 */

package com.jot.decomp;

//**************************************************************************************************

public interface Words
{
	/***********************************************************************************************
	 * Return the word indexed by the given number.  If traverse is set, make sure we can continue
	 * traversing to other words from this point.
	 *
	 * @param nWord
	 *		The nth word in the list (starting with 1).
	 * @param bTraverse
	 *		True if you need to scan through the word list.
	 * @return
	 *		True if we had a letter to scan to.
	 */
	public String
	getWord(
		int nWord,
		boolean bTraverse);

	/***********************************************************************************************
	 * Return a clone of this object.
	 *
	 * @return
	 *		A clone.
	 */
	public Words
	clone();

	/***********************************************************************************************
	 * Return the number of words in our list.
	 *
	 * @return
	 *		The number of words in our list.
	 */
	public int
	getWordCount();

	/***********************************************************************************************
	 * Return the tag value attached to a word.
	 *
	 * @param iTag
	 *		The index to this tag.
	 * @return
	 *		The nth tag value for the active word.
	 */
	public int
	getTagValue(int iTag);

	/***********************************************************************************************
	 * Return true if the given string forms a valid word.
	 *
	 * @param aWord
	 *		An array of letters.
	 * @return
	 *		True if these letters form a word.
	 */
	public boolean
	isWord(char[] aWord);

	/***********************************************************************************************
	 * Return true if the given string forms a valid word.
	 *
	 * @param strWord
	 *		A string of letters.
	 * @return
	 *		True if these letters form a word.
	 */
	public boolean
	isWord(String strWord);

	/***********************************************************************************************
	 * Provide access to the word list data base.  The caller passes a function pointer to the
	 * call-back class "ScanWords."  Its prune() routine can be used to search for a word, add
	 * them to a list, and prune over unwanted parts of the data base (by returning a non-zero
	 * level).
	 *
	 * @param scanWords
	 *		Object which contains a prune() method and storage of found words.
	 * @param bmScanFlags
	 *		SCAN_FLAG bits which descript the scan required.
	 */
	public void
	scan(
		ScanWords scanWords,
		int bmScanFlags
	) throws Exception;

	/***********************************************************************************************
	 * Traverse to an alternate letter within our word.  Return false if there is
	 * no such letter.
	 *
	 * @return
	 *		True if we had a letter to scan to.
	 */
	public boolean
	scanAlternate();

	/***********************************************************************************************
	 * Extend to the next letter in the current word.  Return false if there is no
	 * such letter.
	 *
	 * @return
	 *		True if we had a letter to scan to.
	 */
	public boolean
	scanExtend();

	/***********************************************************************************************
	 * Decrease the depth or length into the current word.  This is used to traverse out of the
	 * end of a sub-list of words.  Returns false if we are at the end of the list.
	 *
	 * @return
	 *		True if we had a letter to scan to.
	 */
	public boolean
	scanRetreat();

	/***********************************************************************************************
	 * Reset to the first letter in the word list.
	 *
	 * @return
	 *		True if we had a letter to scan to.
	 */
	public boolean
	scanRoot();

	/***********************************************************************************************
	 * Jump back up to this level.  The main problem is our knowledge of the word
	 * number at this level.  If it's not known, we'll have to scan for it.
	 *
	 * @param iLevel
	 *		The index level into our word.
	 */
	public void
	setLevel(int iLevel);
}

//**************************************************************************************************

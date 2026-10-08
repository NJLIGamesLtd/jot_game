/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A track Section is a single Position within a List of Sections.
 */

package com.jot.track;

public class SectionPtr
{
	private Section m_section;

	/***********************************************************************************************
	 * Default constructor.
	 */
	public SectionPtr()
	{
		m_section = new Section();
	}

	/***********************************************************************************************
	 * Default constructor.
	 */
	public SectionPtr(Section section)
	{
		m_section = section;
	}

	/***********************************************************************************************
	 * Override to base equals() comparison on the 3D Vector values.
	 *
	 * @param section
	 *		Compare to this Section.
	 * @return
	 *		True if the Vector values match these values.
	 */
	public boolean equals(final Section section)
	{
		return m_section.equals(section);
	}

	/***********************************************************************************************
	 * Return the index of this section into its parent list of sections.
	 *
	 * @return
	 *		The index of this section into its parent list of sections.
	 */
	public int getIndex()
	{
		return m_section.getIndex();
	}

	/***********************************************************************************************
	 * Return the section.
	 *
	 * @return
	 *		The section within a list of sections.
	 */
	public Section getSection()
	{
		return m_section;
	}

	/***********************************************************************************************
	 * Set the section.
	 *
	 * @param section
	 *		The section within a list of sections.
	 */
	public void setSection(Section section)
	{
		m_section = section;
	}
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A track Section is a single Position within a List of Sections.
 */

package com.jot.track;

import com.jot.vector.Orientation;
import com.jot.vector.Position;

public class Section extends Position
{
	private static int s_idQuadrant = 0;

	public static final int s_aNumVelSteps[] =
	{
		0,
		1,
		2 + 1,
		3 + 2 + 1,
		4 + 3 + 2 + 1,
		5 + 4 + 3 + 2 + 1,
		6 + 5 + 4 + 3 + 2 + 1,
		7 + 6 + 5 + 4 + 3 + 2 + 1,
		8 + 7 + 6 + 5 + 4 + 3 + 2 + 1,
		9 + 8 + 7 + 6 + 5 + 4 + 3 + 2 + 1,
		10 + 9 + 8 + 7 + 6 + 5 + 4 + 3 + 2 + 1,
		11 + 10 + 9 + 8 + 7 + 6 + 5 + 4 + 3 + 2 + 1,
	};

	private Section m_prev;
	private Section m_next;
	public int m_iSection;
	private int m_idQuadrant;

	/***********************************************************************************************
	 * Return the next available quadrant ID.
	 *
	 * @return
	 *		The next available quadrant ID.
	 */
	public static int GetNextQuadrantId()
	{
		return ++s_idQuadrant;
	}

	/***********************************************************************************************
	 * Default constructor.
	 */
	public Section()
	{
		super();
		m_prev = m_next = null;
	}

	/***********************************************************************************************
	 * Section constructor requires initial 3D location vector values, an Orientation and
	 * the next Section within the List of Sections.
	 *
	 * @param x
	 *		Initial X space value.
	 * @param y
	 *		Initial Y space value.
	 * @param z
	 *		Initial Z space value.
	 * @param posBase
	 *		The base position that our x,y,z is an offset of.
	 * @param previous
	 *		The previous Section within the List of Sections.
	 * @param next
	 *		The next Section within the List of Sections.
	 */
	public Section(
		int x, int y, int z,
		Position posBase,
		Section previous,
		Section next)
	{
		super(x + posBase.m_x, y + posBase.m_y, z + posBase.m_z, posBase.getOrientation());
		m_prev = previous;
		m_next = next;
	}

	/***********************************************************************************************
	 * Section constructor requires initial 3D location vector values, an Orientation and
	 * the next Section within the List of Sections.
	 *
	 * @param x
	 *		Initial X space value.
	 * @param y
	 *		Initial Y space value.
	 * @param z
	 *		Initial Z space value.
	 * @param orientation
	 *		An orientation object to be shared by this Section.
	 * @param previous
	 *		The previous Section within the List of Sections.
	 * @param next
	 *		The next Section within the List of Sections.
	 */
	public Section(
		int x, int y, int z,
		Orientation orientation,
		Section previous,
		Section next)
	{
		super(x, y, z, orientation);
		m_prev = previous;
		m_next = next;
	}

	/***********************************************************************************************
	 * Section constructor requires initial 3D location vector values, an Orientation and
	 * the next Section within the List of Sections.
	 *
	 * @param x
	 *		Initial X space value.
	 * @param y
	 *		Initial Y space value.
	 * @param z
	 *		Initial Z space value.
	 * @param heading
	 *		The heading of this track position.
	 * @param previous
	 *		The previous Section within the List of Sections.
	 * @param next
	 *		The next Section within the List of Sections.
	 */
	public Section(
		int x, int y, int z,
		int heading,
		Section previous,
		Section next)
	{
		super(x, y, z, heading);
		m_prev = previous;
		m_next = next;
	}

	/***********************************************************************************************
	 * Section constructor that takes a Section and a reference to the next in a List
	 * of Sections.
	 *
	 * @param postion
	 *		Initial section values.
	 * @param previous
	 *		The previous Section within the List of Sections.
	 * @param next
	 *		The next Section within the List of Sections.
	 */
	public Section(
		final Position position,
		Section previous,
		Section next)
	{
		this(position.m_x, position.m_y, position.m_z, position.getOrientation(), previous, next);
	}

	/***********************************************************************************************
	 * Section constructor that takes a Section and a reference to the next in a List
	 * of Sections.
	 *
	 * @param posOffset
	 *		Offset position from the base object.
	 * @param posBase
	 *		The base position that our x,y,z is an offset of.
	 * @param previous
	 *		The previous Section within the List of Sections.
	 * @param next
	 *		The next Section within the List of Sections.
	 */
	public Section(
		final Position posOffset,
		final Position posBase,
		Section previous,
		Section next)
	{
		this(posBase.m_x, posBase.m_y, posBase.m_z,
			posBase.getOrientation().getHeading() + posOffset.getOrientation().getHeading(),
			previous, next);

		double heading = posBase.getHeadingInRadians();
		double cosHeading = Math.cos(heading);
		double sinHeading = Math.sin(heading);
		m_x += posOffset.m_x * cosHeading + posOffset.m_y * sinHeading;
		m_y += posOffset.m_x * sinHeading + posOffset.m_y * cosHeading;
		m_z += posOffset.m_z;
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
		if (section != null)
		{
			return
				m_x == section.m_x &&
				m_y == section.m_y &&
				m_z == section.m_z;
		}
		return false;
	}

	/***********************************************************************************************
	 * Return the index of this section into its parent list of sections.
	 *
	 * @return
	 *		The index of this section into its parent list of sections.
	 */
	public int getIndex()
	{
		return m_iSection;
	}

	/***********************************************************************************************
	 * Return a reference to the next possible Section within a List of Sections.
	 *
	 * @return
	 *		A reference to the next possible Section within a List of Sections.
	 */
	public Section getNext()
	{
		return m_next;
	}

	/***********************************************************************************************
	 * Return a reference to the previous possible Section within a List of Sections.
	 *
	 * @return
	 *		A reference to the previous possible Section within a List of Sections.
	 */
	public Section getPrevious()
	{
		return m_prev;
	}

	/***********************************************************************************************
	 * Get the Track quadrant ID where this TrackCar is currently located.
	 *
	 * @return
	 *		The Track quadrant ID where this TrackCar is currently located.
	 */
	public int getQuadrantId()
	{
		return m_idQuadrant;
	}

	/***********************************************************************************************
	 * Set the index of this section into its parent list of sections.
	 *
	 * @param index
	 *		The index of this section into its parent list of sections.
	 */
	public void setIndex(int index)
	{
		m_iSection = index;
	}

	/***********************************************************************************************
	 * Assign a reference to the next possible Section within a List of Sections.
	 *
	 * @param next
	 *		A reference to the next possible Section within a List of Sections.
	 */
	public void setNext(Section next)
	{
		m_next = next;
	}

	/***********************************************************************************************
	 * Assign a reference to the previous possible Section within a List of Sections.
	 *
	 * @param next
	 *		A reference to the previous possible Section within a List of Sections.
	 */
	public void setPrevious(Section previous)
	{
		m_prev = previous;
	}

	/***********************************************************************************************
	 * Assign the Track quadrant to this Section.
	 *
	 * @param idQuadrant
	 *		The Track quadrant.
	 */
	public void setQuadrantId(int idQuadrant)
	{
		m_idQuadrant = idQuadrant;
	}
}

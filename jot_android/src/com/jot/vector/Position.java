/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Position is a Location that contains an Orientation.
 */

package com.jot.vector;

public class Position extends Location
{
	private Orientation m_orientation;

	/***********************************************************************************************
	 * Constructor that defaults to zero values.
	 */
	public Position()
	{
		super();
		m_orientation = new Orientation();
	}

	/***********************************************************************************************
	 * Copy constructor.
	 *
	 * @param position
	 *		Making a copy of this position.
	 */
	public Position(Position position)
	{
		super(position.m_x, position.m_y, position.m_z);
		m_orientation = new Orientation(position.m_orientation);
	}

	/***********************************************************************************************
	 * Position constructor requires initial 3D location vector values and a heading.
	 *
	 * @param x
	 *		Initial X space value.
	 * @param y
	 *		Initial Y space value.
	 * @param z
	 *		Initial Z space value.
	 * @param heading
	 *		Initial angular heading between -180.0 and 180.0.
	 */
	public Position(int x, int y, int z, int heading)
	{
		super(x, y, z);
		m_orientation = new Orientation(heading);
	}

	/***********************************************************************************************
	 * Position constructor requires initial 3D location vector values and an Orientation.
	 *
	 * @param x
	 *		Initial X space value.
	 * @param y
	 *		Initial Y space value.
	 * @param z
	 *		Initial Z space value.
	 * @param orientation
	 *		An orientation object to be shared by this Position.
	 */
	public Position(int x, int y, int z, Orientation orientation)
	{
		super(x, y, z);
		if ((m_orientation = orientation) == null)
		{
			m_orientation = new Orientation();
		}
	}

	/***********************************************************************************************
	 * Add a delta to our heading in this 3D Vector value.
	 *
	 * @param delta
	 *		The delta value to add to our anglar heading of this Orientation.
	 */
	public void addHeading(int delta)
	{
		m_orientation.addHeading(delta);
	}

	/***********************************************************************************************
	 * Add a delta to our heading in this 3D Vector value.
	 *
	 * @param delta
	 *		The delta value to add to our anglar heading of this Orientation.
	 */
	public void addPitch(int delta)
	{
		m_orientation.addPitch(delta);
	}
	
	/***********************************************************************************************
	 * Add a delta to our heading in this 3D Vector value.
	 *
	 * @param delta
	 *		The delta value to add to our anglar heading of this Orientation.
	 */
	public void addRoll(int delta)
	{
		m_orientation.addRoll(delta);
	}
	
	/***********************************************************************************************
	 * Return the angular heading of this Orientation.
	 *
	 * @return
	 *		The angular heading of this Orientation.
	 */
	public int getHeading()
	{
		return m_orientation.m_z;
	}
	
	/***********************************************************************************************
	 * Return the angular heading of this Orientation in radians.
	 *
	 * @return
	 *		The angular heading of this Orientation in radians.
	 */
	public double getHeadingInRadians()
	{
		return m_orientation.getHeadingInRadians();
	}
	
	/***********************************************************************************************
	 * Return a reference to our Orientation object.
	 *
	 * @return
	 *		A reference to our Orientation object.
	 */
	public Orientation getOrientation()
	{
		return m_orientation;
	}

	/***********************************************************************************************
	 * Return the anglar pitch value of this Orientation.
	 *
	 * @return
	 *		The anglar pitch value of this Orientation.
	 */
	public int getPitch()
	{
		return m_orientation.m_x;
	}
	
	/***********************************************************************************************
	 * Return the anglar pitch value of this Orientation in radians.
	 *
	 * @return
	 *		The anglar pitch value of this Orientation in radians.
	 */
	public double getPitchInRadians()
	{
		return m_orientation.getPitchInRadians();
	}
	
	/***********************************************************************************************
	 * Return the angular roll value of this Orientation.
	 *
	 * @return
	 *		The angular roll value of this Orientation.
	 */
	public int getRoll()
	{
		return m_orientation.m_y;
	}
	
	/***********************************************************************************************
	 * Return the angular roll value of this Orientation in radians.
	 *
	 * @return
	 *		The angular roll value of this Orientation in radians.
	 */
	public double getRollInRadians()
	{
		return m_orientation.getRollInRadians();
	}
	
	/***********************************************************************************************
	 * Assign the angular heading of this Orientation.
	 *
	 * @param heading
	 *		The angular heading of this Orientation.
	 */
	public void setHeading(int heading)
	{
		m_orientation.setHeading(heading);
	}

	/***********************************************************************************************
	 * Assign our Orientation values based on a vector orientation.
	 *
	 * @param orientation
	 *		A reference to our Orientation object.
	 */
	public void setOrientation(Orientation orientation)
	{
		m_orientation.set(orientation);
	}

	/***********************************************************************************************
	 * Assign the angular pitch value of this Orientation.
	 *
	 * @param pitch
	 *		The angular pitch value of this Orientation.
	 */
	public void setPitch(int pitch)
	{
		m_orientation.setPitch(pitch);
	}
	
	/***********************************************************************************************
	 * Assign the angular roll value of this Orientation.
	 *
	 * @param roll
	 *		The angular roll value of this Orientation.
	 */
	public void setRoll(int roll)
	{
		m_orientation.setRoll(roll);
	}
	
	/***********************************************************************************************
	 * Override to output the string representation of this Position based
	 * on its Location and Orientation.
	 *
	 * @return
	 *		A string representation of this Orientation.
	 */
	public String toString()
	{
		return "Position (" + super.toString() + "|" + m_orientation + ")";
	}
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A PositionPtr is a stable pointer object to a Position that can change to store the Location and
 * Orientation.
 */

package com.jot.vector;

public class PositionPtr
{
	private Position m_position;

	/***********************************************************************************************
	 * Constructor that defaults to zero values.
	 */
	public PositionPtr()
	{
		m_position = new Position();
	}

	/***********************************************************************************************
	 * PositionPtr constructor that provides a position.
	 *
	 * @param position
	 *		Initial angular heading between -180.0 and 180.0.
	 */
	public PositionPtr(Position position)
	{
		if ((m_position = position) == null)
		{
			m_position = new Position();
		}
	}

	/***********************************************************************************************
	 * PositionPtr constructor requires initial 3D location vector values and a heading.
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
	public PositionPtr(int x, int y, int z, int heading)
	{
		m_position = new Position(x, y, z, heading);
	}

	/***********************************************************************************************
	 * PositionPtr constructor requires initial 3D location vector values and an Orientation.
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
	public PositionPtr(int x, int y, int z, Orientation orientation)
	{
		m_position = new Position(x, y, z, orientation);
	}

	/***********************************************************************************************
	 * Add a delta to our heading in this 3D Vector value.
	 *
	 * @param delta
	 *		The delta value to add to our anglar heading of this Orientation.
	 */
	public void addHeading(int delta)
	{
		m_position.addHeading(delta);
	}

	/***********************************************************************************************
	 * Add a delta to our heading in this 3D Vector value.
	 *
	 * @param delta
	 *		The delta value to add to our anglar heading of this Orientation.
	 */
	public void addPitch(int delta)
	{
		m_position.addPitch(delta);
	}
	
	/***********************************************************************************************
	 * Add a delta to our heading in this 3D Vector value.
	 *
	 * @param delta
	 *		The delta value to add to our anglar heading of this Orientation.
	 */
	public void addRoll(int delta)
	{
		m_position.addRoll(delta);
	}
	
	/***********************************************************************************************
	 * Return the angular heading of this Orientation.
	 *
	 * @return
	 *		The angular heading of this Orientation.
	 */
	public int getHeading()
	{
		return m_position.getHeading();
	}

	/***********************************************************************************************
	 * Return the location object.
	 *
	 * @return
	 *		The location object.
	 */
	public Location getLocation()
	{
		return m_position;
	}

	/***********************************************************************************************
	 * Return the position object.
	 *
	 * @return
	 *		The position object.
	 */
	public Position getPosition()
	{
		return m_position;
	}

	/***********************************************************************************************
	 * Return a reference to our Orientation object.
	 *
	 * @return
	 *		A reference to our Orientation object.
	 */
	public Orientation getOrientation()
	{
		return m_position.getOrientation();
	}

	/***********************************************************************************************
	 * Return the anglar pitch value of this Orientation.
	 *
	 * @return
	 *		The anglar pitch value of this Orientation.
	 */
	public int getPitch()
	{
		return m_position.getPitch();
	}
	
	/***********************************************************************************************
	 * Return the angular roll value of this Orientation.
	 *
	 * @return
	 *		The angular roll value of this Orientation.
	 */
	public int getRoll()
	{
		return m_position.getRoll();
	}
	
	/***********************************************************************************************
	 * Assign the angular heading of this Orientation.
	 *
	 * @param heading
	 *		The angular heading of this Orientation.
	 */
	public void setHeading(int heading)
	{
		m_position.setHeading(heading);
	}

	/***********************************************************************************************
	 * Immediately assign our existing location to the new location values provided.
	 *
	 * @param x
	 *		New x vector location.
	 * @param y
	 *		New y vector location.
	 */
	public void setLocation(int x, int y)
	{
		m_position.set(x, y);
	}

	/***********************************************************************************************
	 * Immediately assign our existing location to the new location values provided.
	 *
	 * @param x
	 *		New x vector location.
	 * @param y
	 *		New y vector location.
	 * @param z
	 *		New z vector location.
	 */
	public void setLocation(int x, int y, int z)
	{
		m_position.set(x, y, z);
	}

	/***********************************************************************************************
	 * Immediately assign our existing location to the new location values provided.
	 *
	 * @param location
	 *		New vector location.
	 */
	public void setLocation(final Location location)
	{
		m_position.set(location);
	}

	/***********************************************************************************************
	 * Assign our Orientation values based on a vector orientation.
	 *
	 * @param orientation
	 *		A reference to our Orientation object.
	 */
	public void setOrientation(Orientation orientation)
	{
		m_position.setOrientation(orientation);
	}

	/***********************************************************************************************
	 * Assign the angular pitch value of this Orientation.
	 *
	 * @param pitch
	 *		The angular pitch value of this Orientation.
	 */
	public void setPitch(int pitch)
	{
		m_position.setPitch(pitch);
	}
	
	/***********************************************************************************************
	 * Assign a position object to our pointer.
	 *
	 * @param position
	 *		A reference to our Position object.
	 */
	public void setPosition(Position position)
	{
		m_position = position;
	}

	/***********************************************************************************************
	 * Assign the angular roll value of this Orientation.
	 *
	 * @param roll
	 *		The angular roll value of this Orientation.
	 */
	public void setRoll(int roll)
	{
		m_position.setRoll(roll);
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
		return m_position.toString();
	}
}

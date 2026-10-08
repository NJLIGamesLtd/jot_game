/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * An Orientation is Vector extension that represents a 3D vector direction that an item is
 * facing.
 */

package com.jot.vector;

public class Orientation extends Vector
{
	/***********************************************************************************************
	 * Constructor that defaults to zero values.
	 */
	public Orientation()
	{
		super();
	}

	/***********************************************************************************************
	 * Copy constructor.
	 *
	 * @param orientation
	 *		Making a copy of this orientation.
	 */
	public Orientation(Orientation orientation)
	{
		super(orientation.m_x, orientation.m_y, orientation.m_z);
	}

	/***********************************************************************************************
	 * Orientation constructor requires initial heading vector value.
	 *
	 * @param heading
	 *		Initial angular heading between -180.0 and 180.0.
	 */
	public Orientation(int heading)
	{
		super(0, 0, heading);

		while (m_z < -180)
		{
			m_z += 360;
		}
		while (m_z > 180)
		{
			m_z -= 360;
		}
	}

	/***********************************************************************************************
	 * Orientation constructor requires initial 3D vector values.
	 *
	 * @param pitch
	 *		Initial angular pitch value between -180 and 180.
	 * @param roll
	 *		Initial angular roll value between -180 and 180.
	 * @param heading
	 *		Initial angular heading between -180 and 180.
	 * @throws
	 *		Exception if initial values are outside angular bounds.
	 */
	public Orientation(int pitch, int roll, int heading) throws Exception
	{
		super(pitch, roll, heading);

		if (pitch < -180 || pitch > 180)
		{
			throw new Exception("Orientation pitch value restricted between -180 and 180");
		}
		if (roll < -180 || roll > 180)
		{
			throw new Exception("Orientation roll value restricted between -180 and 180");
		}
		if (heading < -180 || heading > 180)
		{
			throw new Exception("Orientation heading restricted between -180 and 180");
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
		setHeading(m_z + delta);
	}

	/***********************************************************************************************
	 * Add a delta to our heading in this 3D Vector value.
	 *
	 * @param delta
	 *		The delta value to add to our angular heading of this Orientation.
	 */
	public void addPitch(int delta)
	{
		setPitch(m_x + delta);
	}
	
	/***********************************************************************************************
	 * Add a delta to our heading in this 3D Vector value.
	 *
	 * @param delta
	 *		The delta value to add to our angular heading of this Orientation.
	 */
	public void addRoll(int delta)
	{
		setRoll(m_y + delta);
	}
	
	/***********************************************************************************************
	 * Return the angular heading of this Orientation.
	 *
	 * @return
	 *		The angular heading of this Orientation.
	 */
	public int getHeading()
	{
		return m_z;
	}
	
	/***********************************************************************************************
	 * Return the angular heading of this Orientation in radians.
	 *
	 * @return
	 *		The angular heading of this Orientation in radians.
	 */
	public double getHeadingInRadians()
	{
		return Math.PI * m_z / 180;
	}
	
	/***********************************************************************************************
	 * Return the angular pitch value of this Orientation.
	 *
	 * @return
	 *		The angular pitch value of this Orientation.
	 */
	public int getPitch()
	{
		return m_x;
	}
	
	/***********************************************************************************************
	 * Return the angular pitch value of this Orientation in radians.
	 *
	 * @return
	 *		The angular pitch value of this Orientation in radians.
	 */
	public double getPitchInRadians()
	{
		return Math.PI * m_x / 180;
	}
	
	/***********************************************************************************************
	 * Return the angular roll value of this Orientation.
	 *
	 * @return
	 *		The angular roll value of this Orientation.
	 */
	public int getRoll()
	{
		return m_y;
	}
	
	/***********************************************************************************************
	 * Return the angular roll value of this Orientation in radians.
	 *
	 * @return
	 *		The angular roll value of this Orientation in radians.
	 */
	public double getRollInRadians()
	{
		return Math.PI * m_y / 180;
	}
	
	/***********************************************************************************************
	 * Assign the angular heading of this Orientation.
	 *
	 * @param heading
	 *		The angular heading of this Orientation.
	 */
	public void setHeading(int heading)
	{
		if ((m_z = heading) > 180)
		{
			do {
			} while ((m_z -= 360) > 180);
		}
		else if (m_z < -180)
		{
			do {
			} while ((m_z += 360) < -180);
		}
	}

	/***********************************************************************************************
	 * Assign the angular pitch value of this Orientation.
	 *
	 * @param pitch
	 *		The angular pitch value of this Orientation.
	 */
	public void setPitch(int pitch)
	{
		if ((m_x = pitch) > 180)
		{
			do {
			} while ((m_x -= 360) > 180);
		}
		else if (m_x < -180)
		{
			do {
			} while ((m_x += 360) < -180);
		}
	}
	
	/***********************************************************************************************
	 * Assign the angular roll value of this Orientation.
	 *
	 * @param roll
	 *		The angular roll value of this Orientation.
	 */
	public void setRoll(int roll)
	{
		if ((m_y = roll) > 180)
		{
			do {
			} while ((m_y -= 360) > 180);
		}
		else if (m_y < -180)
		{
			do {
			} while ((m_y += 360) < -180);
		}
	}
	
	/***********************************************************************************************
	 * Override to output the string representation of this Orientation based
	 * on its class label and 3D Vector values.
	 *
	 * @return
	 *		A string representation of this Orientation.
	 */
	public String toString()
	{
		return "Orientation" + super.toString();
	}
}

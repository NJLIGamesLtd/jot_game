/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Location is Vector extension that represents a 3D vector location within a place or region.
 */

package com.jot.vector;

public class Location extends Vector
{
	/***********************************************************************************************
	 * Constructor that defaults to zero values.
	 */
	public Location()
	{
		super();
	}

	/***********************************************************************************************
	 * Location constructor requires initial 3D vector values.
	 *
	 * @param x
	 *		Initial X space value.
	 * @param y
	 *		Initial Y space value.
	 * @param z
	 *		Initial Z space value.
	 */
	public Location(int x, int y, int z)
	{
		super(x, y, z);
	}

	/***********************************************************************************************
	 * Location constructor requires initial 3D vector values.
	 *
	 * @param location
	 *		Initial 3D vector values.
	 */
	public Location(final Location location)
	{
		super(location.m_x, location.m_y, location.m_z);
	}

	/***********************************************************************************************
	 * Override to output the string representation of this Location based
	 * on its class label and 3D Vector values.
	 *
	 * @return
	 *		A string representation of this Location.
	 */
	public String toString()
	{
		return "Location" + super.toString();
	}
}

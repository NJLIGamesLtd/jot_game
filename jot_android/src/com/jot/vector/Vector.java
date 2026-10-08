/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Vector provides a 3D vector representation for Position, Velocity and such in integer millimeters.
 */

package com.jot.vector;

public class Vector
{
	public int m_x, m_y, m_z;

	/***********************************************************************************************
	 * Constructor that defaults to zero values.
	 */
	public Vector()
	{
		m_x = m_y = m_z = 0;
	}

	/***********************************************************************************************
	 * Vector constructor requires initial 3D vector values.
	 *
	 * @param x
	 *		Initial X space value.
	 * @param y
	 *		Initial Y space value.
	 * @param z
	 *		Initial Z space value.
	 */
	public Vector(int x, int y, int z)
	{
		m_x = x;
		m_y = y;
		m_z = z;
	}

	/***********************************************************************************************
	 * Add a 3D delta to this Vector.
	 *
	 * @param delta
	 *		3D offset value to add to this Vector.
	 */
	public void add(final Vector delta)
	{
		if (delta != null)
		{
			m_x += delta.m_x;
			m_y += delta.m_y;
			m_z += delta.m_z;
		}
	}

	/***********************************************************************************************
	 * Add a multiple of a 3D delta to this Vector.
	 *
	 * @param delta
	 *		3D offset value to add to this Vector.
	 * @param nSteps
	 *		Multiplier to our 3D offset delta.
	 */
	public void add(final Vector delta, int nSteps)
	{
		if (delta != null)
		{
			m_x += delta.m_x * nSteps;
			m_y += delta.m_y * nSteps;
			m_z += delta.m_z * nSteps;
		}
	}

	/***********************************************************************************************
	 * Add a delta to X in this 3D Vector value.
	 *
	 * @param x
	 *		The delta X value.
	 */
	public void addX(int delta)
	{
		m_x += delta;
	}
	
	/***********************************************************************************************
	 * Add a delta to Y in this 3D Vector value.
	 *
	 * @param y
	 *		The delta Y value.
	 */
	public void addY(int delta)
	{
		m_y += delta;
	}
	
	/***********************************************************************************************
	 * Add a delta to Z in this 3D Vector value.
	 *
	 * @param z
	 *		The delta Z value.
	 */
	public void addZ(int delta)
	{
		m_z += delta;
	}

	/***********************************************************************************************
	 * Override to base equals() comparison on the 3D Vector values.
	 *
	 * @param vector
	 *		Compare to this Vector.
	 * @return
	 *		True if the Vector values match these values.
	 */
	public boolean equals(final Vector vector)
	{
		if (vector != null)
		{
			return
				m_x == vector.m_x &&
				m_y == vector.m_y &&
				m_z == vector.m_z;
		}
		return false;
	}

	/***********************************************************************************************
	 * Return the X vector of the 3D Vector value.
	 *
	 * @return
	 *		The X vector of the 3D Vector value.
	 */
	public int getX()
	{
		return m_x;
	}
	
	/***********************************************************************************************
	 * Return the Y vector of the 3D Vector value.
	 *
	 * @return
	 *		The Y vector of the 3D Vector value.
	 */
	public int getY()
	{
		return m_y;
	}
	
	/***********************************************************************************************
	 * Return the Z vector of the 3D Vector value.
	 *
	 * @return
	 *		The Z vector of the 3D Vector value.
	 */
	public int getZ()
	{
		return m_z;
	}

	/***********************************************************************************************
	 * Override to base hashCode() on the 3D Vector values.
	 *
	 * @return
	 *		Hash code based on the 3D Vector values.
	 */
	public int hashCode()
	{
		return m_x + (m_y << 8) + (m_z << 16);
	}
	
	/***********************************************************************************************
	 * Assign the provided Vector values to this Vector without changing the object
	 * instantiation.
	 *
	 * @param vector
	 *		New 3D Vector values.
	 */
	public void set(final Vector vector)
	{
		if (vector != null)
		{
			m_x = vector.m_x;
			m_y = vector.m_y;
			m_z = vector.m_z;
		}
	}

	/***********************************************************************************************
	 * Assign our vector to the new values provided.
	 *
	 * @param x
	 *		New x vector.
	 * @param y
	 *		New y vector.
	 */
	public void set(int x, int y)
	{
		m_x = x;
		m_y = y;
	}

	/***********************************************************************************************
	 * Assign our vector to the new values provided.
	 *
	 * @param x
	 *		New x vector.
	 * @param y
	 *		New y vector.
	 * @param z
	 *		New z vector.
	 */
	public void set(int x, int y, int z)
	{
		m_x = x;
		m_y = y;
		m_z = z;
	}

	/***********************************************************************************************
	 * Set the X vector to this 3D Vector value.
	 *
	 * @param x
	 *		The new X vector of the 3D Vector value.
	 */
	public void setX(int x)
	{
		m_x = x;
	}
	
	/***********************************************************************************************
	 * Set the Y vector to this 3D Vector value.
	 *
	 * @param y
	 *		The new Y vector of the 3D Vector value.
	 */
	public void setY(int y)
	{
		m_y = y;
	}
	
	/***********************************************************************************************
	 * Set the Z vector to this 3D Vector value.
	 *
	 * @param z
	 *		The new Z vector of the 3D Vector value.
	 */
	public void setZ(int z)
	{
		m_z = z;
	}

	/***********************************************************************************************
	 * Override to output the string representation of this Vector based
	 * on its 3D vector values.
	 *
	 * @return
	 *		A string representation of this Vector.
	 */
	public String toString()
	{
		return "[" + m_x + "," + m_y + "," + m_z + "]";
	}
}

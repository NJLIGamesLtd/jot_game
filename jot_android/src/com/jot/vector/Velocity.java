/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Velocity is Vector extension that represents a 3D vector speed of an item.
 */

package com.jot.vector;

import com.jot.simulation.Cycles;

public class Velocity extends Vector
{
	/***********************************************************************************************
	 * Constructor that defaults to zero values.
	 */
	public Velocity()
	{
		super();
	}

	/***********************************************************************************************
	 * Velocity constructor requires initial 3D vector values.
	 *
	 * @param dx
	 *		Initial angular X space Velocity.
	 * @param dy
	 *		Initial angular y space Velocity.
	 * @param dy
	 *		Initial angular Z space Velocity.
	 */
	public Velocity(int dx, int dy, int dz)
	{
		super(dx, dy, dz);
	}
	
	/***********************************************************************************************
	 * Velocity constructor requires initial absolute velocity value along with Locations
	 * the parent item is traveling to and from.
	 *
	 * @param velocity
	 *		Absolute velocity value.
	 * @param lociFrom
	 *		Location we are traveling from.
	 * @param lociTo
	 *		Location we are traveling to.
	 * @throws
	 *		Exception if either Location objects are null.
	 */
	public Velocity(int velocity, Location lociFrom, Location lociTo) throws Exception
	{
		super(0, 0, 0);
		
		if (lociFrom == null || lociTo == null)
		{
			throw new Exception("Velocity requires a from and to Position object.");
		}
		
		// Normalize the velocity based on the simulation update frequency.
		velocity /= Cycles.GetCyclesPerSecond();

		// Generate the angular deltas.
		int dx = m_x = lociTo.m_x - lociFrom.m_x;
		int dy = m_y = lociTo.m_y - lociFrom.m_y;
		int dz = m_z = lociTo.m_z - lociFrom.m_z;
		
		// Create the angular velocities.
		if (dz == 0)
		{
			if (dy == 0)
			{
				m_x = velocity;
				if (dx < 0)
				{
					m_x = -m_x;
				}
			}
			else if (dx == 0)
			{
				m_y = velocity;
				if (dy < 0)
				{
					m_y = -m_y;
				}
			}
			else
			{
				double angle = Math.atan((double)dy / dx);

				double va = velocity * Math.cos(angle);
				if (va < 0.0)
				{
					va -= 0.5;
				}
				else
				{
					va += 0.5;
				}
				m_x = (int)va;

				va = velocity * Math.sin(angle);
				if (va < 0.0)
				{
					va -= 0.5;
				}
				else
				{
					va += 0.5;
				}
				m_y = (int)va;

				if (dx < 0)
				{
					m_x = -m_x;
					m_y = -m_y;
				}
			}
		}
		else if (dx == 0)
		{
			if (dy == 0)
			{
				m_z = velocity;
				if (dz < 0)
				{
					m_z = -m_z;
				}
			}
			else
			{
				double angle = Math.atan((double)dy / dz);

				double va = velocity * Math.cos(angle);
				if (va < 0.0)
				{
					va -= 0.5;
				}
				else
				{
					va += 0.5;
				}
				m_z = (int)va;

				va = velocity * Math.sin(angle);
				if (va < 0.0)
				{
					va -= 0.5;
				}
				else
				{
					va += 0.5;
				}
				m_y = (int)va;

				if (dz < 0)
				{
					m_z = -m_z;
					m_y = -m_y;
				}
			}
		}
		else if (dy == 0)
		{
			double angle = Math.atan((double)dx / dz);

			double va = velocity * Math.cos(angle);
			if (va < 0.0)
			{
				va -= 0.5;
			}
			else
			{
				va += 0.5;
			}
			m_z = (int)va;

			va = velocity * Math.sin(angle);
			if (va < 0.0)
			{
				va -= 0.5;
			}
			else
			{
				va += 0.5;
			}
			m_x = (int)va;

			if (dz < 0)
			{
				m_z = -m_z;
				m_x = -m_x;
			}
		}
		else
		{
			double dxy = Math.sqrt((double)(dx * dx + dy * dy));
			double theta = Math.atan((double)dy / dx);
			double phi = Math.atan((double)dxy / dz);
			dxy = Math.abs(Math.sin(phi));

			double va = velocity * Math.cos(theta) * dxy;
			if (va < 0.0)
			{
				va -= 0.5;
			}
			else
			{
				va += 0.5;
			}
			m_x = (int)va;

			va = velocity * Math.sin(theta) * dxy;
			if (va < 0.0)
			{
				va -= 0.5;
			}
			else
			{
				va += 0.5;
			}
			m_y = (int)va;

			if (dx < 0)
			{
				m_x = -m_x;
				m_y = -m_y;
			}

			va = velocity * Math.cos(phi);
			if (va < 0.0)
			{
				va -= 0.5;
			}
			else
			{
				va += 0.5;
			}
			m_z = (int)va;

			if (dz < 0)
			{
				m_z = -m_z;
			}
		}
	}	

	/***********************************************************************************************
	 * Return the delta X vector of this Velocity.
	 *
	 * @return
	 *		The delta X vector of this Velocity.
	 */
	public int getDx()
	{
		return m_x;
	}
	
	/***********************************************************************************************
	 * Return the delta Y vector of this Velocity.
	 *
	 * @return
	 *		The delta Y vector of this Velocity.
	 */
	public int getDy()
	{
		return m_y;
	}
	
	/***********************************************************************************************
	 * Return the delta Z vector of this Velocity.
	 *
	 * @return
	 *		The delta Z vector of this Velocity.
	 */
	public int getDz()
	{
		return m_z;
	}

	/***********************************************************************************************
	 * Assign the delta X vector of this Velocity.
	 *
	 * @param dx
	 *		The delta X vector of this Velocity.
	 */
	public void setDx(int dx)
	{
		m_x = dx;
	}
	
	/***********************************************************************************************
	 * Assign the delta Y vector of this Velocity.
	 *
	 * @param dy
	 *		The delta Y vector of this Velocity.
	 */
	public void setDy(int dy)
	{
		m_y = dy;
	}
	
	/***********************************************************************************************
	 * Assign the delta Z vector of this Velocity.
	 *
	 * @param dz
	 *		The delta Z vector of this Velocity.
	 */
	public void setDz(int dz)
	{
		m_z = dz;
	}

	/***********************************************************************************************
	 * Override to output the string representation of this Velocity based
	 * on its class label and 3D Vector values.
	 *
	 * @return
	 *		A string representation of this Velocity.
	 */
	public String toString()
	{
		return "Velocity" + super.toString();
	}
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Cycles object defines the count of time intervals between Simulation updates.
 */

package com.jot.simulation;

public class Cycles
{
	public static final int MILLISECONDS_PER_CYCLE = 100;

	private static final int s_nPerSecond = (1000 / MILLISECONDS_PER_CYCLE);

	private int m_nCycles;

	/***********************************************************************************************
	 * Cycles constructor based on an integer count.
	 *
	 * @param count
	 *	    The cycle count.
	 */
	public Cycles(int count)
	{
		m_nCycles = count;
	}

	/***********************************************************************************************
	 * Cycles constructor based on the time in seconds.
	 *
	 * @param dSeconds
	 *	    The decimal time in seconds for this Cycles duration.
	 */
	public Cycles(double dSeconds)
	{
		m_nCycles = (int)(dSeconds * s_nPerSecond);
	}

	/***********************************************************************************************
	 * Return the constant value that defines the cycle count per second.
	 *
	 * @return
	 *	    The number of cycles per second.
	 */
	public static int GetCyclesPerSecond()
	{
		return s_nPerSecond;
	}

	/***********************************************************************************************
	 * Increment our cycle count and return the new Cycles count as an int.
	 *
	 * @return
	 *	    The number of Cycles as an int.
	 */
	public int increment()
	{
		return ++m_nCycles;
	}

	/***********************************************************************************************
	 * Increment by the provided Cycles and return true if this reached a maxed value. If the
	 * provided count of cycles is greater than this, assign the new count to one from the max
	 * and also return true.
	 *
	 * @param cyclesIncrement
	 *		Number of Cycles to reduce our count by.
	 * @param cyclesMax
	 *		Maximum number of Cycles to increment to.
	 * @return
	 *	    True if the new Cycle count has reached the max value
	 */
	public boolean incrementToMaxEnd(Cycles cyclesIncrement, Cycles cyclesMax)
	{
		if ((m_nCycles += cyclesIncrement.m_nCycles) >= cyclesMax.m_nCycles)
		{
			m_nCycles = cyclesMax.m_nCycles - 1;
			return true;
		}
		return false;
	}

	/***********************************************************************************************
	 * Increment by the provided Cycles and return true if this reached a maxed value. If the max
	 * was reached, subtract off the max value.
	 *
	 * @param cyclesIncrement
	 *		Number of Cycles to reduce our count by.
	 * @return
	 *	    True if the new Cycle count has reached the max value
	 */
	public boolean incrementToMaxLoop(Cycles cyclesIncrement, Cycles cyclesMax)
	{
		if ((m_nCycles += cyclesIncrement.m_nCycles) >= cyclesMax.m_nCycles)
		{
			m_nCycles -= cyclesMax.m_nCycles;
			return true;
		}
		return false;
	}

	/***********************************************************************************************
	 * Decrement our cycle count and return the new Cycles count as an int.
	 *
	 * @return
	 *	    The number of Cycles as an int.
	 */
	public int decrement()
	{
		return --m_nCycles;
	}

	/***********************************************************************************************
	 * Decrement by the provided Cycles and return true if this reached zero. If the provided count
	 * of cycles is greater than this, assign the new count to zero and also return true.
	 *
	 * @param cycles
	 *		Number of Cycles to reduce our count by.
	 * @return
	 *	    True if the new Cycle count is zero.
	 */
	public boolean decrementToZero(Cycles cycles)
	{
		if ((m_nCycles -= cycles.m_nCycles) < 0)
		{
			m_nCycles = 0;
		}
		return m_nCycles == 0;
	}

	/***********************************************************************************************
	 * Return the Cycles as an int.
	 *
	 * @return
	 *	    The number of Cycles as an int.
	 */
	public int getInt()
	{
		return m_nCycles;
	}

	/***********************************************************************************************
	 * Return the number of milliseconds within our Cycles.
	 *
	 * @return
	 *	    The number of milliseconds within our Cycles.
	 */
	public int getMilliseconds()
	{
		return m_nCycles * MILLISECONDS_PER_CYCLE;
	}

	/***********************************************************************************************
	 * Assign our Cycles count based on the integer value.
	 *
	 * @param nCycles
	 *		New integer cycle count.
	 */
	public void set(int nCycles)
	{
		m_nCycles = nCycles;
	}
}

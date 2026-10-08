/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A lift Door is a device to control the timing of entering and exiting people.
 */

package com.jot.lift;

import com.jot.item.*;
import com.jot.simulation.Cycles;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.*;

/***************************************************************************************************
 * A Door can be used on any type of lift car.  It provides a delay until people can get
 * in or out.
 */
public class Door extends PlaceImpl
{
	public enum StatusEnum
	{
		CLOSED,
		CLOSING,
		OPENING,
		OPEN;
	}
	
	private static final Cycles s_cyclesDef = new Cycles(2.0);
	private static final int s_dMassDef = 50;
	private static final Size s_sizeDef = new Size(1500, 200, 2300);
	
	private Door m_doorSpouse;
	private StatusEnum m_eStatus;
	private int m_nCyclesTotal;
	private int m_iCycle;
	private long m_nTime;
	private int m_iCloseDelay;		// Delay door close for a period after it is open.

	/***********************************************************************************************
	 * Return the default time needed to open or close a lift Door in Cycles.
	 *
	 * @return
	 *	    The default time needed to open or close a lift Door in Cycles.
	 */
	public static Cycles GetDefaultCycles()
	{
		return s_cyclesDef;
	}

	/***********************************************************************************************
	 * Door initializer.
	 *
	 * @param cycles
	 *	    The Cycles needed for the door to open or close.
	 */
	private void initialize(Cycles cycles) throws Exception
	{
		Trace.startOf("lift.Door.initialize()");

		// By default, we have no spouse.
		m_doorSpouse = null;
		
		// By default, our door is closed.
		m_eStatus = StatusEnum.CLOSED;
		if (cycles == null)
		{
			cycles = s_cyclesDef;
		}
		if ((m_nCyclesTotal = cycles.getInt()) <= 0)
		{
			throw new Exception("Cycles in open/close stages must be greater than zero.");
		}
		m_iCycle = 0;
		m_iCloseDelay = 0;

		Trace.endOf("lift.Door.initialize()");
	}

	/***********************************************************************************************
	 * Door constructor based on its size and position.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param cycles
	 *	    The Cycles needed for the door to open or close.
	 */
	public Door(Position position, Cycles cycles) throws Exception
	{
		super(s_dMassDef, s_sizeDef, position);
		initialize(cycles);
	}

	/***********************************************************************************************
	 * Door constructor based on its size and position.
	 *
	 * @param mass
	 *	    The mass of this item.
	 * @param size
	 *	    The vector size of this item.
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param cycles
	 *	    The Cycles needed for the door to open or close.
	 */
	public Door(
		int mass,
		Size size,
		Position position,
		Cycles cycles
	) throws Exception
	{
		super(mass, size, position);
		initialize(cycles);
	}

	/***********************************************************************************************
	 * Tell this Door and its spouse to close.
	 */
	public void close(boolean bDelay)
	{
		if (m_eStatus == StatusEnum.OPEN)
		{
			if (bDelay)
			{
				if (m_iCloseDelay == 0)
				{
					m_doorSpouse.m_iCloseDelay = m_iCloseDelay = 2 * Cycles.GetCyclesPerSecond();
				}
			}
			else
			{
				m_doorSpouse.m_iCloseDelay = m_iCloseDelay = 0;
				m_doorSpouse.m_eStatus = m_eStatus = StatusEnum.CLOSING;
			}
		}
	}

	/***********************************************************************************************
	 * Get the door we are married up with; null if not.
	 */
	public Door getSpouse()
	{
		return m_doorSpouse;
	}

	/***********************************************************************************************
	 * Return true if this Door is closed.
	 *
	 * @return
	 *	    True if closed.
	 */
	public boolean isClosed()
	{
		return m_eStatus == StatusEnum.CLOSED;
	}

	/***********************************************************************************************
	 * Return true if this Door is in the process of closing.
	 *
	 * @return
	 *	    True if this Door is closing.
	 */
	public boolean isClosing()
	{
		return (m_iCloseDelay > 0) || (m_eStatus == StatusEnum.CLOSING);
	}

	/***********************************************************************************************
	 * Return true if the given Door has the same open status as this.
	 *
	 * @return
	 *	    True if the Door has the same open status.
	 */
	public boolean isEqualStatus(Door door)
	{
		return (
			m_eStatus == door.m_eStatus &&
			m_iCycle == door.m_iCycle);
	}

	/***********************************************************************************************
	 * Return true if this Door is open.
	 *
	 * @return
	 *	    True if open.
	 */
	public boolean isOpen()
	{
		return m_eStatus == StatusEnum.OPEN;
	}

	/***********************************************************************************************
	 * Return true if this Door is open enough to exit or enter.
	 *
	 * @return
	 *	    True if open enough to exit or enter.
	 */
	public boolean isOpenEnough()
	{
		return (m_eStatus == StatusEnum.OPEN ||
			(m_eStatus == StatusEnum.OPENING && m_iCycle >= (m_nCyclesTotal >> 1)));
	}

	/***********************************************************************************************
	 * Return true if this Door is opening.
	 *
	 * @return
	 *	    True if opening.
	 */
	public boolean isOpening()
	{
		return m_eStatus == StatusEnum.OPENING;
	}

	/***********************************************************************************************
	 * Tell this Door and its spouse to open. Do NOT open this Door if it has no spouse.
	 */
	public void open()
	{
		if (m_eStatus != StatusEnum.OPEN && m_doorSpouse != null)
		{
			m_eStatus = m_doorSpouse.m_eStatus = StatusEnum.OPENING;
			m_iCloseDelay = m_doorSpouse.m_iCloseDelay = 0;
		}
	}

	/***********************************************************************************************
	 * Set our spouse and trigger the paired doors to open.
	 */
	public void setSpouse(Door spouse)
	{
		m_doorSpouse = spouse;
		spouse.m_doorSpouse = this;
		open();
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		// Avoid redundant runs of the same update.
		long nTime = sim.getTime();
		if (nTime != m_nTime)
		{
			m_nTime = nTime;
			if (m_doorSpouse != null)
			{
				m_doorSpouse.m_nTime = nTime;
			}

			int nCyclesUpdate = sim.getUpdateCyclesCount();

			// If we have a delay to close the door, tick this off and set the door to CLOSING
			// once we get to zero.
			if (m_iCloseDelay > 0)
			{
				if (nCyclesUpdate >= m_iCloseDelay)
				{
					nCyclesUpdate -= m_iCloseDelay;
					m_iCloseDelay = 0;
					m_eStatus = StatusEnum.CLOSING;
				}
				else
				{
					m_iCloseDelay -= nCyclesUpdate;
					nCyclesUpdate = 0;
				}
			}

			switch (m_eStatus)
			{
				case CLOSING:
					if ((m_iCycle -= nCyclesUpdate) <= 0)
					{
						m_iCycle = 0;
						m_eStatus = StatusEnum.CLOSED;
					}
					break;
				
				case OPENING:
					if ((m_iCycle += nCyclesUpdate) >= m_nCyclesTotal)
					{
						m_iCycle = m_nCyclesTotal;
						m_eStatus = StatusEnum.OPEN;
					}
					break;
				
				default:
					return;
			}

			// If we have one, update the status of the spouse.  And once the doors are fully
			// closed, break the marriage.
			if (m_doorSpouse != null)
			{
				m_doorSpouse.m_iCycle = m_iCycle;
				m_doorSpouse.m_iCloseDelay = m_iCloseDelay;
				if ((m_doorSpouse.m_eStatus = m_eStatus) == StatusEnum.CLOSED)
				{
					m_doorSpouse.m_doorSpouse = null;
					m_doorSpouse = null;
				}
			}
		}
	}
}

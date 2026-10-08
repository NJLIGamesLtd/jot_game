/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A RollaCar is a TrackCar that rides within a rollavator Box, Carousel or Lift.
 */

package com.jot.rollavator;

import com.jot.item.Drone;
import com.jot.lift.LiftCar;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.vector.Position;

public class RollaCar extends LiftCar
{
	protected int m_idFloorGroup;
	protected Box m_boxParent;

	/***********************************************************************************************
	 * RollaCar constructor based on its parent Rollavator and track Section.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param rollavator
	 *		The Rollavator that this LiftCar will ride within.
	 */
	public RollaCar(Position position, Rollavator rollavator) throws Exception
	{
		super(position, rollavator);

		// By default, we have no floor grouping.
		m_idFloorGroup = 0;

		m_boxParent = null;
	}

	/***********************************************************************************************
	 * Return the destination level group ID for this car based on a group being 10, 4 meter floors.
	 *
	 * @return
	 *		The destination level group ID of this car based on a group being 10, 4 meter floors.
	 */
	public int getFloorGroupDestinationID()
	{
		return m_idFloorGroup;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public int getFloorGroupId()
	{
		return m_idFloorGroup;
	}

	/***********************************************************************************************
	 * Return the parent lift that holds this car.
	 *
	 * @return
	 *		The parent lift box that holds this car.
	 */
	public Box getParent()
	{
		return m_boxParent;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public Position getPosition()
	{
		if (m_boxParent != null && m_boxParent.getClass() == BoxCarousel.class)
		{
			return m_boxParent.getPosition();
		}
		return m_position;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public boolean offer(Drone drone) throws Exception
	{
		// Most of the logic for this is in LiftCar.
		if (super.offer(drone))
		{
			// If we are within a BoxPort, switch our track direction
			// to back away from the door and into the BoxReturn.
			if (m_boxParent != null && m_boxParent.getClass() == BoxPort.class)
			{
				setDirection(false);
			}
			return true;
		}
		return false;
	}

	/***********************************************************************************************
	 * Assign the destination level group ID for this car based on a group being 10, 4 meter floors.
	 *
	 * @param idFloorGroup
	 *		The destination level group ID of this car based on a group being 10, 4 meter floors.
	 */
	public void setFloorGroupDestinationID(int idFloorGroup)
	{
		m_idFloorGroup = idFloorGroup;
	}

	/***********************************************************************************************
	 * Assign the parent lift that now holds this car.
	 *
	 * @param parent
	 *		The parent lift box that now holds this car.
	 */
	public void setParent(Box parent)
	{
		if (m_boxParent != null)
		{
			Trace.critical("RollaCar (" + this + ") now owned by " + parent);
		}

		m_boxParent = parent;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		// Most of the update in under the parent LiftCar class.
		super.update(sim);

		// If the door is open and we're on a office level trying to move over
		// towards a BoxReturn, close the door.
		if (m_door.isOpen() &&
			m_boxParent != null &&
			m_boxParent.getClass() == BoxReturn.class)
		{
			m_door.close(true);
		}
	}
}

/**************************************************************************************************/

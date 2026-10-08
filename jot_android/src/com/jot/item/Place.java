/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Place is an Item that can have Drone items directed towards and away.
 */

package com.jot.item;

import com.jot.simulation.Simulation;

public interface Place extends Item
{
	/***********************************************************************************************
	 * Add a drone to for testing purposes.
	 *
	 * @param drone
	 *		A computer directed item.
	 */
	public void add(Drone drone);

	/***********************************************************************************************
	 * Return the level ID of this place based on 4 meter floors.
	 *
	 * @return
	 *		The level ID of this place based on 4 meter floors.
	 */
	public int getFloorLevelID();

	/***********************************************************************************************
	 * Return the level group ID of this place based on a group being 10, 4 meter floors.
	 *
	 * @return
	 *		The level group ID of this place based on a group being 10, 4 meter floors.
	 */
	public int getFloorGroupID();

	/***********************************************************************************************
	 * If the given directed Drone is in range of this Place, then accept the Drone and return true.
	 * Else, reject the Drone and return false.
	 *
	 * @param drone
	 *		A computer directed item.
	 * @return
	 *		true if the item was drawn into control of this place.
	 */
	public boolean offer(Drone drone) throws Exception;

	/***********************************************************************************************
	 * If there is a Drone being directed from this Place, remove it from this Place and return its
	 * reference so it can be added and directed by another.
	 *
	 * @return
	 *		An item that was removed from this place, else null.
	 */
	public Drone poll();

	/***********************************************************************************************
	 * Run a single update of the simulation.
	 *
	 * @param sim
	 *		A Simulation object to control and display updates.
	 */
	public void update(Simulation sim) throws Exception;
}

/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Drone is a directed Item. This can be by any means of transport.
 */

package com.jot.item;

public interface Drone extends Place
{
	/***********************************************************************************************
	 * Get the floor group id where this drone is headed.
	 *
	 * @return
	 *		The floor group id where this drone is headed.
	 */
	public int getFloorGroupId();

	/***********************************************************************************************
	 * Return the Place this Drone currently resides.
	 *
	 * @return
	 *		The Place this Drone currently resides.
	 */
	public Place getPlace();

	/***********************************************************************************************
	 * Return true if this drone's destination is on the same z-axis level as provided.
	 *
	 * @param level
	 *		The z-axis location to compare against.
	 * @return
	 *		true if this drone's destination is on the same z-axis level as provided.
	 */
	public boolean hasDestination(int level);

	/***********************************************************************************************
	 * If available, return a reference to the active destination.
	 *
	 * @return
	 *		The active destination or null if not available.
	 */
	public Place peekDestination();
	
	/***********************************************************************************************
	 * If available, pop off and return a reference to the active destination.  If there is a
	 * next destination, make this the new active direction.
	 *
	 * @return
	 *		The active destination or null if not available.
	 */
	public Place popDestination() throws Exception;

	/***********************************************************************************************
	 * Push a new destination to the stack.  Make this new destination active.
	 *
	 * @param destination
	 *		The next place in the queue that we want to move towards.
	 */
	public void pushDestination(Place destination) throws Exception;

	/***********************************************************************************************
	 * Set the floor group id where this drone is headed.
	 *
	 * @param idFloorGroup
	 *		The floor group id where this drone is headed.
	 */
	public void setFloorGroupId(int idFloorGroup);

	/***********************************************************************************************
	 * Set the Place this Drone currently resides.
	 *
	 * @param place
	 *		The Place this Drone currently resides.
	 */
	public void setPlace(Place place);
}

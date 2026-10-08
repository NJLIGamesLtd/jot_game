/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A TrackCar is a Place that can be directed across a Track.
 */

package com.jot.track;

import com.jot.item.Drone;
import com.jot.vector.Position;

public interface TrackCar extends Drone
{
	/***********************************************************************************************
	 * Return true if the box can move.
	 *
	 * @return
	 *	    True if the box is moving.
	 */
	public boolean canMove(boolean recurse) throws Exception;

	/***********************************************************************************************
	 * Return true if this TrackCar will collide with another Car at the given Position.
	 *
	 * @param position
	 *		Position that we want to avoid colliding with.
	 * @return
	 *		Boolean value that states our TrackCar will collide with a Car at this Position.
	 */
	public boolean collides(Position position);

	/***********************************************************************************************
	 * Return true if this TrackCar will collide with another TrackCar.
	 *
	 * @param car
	 *		TrackCar that we want to avoid colliding with.
	 * @return
	 *		Boolean value that states our TrackCar will collide with the other TrackCar.
	 */
	public boolean collides(TrackCar car);

	/***********************************************************************************************
	 * Return the Position this TrackCar is trying to reach.
	 *
	 * @return
	 *		The Position this TrackCar is trying to reach.
	 */
	public Position getDestination();

	/***********************************************************************************************
	 * Return the directed direction of our TrackCar; true for forward and false for reverse.
	 *
	 * @return
	 *		True if we're directed forward.
	 */
	public boolean getDirection();

	/***********************************************************************************************
	 * If available, return a reference to the Drone that this TrackCar is following, as on a Track.
	 *
	 * @return
	 *		The TrackCar that this Car is following, else null.
	 */
	public TrackCar getLeader();

	/***********************************************************************************************
	 * Get the Track quadrant ID where this TrackCar is currently located.
	 *
	 * @return
	 *		The Track quadrant ID where this TrackCar is currently located.
	 */
	public int getQuadrantId();

	/***********************************************************************************************
	 * Return the track car's section location.
	 *
	 * @return
	 *		Our location to this track section.
	 */
	public Section getSection();

	/***********************************************************************************************
	 * Return the number of steps per tick in our car velocity.
	 *
	 * @return
	 *		The number of steps per tick in our car velocity.
	 */
	public int getTickVelocity();

	/***********************************************************************************************
	 * Return true if there is a next Section in the Track to move to.
	 *
	 * @return
	 *	    true if there is a next Section in the Track to move to.
	 */
	public boolean isNext();

	/***********************************************************************************************
	 * Return true if there is a previous Section in the Track to move to.
	 *
	 * @return
	 *	    true if there is a previous Section in the Track to move to.
	 */
	public boolean isPrevious();

	/***********************************************************************************************
	 * Move this TrackCar by the active velocity and adjust the velocity one step if needed. If
	 * this was moving to fast to avoid a collision, throw an exception.
	 */
	public void move() throws Exception;

	/***********************************************************************************************
	 * Assign a reference to the next possible Section within a List of Sections.
	 *
	 * @return
	 *		true if there was a next section to move to.
	 */
	public boolean moveToNext();

	/***********************************************************************************************
	 * Assign a reference to the previous possible Section within a List of Sections.
	 *
	 * @return
	 *		true if there was a previous section to move to.
	 */
	public boolean moveToPrevious();

	/***********************************************************************************************
	 * Assign the Position this TrackCar needs to reach.
	 *
	 * @param posDestination
	 *		The Position this TrackCar is trying to reach.
	 */
	public void setDestination(Position posDestination);

	/***********************************************************************************************
	 * Assign the direction of our TrackCar.
	 *
	 * @param bMoveNext
	 *		true to have this move forward.
	 */
	public void setDirection(boolean bMoveNext);

	/***********************************************************************************************
	 * Assign a TrackCar to be infront of this TrackCar on our Track so we avoid a collision.
	 *
	 * @param car
	 *		An TrackCar that this TrackCar will follow.
	 */
	public void setLeader(TrackCar car);

	/***********************************************************************************************
	 * Assign the track section of our TrackCar.
	 *
	 * @param section
	 *		Assign our location to this track section.
	 */
	public void setSection(Section section);
}

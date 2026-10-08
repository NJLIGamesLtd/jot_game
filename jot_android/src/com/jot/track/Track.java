/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A Track is a Place defines a track Section path for TrackCar(s) to follow.
 */

package com.jot.track;

import com.jot.item.Place;

public interface Track extends Place
{
	/***********************************************************************************************
	 * Force add the given car to our track at the specified section. This can be used to provide
	 * an initial location for the car.
	 *
	 * @param iPos
	 *		The 0-based index into our Track where the car should be added.
	 * @param car
	 *		The TrackCar to be added.
	 * @param leader
	 *		The TrackCar our new TrackCar should be following.
	 */
	public void add(int iPos, TrackCar car, TrackCar leader);

	/***********************************************************************************************
	 * Return the z-axis level of this, accounting for the level of the parent base.
	 *
	 * @return
	 *	    The overall z-axis level.
	 */
	public int getBaseLevel();

	/***********************************************************************************************
	 * Return the Nth Section within the Track.
	 *
	 * @param iSection
	 *		Index to the Nth section.
	 * @return
	 *	    The Nth Section within the Track.
	 */
	public Section getSection(int iSection) throws Exception;

	/***********************************************************************************************
	 * Return the first Section within the Track.
	 *
	 * @return
	 *	    The first Section within the Track.
	 */
	public Section getSectionFirst();

	/***********************************************************************************************
	 * Return the last Section within the Track.
	 *
	 * @return
	 *	    The last Section within the Track.
	 */
	public Section getSectionLast();

	/***********************************************************************************************
	 * Return the lead TrackCar that resides within this lift, or null if it is empty.
	 *
	 * @return
	 *	    The lead TrackCar that resides within this Track, or null if it is empty.
	 */
	public TrackCar getTrackCar();

	/***********************************************************************************************
	 * In case we have a list of TrackCar objects, return the last one added or null if empty.
	 *
	 * @return
	 *	    The last TrackCar that resides within this Track, or null if it is empty.
	 */
	public TrackCar getTrackCarLast();

	/***********************************************************************************************
	 * Mark the Track Section at the given Nth Position as the start of a new quadrant.  This
	 * allows a TrackCar to be indentified by its quadrant.
	 *
	 * @param iPos
	 *		The Nth position which marks the start of the next quadrant.
	 * @return
	 *		The ID of this new marked quadrant.
	 * @throws
	 *		Exception if track is not defined, or if section does not exist.
	 */
	public int markQuadrant(int iPos) throws Exception;

	/***********************************************************************************************
	 * Offer the TrackCar onto this Track.  Return true for success or false if we cannot accept
	 * the car at this time.
	 *
	 * @param car
	 *		The TrackCar being offered.
	 * @return
	 *		True for success, false to reject the offer.
	 */
	public boolean offer(TrackCar car) throws Exception;

	/***********************************************************************************************
	 * Remove a TrackCar from our Track.
	 *
	 * @param car
	 *	    The TrackCar to be removed from the Track.
	 */
	public void removeTrackCar(TrackCar car);

	/***********************************************************************************************
	 * Assign a Track object has the destination at the end of a Track.
	 *
	 * @param destination
	 *		A Track which our Track leads to.
	 */
	public void setDestinationEnd(Track destination);

	/***********************************************************************************************
	 * Assign a Track object has the destination at the start of a Track.
	 *
	 * @param destination
	 *		A Track which our Track leads to.
	 */
	public void setDestinationStart(Track destination);
}

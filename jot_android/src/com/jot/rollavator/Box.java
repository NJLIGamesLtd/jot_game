/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator Box is a TrackCar that rides on a Track and also contains an internal Track for
 * a Car to be held and transfered to another Track.
 */

package com.jot.rollavator;

import com.jot.track.Section;
import com.jot.track.Track;
import com.jot.track.TrackCar;

public interface Box extends TrackCar
{
	/***********************************************************************************************
	 * Return the z-axis level of this, accounting for the level of the parent base.
	 *
	 * @return
	 *	    The overall z-axis level.
	 */
	public int getBaseLevel();

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
	 * Return the Track this LoopBox is married to.
	 *
	 * @return
	 *	    The Train this LoopBox is married with.
	 */
	public Box getSpouse();

	/***********************************************************************************************
	 * Return the Track that carries TrackCar(s) within this Box.
	 *
	 * @return
	 *	    The Track that carries TrackCar(s) within this Box.
	 */
	public Track getTrack();

	/***********************************************************************************************
	 * Return the lead TrackCar that resides within this lift, or null if it is empty.
	 *
	 * @return
	 *	    The lead TrackCar that resides within this lift, or null if it is empty.
	 */
	public TrackCar getTrackCar();

	/***********************************************************************************************
	 * Return true if the last Car offered has been fully loaded onto our Track.
	 *
	 * @return
	 *		True if the last Car offered has been fully loaded onto our Track.
	 */
	public boolean isLoaded();

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
	 * Assigns a spouse to this Box.
	 *
	 * @param spouse
	 *	    The Box that is married to this Box.
	 */
	public void setSpouse(Box spouse) throws Exception;
}

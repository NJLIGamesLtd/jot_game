/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Simulatio is an abstract class to simulate some type of worldly environment in real-time based
 * on a fixed time interval.
 */

package com.jot.simulation;

import com.jot.trace.Trace;

import java.sql.Time;
import java.util.Timer;
import java.applet.*;
import java.util.*;

public class Simulation extends Applet implements Runnable
{
	private static final long serialVersionUID = 1L;

	protected Thread m_threadSim = null;

	/***********************************************************************************************
	 * SimTaskPaint provides an asynchronous TimerTask for painting an active animation of
	 * the simulation.
	 */
	class SimTaskPaint extends TimerTask
	{		
		/*******************************************************************************************
		 * Cause regular repaints of the simulation.
		 */
		public void run()
		{
			repaint();
		}
	}

	/***********************************************************************************************
	 * SimTaskUpdate provides an asynchronous TimerTask for updating the state of the
	 * simulation.
	 */
	class SimTaskUpdate extends TimerTask
	{		
		/*******************************************************************************************
		 * Cause regular updates of the simulation.
		 */
		public void run()
		{
			try
			{
				update();
			}
			catch (Exception e)
			{
			}
		}
	}

	protected Time m_time;
	protected int m_nCycles;
	protected Cycles m_cyclesUpdate;
	protected boolean m_bInUpdate = false;

	private SimTaskPaint m_simTaskPaint;
	private SimTaskUpdate m_simTaskUpdate;
	private Timer m_timerPaint;
	private Timer m_timerUpdate;
	private int m_nPaint = 0;
	private int m_nCyclesTotal;
	private boolean m_bComplete = false;

	/***********************************************************************************************
	 * Internal constructor based on a specific initial starting time.
	 *
	 * @param nTimeStart
	 *		Initial time setting to base the start of the simulation.
	 */
	private void initialize(long nTimeStart)
	{
		Trace.startOf("Simulation.initialize()");

		m_simTaskUpdate = new SimTaskUpdate();
		m_simTaskPaint = new SimTaskPaint();
		
		m_timerUpdate = new Timer();
		m_timerPaint = new Timer();
		
		m_time = new Time(nTimeStart);

		m_bComplete = false;
		m_bInUpdate = false;
		m_nCycles = 0;
		m_nCyclesTotal = 0;
		m_cyclesUpdate = new Cycles(0);

		Trace.endOf("Simulation.initialize()");
	}

	/***********************************************************************************************
	 * Simulation constructor based on a specific initial starting time.
	 *
	 * @param nTimeStart
	 *		Initial time setting to base the start of the simulation.
	 */
	public Simulation(long nTimeStart)
	{
		initialize(nTimeStart);
	}

	/***********************************************************************************************
	 * Simulation default constructor.
	 */
	public Simulation()
	{
		initialize(0L);
	}

	/***********************************************************************************************
	 * Runnable start method used to trigger the timed updates.
	 */
	public void start()
	{
		Trace.startOf("Simulation.start()");

		m_timerUpdate.schedule(m_simTaskUpdate, 0/*delay*/, 100/*interval*/);
		m_timerPaint.schedule(m_simTaskPaint, 10/*delay*/, 500/*interval*/);
		
		Trace.endOf("Simuation.start()");
	}
	
	/***********************************************************************************************
	 * Runnable init method override.
	 */
	public void init()
	{
	}

	/***********************************************************************************************
	 * Return the expected time at the end of the active update.
	 *
	 * @return
	 *		The expected time at the end of the active update.
	 */
	public long getTime()
	{
		return m_time.getTime();
	}

	/***********************************************************************************************
	 * Return the number of cycles needed to complete the active update.
	 *
	 * @return
	 *		The Cycles count needed to complete the active update.
	 */
	public Cycles getUpdateCycles()
	{
		return m_cyclesUpdate;
	}

	/***********************************************************************************************
	 * Return the number of cycles needed to complete the active update.
	 *
	 * @return
	 *		The Cycles count needed to complete the active update.
	 */
	public int getUpdateCyclesCount()
	{
		return m_cyclesUpdate.getInt();
	}

	/***********************************************************************************************
	 * Return true if the simulation has completed and is ready for termination.
	 *
	 * @return
	 *		True if the simulation has completed and is ready for termination.
	 */
	public boolean isComplete()
	{
		if (m_bComplete)
		{
			Trace.important("Simulation complete");
		}
		return m_bComplete;
	}

	/***********************************************************************************************
	 * Time interval based update method for repainting the current state of the simulation.
	 */
	public void repaint()
	{
		Trace.trivial("Simulation.repaint() called: " + ++m_nPaint);
		super.repaint();
		if (isComplete())
		{
			m_timerPaint.cancel();
		}
	}
	
	/***********************************************************************************************
	 * Runnable override method to trigger the simulation to execute.
	 */
	public void run()
	{
		Trace.important("Simulation.run()");
	}
	
	/***********************************************************************************************
	 * Runnable override method to trigger the simulation to terminate.
	 */
	public void stop()
	{
		Trace.important("Simulation.stop()");
	}

	/***********************************************************************************************
	 * Start an update and return true if we're ready to being.  Return false if we are already
	 * in the middle of an update.
	 */
	public boolean updateBegin()
	{
		// If we're in the middle of a update, increment the time cycle for the next update.
		++m_nCycles;
		if (m_bInUpdate)
		{
			return false;
		}
		m_bInUpdate = true;

		// Update the time by our interval.
		updateTime(m_nCycles);
		m_nCycles = 0;
		
		return true;
	}

	/***********************************************************************************************
	 * Start an update and return true if we're ready to being.  Return false if we are already
	 * in the middle of an update.
	 */
	public void updateEnd()
	{
		m_bInUpdate = false;
	}

	/***********************************************************************************************
	 * Time interval based update method for moving the environment a number of Cycles forward.
	 */
	public void update() throws Exception
	{
		Trace.startOf("Simulation.update(): " + ++m_nCyclesTotal);
		if (m_nCyclesTotal > 100)
		{
			m_bComplete = true;
		}
		if (m_bComplete)
		{
			m_timerUpdate.cancel();
		}
		else
		{
			if (updateBegin())
			{
				Trace.important("Simulation.update(): Place specific update here.");

				updateEnd();
			}
		}
		Trace.endOf("Simulation.update()");
	}

	/***********************************************************************************************
	 * Update the time by a Cycles count.
	 */
	public void updateTime(int nCycles)
	{
		// Update the time by our interval.
		m_cyclesUpdate.set(nCycles);
		m_time.setTime(m_time.getTime() + m_cyclesUpdate.getMilliseconds());
	}

	/***********************************************************************************************
	 * Update the time by a Cycles count.
	 */
	public void updateTime(Cycles cycles)
	{
		// Update the time by our interval.
		m_cyclesUpdate.set(cycles.getInt());
		m_time.setTime(m_time.getTime() + m_cyclesUpdate.getMilliseconds());
	}
}

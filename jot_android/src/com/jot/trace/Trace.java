/* *************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * Trace is a development tool for outputting various levels of debugging information.
 */

package com.jot.trace;

public class Trace
{
	// Trace level enumerations that allow us to pick and choose which
	// to output or log.
	public static enum TraceEnum
	{
		TRIVIAL(0),
		IMPORTANT(1),
		CRITICAL(3),
		WARNING(2),
		ERROR(3),
		FATAL(4);
		
		protected final int trace;
		
		TraceEnum(int trace)
		{
			this.trace = trace;
		}
	}

	private static int s_nLevel = 0;

	// During execution, output these traces to the console.
	private static final int TRACE_OUTPUT =
		//(1 << TraceEnum.TRIVIAL.trace) |
		//(1 << TraceEnum.IMPORTANT.trace) |
		(1 << TraceEnum.CRITICAL.trace) |
		(1 << TraceEnum.WARNING.trace) |
		(1 << TraceEnum.ERROR.trace) |
		(1 << TraceEnum.FATAL.trace);

	// UNSUPPORTED - During execution, log these to our trace file.
	private static final int TRACE_LOG =
		//(1 << TraceEnum.TRIVIAL.trace) |
		//(1 << TraceEnum.IMPORTANT.trace) |
		(1 << TraceEnum.CRITICAL.trace) |
		(1 << TraceEnum.WARNING.trace) |
		(1 << TraceEnum.ERROR.trace) |
		(1 << TraceEnum.FATAL.trace);

	/***********************************************************************************************
	 * If our interest level is set as such, output this trivial message.
	 *
	 * @param strMsg
	 *		A debugging message string.
	 */
	public static void trivial(String strMsg)
	{
		if ((TRACE_OUTPUT & (1 << TraceEnum.TRIVIAL.trace)) != 0)
		{
			output(strMsg);
		}
	}

	/***********************************************************************************************
	 * If our interest level is set as such, output this important message.
	 *
	 * @param strMsg
	 *		A debugging message string.
	 */
	public static void important(String strMsg)
	{
		if ((TRACE_OUTPUT & (1 << TraceEnum.IMPORTANT.trace)) != 0)
		{
			output(strMsg);
		}
	}

	/***********************************************************************************************
	 * If our interest level is set as such, output this critical message.
	 *
	 * @param strMsg
	 *		A debugging message string.
	 */
	public static void critical(String strMsg)
	{
		if ((TRACE_OUTPUT & (1 << TraceEnum.CRITICAL.trace)) != 0)
		{
			output(strMsg);
		}
	}

	/***********************************************************************************************
	 * If our interest level is set as such, output this warning message.
	 *
	 * @param strMsg
	 *		A debugging message string.
	 */
	public static void warning(String strMsg)
	{
		if ((TRACE_OUTPUT & (1 << TraceEnum.WARNING.trace)) != 0)
		{
			output(strMsg);
		}
	}

	/***********************************************************************************************
	 * If our interest level is set as such, output this error message.
	 *
	 * @param strMsg
	 *		A debugging message string.
	 */
	public static void error(String strMsg)
	{
		if ((TRACE_OUTPUT & (1 << TraceEnum.ERROR.trace)) != 0)
		{
			output(strMsg);
		}
	}

	/***********************************************************************************************
	 * If our interest level is set as such, output this fatal message.
	 *
	 * @param strMsg
	 *		A debugging message string.
	 */
	public static void fatal(String strMsg) throws Exception
	{
		if ((TRACE_OUTPUT & (1 << TraceEnum.FATAL.trace)) != 0)
		{
			output(strMsg);
		}
		throw new Exception(strMsg);
	}

	/***********************************************************************************************
	 * Output a trivial message to mark the start of a method and increase our
	 * indentation accordingly.
	 *
	 * @param strMsg
	 *		A debugging message string.
	 */
	public static void startOf(String strMsg)
	{
		trivial("Start of " + strMsg);
		++s_nLevel;
	}

	/***********************************************************************************************
	 * Output a trivial message to mark the end of a method and decrease our
	 * indentation accordingly.
	 *
	 * @param strMsg
	 *		A debugging message string.
	 */
	public static void endOf(String strMsg)
	{
		--s_nLevel;
		trivial("End of " + strMsg);
	}

	/***********************************************************************************************
	 * Output the message.
	 *
	 * @param strMsg
	 *		A debugging message string.
	 */
	private static void output(String strMsg)
	{
		for (int i = 0; i < s_nLevel; ++i)
		{
			System.out.print(' ');
		}
		System.out.println(strMsg);
	}
}

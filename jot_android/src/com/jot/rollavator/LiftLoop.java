/***************************************************************************************************
 * @author	Jot Russell <author.jotrussell@gmail.com> (copyright 2015, all rights reserved)
 * @version	0.0
 * @since	2013-11-30
 *
 * A rollavator Loop is a Lift with multiple LoopBox objects that travel around in a loop.
 */

package com.jot.rollavator;

import com.jot.item.StructureLevel;
import com.jot.simulation.Simulation;
import com.jot.trace.Trace;
import com.jot.track.Loop;
import com.jot.track.TrackCar;
import com.jot.vector.Location;
import com.jot.vector.Orientation;
import com.jot.vector.Position;
import com.jot.vector.Size;

public class LiftLoop extends Loop
{
	public static enum PosEnum
	{
		TO_LOBBY(0),
		FROM_LOBBY(1),
		TO_SKY(2),
		FROM_SKY(3),
		UP_LEVEL_STACK(4),
		DOWN_LEVEL_STACK(5),
		NPOS(6);

		public final int m_value;
		
		PosEnum(int value)
		{
			m_value = value;
		}

		public PosEnum
		getNext()
		{
			switch (this)
			{
				case UP_LEVEL_STACK:
					return TO_SKY;
				case TO_SKY:
					return FROM_SKY;
				case FROM_SKY:
					return DOWN_LEVEL_STACK;
				case DOWN_LEVEL_STACK:
					return TO_LOBBY;
				case TO_LOBBY:
					return FROM_LOBBY;
				case FROM_LOBBY:
				default:
					return UP_LEVEL_STACK;
			}
		}

		public PosEnum
		getTwin()
		{
			switch (this)
			{
				case TO_LOBBY:
					return TO_SKY;
				case FROM_LOBBY:
					return FROM_SKY;
				case TO_SKY:
					return TO_LOBBY;
				case FROM_SKY:
					return FROM_LOBBY;
				case UP_LEVEL_STACK:
					return DOWN_LEVEL_STACK;
				case DOWN_LEVEL_STACK:
				default:
					return UP_LEVEL_STACK;
			}
		}
	}

	protected static final int s_mass = 1000;
	protected static final Size s_size = new Size(4000, 1600, 4000);
	protected static final int s_nMaxBoxes = 32;
	protected static final int s_nStepsPerMeter = 20;

	private static final Location[] s_aLocBottom =
	{
		new Location(0,  1000, 3950),
		new Location(0,  1000, 3900),
		new Location(0,  1000, 3850),
		new Location(0,  1000, 3800),
		new Location(0,  1000, 3750),
		new Location(0,  1000, 3700),
		new Location(0,  1000, 3650),
		new Location(0,  1000, 3600),
		new Location(0,  1000, 3550),
		new Location(0,  1000, 3500),
		new Location(0,  1000, 3450),
		new Location(0,  1000, 3400),
		new Location(0,  1000, 3350),
		new Location(0,  1000, 3300),
		new Location(0,  1000, 3250),
		new Location(0,  1000, 3200),
		new Location(0,  1000, 3150),
		new Location(0,  1000, 3100),
		new Location(0,  1000, 3050),
		new Location(0,  1000, 3000),	// TO_LOBBY [s_nStepsPerMeter]
		new Location(0,   999, 2985),
		new Location(0,   999, 2970),
		new Location(0,   998, 2960),
		new Location(0,   998, 2950),
		new Location(0,   997, 2940),
		new Location(0,   997, 2930),
		new Location(0,   996, 2920),
		new Location(0,   996, 2910),
		new Location(0,   995, 2900),
		new Location(0,   995, 2890),
		new Location(0,   994, 2880),
		new Location(0,   994, 2870),
		new Location(0,   993, 2860),
		new Location(0,   993, 2850),
		new Location(0,   992, 2840),
		new Location(0,   992, 2830),
		new Location(0,   991, 2820),
		new Location(0,   991, 2810),
		new Location(0,   990, 2800),
		new Location(0,   989, 2790),
		new Location(0,   988, 2780),
		new Location(0,   987, 2770),
		new Location(0,   986, 2760),
		new Location(0,   985, 2750),
		new Location(0,   984, 2740),
		new Location(0,   983, 2730),
		new Location(0,   982, 2720),
		new Location(0,   981, 2710),
		new Location(0,   980, 2700),
		new Location(0,   979, 2690),
		new Location(0,   978, 2680),
		new Location(0,   977, 2670),
		new Location(0,   976, 2660),
		new Location(0,   975, 2650),
		new Location(0,   974, 2640),
		new Location(0,   973, 2630),
		new Location(0,   972, 2620),
		new Location(0,   971, 2610),
		new Location(0,   970, 2600),
		new Location(0,   969, 2590),
		new Location(0,   968, 2580),
		new Location(0,   967, 2570),
		new Location(0,   966, 2560),
		new Location(0,   965, 2550),
		new Location(0,   964, 2540),
		new Location(0,   963, 2530),
		new Location(0,   962, 2520),
		new Location(0,   961, 2510),
		new Location(0,   960, 2500),
		new Location(0,   958, 2490),
		new Location(0,   956, 2480),
		new Location(0,   954, 2470),
		new Location(0,   952, 2460),
		new Location(0,   950, 2450),
		new Location(0,   948, 2440),
		new Location(0,   946, 2430),
		new Location(0,   944, 2420),
		new Location(0,   942, 2410),
		new Location(0,   940, 2400),
		new Location(0,   938, 2390),
		new Location(0,   936, 2380),
		new Location(0,   934, 2370),
		new Location(0,   932, 2360),
		new Location(0,   930, 2350),
		new Location(0,   928, 2340),
		new Location(0,   926, 2330),
		new Location(0,   924, 2320),
		new Location(0,   922, 2310),
		new Location(0,   920, 2300),
		new Location(0,   918, 2290),
		new Location(0,   916, 2280),
		new Location(0,   914, 2270),
		new Location(0,   912, 2260),
		new Location(0,   910, 2250),
		new Location(0,   908, 2240),
		new Location(0,   906, 2230),
		new Location(0,   904, 2220),
		new Location(0,   902, 2210),
		new Location(0,   900, 2200),
		new Location(0,   896, 2190),
		new Location(0,   893, 2180),
		new Location(0,   890, 2170),
		new Location(0,   886, 2160),
		new Location(0,   883, 2150),
		new Location(0,   880, 2140),
		new Location(0,   876, 2130),
		new Location(0,   873, 2120),
		new Location(0,   870, 2110),
		new Location(0,   866, 2100),
		new Location(0,   863, 2090),
		new Location(0,   860, 2080),
		new Location(0,   856, 2070),
		new Location(0,   853, 2060),
		new Location(0,   850, 2050),
		new Location(0,   846, 2040),
		new Location(0,   843, 2030),
		new Location(0,   840, 2020),
		new Location(0,   836, 2010),
		new Location(0,   833, 2000),
		new Location(0,   830, 1990),
		new Location(0,   826, 1980),
		new Location(0,   823, 1970),
		new Location(0,   820, 1960),
		new Location(0,   816, 1950),
		new Location(0,   813, 1940),
		new Location(0,   810, 1930),
		new Location(0,   806, 1920),
		new Location(0,   803, 1910),
		new Location(0,   800, 1900),
		new Location(0,   796, 1890),
		new Location(0,   793, 1880),
		new Location(0,   790, 1870),
		new Location(0,   786, 1860),
		new Location(0,   783, 1850),
		new Location(0,   780, 1840),
		new Location(0,   776, 1830),
		new Location(0,   773, 1820),
		new Location(0,   770, 1810),
		new Location(0,   766, 1800),
		new Location(0,   763, 1790),
		new Location(0,   760, 1780),
		new Location(0,   756, 1770),
		new Location(0,   753, 1760),
		new Location(0,   750, 1750),
		new Location(0,   746, 1740),
		new Location(0,   743, 1730),
		new Location(0,   740, 1720),
		new Location(0,   736, 1710),
		new Location(0,   733, 1700),
		new Location(0,   730, 1690),
		new Location(0,   726, 1680),
		new Location(0,   723, 1670),
		new Location(0,   720, 1660),
		new Location(0,   716, 1650),
		new Location(0,   713, 1640),
		new Location(0,   710, 1630),
		new Location(0,   706, 1620),
		new Location(0,   703, 1610),
		new Location(0,   700, 1600),
		new Location(0,   695, 1590),
		new Location(0,   690, 1580),
		new Location(0,   685, 1570),
		new Location(0,   680, 1560),
		new Location(0,   675, 1550),
		new Location(0,   670, 1540),
		new Location(0,   665, 1530),
		new Location(0,   660, 1520),
		new Location(0,   655, 1510),
		new Location(0,   650, 1500),
		new Location(0,   645, 1490),
		new Location(0,   640, 1480),
		new Location(0,   635, 1470),
		new Location(0,   630, 1460),
		new Location(0,   625, 1450),
		new Location(0,   620, 1440),
		new Location(0,   615, 1430),
		new Location(0,   610, 1420),
		new Location(0,   605, 1410),
		new Location(0,   600, 1400),
		new Location(0,   590, 1390),
		new Location(0,   580, 1380),
		new Location(0,   570, 1370),
		new Location(0,   560, 1360),
		new Location(0,   550, 1350),
		new Location(0,   540, 1340),
		new Location(0,   530, 1330),
		new Location(0,   520, 1320),
		new Location(0,   510, 1310),
		new Location(0,   500, 1300),
		new Location(0,   490, 1290),
		new Location(0,   480, 1280),
		new Location(0,   470, 1270),
		new Location(0,   460, 1260),
		new Location(0,   450, 1250),
		new Location(0,   440, 1240),
		new Location(0,   430, 1230),
		new Location(0,   420, 1220),
		new Location(0,   410, 1210),
		new Location(0,   400, 1200),
		new Location(0,   380, 1190),
		new Location(0,   360, 1180),
		new Location(0,   340, 1170),
		new Location(0,   320, 1160),
		new Location(0,   300, 1150),
		new Location(0,   280, 1140),
		new Location(0,   260, 1130),
		new Location(0,   240, 1120),
		new Location(0,   220, 1110),
		new Location(0,   200, 1100),
		new Location(0,   180, 1090),
		new Location(0,   160, 1080),
		new Location(0,   140, 1070),
		new Location(0,   120, 1060),
		new Location(0,   100, 1050),
		new Location(0,    80, 1040),
		new Location(0,    60, 1030),
		new Location(0,    40, 1020),
		new Location(0,    20, 1010),
		new Location(0,    10, 1005),
		new Location(0,     0, 1000),
		new Location(0,   -10, 1005),
		new Location(0,   -20, 1010),
		new Location(0,   -40, 1020),
		new Location(0,   -60, 1030),
		new Location(0,   -80, 1040),
		new Location(0,  -100, 1050),
		new Location(0,  -120, 1060),
		new Location(0,  -140, 1070),
		new Location(0,  -160, 1080),
		new Location(0,  -180, 1090),
		new Location(0,  -200, 1100),
		new Location(0,  -220, 1110),
		new Location(0,  -240, 1120),
		new Location(0,  -260, 1130),
		new Location(0,  -280, 1140),
		new Location(0,  -300, 1150),
		new Location(0,  -320, 1160),
		new Location(0,  -340, 1170),
		new Location(0,  -360, 1180),
		new Location(0,  -380, 1190),
		new Location(0,  -400, 1200),
		new Location(0,  -410, 1210),
		new Location(0,  -420, 1220),
		new Location(0,  -430, 1230),
		new Location(0,  -440, 1240),
		new Location(0,  -450, 1250),
		new Location(0,  -460, 1260),
		new Location(0,  -470, 1270),
		new Location(0,  -480, 1280),
		new Location(0,  -490, 1290),
		new Location(0,  -500, 1300),
		new Location(0,  -510, 1310),
		new Location(0,  -520, 1320),
		new Location(0,  -530, 1330),
		new Location(0,  -540, 1340),
		new Location(0,  -550, 1350),
		new Location(0,  -560, 1360),
		new Location(0,  -570, 1370),
		new Location(0,  -580, 1380),
		new Location(0,  -590, 1390),
		new Location(0,  -600, 1400),
		new Location(0,  -605, 1410),
		new Location(0,  -610, 1420),
		new Location(0,  -615, 1430),
		new Location(0,  -620, 1440),
		new Location(0,  -625, 1450),
		new Location(0,  -630, 1460),
		new Location(0,  -635, 1470),
		new Location(0,  -640, 1480),
		new Location(0,  -645, 1490),
		new Location(0,  -650, 1500),
		new Location(0,  -655, 1510),
		new Location(0,  -660, 1520),
		new Location(0,  -665, 1530),
		new Location(0,  -670, 1540),
		new Location(0,  -675, 1550),
		new Location(0,  -680, 1560),
		new Location(0,  -685, 1570),
		new Location(0,  -690, 1580),
		new Location(0,  -695, 1590),
		new Location(0,  -700, 1600),
		new Location(0,  -703, 1610),
		new Location(0,  -706, 1620),
		new Location(0,  -710, 1630),
		new Location(0,  -713, 1640),
		new Location(0,  -716, 1650),
		new Location(0,  -720, 1660),
		new Location(0,  -723, 1670),
		new Location(0,  -726, 1680),
		new Location(0,  -730, 1690),
		new Location(0,  -733, 1700),
		new Location(0,  -736, 1710),
		new Location(0,  -740, 1720),
		new Location(0,  -743, 1730),
		new Location(0,  -746, 1740),
		new Location(0,  -750, 1750),
		new Location(0,  -753, 1760),
		new Location(0,  -756, 1770),
		new Location(0,  -760, 1780),
		new Location(0,  -763, 1790),
		new Location(0,  -766, 1800),
		new Location(0,  -770, 1810),
		new Location(0,  -773, 1820),
		new Location(0,  -776, 1830),
		new Location(0,  -780, 1840),
		new Location(0,  -783, 1850),
		new Location(0,  -786, 1860),
		new Location(0,  -790, 1870),
		new Location(0,  -793, 1880),
		new Location(0,  -796, 1890),
		new Location(0,  -800, 1900),
		new Location(0,  -803, 1910),
		new Location(0,  -806, 1920),
		new Location(0,  -810, 1930),
		new Location(0,  -813, 1940),
		new Location(0,  -816, 1950),
		new Location(0,  -820, 1960),
		new Location(0,  -823, 1970),
		new Location(0,  -826, 1980),
		new Location(0,  -830, 1990),
		new Location(0,  -833, 2000),
		new Location(0,  -836, 2010),
		new Location(0,  -840, 2020),
		new Location(0,  -843, 2030),
		new Location(0,  -846, 2040),
		new Location(0,  -850, 2050),
		new Location(0,  -853, 2060),
		new Location(0,  -856, 2070),
		new Location(0,  -860, 2080),
		new Location(0,  -863, 2090),
		new Location(0,  -866, 2100),
		new Location(0,  -870, 2110),
		new Location(0,  -873, 2120),
		new Location(0,  -876, 2130),
		new Location(0,  -880, 2140),
		new Location(0,  -883, 2150),
		new Location(0,  -886, 2160),
		new Location(0,  -890, 2170),
		new Location(0,  -893, 2180),
		new Location(0,  -896, 2190),
		new Location(0,  -900, 2200),
		new Location(0,  -902, 2210),
		new Location(0,  -904, 2220),
		new Location(0,  -906, 2230),
		new Location(0,  -908, 2240),
		new Location(0,  -910, 2250),
		new Location(0,  -912, 2260),
		new Location(0,  -914, 2270),
		new Location(0,  -916, 2280),
		new Location(0,  -918, 2290),
		new Location(0,  -920, 2300),
		new Location(0,  -922, 2310),
		new Location(0,  -924, 2320),
		new Location(0,  -926, 2330),
		new Location(0,  -928, 2340),
		new Location(0,  -930, 2350),
		new Location(0,  -932, 2360),
		new Location(0,  -934, 2370),
		new Location(0,  -936, 2380),
		new Location(0,  -938, 2390),
		new Location(0,  -940, 2400),
		new Location(0,  -942, 2410),
		new Location(0,  -944, 2420),
		new Location(0,  -946, 2430),
		new Location(0,  -948, 2440),
		new Location(0,  -950, 2450),
		new Location(0,  -952, 2460),
		new Location(0,  -954, 2470),
		new Location(0,  -956, 2480),
		new Location(0,  -958, 2490),
		new Location(0,  -960, 2500),
		new Location(0,  -961, 2510),
		new Location(0,  -962, 2520),
		new Location(0,  -963, 2530),
		new Location(0,  -964, 2540),
		new Location(0,  -965, 2550),
		new Location(0,  -966, 2560),
		new Location(0,  -967, 2570),
		new Location(0,  -968, 2580),
		new Location(0,  -969, 2590),
		new Location(0,  -970, 2600),
		new Location(0,  -971, 2610),
		new Location(0,  -972, 2620),
		new Location(0,  -973, 2630),
		new Location(0,  -974, 2640),
		new Location(0,  -975, 2650),
		new Location(0,  -976, 2660),
		new Location(0,  -977, 2670),
		new Location(0,  -978, 2680),
		new Location(0,  -979, 2690),
		new Location(0,  -980, 2700),
		new Location(0,  -981, 2710),
		new Location(0,  -982, 2720),
		new Location(0,  -983, 2730),
		new Location(0,  -984, 2740),
		new Location(0,  -985, 2750),
		new Location(0,  -986, 2760),
		new Location(0,  -987, 2770),
		new Location(0,  -988, 2780),
		new Location(0,  -989, 2790),
		new Location(0,  -990, 2800),
		new Location(0,  -991, 2810),
		new Location(0,  -991, 2820),
		new Location(0,  -992, 2830),
		new Location(0,  -992, 2840),
		new Location(0,  -993, 2850),
		new Location(0,  -993, 2860),
		new Location(0,  -994, 2870),
		new Location(0,  -994, 2880),
		new Location(0,  -995, 2890),
		new Location(0,  -995, 2900),
		new Location(0,  -996, 2910),
		new Location(0,  -996, 2920),
		new Location(0,  -997, 2930),
		new Location(0,  -997, 2940),
		new Location(0,  -998, 2950),
		new Location(0,  -998, 2960),
		new Location(0,  -999, 2970),
		new Location(0,  -999, 2985),
		new Location(0, -1000, 3000),	// FROM_LOBBY  [size - s_nStepsPerMeter - 1]
		new Location(0, -1000, 3050),
		new Location(0, -1000, 3100),
		new Location(0, -1000, 3150),
		new Location(0, -1000, 3200),
		new Location(0, -1000, 3250),
		new Location(0, -1000, 3300),
		new Location(0, -1000, 3350),
		new Location(0, -1000, 3400),
		new Location(0, -1000, 3450),
		new Location(0, -1000, 3500),
		new Location(0, -1000, 3550),
		new Location(0, -1000, 3600),
		new Location(0, -1000, 3650),
		new Location(0, -1000, 3700),
		new Location(0, -1000, 3750),
		new Location(0, -1000, 3800),
		new Location(0, -1000, 3850),
		new Location(0, -1000, 3900),
		new Location(0, -1000, 3950),
	};

	private static final Location[] s_aLocTop =
	{
		new Location(0, -1000,    0),
		new Location(0, -1000,   50),
		new Location(0, -1000,  100),
		new Location(0, -1000,  150),
		new Location(0, -1000,  200),
		new Location(0, -1000,  250),
		new Location(0, -1000,  300),
		new Location(0, -1000,  350),
		new Location(0, -1000,  400),
		new Location(0, -1000,  450),
		new Location(0, -1000,  500),
		new Location(0, -1000,  550),
		new Location(0, -1000,  600),
		new Location(0, -1000,  650),
		new Location(0, -1000,  700),
		new Location(0, -1000,  750),
		new Location(0, -1000,  800),
		new Location(0, -1000,  850),
		new Location(0, -1000,  900),
		new Location(0, -1000,  950),
		new Location(0, -1000, 1000),
		new Location(0, -1000, 1050),
		new Location(0, -1000, 1100),
		new Location(0, -1000, 1150),
		new Location(0, -1000, 1200),
		new Location(0, -1000, 1250),
		new Location(0, -1000, 1300),
		new Location(0, -1000, 1350),
		new Location(0, -1000, 1400),
		new Location(0, -1000, 1450),
		new Location(0, -1000, 1500),
		new Location(0, -1000, 1550),
		new Location(0, -1000, 1600),
		new Location(0, -1000, 1650),
		new Location(0, -1000, 1700),
		new Location(0, -1000, 1750),
		new Location(0, -1000, 1800),
		new Location(0, -1000, 1850),
		new Location(0, -1000, 1900),
		new Location(0, -1000, 1950),
		new Location(0, -1000, 2000),
		new Location(0, -1000, 2050),
		new Location(0, -1000, 2100),
		new Location(0, -1000, 2150),
		new Location(0, -1000, 2200),
		new Location(0, -1000, 2250),
		new Location(0, -1000, 2300),
		new Location(0, -1000, 2350),
		new Location(0, -1000, 2400),
		new Location(0, -1000, 2450),
		new Location(0, -1000, 2500),
		new Location(0, -1000, 2550),
		new Location(0, -1000, 2600),
		new Location(0, -1000, 2650),
		new Location(0, -1000, 2700),
		new Location(0, -1000, 2750),
		new Location(0, -1000, 2800),
		new Location(0, -1000, 2850),
		new Location(0, -1000, 2900),
		new Location(0, -1000, 2950),
		new Location(0, -1000, 3000),	// TO_SKY  [1 * s_nStepsPerMeter]
		new Location(0,  -999, 3020),
		new Location(0,  -999, 3030),
		new Location(0,  -998, 3040),
		new Location(0,  -998, 3050),
		new Location(0,  -997, 3060),
		new Location(0,  -997, 3070),
		new Location(0,  -996, 3080),
		new Location(0,  -996, 3090),
		new Location(0,  -995, 3100),
		new Location(0,  -995, 3110),
		new Location(0,  -994, 3120),
		new Location(0,  -994, 3130),
		new Location(0,  -993, 3140),
		new Location(0,  -993, 3150),
		new Location(0,  -992, 3160),
		new Location(0,  -992, 3170),
		new Location(0,  -991, 3180),
		new Location(0,  -991, 3190),
		new Location(0,  -990, 3200),
		new Location(0,  -989, 3210),
		new Location(0,  -988, 3220),
		new Location(0,  -987, 3230),
		new Location(0,  -986, 3240),
		new Location(0,  -985, 3250),
		new Location(0,  -984, 3260),
		new Location(0,  -983, 3270),
		new Location(0,  -982, 3280),
		new Location(0,  -981, 3290),
		new Location(0,  -980, 3300),
		new Location(0,  -979, 3310),
		new Location(0,  -978, 3320),
		new Location(0,  -977, 3330),
		new Location(0,  -976, 3340),
		new Location(0,  -975, 3350),
		new Location(0,  -974, 3360),
		new Location(0,  -973, 3370),
		new Location(0,  -972, 3380),
		new Location(0,  -971, 3390),
		new Location(0,  -970, 3400),
		new Location(0,  -969, 3410),
		new Location(0,  -968, 3420),
		new Location(0,  -967, 3430),
		new Location(0,  -966, 3440),
		new Location(0,  -965, 3450),
		new Location(0,  -964, 3460),
		new Location(0,  -963, 3470),
		new Location(0,  -962, 3480),
		new Location(0,  -961, 3490),
		new Location(0,  -960, 3500),
		new Location(0,  -958, 3510),
		new Location(0,  -956, 3520),
		new Location(0,  -954, 3530),
		new Location(0,  -952, 3540),
		new Location(0,  -950, 3550),
		new Location(0,  -948, 3560),
		new Location(0,  -946, 3570),
		new Location(0,  -944, 3580),
		new Location(0,  -942, 3590),
		new Location(0,  -940, 3600),
		new Location(0,  -938, 3610),
		new Location(0,  -936, 3620),
		new Location(0,  -934, 3630),
		new Location(0,  -932, 3640),
		new Location(0,  -930, 3650),
		new Location(0,  -928, 3660),
		new Location(0,  -926, 3670),
		new Location(0,  -924, 3680),
		new Location(0,  -922, 3690),
		new Location(0,  -920, 3700),
		new Location(0,  -918, 3710),
		new Location(0,  -916, 3720),
		new Location(0,  -914, 3730),
		new Location(0,  -912, 3740),
		new Location(0,  -910, 3750),
		new Location(0,  -908, 3760),
		new Location(0,  -906, 3770),
		new Location(0,  -904, 3780),
		new Location(0,  -902, 3790),
		new Location(0,  -900, 3800),
		new Location(0,  -896, 3810),
		new Location(0,  -893, 3820),
		new Location(0,  -890, 3830),
		new Location(0,  -886, 3840),
		new Location(0,  -883, 3850),
		new Location(0,  -880, 3860),
		new Location(0,  -876, 3870),
		new Location(0,  -873, 3880),
		new Location(0,  -870, 3890),
		new Location(0,  -866, 3900),
		new Location(0,  -863, 3910),
		new Location(0,  -860, 3920),
		new Location(0,  -856, 3930),
		new Location(0,  -853, 3940),
		new Location(0,  -850, 3950),
		new Location(0,  -846, 3960),
		new Location(0,  -843, 3970),
		new Location(0,  -840, 3980),
		new Location(0,  -836, 3990),
		new Location(0,  -833, 4000),
		new Location(0,  -830, 4010),
		new Location(0,  -826, 4020),
		new Location(0,  -823, 4030),
		new Location(0,  -820, 4040),
		new Location(0,  -816, 4050),
		new Location(0,  -813, 4060),
		new Location(0,  -810, 4070),
		new Location(0,  -806, 4080),
		new Location(0,  -803, 4090),
		new Location(0,  -800, 4100),
		new Location(0,  -796, 4110),
		new Location(0,  -793, 4120),
		new Location(0,  -790, 4130),
		new Location(0,  -786, 4140),
		new Location(0,  -783, 4150),
		new Location(0,  -780, 4160),
		new Location(0,  -776, 4170),
		new Location(0,  -773, 4180),
		new Location(0,  -770, 4190),
		new Location(0,  -766, 4200),
		new Location(0,  -763, 4210),
		new Location(0,  -760, 4220),
		new Location(0,  -756, 4230),
		new Location(0,  -753, 4240),
		new Location(0,  -750, 4250),
		new Location(0,  -746, 4260),
		new Location(0,  -743, 4270),
		new Location(0,  -740, 4280),
		new Location(0,  -736, 4290),
		new Location(0,  -733, 4300),
		new Location(0,  -730, 4310),
		new Location(0,  -726, 4320),
		new Location(0,  -723, 4330),
		new Location(0,  -720, 4340),
		new Location(0,  -716, 4350),
		new Location(0,  -713, 4360),
		new Location(0,  -710, 4370),
		new Location(0,  -706, 4380),
		new Location(0,  -703, 4390),
		new Location(0,  -700, 4400),
		new Location(0,  -695, 4410),
		new Location(0,  -690, 4420),
		new Location(0,  -685, 4430),
		new Location(0,  -680, 4440),
		new Location(0,  -675, 4450),
		new Location(0,  -670, 4460),
		new Location(0,  -665, 4470),
		new Location(0,  -660, 4480),
		new Location(0,  -655, 4490),
		new Location(0,  -650, 4500),
		new Location(0,  -645, 4510),
		new Location(0,  -640, 4520),
		new Location(0,  -635, 4530),
		new Location(0,  -630, 4540),
		new Location(0,  -625, 4550),
		new Location(0,  -620, 4560),
		new Location(0,  -615, 4570),
		new Location(0,  -610, 4580),
		new Location(0,  -605, 4590),
		new Location(0,  -600, 4600),
		new Location(0,  -590, 4610),
		new Location(0,  -580, 4620),
		new Location(0,  -570, 4630),
		new Location(0,  -560, 4640),
		new Location(0,  -550, 4650),
		new Location(0,  -540, 4660),
		new Location(0,  -530, 4670),
		new Location(0,  -520, 4680),
		new Location(0,  -510, 4690),
		new Location(0,  -500, 4700),
		new Location(0,  -490, 4710),
		new Location(0,  -480, 4720),
		new Location(0,  -470, 4730),
		new Location(0,  -460, 4740),
		new Location(0,  -450, 4750),
		new Location(0,  -440, 4760),
		new Location(0,  -430, 4770),
		new Location(0,  -420, 4780),
		new Location(0,  -410, 4790),
		new Location(0,  -400, 4800),
		new Location(0,  -380, 4810),
		new Location(0,  -360, 4820),
		new Location(0,  -340, 4830),
		new Location(0,  -320, 4840),
		new Location(0,  -300, 4850),
		new Location(0,  -280, 4860),
		new Location(0,  -260, 4870),
		new Location(0,  -240, 4880),
		new Location(0,  -220, 4890),
		new Location(0,  -200, 4900),
		new Location(0,  -180, 4910),
		new Location(0,  -160, 4920),
		new Location(0,  -140, 4930),
		new Location(0,  -120, 4940),
		new Location(0,  -100, 4950),
		new Location(0,   -80, 4960),
		new Location(0,   -60, 4970),
		new Location(0,   -40, 4980),
		new Location(0,   -20, 4990),
		new Location(0,   -10, 4995),
		new Location(0,     0, 5000),
		new Location(0,    10, 4995),
		new Location(0,    20, 4990),
		new Location(0,    40, 4980),
		new Location(0,    60, 4970),
		new Location(0,    80, 4960),
		new Location(0,   100, 4950),
		new Location(0,   120, 4940),
		new Location(0,   140, 4930),
		new Location(0,   160, 4920),
		new Location(0,   180, 4910),
		new Location(0,   200, 4900),
		new Location(0,   220, 4890),
		new Location(0,   240, 4880),
		new Location(0,   260, 4870),
		new Location(0,   280, 4860),
		new Location(0,   300, 4850),
		new Location(0,   320, 4840),
		new Location(0,   340, 4830),
		new Location(0,   360, 4820),
		new Location(0,   380, 4810),
		new Location(0,   400, 4800),
		new Location(0,   410, 4790),
		new Location(0,   420, 4780),
		new Location(0,   430, 4770),
		new Location(0,   440, 4760),
		new Location(0,   450, 4750),
		new Location(0,   460, 4740),
		new Location(0,   470, 4730),
		new Location(0,   480, 4720),
		new Location(0,   490, 4710),
		new Location(0,   500, 4700),
		new Location(0,   510, 4690),
		new Location(0,   520, 4680),
		new Location(0,   530, 4670),
		new Location(0,   540, 4660),
		new Location(0,   550, 4650),
		new Location(0,   560, 4640),
		new Location(0,   570, 4630),
		new Location(0,   580, 4620),
		new Location(0,   590, 4610),
		new Location(0,   600, 4600),
		new Location(0,   605, 4590),
		new Location(0,   610, 4580),
		new Location(0,   615, 4570),
		new Location(0,   620, 4560),
		new Location(0,   625, 4550),
		new Location(0,   630, 4540),
		new Location(0,   635, 4530),
		new Location(0,   640, 4520),
		new Location(0,   645, 4510),
		new Location(0,   650, 4500),
		new Location(0,   655, 4490),
		new Location(0,   660, 4480),
		new Location(0,   665, 4470),
		new Location(0,   670, 4460),
		new Location(0,   675, 4450),
		new Location(0,   680, 4440),
		new Location(0,   685, 4430),
		new Location(0,   690, 4420),
		new Location(0,   695, 4410),
		new Location(0,   700, 4400),
		new Location(0,   703, 4390),
		new Location(0,   706, 4380),
		new Location(0,   710, 4370),
		new Location(0,   713, 4360),
		new Location(0,   716, 4350),
		new Location(0,   720, 4340),
		new Location(0,   723, 4330),
		new Location(0,   726, 4320),
		new Location(0,   730, 4310),
		new Location(0,   733, 4300),
		new Location(0,   736, 4290),
		new Location(0,   740, 4280),
		new Location(0,   743, 4270),
		new Location(0,   746, 4260),
		new Location(0,   750, 4250),
		new Location(0,   753, 4240),
		new Location(0,   756, 4230),
		new Location(0,   760, 4220),
		new Location(0,   763, 4210),
		new Location(0,   766, 4200),
		new Location(0,   770, 4190),
		new Location(0,   773, 4180),
		new Location(0,   776, 4170),
		new Location(0,   780, 4160),
		new Location(0,   783, 4150),
		new Location(0,   786, 4140),
		new Location(0,   790, 4130),
		new Location(0,   793, 4120),
		new Location(0,   796, 4110),
		new Location(0,   800, 4100),
		new Location(0,   803, 4090),
		new Location(0,   806, 4080),
		new Location(0,   810, 4070),
		new Location(0,   813, 4060),
		new Location(0,   816, 4050),
		new Location(0,   820, 4040),
		new Location(0,   823, 4030),
		new Location(0,   826, 4020),
		new Location(0,   830, 4010),
		new Location(0,   833, 4000),
		new Location(0,   836, 3990),
		new Location(0,   840, 3980),
		new Location(0,   843, 3970),
		new Location(0,   846, 3960),
		new Location(0,   850, 3950),
		new Location(0,   853, 3940),
		new Location(0,   856, 3930),
		new Location(0,   860, 3920),
		new Location(0,   863, 3910),
		new Location(0,   866, 3900),
		new Location(0,   870, 3890),
		new Location(0,   873, 3880),
		new Location(0,   876, 3870),
		new Location(0,   880, 3860),
		new Location(0,   883, 3850),
		new Location(0,   886, 3840),
		new Location(0,   890, 3830),
		new Location(0,   893, 3820),
		new Location(0,   896, 3810),
		new Location(0,   900, 3800),
		new Location(0,   902, 3790),
		new Location(0,   904, 3780),
		new Location(0,   906, 3770),
		new Location(0,   908, 3760),
		new Location(0,   910, 3750),
		new Location(0,   912, 3740),
		new Location(0,   914, 3730),
		new Location(0,   916, 3720),
		new Location(0,   918, 3710),
		new Location(0,   920, 3700),
		new Location(0,   922, 3690),
		new Location(0,   924, 3680),
		new Location(0,   926, 3670),
		new Location(0,   928, 3660),
		new Location(0,   930, 3650),
		new Location(0,   932, 3640),
		new Location(0,   934, 3630),
		new Location(0,   936, 3620),
		new Location(0,   938, 3610),
		new Location(0,   940, 3600),
		new Location(0,   942, 3590),
		new Location(0,   944, 3580),
		new Location(0,   946, 3570),
		new Location(0,   948, 3560),
		new Location(0,   950, 3550),
		new Location(0,   952, 3540),
		new Location(0,   954, 3530),
		new Location(0,   956, 3520),
		new Location(0,   958, 3510),
		new Location(0,   960, 3500),
		new Location(0,   961, 3490),
		new Location(0,   962, 3480),
		new Location(0,   963, 3470),
		new Location(0,   964, 3460),
		new Location(0,   965, 3450),
		new Location(0,   966, 3440),
		new Location(0,   967, 3430),
		new Location(0,   968, 3420),
		new Location(0,   969, 3410),
		new Location(0,   970, 3400),
		new Location(0,   971, 3390),
		new Location(0,   972, 3380),
		new Location(0,   973, 3370),
		new Location(0,   974, 3360),
		new Location(0,   975, 3350),
		new Location(0,   976, 3340),
		new Location(0,   977, 3330),
		new Location(0,   978, 3320),
		new Location(0,   979, 3310),
		new Location(0,   980, 3300),
		new Location(0,   981, 3290),
		new Location(0,   982, 3280),
		new Location(0,   983, 3270),
		new Location(0,   984, 3260),
		new Location(0,   985, 3250),
		new Location(0,   986, 3240),
		new Location(0,   987, 3230),
		new Location(0,   988, 3220),
		new Location(0,   989, 3210),
		new Location(0,   990, 3200),
		new Location(0,   991, 3190),
		new Location(0,   991, 3180),
		new Location(0,   992, 3170),
		new Location(0,   992, 3160),
		new Location(0,   993, 3150),
		new Location(0,   993, 3140),
		new Location(0,   994, 3130),
		new Location(0,   994, 3120),
		new Location(0,   995, 3110),
		new Location(0,   995, 3100),
		new Location(0,   996, 3090),
		new Location(0,   996, 3080),
		new Location(0,   997, 3070),
		new Location(0,   997, 3060),
		new Location(0,   998, 3050),
		new Location(0,   998, 3040),
		new Location(0,   999, 3030),
		new Location(0,   999, 3020),
		new Location(0,  1000, 3000),	// FROM_SKY [size - (1 * s_nStepsPerMeter) - 1]
		new Location(0,  1000, 2950),
		new Location(0,  1000, 2900),
		new Location(0,  1000, 2850),
		new Location(0,  1000, 2800),
		new Location(0,  1000, 2750),
		new Location(0,  1000, 2700),
		new Location(0,  1000, 2650),
		new Location(0,  1000, 2600),
		new Location(0,  1000, 2550),
		new Location(0,  1000, 2500),
		new Location(0,  1000, 2450),
		new Location(0,  1000, 2400),
		new Location(0,  1000, 2350),
		new Location(0,  1000, 2300),
		new Location(0,  1000, 2250),
		new Location(0,  1000, 2200),
		new Location(0,  1000, 2150),
		new Location(0,  1000, 2100),
		new Location(0,  1000, 2050),
		new Location(0,  1000, 2000),
		new Location(0,  1000, 1950),
		new Location(0,  1000, 1900),
		new Location(0,  1000, 1850),
		new Location(0,  1000, 1800),
		new Location(0,  1000, 1750),
		new Location(0,  1000, 1700),
		new Location(0,  1000, 1650),
		new Location(0,  1000, 1600),
		new Location(0,  1000, 1550),
		new Location(0,  1000, 1500),
		new Location(0,  1000, 1450),
		new Location(0,  1000, 1400),
		new Location(0,  1000, 1350),
		new Location(0,  1000, 1300),
		new Location(0,  1000, 1250),
		new Location(0,  1000, 1200),
		new Location(0,  1000, 1150),
		new Location(0,  1000, 1100),
		new Location(0,  1000, 1050),
		new Location(0,  1000, 1000),
		new Location(0,  1000,  950),
		new Location(0,  1000,  900),
		new Location(0,  1000,  850),
		new Location(0,  1000,  800),
		new Location(0,  1000,  750),
		new Location(0,  1000,  700),
		new Location(0,  1000,  650),
		new Location(0,  1000,  600),
		new Location(0,  1000,  550),
		new Location(0,  1000,  500),
		new Location(0,  1000,  450),
		new Location(0,  1000,  400),
		new Location(0,  1000,  350),
		new Location(0,  1000,  300),
		new Location(0,  1000,  250),
		new Location(0,  1000,  200),
		new Location(0,  1000,  150),
		new Location(0,  1000,  100),
		new Location(0,  1000,   50),
		new Location(0,  1000,    0),
	};

	private static final Location[] s_aLocMiddleUp =
	{
		new Location(0, -1000,    0),	// UP_LEVEL_STACK
		new Location(0, -1000,   50),
		new Location(0, -1000,  100),
		new Location(0, -1000,  150),
		new Location(0, -1000,  200),
		new Location(0, -1000,  250),
		new Location(0, -1000,  300),
		new Location(0, -1000,  350),
		new Location(0, -1000,  400),
		new Location(0, -1000,  450),
		new Location(0, -1000,  500),
		new Location(0, -1000,  550),
		new Location(0, -1000,  600),
		new Location(0, -1000,  650),
		new Location(0, -1000,  700),
		new Location(0, -1000,  750),
		new Location(0, -1000,  800),
		new Location(0, -1000,  850),
		new Location(0, -1000,  900),
		new Location(0, -1000,  950),
		new Location(0, -1000, 1000),
		new Location(0, -1000, 1050),
		new Location(0, -1000, 1100),
		new Location(0, -1000, 1150),
		new Location(0, -1000, 1200),
		new Location(0, -1000, 1250),
		new Location(0, -1000, 1300),
		new Location(0, -1000, 1350),
		new Location(0, -1000, 1400),
		new Location(0, -1000, 1450),
		new Location(0, -1000, 1500),
		new Location(0, -1000, 1550),
		new Location(0, -1000, 1600),
		new Location(0, -1000, 1650),
		new Location(0, -1000, 1700),
		new Location(0, -1000, 1750),
		new Location(0, -1000, 1800),
		new Location(0, -1000, 1850),
		new Location(0, -1000, 1900),
		new Location(0, -1000, 1950),
		new Location(0, -1000, 2000),
		new Location(0, -1000, 2050),
		new Location(0, -1000, 2100),
		new Location(0, -1000, 2150),
		new Location(0, -1000, 2200),
		new Location(0, -1000, 2250),
		new Location(0, -1000, 2300),
		new Location(0, -1000, 2350),
		new Location(0, -1000, 2400),
		new Location(0, -1000, 2450),
		new Location(0, -1000, 2500),
		new Location(0, -1000, 2550),
		new Location(0, -1000, 2600),
		new Location(0, -1000, 2650),
		new Location(0, -1000, 2700),
		new Location(0, -1000, 2750),
		new Location(0, -1000, 2800),
		new Location(0, -1000, 2850),
		new Location(0, -1000, 2900),
		new Location(0, -1000, 2950),
		new Location(0, -1000, 3000),
		new Location(0, -1000, 3050),
		new Location(0, -1000, 3100),
		new Location(0, -1000, 3150),
		new Location(0, -1000, 3200),
		new Location(0, -1000, 3250),
		new Location(0, -1000, 3300),
		new Location(0, -1000, 3350),
		new Location(0, -1000, 3400),
		new Location(0, -1000, 3450),
		new Location(0, -1000, 3500),
		new Location(0, -1000, 3550),
		new Location(0, -1000, 3600),
		new Location(0, -1000, 3650),
		new Location(0, -1000, 3700),
		new Location(0, -1000, 3750),
		new Location(0, -1000, 3800),
		new Location(0, -1000, 3850),
		new Location(0, -1000, 3900),
		new Location(0, -1000, 3950),
	};

	private static final Location[] s_aLocMiddleDown =
	{
		new Location(0, 1000, 3950),
		new Location(0, 1000, 3900),
		new Location(0, 1000, 3850),
		new Location(0, 1000, 3800),
		new Location(0, 1000, 3750),
		new Location(0, 1000, 3700),
		new Location(0, 1000, 3650),
		new Location(0, 1000, 3600),
		new Location(0, 1000, 3550),
		new Location(0, 1000, 3500),
		new Location(0, 1000, 3450),
		new Location(0, 1000, 3400),
		new Location(0, 1000, 3350),
		new Location(0, 1000, 3300),
		new Location(0, 1000, 3250),
		new Location(0, 1000, 3200),
		new Location(0, 1000, 3150),
		new Location(0, 1000, 3100),
		new Location(0, 1000, 3050),
		new Location(0, 1000, 3000),
		new Location(0, 1000, 2950),
		new Location(0, 1000, 2900),
		new Location(0, 1000, 2850),
		new Location(0, 1000, 2800),
		new Location(0, 1000, 2750),
		new Location(0, 1000, 2700),
		new Location(0, 1000, 2650),
		new Location(0, 1000, 2600),
		new Location(0, 1000, 2550),
		new Location(0, 1000, 2500),
		new Location(0, 1000, 2450),
		new Location(0, 1000, 2400),
		new Location(0, 1000, 2350),
		new Location(0, 1000, 2300),
		new Location(0, 1000, 2250),
		new Location(0, 1000, 2200),
		new Location(0, 1000, 2150),
		new Location(0, 1000, 2100),
		new Location(0, 1000, 2050),
		new Location(0, 1000, 2000),
		new Location(0, 1000, 1950),
		new Location(0, 1000, 1900),
		new Location(0, 1000, 1850),
		new Location(0, 1000, 1800),
		new Location(0, 1000, 1750),
		new Location(0, 1000, 1700),
		new Location(0, 1000, 1650),
		new Location(0, 1000, 1600),
		new Location(0, 1000, 1550),
		new Location(0, 1000, 1500),
		new Location(0, 1000, 1450),
		new Location(0, 1000, 1400),
		new Location(0, 1000, 1350),
		new Location(0, 1000, 1300),
		new Location(0, 1000, 1250),
		new Location(0, 1000, 1200),
		new Location(0, 1000, 1150),
		new Location(0, 1000, 1100),
		new Location(0, 1000, 1050),
		new Location(0, 1000, 1000),
		new Location(0, 1000,  950),
		new Location(0, 1000,  900),
		new Location(0, 1000,  850),
		new Location(0, 1000,  800),
		new Location(0, 1000,  750),
		new Location(0, 1000,  700),
		new Location(0, 1000,  650),
		new Location(0, 1000,  600),
		new Location(0, 1000,  550),
		new Location(0, 1000,  500),
		new Location(0, 1000,  450),
		new Location(0, 1000,  400),
		new Location(0, 1000,  350),
		new Location(0, 1000,  300),
		new Location(0, 1000,  250),
		new Location(0, 1000,  200),
		new Location(0, 1000,  150),
		new Location(0, 1000,  100),
		new Location(0, 1000,   50),
		new Location(0, 1000,    0),	// DOWN_LEVEL_STACK [size - 1]
	};

	private StructureLevel[] m_aLevels;

	/***********************************************************************************************
	 * Loop constructor based on its position and structure levels it connects with.
	 *
	 * @param position
	 *	    The vector position (location and orientation) of this item.
	 * @param towerLevels
	 *	    The structure levels this lift connects with.
	 * @param rollavator
	 *	    The Rollavator this is owned by.
	 * @param nLevelStacks
	 *		The number of level stacks that we might have to interface with.
	 * @param nLevelsPerStack
	 *		The number of levels per stack.
	 */
	public LiftLoop(
		Position position,
		StructureLevel[] aLevels,
		Rollavator rollavator,
		int nLevelStacks,
		int nLevelsPerStack
	) throws Exception
	{
		super(s_mass, s_size, position, null);
		
		Trace.startOf("Rollavator.LiftLoop()");

		// Save a reference to our tower levels.
		m_aLevels = aLevels;

		// Fix the size to account for each level in our structure.
		int nLevels = aLevels.length;
		m_size = new Size(s_size.m_x, s_size.m_y, nLevels * s_size.m_z);

		// Build a list of positions for our track.
		int iPos = 0;
		int nPositions = s_aLocTop.length + s_aLocBottom.length;
		if (nLevels > 1)
		{
			nPositions += (nLevels - 3) * (s_aLocMiddleUp.length + s_aLocMiddleDown.length);
		}
		Position[] aPosTrack = new Position[nPositions];
		int[] aIndexPositions = new int[PosEnum.NPOS.m_value + 2 * (nLevelStacks - 1)];

		// The connection to the stack of levels heading up will occur at the bottom of each
		// stack.  By default, assign the bottom stack position, nine levels up from the top
		// of the lobby.
		// SPK: Allow this position to be set when a stack level destination is set.
		aIndexPositions[PosEnum.UP_LEVEL_STACK.m_value] = iPos + 9 * 4 * s_nStepsPerMeter;
		iPos = addLevelPositions(aPosTrack, iPos, 1, nLevels - 3, s_aLocMiddleUp);

		// The connection to the sky level carousel is three meters above its base.
		int iPosTopBegin = iPos;
		aIndexPositions[PosEnum.TO_SKY.m_value] = iPos + 3 * s_nStepsPerMeter;
		iPos = addLevelPositions(aPosTrack, iPos, nLevels - 2, nLevels - 2, s_aLocTop);
		aIndexPositions[PosEnum.FROM_SKY.m_value] = iPos - (3 * s_nStepsPerMeter) - 1;
		int iPosTopEnd = iPos;

		// The connection to the stack of levels heading down will be calculated later
		// to be at opposite locations in the loop.
		aIndexPositions[PosEnum.DOWN_LEVEL_STACK.m_value] = iPos + 4 * nLevelsPerStack * s_nStepsPerMeter;
		iPos = addLevelPositions(aPosTrack, iPos, nLevels - 3, 1, s_aLocMiddleDown);

		// The connection to the lobby carousel is three meters above its base or one meter
		// below its top.
		int iPosBottomBegin = iPos - 2 * s_nStepsPerMeter;
		aIndexPositions[PosEnum.TO_LOBBY.m_value] = iPos + s_nStepsPerMeter - 1;
		iPos = addLevelPositions(aPosTrack, iPos, 0, 0, s_aLocBottom);
		aIndexPositions[PosEnum.FROM_LOBBY.m_value] = iPos - s_nStepsPerMeter;
		int iPosBottomEnd = iPos + 2 * s_nStepsPerMeter;

		// For each stack, store a position index to its base.
		int iLevelUp = aIndexPositions[PosEnum.UP_LEVEL_STACK.m_value];
		int nLevelDownOffset = aIndexPositions[PosEnum.DOWN_LEVEL_STACK.m_value] - iLevelUp;
		for (int iStack = 0; iStack < nLevelStacks; ++iStack)
		{
			// Force the up and down level stack positions to be at opposite locations in the
			// loop.  This way, only one stop is needed to interface with two level stacks.
			aIndexPositions[PosEnum.UP_LEVEL_STACK.m_value + 2 * iStack] = iLevelUp;
			aIndexPositions[PosEnum.DOWN_LEVEL_STACK.m_value + 2 * iStack] = iLevelUp + nLevelDownOffset;
			iLevelUp += 4 * nLevelsPerStack * s_nStepsPerMeter;
		}

		// Lay the track as a loop to the top and back to the bottom.
		setTrack(aPosTrack, position);

		// Need a pair of position destinations for a stack level, lobby and sky carousel.
		Position[] aPosDests = new Position[aIndexPositions.length];
		for (int i = 0; i < aIndexPositions.length; ++i)
		{
			aPosDests[i] = m_aSections[aIndexPositions[i]];
		}

		// Mark the top and bottom sections with a non-zero quadrant ID so as to avoid
		// collisions.
		for (iPos = iPosTopBegin; iPos < iPosTopEnd; ++iPos)
		{
			m_aSections[iPos % nPositions].setQuadrantId(1);
		}
		for (iPos = iPosBottomBegin; iPos < iPosBottomEnd; ++iPos)
		{
			m_aSections[iPos % nPositions].setQuadrantId(2);
		}

		// Create a single Box pair for each four levels, up to an upper limit.
		int nBoxes = (nLevels >> 2) & ~1;
		if (nBoxes > s_nMaxBoxes)
		{
			nBoxes = s_nMaxBoxes;
		}

		// Fill our array with the lift boxes.
		iPos = 0;
		int iBox;
		BoxLoop box = new BoxLoop(position, aPosDests, rollavator);
		for (iBox = 0; iBox < (nBoxes - 1); ++iBox)
		{
			// Make sure the second set of paired boxes are exactly opposite
			// on the track from their twin.
			if (iBox == nBoxes >> 1)
			{
				iPos = nPositions >> 1;
			}
			
			// Create a leader in front of our next lift box so we
			// can check to make sure a collision doesn't occur.
			BoxLoop leader = new BoxLoop(position, aPosDests, rollavator);
			add(iPos, box, leader);
			box = leader;

			// Update to the next track position.
			iPos += (nPositions / nBoxes);
		}
		// Add the last loop box, using the first as it's leader.
		add(iPos, box, m_lCars.get(0));

		// Now pair up the twins.
		nBoxes >>= 1;
		for (iBox = 0; iBox < nBoxes; ++iBox)
		{
			BoxLoop box1 = (BoxLoop)m_lCars.get(iBox);
			BoxLoop box2 = (BoxLoop)m_lCars.get(iBox + nBoxes);
			box1.setTwin(box2);
			box2.setTwin(box1);

			// For each box pairs, set the destination to the next higher/lower
			// level stack.
			box1.setDestination(PosEnum.UP_LEVEL_STACK, true, iBox + 1);
		}

		Trace.endOf("Rollavator.LiftLoop()");
	}

	/***********************************************************************************************
	 * Internal method for adding track positions to our lift.
	 *
	 * @param posList
	 *	    Return track positions list that should eventually form a complete loop.
	 * @param iPos
	 *	    Add new track positions to complete loop starting at this position.
	 * @param iLevelFirst
	 *	    Initial structure level for the new set of track positions.
	 * @param iLevelLast
	 *	    Ending structure level for the new set of track positions.
	 * @param locList
	 *	    Add this partial set of track positions.
	 * @return
	 *		Return the latest end of the track position loop.
	 */
	private int addLevelPositions(
		Position[] posList,
		int iPos,
		int iLevelFirst,
		int iLevelLast,
		final Location[] locList)
	{
		Orientation orientation = new Orientation();

		if (iLevelFirst <= iLevelLast)
		{
			for (int iLevel = iLevelFirst; iLevel <= iLevelLast; ++iLevel)
			{
				for (int iLoc = 0; iLoc < locList.length; ++iLoc)
				{
					posList[iPos++] = new Position(
						locList[iLoc].m_x,
						locList[iLoc].m_y,
						(4000 * iLevel) + locList[iLoc].m_z,
						orientation);
				}
			}
		}
		else
		{
			for (int iLevel = iLevelFirst; iLevel >= iLevelLast; --iLevel)
			{
				for (int iLoc = 0; iLoc < locList.length; ++iLoc)
				{
					posList[iPos++] = new Position(
						locList[iLoc].m_x,
						locList[iLoc].m_y,
						(4000 * iLevel) + locList[iLoc].m_z,
						orientation);
				}
			}
		}
		return iPos;
	}

	/***********************************************************************************************
	 * If found, return the rollavator Box that is at the given position.
	 */
	public Box getBox(int iPosition)
	{
		if (iPosition < m_aSections.length)
		{
			return getBox(m_aSections[iPosition]);
		}
		return null;
	}

	/***********************************************************************************************
	 * If found, return the rollavator Box that is at the given position.
	 */
	public Box getBox(Position position)
	{
		for (TrackCar car: m_lCars)
		{
			Box box = (BoxLoop)car;
			if (position.equals(box.getPosition()))
			{
				return box;
			}
		}
		return null;
	}

	/***********************************************************************************************
	 * {@inheritDoc}
	 */
	public void update(Simulation sim) throws Exception
	{
		Trace.startOf("rollavator.LiftLoop.update()");

		// Avoid redundant runs of the same update.
		long nTime = sim.getTime();
		if (nTime != m_nTime)
		{
			m_nTime = nTime;

			// Each BoxLoop twin is updated together.
			int nCars = m_lCars.size() >> 1;

			// Update each of the box cars.
			for (int i = 0; i < nCars; ++i)
			{
				BoxLoop box = (BoxLoop)m_lCars.get(i);

				// Run an update internal to the box.
				box.update(sim);
			}
		}

		Trace.endOf("rollavator.LiftLoop.update()");
	}
}

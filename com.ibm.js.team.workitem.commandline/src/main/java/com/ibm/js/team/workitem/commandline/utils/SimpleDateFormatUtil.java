/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.utils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;

/**
 * Class to handle conversion of dates into timestamps
 * 
 */
public class SimpleDateFormatUtil {

	public static final String DURATION_HOURS = "hours";
	public static final String DURATION_MINUTES = "minutes";
	public static final String SIMPLE_DATE_FORMAT_PATTERN_YYYY_MM_DD_HH_MM_SS_Z = "yyyy/MM/dd HH:mm:ss z";
	public static final String SIMPLE_DATE_FORMAT_PATTERN_YYYY_MM_DD = "yyyy/MM/dd";

	/**
	 * 
	 * Uses java.text.SimpleDateFormat to parse the string using a pattern Create a
	 * new timeStamp from a String using a pattern 'yyyy/MM/dd hh:mm:ss z'
	 * 
	 * @see http
	 *      ://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
	 * 
	 * @param aDate
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static Timestamp createTimeStamp(String aDate) {
		return createTimeStamp(aDate, null);
	}

	/**
	 * 
	 * Uses java.text.SimpleDateFormat to parse the string using a pattern Create a
	 * new timeStamp from a String using a pattern
	 * 
	 * @param aDate
	 * @param timeFormatPattern A SimpleDateFormat pattern to parse the string, a
	 *                          default pattern 'yyyy/MM/dd hh:mm:ss z' if null.
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static Timestamp createTimeStamp(String aDate, String timeFormatPattern) {
		if (null == timeFormatPattern) {
			timeFormatPattern = SIMPLE_DATE_FORMAT_PATTERN_YYYY_MM_DD_HH_MM_SS_Z;
		}
		SimpleDateFormat sDFormat = new SimpleDateFormat(timeFormatPattern);
		try {
			Date date = sDFormat.parse(aDate);
			return new Timestamp(date.getTime());
		} catch (ParseException e) {
			throw new WorkItemCommandLineException(
					"Parse Exception! Input: " + aDate + " Parsing pattern: " + timeFormatPattern, e);
		}
	}

	/**
	 * Chacks if dates date1 and date2 are the same day
	 * 
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static boolean sameDay(Timestamp date1, Timestamp date2) {
		SimpleDateFormat sDFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN_YYYY_MM_DD);
		return sDFormat.format(date1).equals(sDFormat.format(date2));
	}

	/**
	 * Get a string representation for a timestamp in a specified pattern
	 * 
	 * @param date
	 * @param timeFormatPattern
	 * @return
	 */
	public static String getDate(Timestamp date, String timeFormatPattern) {
		if (null == timeFormatPattern) {
			timeFormatPattern = SIMPLE_DATE_FORMAT_PATTERN_YYYY_MM_DD_HH_MM_SS_Z;
		}
		SimpleDateFormat sDFormat = new SimpleDateFormat(timeFormatPattern);
		return sDFormat.format(date);
	}

	/**
	 * Takes a duration and converts it into a long containing the miliseconds.
	 * Input format: 3 hours 3 minutes Input format: 3 hours Input format: 3 minutes
	 * Input format: 3600000
	 * 
	 * @param duration
	 * @return
	 */
	public static Long convertDurationToMiliseconds(String duration) {
		long time = 0;
		if (duration == null) {
			return time;
		}
		int hoursIndex = duration.indexOf(DURATION_HOURS);
		int minsIndex = duration.indexOf(DURATION_MINUTES);
		if (hoursIndex < 0 && minsIndex < 0) {
			return new Long(duration);
		}
		if (hoursIndex > 0) {
			String hours = duration.substring(0, hoursIndex - 1).trim();
			time += TimeUnit.HOURS.toMillis(new Long(hours));
		}
		if (minsIndex > 0) {
			int start = 0;
			if (hoursIndex > 0) {
				start = hoursIndex + DURATION_HOURS.length();
			}
			String minutes = duration.substring(start, duration.length() - DURATION_MINUTES.length()).trim();
			time += TimeUnit.MINUTES.toMillis(new Long(minutes));
		}
		return time;
	}

	/**
	 * Creates a presentation in hours, minutes example result format: 1 hours, 20
	 * minutes
	 * 
	 * @param milliseconds
	 * @return
	 */
	public static String convertToTimeSpent(Long milliseconds) {

		long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
		long minutesleft = milliseconds - TimeUnit.HOURS.toMillis(hours);
		long minutes = TimeUnit.MILLISECONDS.toDays(minutesleft);
		// String result =(days>0)?days + " days ":"";
		String result = (hours > 0) ? hours + " " + DURATION_HOURS + " " : "";
		result += (minutes > 0) ? minutes + " " + DURATION_MINUTES : "";
		return result.trim();
	}
}

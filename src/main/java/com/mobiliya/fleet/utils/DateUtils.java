package com.mobiliya.fleet.utils;


import android.annotation.SuppressLint;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by prashant on 02/20/2018.
 */

@SuppressWarnings({"ALL", "unused"})
public class DateUtils {

    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat FULL_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static Date ValueToDate(String val) throws ParseException {
        Date resultDate = null;
        if (val != null && !val.equals(StringUtils.EMPTY_STRING)) {
            resultDate = DateUtils.FULL_DATE_TIME_FORMAT.parse(val);
        }
        return resultDate;
    }

    public static String DateToValue(Object date) throws ParseException {
        return DateUtils.FULL_DATE_TIME_FORMAT.format(date);
    }

    public static String DateToString(Object date) {
        return DateUtils.FULL_DATE_TIME_FORMAT.format(date);
    }

    public static Date StringToDate(String val) {
        Date resultDate = null;
        if (val != null && !val.equals(StringUtils.EMPTY_STRING)) {
            try {
                resultDate = DateUtils.FULL_DATE_TIME_FORMAT.parse(val);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return resultDate;
    }

    public static String getUtcToLocalTime(long time) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE, MMMM d, hh:mm a"); //this format changeable
        dateFormatter.setTimeZone(java.util.TimeZone.getDefault());
        String dateString = dateFormatter.format(new Date(time));
        return dateString;
    }

    public static String getShortUtcToLocalTime(long time) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE, MMMM d'th', hh:mm a"); //this format changeable
        //dateFormatter.setTimeZone(java.util.TimeZone.getDefault());
        String dateString = dateFormatter.format(new Date(time));
        return dateString;
    }

    @SuppressLint("SimpleDateFormat")
    public static String tripListFormat(String title) {
        String currentdate = null;
        try {
            @SuppressLint("SimpleDateFormat") DateFormat formatter2 = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
            Date date = formatter2.parse(title);

            currentdate = new SimpleDateFormat("EEEE, MMMM d'th', hh:mm a").format(date);
        } catch (Exception ex) {
            ex.getMessage();
        }
        return currentdate;
    }

    @SuppressLint("SimpleDateFormat")
    public static String tripOngoingFormat(String title) {
        String currentdate = null;
        try {
            @SuppressLint("SimpleDateFormat") DateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date date = formatter2.parse(title);

            currentdate = new SimpleDateFormat("EEEE, MMMM d hh:mm a").format(date);
        } catch (Exception ex) {
            ex.getMessage();
        }
        return currentdate;
    }

    @SuppressLint("SimpleDateFormat")
    public static String tripDetailFormat(String title) {

        String currentdate = null;
        try {
            @SuppressLint("SimpleDateFormat") DateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date date = formatter1.parse(title);

            currentdate = new SimpleDateFormat("dd MMM yyyy, hh:mma").format(date);
        } catch (Exception ex) {
            ex.getMessage();
        }
        return currentdate;
    }

    @SuppressLint("SimpleDateFormat")
    public static String tripDetailHeaderFormat(String title) {
        String currentdate = null;
        try {
            @SuppressLint("SimpleDateFormat") DateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date date = formatter1.parse(title);

            currentdate = new SimpleDateFormat("EEEE, MMMM d hh:mm a").format(date);
        } catch (Exception ex) {
            ex.getMessage();
        }
        return currentdate;
    }

    public static Date getDateFromLong(long time) {
        return new Date(time);
    }

    public static long getUtcToLocalTimeLong() {
        long ts = System.currentTimeMillis();
        return ts;
    }

    public static String getLocalTimeString() {
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date date = new Date();
        String format = dateFormat.format(date);
        return format;
    }

    public static String getTimeDifference(Date start) {
        String diffence = "00";

        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String start_str = null;
        String end_str = null;
        Date start_date = null;
        Date end_date = null;

        try {
            start_str = formater.format(start);
            end_str = formater.format(new Date());

            start_date = formater.parse(start_str);
            end_date = formater.parse(end_str);

            //in milliseconds
            long diff = end_date.getTime() - start_date.getTime();

            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);


            if (diffDays == 0) {

                if (diffMinutes == 0) {
                    if (diffSeconds != 0) {
                        diffence = diffSeconds + " sec";
                        return diffence;
                    } else {
                        diffence = diffSeconds + "";
                        return diffence;
                    }
                } else {
                    diffence = diffMinutes + "." + diffSeconds + " mins";
                    return diffence;
                }

            } else {
                diffence = diffDays + " days";
                return diffence;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return diffence;
    }

    public static String dashbordTripTime(String startTime) {
        String currentdate = null;
        try {
            @SuppressLint("SimpleDateFormat") DateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date date = formatter1.parse(startTime);

            currentdate = new SimpleDateFormat("EEEE, MMMM dd").format(date);
        } catch (Exception ex) {
            ex.getMessage();
        }
        return currentdate;
    }

    public static Date getDateFromString(String startTime) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date format = null;
        try {
            format = dateFormat.parse(startTime);
        } catch (Exception ex) {
            ex.getMessage();
        }
        return format;
    }

    public static String getDateForLastsync(String startTime) {
        DateFormat from_dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date date = null;
        String currentdate = null;
        try {
            date = from_dateFormat.parse(startTime);

            currentdate = new SimpleDateFormat("EEEE, MMMM dd HH:mm a").format(date);
        } catch (Exception ex) {
            ex.getMessage();
        }
        return currentdate;
    }
}

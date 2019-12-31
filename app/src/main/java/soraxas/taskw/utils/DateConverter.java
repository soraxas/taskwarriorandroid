package soraxas.taskw.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateConverter {


    public static String convertToString(Date date, int countDownThreshold, boolean forceIgnoreMidnightTime) {
        Date currentTime = Calendar.getInstance().getTime();

        String timeString = "";

        if (!forceIgnoreMidnightTime) {
            // include time in the formatting as well if it's not 00:00 (which is default for TW)
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            if (!(c.get(Calendar.HOUR_OF_DAY) == 0 && c.get(Calendar.MINUTE) == 0)) { // 00:00
                timeString = new SimpleDateFormat(" @ HH:mm").format(date);
            }
        }

        String dateString = "";
        // use count down threshold
        if (countDownThreshold > -1) {
            long differences = date.getTime() - currentTime.getTime();
            long elapsedDays = differences / (1000 * 60 * 60 * 24);

            // if the delta date is within x days, displace the differences instead.
            if (elapsedDays < countDownThreshold) {
                if (elapsedDays == 0) {
                    dateString = "today";
                } else if (elapsedDays == 1) {
                    dateString = String.format("in %d day", elapsedDays);
                } else {
                    dateString = String.format("in %d days", elapsedDays);
                }
            }
        }
        if (dateString == ""){
            // parse as simple datetime
            String datetime_pattern;
            // if the due date year is not same as current year, include the year in formatting as well
            if (date.getYear() == currentTime.getYear()) {
                datetime_pattern = "dd MMM";
            } else {
                datetime_pattern = "dd MMM YYYY";
            }
            dateString = new SimpleDateFormat(datetime_pattern).format(date);
        }
        return dateString + timeString;
    }
}

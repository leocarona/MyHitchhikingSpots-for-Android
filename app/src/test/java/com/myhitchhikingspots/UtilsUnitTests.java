package com.myhitchhikingspots;

import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.Utils;

import static com.google.common.truth.Truth.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

import java.util.Date;

public class UtilsUnitTests {
    @Test
    public void forceDateTimeIntoUTC_currentDateInLocalTimezone_ReturnsSameValues() {
        DateTime dtInLocalTimeZone = DateTime.now();
        DateTime dtUTC = Utils.forceTimeZoneToUTC(dtInLocalTimeZone);

        assertThat(dtUTC.getYearOfEra()).isEqualTo(dtInLocalTimeZone.getYearOfEra());
        assertThat(dtUTC.getMonthOfYear()).isEqualTo(dtInLocalTimeZone.getMonthOfYear());
        assertThat(dtUTC.getDayOfMonth()).isEqualTo(dtInLocalTimeZone.getDayOfMonth());
        assertThat(dtUTC.getHourOfDay()).isEqualTo(dtInLocalTimeZone.getHourOfDay());
        assertThat(dtUTC.getMinuteOfHour()).isEqualTo(dtInLocalTimeZone.getMinuteOfHour());
        assertThat(dtUTC.getZone()).isNotEqualTo(dtInLocalTimeZone.getZone());
    }

    @Test
    public void forceDateTimeIntoUTC_dateInLocalTimezone_ReturnsSameValues() {
        Long StartDateTimeMillis = 1545995880000l;

        Spot s = new Spot();
        s.setStartDateTimeMillis(StartDateTimeMillis);

        DateTime dtInLocalTimeZone = new DateTime(s.getStartDateTimeMillis());
        s.setStartDateTime(Utils.forceTimeZoneToUTC(dtInLocalTimeZone));

        DateTime dtUTC = s.getStartDateTime();

        assertThat(dtUTC.getYearOfEra()).isEqualTo(dtInLocalTimeZone.getYearOfEra());
        assertThat(dtUTC.getMonthOfYear()).isEqualTo(dtInLocalTimeZone.getMonthOfYear());
        assertThat(dtUTC.getDayOfMonth()).isEqualTo(dtInLocalTimeZone.getDayOfMonth());
        assertThat(dtUTC.getHourOfDay()).isEqualTo(dtInLocalTimeZone.getHourOfDay());
        assertThat(dtUTC.getMinuteOfHour()).isEqualTo(dtInLocalTimeZone.getMinuteOfHour());
        assertThat(dtUTC.getZone()).isNotEqualTo(dtInLocalTimeZone.getZone());
    }

    @Test
    public void getExportFileName_dateInLocalTimezone_ReturnsFileNameContainingTheSameDate() {
        DateTime date = new DateTime(2019, 1, 1, 12, 0, 0, DateTimeZone.UTC);
        String expectedFormattedString = "2019_01_01_1200-my_hitchhiking_spots.csv";

        assertThat(Utils.getExportFileName(date)).isEqualTo(expectedFormattedString);
    }
}


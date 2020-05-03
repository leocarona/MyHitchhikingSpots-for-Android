package com.myhitchhikingspots;

import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.Utils;

import static com.google.common.truth.Truth.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

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
    public void getNewExportFileName_dateTimeInSaoPauloTimeZone_ReturnsFileNameContainingOffsetBetweenSaoPauloAndUTC() {
        DateTimeZone saoPauloTZ = DateTimeZone.forID("America/Sao_Paulo");

        //12:00:00 in Sao Paulo on 2019/01/01 (BRST) was 14:00:00 in UTC.
        DateTime date = new DateTime(2019, 1, 1, 12, 0, 0, saoPauloTZ);

        //The new format includes the offset (-0200 in this case) to the name of the file.
        String expectedFormattedString = "2019_01_01_1200-0200#my_hitchhiking_spots.csv";

        assertThat(Utils.getNewExportFileName(date, saoPauloTZ)).isEqualTo(expectedFormattedString);
    }

    @Test
    public void extractDateTimeFromFileName_fileInOldNameFormatExportedInSaoPauloTimeZone_ReturnsExtractedDateTimeInSaoPauloTimeZone() throws IllegalArgumentException {
        DateTimeZone saoPauloTZ = DateTimeZone.forID("America/Sao_Paulo");

        //12:00:00 in Sao Paulo on 2019/01/01 (BRST) was 14:00:00 in UTC.
        DateTime expectedDateTime = new DateTime(2019, 1, 1, 12, 0, 0, saoPauloTZ);

        //The old name format used to include only the local datetime (12:00 in this case) as-is to the name of the file.
        //Refer to Constants.OLD_EXPORT_CSV_FILENAME_FORMAT.
        assertThat(Utils.extractDateTimeFromFileName("2019_01_01_1200-my_hitchhiking_spots.csv", saoPauloTZ)).isEqualTo(expectedDateTime);
        assertThat(Utils.extractDateTimeFromFileName("2019_01_01_1200-anything-else", saoPauloTZ)).isEqualTo(expectedDateTime);
    }

    @Test
    public void extractDateTimeFromFileName_fileInNewNameFormatExportedInSaoPauloTimeZone_ReturnsSameDateTimeInChosenTimeZone() throws IllegalArgumentException {
        //UTC is our chosen timezone on this scenario.
        DateTimeZone chosenTZ = DateTimeZone.UTC;

        //User exports his database when he was in Sao Paulo timezone, then he flies to another timezone (UTC, on this scenario).
        DateTime dateTimeInSaoPaulo = new DateTime(2019, 1, 1, 10, 0, 0, DateTimeZone.forID("America/Sao_Paulo"));

        //Get datetime in UTC that corresponds to the moment when in Sao Paulo was '2019/01/01 10:00' (defined on dateTimeInSaoPaulo)
        DateTime dateTimeInChosenTZ = dateTimeInSaoPaulo.withZone(chosenTZ);

        //Name of the file generated within Sao Paulo timezone.
        String fileName = "2019_01_01_1000-0200#my_hitchhiking_spots.csv";

        //Extracting the datetime of the moment when the file name was generated, although now been in a different timezone.
        DateTime expectedUTCDateTime = Utils.extractDateTimeFromFileName(fileName, chosenTZ);

        assertThat(expectedUTCDateTime).isEqualTo(dateTimeInChosenTZ);
    }

    @Test
    public void extractDateTimeFromFileName_fileInNewNameFormatExportedInUTCTimeZone_ReturnsSameDateTimeInChosenTimeZone() throws IllegalArgumentException {
        //Singapore is our chosen timezone on this scenario.
        DateTimeZone chosenTZ = DateTimeZone.forID("Asia/Singapore");

        //Name of the file generated at 12:00:00 UTC on 2019/01/01.
        String fileName = "2019_01_01_1200-0000#my_hitchhiking_spots.csv";
        DateTime dateTimeInUTC = new DateTime(2019, 1, 1, 12, 0, 0, DateTimeZone.UTC);

        //Get datetime in Singapore that corresponds to the moment when in UTC was '2019/01/01 12:00' (defined on dateTimeInUTC)
        DateTime dateTimeInChosenTZ = dateTimeInUTC.withZone(chosenTZ);

        //12:00:00 in UTC on 2019/01/01 was 20:00:00 in Singapore.
        //Extracting the datetime of the moment when the file name was generated, although now been in a different timezone.
        DateTime extractedDateTime = Utils.extractDateTimeFromFileName(fileName, chosenTZ);

        assertThat(extractedDateTime).isEqualTo(dateTimeInChosenTZ);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractDateTimeFromFileName_fileNameHasNoDateTime_ThrowsIllegalArgumentException() throws IllegalArgumentException {
        assertThat(Utils.extractDateTimeFromFileName("anything-else", DateTimeZone.UTC)).isEqualTo(null);
    }

    @Test
    public void dateTimeToString_dateTimeInUTCOnADifferentYear_ReturnDayMonthYearAndTime() {
        DateTime dateTimeInUTC = new DateTime(2015, 1, 1, 12, 0, 0, DateTimeZone.UTC);

        assertThat(Utils.dateTimeToString(dateTimeInUTC)).isEqualTo("01/01/2015, 12:00");
    }

    @Test
    public void dateTimeToString_dateTimeInUTCOnTheSameYear_ReturnDayMonthAndTimeWithoutYear() {
        DateTime dateTimeInUTC = new DateTime(DateTime.now(DateTimeZone.UTC).getYearOfEra(), 1, 1, 12, 0, 0, DateTimeZone.UTC);

        assertThat(Utils.dateTimeToString(dateTimeInUTC)).isEqualTo("01/Jan, 12:00");
    }
}


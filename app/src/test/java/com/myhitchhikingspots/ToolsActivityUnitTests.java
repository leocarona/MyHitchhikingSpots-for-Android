package com.myhitchhikingspots;

import com.myhitchhikingspots.utilities.Utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class ToolsActivityUnitTests {
    @Test
    public void shouldFixStartDateTimes_fileNameContainingDateBeforeVersion27_ReturnsTrue() {
        DateTimeZone saoPauloTZ = DateTimeZone.forID("America/Sao_Paulo");
        String fileNameOfFileExportedBeforeReleaseOfVersion27 = "2018_01_01_0000-my_hitchhiking_spots.csv";

        assertThat(ToolsActivity.shouldFixStartDateTimes(fileNameOfFileExportedBeforeReleaseOfVersion27)).isEqualTo(true);

        //The code below isn't really a Unit Test since it depends on other methods such as Utils.getNewExportFileName (which is already being tested by UtilsUnitTest).
        // But let's keep the code below anyways as it might help on figuring out what might be causing some other issue.
        DateTime version27_releasedOn = DateTime.parse(Constants.APP_VERSION27_WAS_RELEASED_ON_UTCDATETIME);
        DateTime dateTimeBeforeReleaseOfVersion27 = version27_releasedOn.withDurationAdded(Duration.millis(1), -1);
        String fileName = Utils.getNewExportFileName(dateTimeBeforeReleaseOfVersion27, saoPauloTZ);

        assertThat(ToolsActivity.shouldFixStartDateTimes(fileName)).isEqualTo(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFixStartDateTimes_fileNameContainingNoDate_ReturnsFalse() throws IllegalArgumentException {
        String fileName = "file-has-been-rename-to-anything-else.csv";

        ToolsActivity.shouldFixStartDateTimes(fileName);
    }

    @Test
    public void shouldFixStartDateTimes_fileNameContainingDateAfterVersion27_ReturnsFalse() {
        DateTimeZone saoPauloTZ = DateTimeZone.forID("America/Sao_Paulo");
        String fileNameOfFileExportedAfterReleaseOfVersion27 = "2020_01_01_0000+0000#my_hitchhiking_spots.csv";

        assertThat(ToolsActivity.shouldFixStartDateTimes(fileNameOfFileExportedAfterReleaseOfVersion27)).isEqualTo(false);

        //The code below isn't really a Unit Test since it depends on other methods such as Utils.getNewExportFileName (which is already being tested by UtilsUnitTest).
        // But let's keep the code below anyways as it might help on figuring out what might be causing some other issue.
        DateTime version27_releasedOn = DateTime.parse(Constants.APP_VERSION27_WAS_RELEASED_ON_UTCDATETIME);
        DateTime dateTimeAfterReleaseOfVersion27 = version27_releasedOn.withDurationAdded(Duration.millis(1), 1);
        String fileName = Utils.getNewExportFileName(dateTimeAfterReleaseOfVersion27, saoPauloTZ);

        assertThat(ToolsActivity.shouldFixStartDateTimes(fileName)).isEqualTo(false);
    }
}


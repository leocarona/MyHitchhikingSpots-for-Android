package com.myhitchhikingspots;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.savvi.rangedatepicker.CalendarPickerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * Create a calendar with a list of specific dates that can be selected by the user, and all the rest of the dates are deactivated.
 * Define the selectable dates using .withAllDatesDeactivatedExcept(list)
 * When a range is selected, onDatesSelected is called.
 * If a range is already selected and the user selects one more date, the previously selected range is deselected and the user can select it again.
 */
public class DateRangePickerDialog extends Dialog {
    private Context context;
    private CalendarPickerView calendar;
    private final ArrayList<Date> possibleBeginningDates = new ArrayList<>();
    private final ArrayList<Date> possibleEndingDates = new ArrayList<>();
    private DateRangeListener listener;

    public DateRangePickerDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    public void setRangeOptions(ArrayList<Date> beginDates, ArrayList<Date> endDates, DateRangeListener listener) {
        this.possibleBeginningDates.addAll(beginDates);
        this.possibleEndingDates.addAll(endDates);
        this.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.date_range_picker_layout);

        final Calendar nextCalendarDay = Calendar.getInstance();
        final Calendar lastCalendarDay = Calendar.getInstance();

        calendar = findViewById(R.id.calendar_view);

        if (!possibleBeginningDates.isEmpty())
            lastCalendarDay.setTime(possibleBeginningDates.get(0));
        if (!possibleEndingDates.isEmpty())
            nextCalendarDay.setTime(possibleEndingDates.get(possibleEndingDates.size() - 1));

        nextCalendarDay.add(Calendar.DAY_OF_MONTH, 1);
        lastCalendarDay.add(Calendar.DAY_OF_MONTH, -1);


        calendar.init(lastCalendarDay.getTime(), nextCalendarDay.getTime(), new SimpleDateFormat("MMMM, YYYY", Locale.getDefault()))
                .inMode(CalendarPickerView.SelectionMode.RANGE)
                .withAllDatesDeactivatedExcept(possibleBeginningDates);

        // When the beginning date of the range has been selected, let the user choose one of the possible ending dates.
        // When a range is selected call onDatesSelected(list).
        // If user selects another date once a range is already selected, that range will be deselected so that user can select it again.
        calendar.setOnDateSelectedListener(new CalendarPickerView.OnDateSelectedListener() {
            @Override
            public void onDateSelected(Date date) {
                // If only beginning date has been selected, activate possibleEndingDates
                if (calendar.getSelectedDates().size() == 1)
                    calendar.replaceActivatedDates(possibleEndingDates);
                else // If a range has been selected
                    if (calendar.getSelectedDates().size() > 1)
                        // Remove all activated dates
                        calendar.replaceActivatedDates(new ArrayList<>());

                updateClearSelectedDatesButtonState();
                updateSelectDatesButtonState();
            }

            @Override
            public void onDateUnselected(Date date) {
            }
        });

        // When user tries to select a third date, deselect the previous range and let the user choose one of the possible beginning dates again.
        calendar.setOnInvalidDateSelectedListener(new CalendarPickerView.OnInvalidDateSelectedListener() {
            @Override
            public void onInvalidDateSelected(Date date) {
                if (!calendar.getSelectedDates().isEmpty()) {
                    // Deselect all dates
                    calendar.clearSelectedDates();
                    // Activate possibleBeginningDates again
                    calendar.replaceActivatedDates(possibleBeginningDates);
                    updateClearSelectedDatesButtonState();
                }

                    if (possibleBeginningDates.contains(date))
                        calendar.selectDate(date);
                } else
                    // Deselect all dates
                    calendar.clearSelectedDates();
                updateSelectDatesButtonState();
            }
        });

        calendar.scrollToDate(nextCalendarDay.getTime());

        findViewById(R.id.select_dates).setEnabled(false);
        findViewById(R.id.select_dates).setOnClickListener((v) -> {
            if (calendar.getSelectedDates().size() > 0)
                onDatesSelected(calendar.getSelectedDates());
        });
        findViewById(R.id.clear_selected_dates).setOnClickListener((v) -> {
            // Deselect all dates
            calendar.clearSelectedDates();
            onDatesClear();
        });
        updateClearSelectedDatesButtonState();

        if (getWindow() != null)
            getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    private void updateSelectDatesButtonState() {
        findViewById(R.id.select_dates).setEnabled(!calendar.getSelectedDates().isEmpty());
    }

    private void updateClearSelectedDatesButtonState() {
        View clearSelectedDatesButton = findViewById(R.id.clear_selected_dates);
        if (clearSelectedDatesButton == null)
            return;

        if (calendar.getSelectedDates().isEmpty())
            clearSelectedDatesButton.setVisibility(View.INVISIBLE);
        else
            clearSelectedDatesButton.setVisibility(View.VISIBLE);
    }

    private void onDatesSelected(List<Date> selectedDates) {
        if (listener != null)
            listener.onRangeSelected(selectedDates);
    }

    private void onDatesClear() {
        if (listener != null)
            listener.onRangeCleared();
    }

    public interface DateRangeListener {
        void onRangeSelected(List<Date> selectedDates);

        void onRangeCleared();
    }
}

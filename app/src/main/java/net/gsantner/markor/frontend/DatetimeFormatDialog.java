/*#######################################################
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.os.ConfigurationCompat;

import net.gsantner.markor.R;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DatetimeFormatDialog {
    private static final String RECENT_FORMAT_STRING = "datetimformat_dialog__recent_format_string_history";

    private final static String[] PREDEFINED_DATE_TIME_FORMATS = {
            "hh:mm",
            "HH:mm",
            "hh:mm:ss",
            "HH:mm:ss",
            "yyyy/MM/dd",
            "yyyy.MM.dd",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd.MM.yyyy",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "'[Week 'w']' EEEE"
    };

    /**
     * @param activity {@link Activity} from which is {@link DatetimeFormatDialog} called
     * @param hlEditor {@link HighlightingEditor} which 'll add selected result to cursor position
     */
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n, InflateParams"})
    public static void showDatetimeFormatDialog(final Activity activity, final HighlightingEditor hlEditor) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View viewRoot = activity.getLayoutInflater().inflate(R.layout.time_format_dialog, null);

        final GsContextUtils cu = new GsContextUtils();

        final Locale locale = ConfigurationCompat.getLocales(activity.getResources().getConfiguration()).get(0);

        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);

        final AtomicReference<Dialog> dialog = new AtomicReference<>();
        final AtomicReference<GsCallback.a1<String>> callbackInsertTextToEditor = new AtomicReference<>();
        final ListPopupWindow popupWindow = new ListPopupWindow(activity);
        final EditText formatEditText = viewRoot.findViewById(R.id.datetime_format_input);
        final TextView previewTextView = viewRoot.findViewById(R.id.formatted_example);
        final Button datePickButton = viewRoot.findViewById(R.id.start_datepicker_button);
        final Button timePickButton = viewRoot.findViewById(R.id.start_timepicker_button);
        final CheckBox formatInsteadCheckbox = viewRoot.findViewById(R.id.get_format_instead_date_or_time_checkbox);
        final CheckBox alwaysNowCheckBox = viewRoot.findViewById(R.id.always_use_current_datetime_checkbox);

        final String recentFormat = getRecentFormat(activity);
        final List<String> allFormats = getAllFormats();

        // Popup window for ComboBox
        popupWindow.setAdapter(new SimpleAdapter(activity, createAdapterData(locale, allFormats),
                android.R.layout.simple_expandable_list_item_2, new String[]{"format", "date"},
                new int[]{android.R.id.text1, android.R.id.text2}
        ));

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            formatEditText.setText(allFormats.get(position));
            popupWindow.dismiss();
            setToNow(cal, alwaysNowCheckBox.isChecked());
            previewTextView.setText(cu.formatDateTime(locale, formatEditText.getText().toString(), cal.getTimeInMillis()));
        });

        popupWindow.setAnchorView(formatEditText);
        popupWindow.setModal(true);
        viewRoot.findViewById(R.id.datetime_format_input_show_spinner).setOnClickListener(v -> popupWindow.show());

        // monitor format input at combobox and update resulting value
        formatEditText.addTextChangedListener(new TextWatcher() {
            private final static int DELAY = 100;
            private long editTime = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                editTime = System.currentTimeMillis();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editTime = System.currentTimeMillis();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (editTime + DELAY > System.currentTimeMillis()) {
                    setToNow(cal, alwaysNowCheckBox.isChecked());
                    previewTextView.setText(cu.formatDateTime(locale, formatEditText.getText().toString(), cal.getTimeInMillis()));
                    final boolean error = previewTextView.getText().toString().isEmpty() && !formatEditText.getText().toString().isEmpty();
                    formatEditText.setError(error ? "^^^!!!  'normal text'" : null);
                    previewTextView.setVisibility(error ? View.GONE : View.VISIBLE);
                }
            }
        });

        formatEditText.setText(recentFormat);
        viewRoot.findViewById(R.id.time_format_just_date).setOnClickListener(b -> callbackInsertTextToEditor.get().callback(cu.getLocalizedDateFormat(activity)));
        viewRoot.findViewById(R.id.time_format_just_time).setOnClickListener(b -> callbackInsertTextToEditor.get().callback(cu.getLocalizedTimeFormat(activity)));

        // Pick Date Dialog
        datePickButton.setOnClickListener(button -> new DatePickerDialog(activity, (view, year, month, day) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    previewTextView.setText(cu.formatDateTime(locale, formatEditText.getText().toString(), cal.getTimeInMillis()));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        );

        // Pick Time Dialog
        timePickButton.setOnClickListener(button -> new TimePickerDialog(activity, (timePicker, hour, min) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, min);
                    previewTextView.setText(cu.formatDateTime(locale, formatEditText.getText().toString(), cal.getTimeInMillis()));
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        );

        // hide buttons when both check box are checked
        View.OnClickListener onOptionsChangedListener = v -> {
            boolean dateChangeable = !formatInsteadCheckbox.isChecked() && !alwaysNowCheckBox.isChecked();
            timePickButton.setEnabled(dateChangeable);
            datePickButton.setEnabled(dateChangeable);
            alwaysNowCheckBox.setEnabled(!formatInsteadCheckbox.isChecked());
        };
        formatInsteadCheckbox.setOnClickListener(onOptionsChangedListener);
        alwaysNowCheckBox.setOnClickListener(onOptionsChangedListener);

        callbackInsertTextToEditor.set((selectedFormat) -> {
            setToNow(cal, alwaysNowCheckBox.isChecked());
            String text = cu.formatDateTime(locale, selectedFormat, cal.getTimeInMillis());
            previewTextView.setText(text);
            hlEditor.insertOrReplaceTextOnCursor(getOutput(
                    formatInsteadCheckbox.isChecked(), text, formatEditText.getText().toString())
            );
            dialog.get().dismiss();
        });

        // set builder and implement buttons to discard and submit
        builder.setView(viewRoot);
        builder.setNeutralButton(R.string.help, (dlgI, which) -> cu.openWebpageInExternalBrowser(activity, "https://developer.android.com/reference/java/text/SimpleDateFormat#date-and-time-patterns"));
        builder.setNegativeButton(R.string.cancel, null);
        // OK
        builder.setPositiveButton(android.R.string.ok, (dlgI, which) -> {
            final String current = formatEditText.getText().toString();
            saveRecentFormat(activity, current);
            callbackInsertTextToEditor.get().callback(current);
        });

        dialog.set(builder.show());
    }

    /**
     * @param isUseFormatInstead {@link Boolean} information if we want _datetime or format
     * @param datetime           selected _datetime as {@link String} based on given format
     * @param format             {@link String} pattern used to convert _datetime into text output
     * @return @datetime or @format, based on @isUseFormatInstead
     */
    private static String getOutput(Boolean isUseFormatInstead, String datetime, String format) {
        return isUseFormatInstead != null && isUseFormatInstead ? format : datetime;
    }

    // >

    /**
     * Combine recent formats with the predefined formats.
     *
     * @return Formats list.
     */
    private static List<String> getAllFormats() {
        return new ArrayList<>(Arrays.asList(PREDEFINED_DATE_TIME_FORMATS));
    }
    // <

    /**
     * Create data for adapter.
     *
     * @param locale  Locale for generating date-time strings
     * @param formats List of formats to generate format-time pair maps for
     * @return List of format-pair maps
     */
    private static List<Map<String, String>> createAdapterData(final Locale locale, final List<String> formats) {
        List<Map<String, String>> formatsAndParsed = new ArrayList<>();
        final long currentMillis = System.currentTimeMillis();

        // Add recent formats
        for (final String f : formats) {
            Map<String, String> pair = new HashMap<>(2);
            pair.put("format", f);
            pair.put("date", GsContextUtils.instance.formatDateTime(locale, f, currentMillis, ""));
            formatsAndParsed.add(pair);
        }

        return formatsAndParsed;
    }

    /**
     * set cal to current time if doIt is set
     */
    private static void setToNow(final Calendar cal, boolean doIt) {
        if (doIt) {
            cal.setTime(new Date());
        }
    }

    // >
    private static void saveRecentFormat(final Activity activity, final String format) {
        if (format == null || format.trim().isEmpty()) {
            return;
        }

        final SharedPreferences.Editor edit = activity.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE).edit();
        edit.putString(RECENT_FORMAT_STRING, format).apply();
    }
    // <

    // >

    /**
     * Load recent format from settings.
     *
     * @param context Context in order to get settings
     * @return List of Strings representing recently used formats
     */
    private static String getRecentFormat(final Context context) {
        final SharedPreferences settings = context.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);

        return settings.getString(RECENT_FORMAT_STRING, "");
    }
    // <

    // >

    /**
     * Get a date string for the recently used format
     *
     * @param context Activity in order to get settings
     * @return String representing current date/time in last used format
     */
    public static String getRecentDate(final Context context) {
        final String format = getRecentFormat(context);
        if (format.length() > 0) {
            return GsContextUtils.instance.formatDateTime(context, format, System.currentTimeMillis());
        } else {
            return "";
        }
    }
    // <
}

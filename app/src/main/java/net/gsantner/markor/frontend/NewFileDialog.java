/*#######################################################
 *
 *   Maintained 2018-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.format.wikitext.WikitextActionButtons;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsAndroidSpinnerOnItemSelectedAdapter;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;


public class NewFileDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = NewFileDialog.class.getName();
    public static final String EXTRA_DIR = "EXTRA_DIR";
    public static final String EXTRA_ALLOW_CREATE_DIR = "EXTRA_ALLOW_CREATE_DIR";
    private GsCallback.a2<Boolean, File> callback;
    private EditText fileNameEdit;


    public static NewFileDialog newInstance(final File sourceFile, final boolean allowCreateDir, final GsCallback.a2<Boolean, File> callback) {
        NewFileDialog dialog = new NewFileDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DIR, sourceFile);
        args.putSerializable(EXTRA_ALLOW_CREATE_DIR, allowCreateDir);
        dialog.setArguments(args);
        dialog.callback = callback;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_DIR);
        final boolean allowCreateDir = getArguments().getBoolean(EXTRA_ALLOW_CREATE_DIR);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = makeDialog(file, allowCreateDir, inflater);
        AlertDialog dialog = dialogBuilder.show();
        // >
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        positive.setEnabled(false);
        neutral.setEnabled(false);
        fileNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() == 0) {
                    positive.setEnabled(false);
                    neutral.setEnabled(false);
                } else {
                    if (!positive.isEnabled()) {
                        positive.setEnabled(true);
                    }

                    if (!neutral.isEnabled()) {
                        neutral.setEnabled(true);
                    }
                }
            }
        });
        // <
        Window w;
        if ((w = dialog.getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    @SuppressLint("SetTextI18n")
    private AlertDialog.Builder makeDialog(final File basedir, final boolean allowCreateDir, LayoutInflater inflater) {
        View root;
        AlertDialog.Builder dialogBuilder;
        final AppSettings appSettings = ApplicationObject.settings();
        dialogBuilder = new AlertDialog.Builder(inflater.getContext(), R.style.Theme_AppCompat_DayNight_Dialog);
        root = inflater.inflate(R.layout.new_file_dialog, null);

        fileNameEdit = root.findViewById(R.id.new_file_dialog__name);
        final EditText fileExtEdit = root.findViewById(R.id.new_file_dialog__ext);
        final CheckBox encryptCheckbox = root.findViewById(R.id.new_file_dialog__encrypt);
        final CheckBox utf8BomCheckbox = root.findViewById(R.id.new_file_dialog__utf8_bom);
        final Spinner typeSpinner = root.findViewById(R.id.new_file_dialog__type); // File type
        final Spinner templateSpinner = root.findViewById(R.id.new_file_dialog__template); // Template
        ArrayList<String> extensions = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.new_file_types__file_extension)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && appSettings.isDefaultPasswordSet()) {
            encryptCheckbox.setChecked(appSettings.getNewFileDialogLastUsedEncryption());
        } else {
            encryptCheckbox.setVisibility(View.GONE);
        }
        utf8BomCheckbox.setChecked(appSettings.getNewFileDialogLastUsedUtf8Bom());
        utf8BomCheckbox.setVisibility(appSettings.isExperimentalFeaturesEnabled() ? View.VISIBLE : View.GONE);

        // Filter
        fileNameEdit.setFilters(new InputFilter[]{GsContextUtils.instance.makeFilenameInputFilter()});
        fileExtEdit.setFilters(fileNameEdit.getFilters());
        // Focus
        fileNameEdit.requestFocus();
        new Handler().postDelayed(new GsContextUtils.DoTouchView(fileNameEdit), 200);

        // >
        int lastPosition = appSettings.getNewFileDialogLastUsedType();
        String extension = appSettings.getNewFileDialogLastUsedExtension();
        fileExtEdit.setText(extension);

        int index = 0;
        for (; index < extensions.size(); index++) {
            if (extension.equals(extensions.get(index))) {
                if (!extension.equals(".txt")) {
                    typeSpinner.setSelection(index);
                    break;
                } else if (index == lastPosition) {
                    typeSpinner.setSelection(lastPosition);
                    break;
                }
            }
        }
        if (index == extensions.size()) {
            typeSpinner.setSelection(index - 1);
        }
        // <

        loadTemplatesIntoSpinner(appSettings, templateSpinner);

        final AtomicBoolean typeSpinnerNoTriggerOnFirst = new AtomicBoolean(true);
        typeSpinner.setOnItemSelectedListener(new GsAndroidSpinnerOnItemSelectedAdapter(position -> {
            if (position == 3 && !templateSpinner.getSelectedItem().toString().contains("wiki")) { // If Wikitext
                templateSpinner.setSelection(7); // Zim empty
            }

            if (typeSpinnerNoTriggerOnFirst.getAndSet(false)) {
                return;
            }

            String ext = position < extensions.size() ? extensions.get(position) : "";
            if (ext != null) {
                if (encryptCheckbox.isChecked()) {
                    fileExtEdit.setText(ext + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
                } else {
                    fileExtEdit.setText(ext);
                }
            }
            fileNameEdit.setSelection(fileNameEdit.length());
            appSettings.setNewFileDialogLastUsedType(typeSpinner.getSelectedItemPosition());
        }));

        templateSpinner.setOnItemSelectedListener(new GsAndroidSpinnerOnItemSelectedAdapter(position -> {
            // >
            String label = templateSpinner.getSelectedItem().toString();
            int i = label.lastIndexOf('.');
            if (i == -1) {
                return;
            }

            String end = label.substring(i);
            i = extensions.indexOf(end);
            if (end.equals(".txt")) {
                if (label.contains("todo")) {
                    end = ".todo" + end;
                    i = extensions.indexOf(end);
                }
                if (label.contains("wiki")) {
                    i = extensions.lastIndexOf(end);
                }
            }
            fileExtEdit.setText(end);
            typeSpinner.setSelection(i);
            // <

            String prefix = null;

            if (position == 3 || position == 10) { // Jekyll
                prefix = TodoTxtTask.DATEF_YYYY_MM_DD.format(new Date()) + "-";
            } else if (position == 9) { // ZettelKasten
                prefix = new SimpleDateFormat("yyyyMMddHHmm", Locale.ROOT).format(new Date()) + "-";
            }
            if (!TextUtils.isEmpty(prefix) && !fileNameEdit.getText().toString().startsWith(prefix)) {
                fileNameEdit.setText(prefix + fileNameEdit.getText().toString());
            }
            fileNameEdit.setSelection(fileNameEdit.length());
        }));

        encryptCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            final String currentExtention = fileExtEdit.getText().toString();
            if (isChecked) {
                if (!currentExtention.endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION)) {
                    fileExtEdit.setText(currentExtention + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
                }
            } else if (currentExtention.endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION)) {
                fileExtEdit.setText(currentExtention.replace(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION, ""));
            }
            appSettings.setNewFileDialogLastUsedEncryption(isChecked);
        });

        utf8BomCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appSettings.setNewFileDialogLastUsedUtf8Bom(isChecked);
        });

        dialogBuilder.setView(root);
        fileNameEdit.requestFocus();

        final MarkorContextUtils cu = new MarkorContextUtils(getContext());
        dialogBuilder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());
        dialogBuilder.setPositiveButton(getString(R.string.create_file), (dialogInterface, i) -> {
            if (ez(fileNameEdit)) {
                return;
            }

            appSettings.setNewFileDialogLastUsedExtension(fileExtEdit.getText().toString().trim());
            final String usedFilename = getFileNameWithoutExtension(fileNameEdit.getText().toString(), templateSpinner.getSelectedItemPosition());
            final File f = new File(basedir, Document.normalizeFilename(usedFilename.trim()) + fileExtEdit.getText().toString().trim());
            final Pair<byte[], Integer> templateContents = getTemplateContent(templateSpinner, basedir, f.getName(), encryptCheckbox.isChecked());
            cu.writeFile(getActivity(), f, false, (arg_ok, arg_fos) -> {
                try {
                    if (appSettings.getNewFileDialogLastUsedUtf8Bom()) {
                        arg_fos.write(0xEF);
                        arg_fos.write(0xBB);
                        arg_fos.write(0xBF);
                    }
                    if (templateContents.first != null && (!f.exists() || f.length() < GsContextUtils.TEXTFILE_OVERWRITE_MIN_TEXT_LENGTH)) {
                        arg_fos.write(templateContents.first);
                    }
                } catch (Exception ignored) {
                }
                if (templateContents.second >= 0) {
                    appSettings.setLastEditPosition(f.getAbsolutePath(), templateContents.second);
                }
                callback(arg_ok || f.exists(), f);
                dialogInterface.dismiss();
            });
        });

        dialogBuilder.setNeutralButton(R.string.create_folder, (dialogInterface, i) -> {
            if (ez(fileNameEdit)) {
                return;
            }
            final String usedFoldername = getFileNameWithoutExtension(fileNameEdit.getText().toString().trim(), templateSpinner.getSelectedItemPosition());
            File f = new File(basedir, usedFoldername);
            if (cu.isUnderStorageAccessFolder(getContext(), f, true)) {
                DocumentFile dof = cu.getDocumentFile(getContext(), f, true);
                callback(dof != null && dof.exists(), f);
            } else {
                callback(f.mkdirs() || f.exists(), f);
            }
            dialogInterface.dismiss();
        });

        if (!allowCreateDir) {
            dialogBuilder.setNeutralButton("", null);
        }

        return dialogBuilder;
    }

    private void loadTemplatesIntoSpinner(final AppSettings appSettings, final Spinner templateSpinner) {
        List<String> templates = new ArrayList<>();
        for (int i = 0; i < templateSpinner.getCount(); i++) {
            templates.add((String) templateSpinner.getAdapter().getItem(i));
        }
        templates.addAll(MarkorDialogFactory.getSnippets(appSettings).keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, templates.toArray(new String[0]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        templateSpinner.setAdapter(adapter);
    }

    private boolean ez(EditText et) {
        return et.getText().toString().trim().isEmpty();
    }

    private String getFileNameWithoutExtension(String typedFilename, int selectedTemplatePos) {
        if (selectedTemplatePos == 7) {
            // Wikitext files always use underscores instead of spaces
            return typedFilename.trim().replace(' ', '_');
        }
        return typedFilename.trim();
    }

    private void callback(boolean ok, File file) {
        try {
            callback.callback(ok, file);
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("TrulyRandom")
    private Pair<byte[], Integer> getTemplateContent(final Spinner templateSpinner, final File basedir, final String filename, final boolean encrypt) {
        String text = null;
        try {
            switch (templateSpinner.getSelectedItemPosition()) {
                case 1:
                    // markor-markdown-reference.md
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/markor-markdown-reference.md"));
                    break;
                case 2:
                    // todo.sample.txt
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/todo.sample.txt"));
                    break;
                case 3:
                    // 2029-01-01-jekyll-post.md
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/2029-01-01-jekyll-post.md"));
                    break;
                case 4:
                    // cooking-recipe.md
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/cooking-recipe.md"));
                    break;
                case 5:
                    // presentation-beamer.md
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/presentation-beamer.md"));
                    break;
                case 6:
                    // zim-wiki-reference.zim.txt
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/zim-wiki-reference.zim.txt"));
                    break;
                case 7:
                    // ^ zim_wiki_empty.txt
                    text = WikitextActionButtons.createWikitextHeaderAndTitleContents(filename.replaceAll("(\\.((zim)|(txt)))*$", "").trim().replace(' ', '_'), new Date(), getResources().getString(R.string.created));
                    break;
                case 8:
                    // hugo-post-front-matter.md
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/hugo-post-front-matter.md"));
                    text = TextViewUtils.interpolateEscapedDateTime(text);
                    if (basedir != null && new File(basedir.getParentFile(), ".notabledir").exists()) {
                        text = text.replace("created:", "modified:");
                    }
                    break;
                case 9:
                    // 202901012359-zettelkasten.md
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/202901012359-zettelkasten.md"));
                    break;
                case 10:
                    // 2029-01-01-jekyll-post.adoc
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/2029-01-01-jekyll-post.adoc"));
                    break;
                case 11:
                    // sample.csv
                    text = GsFileUtils.readCloseTextStream(this.getResources().getAssets().open("samples/sample.csv"));
                    break;
                default:
                    final Map<String, File> snippets = MarkorDialogFactory.getSnippets(ApplicationObject.settings());
                    if (templateSpinner.getSelectedItem() instanceof String && snippets.containsKey((String) templateSpinner.getSelectedItem())) {
                        text = TextViewUtils.interpolateEscapedDateTime(GsFileUtils.readTextFileFast(snippets.get((String) templateSpinner.getSelectedItem())).first);
                        break;
                    }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (text == null) {
            return new Pair<>(null, -1);
        }

        final int startingIndex = text.indexOf(HighlightingEditor.PLACE_CURSOR_HERE_TOKEN);
        text = text.replace(HighlightingEditor.PLACE_CURSOR_HERE_TOKEN, "");

        final byte[] bytes;
        if (encrypt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final char[] pass = ApplicationObject.settings().getDefaultPassword();
            bytes = new JavaPasswordbasedCryption(Build.VERSION.SDK_INT, new SecureRandom()).encrypt(text, pass);
        } else {
            bytes = text.getBytes();
        }

        return Pair.create(bytes, startingIndex);
    }
}

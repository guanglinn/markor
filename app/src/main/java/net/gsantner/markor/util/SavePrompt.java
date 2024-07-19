package net.gsantner.markor.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.activity.DocumentEditAndViewFragment;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.frontend.base.GsFragmentBase;

public class SavePrompt {
    public static class DocumentSavingState {
        public Document document;
        public HighlightingEditor editor;
        public boolean savingManual = false; // If manual-save
        public boolean savingIntent = true; // Intent to save the document

        public DocumentSavingState(Document document, HighlightingEditor editor) {
            this.document = document;
            this.editor = editor;
        }

        public boolean isContentSame() {
            return document.isContentSame(editor.getText());
        }
    }

    public static void showDialogWithTextView(final Activity context, @StringRes int resTitleId, String text, String positiveButtonText, String negativeButtonText, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        ScrollView scroll = new ScrollView(context);
        AppCompatTextView textView = new AppCompatTextView(context);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());

        scroll.setPadding(padding, padding, padding, padding);
        scroll.addView(textView);
        textView.setMovementMethod(new LinkMovementMethod());
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(scroll);
        builder.setPositiveButton(positiveButtonText, positiveListener);
        builder.setNegativeButton(negativeButtonText, negativeListener);
        if (resTitleId != 0) {
            builder.setTitle(resTitleId);
        }

        try {
            Window window;
            if ((window = builder.show().getWindow()) != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        } catch (Exception ignored) {
        }
    }

    // For DocumentActivity Show Save Prompt
    public static void firstActionOnBackPressed(GsFragmentBase currentFragment, DocumentActivity documentActivity) {
        if (currentFragment instanceof DocumentEditAndViewFragment) {
            DocumentEditAndViewFragment editFragment = ((DocumentEditAndViewFragment) currentFragment);
            // If the text was modified, show an alert dialog to
            // ask user's intent to save the modifications
            if (!editFragment.documentSavingState.isContentSame()) {
                String content = documentActivity.getResources().getString(R.string.save_prompt);
                String positive = documentActivity.getResources().getString(R.string.save);
                String negative = documentActivity.getResources().getString(R.string.cancel);
                showDialogWithTextView(documentActivity, R.string.save, content, positive, negative, (dialog, which) -> {
                    editFragment.documentSavingState.savingManual = true;
                    editFragment.documentSavingState.savingIntent = true;
                    documentActivity.onBackPressed();
                }, (dialog, which) -> {
                    editFragment.documentSavingState.savingManual = true;
                    editFragment.documentSavingState.savingIntent = false;
                    documentActivity.onBackPressed();
                });
            } else {
                documentActivity.onBackPressed();
            }
        }
    }

    // For DocumentEditAndViewFragment
    public static void firstActionOnPause(DocumentEditAndViewFragment documentEditAndViewFragment, DocumentSavingState documentSavingState) {
        if (documentSavingState.savingManual) {
            documentSavingState.savingManual = false;
            documentEditAndViewFragment.saveDocument(true); // manual-save
        } else {
            documentEditAndViewFragment.saveDocument(false);
        }
    }

    // For DocumentEditAndViewFragment
    public static boolean getSavingIntent(DocumentSavingState documentSavingState) {
        boolean intent = documentSavingState.savingIntent;
        if (!documentSavingState.savingIntent) {
            documentSavingState.savingIntent = true; // Reset to default value
        }
        return intent;
    }

    // For DocumentEditAndViewFragment
    public static Boolean firstActionIfContentDifferent(boolean forceSaveEmpty, boolean saveIntent, DocumentEditAndViewFragment fragment, Document document, CharSequence text, MarkorContextUtils utils) {
        // Manual-save
        if (forceSaveEmpty) {
            if (saveIntent) {
                if (document.saveContent(fragment.getActivity(), text, utils, true)) {
                    fragment.checkTextChangeState();
                    return true;
                } else {
                    fragment.errorClipText();
                    return false; // Failure only if saveContent somehow fails
                }
            } else {
                return true;
            }
        }
        return null;
    }
}

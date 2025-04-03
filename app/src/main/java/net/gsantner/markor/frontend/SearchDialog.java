package net.gsantner.markor.frontend;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.gsantner.markor.R;


public class SearchDialog extends Fragment {

    private final static String TAG = SearchDialog.class.getName();
    private final static int CHECKED_COLOR = 0xFFA6E7FF;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final int displayWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        view.setVisibility(View.INVISIBLE);
        if (displayWidth < 1080) {
            view.getLayoutParams().width = displayWidth - 20;
        } else {
            view.getLayoutParams().width = 1020;
        }

        view.bringToFront();
        view.post(() -> {
            view.setX((displayWidth - view.getWidth()) / 2f);
            view.setY(0);
            setReplaceLayoutVisibility(view, false);
            requestEditTextFocus(view);

            view.setVisibility(View.VISIBLE);
        });

        EditText searchEditText = view.findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        view.findViewById(R.id.closeImageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide(getActivity());
            }
        });

        final View fragmentView = view;
        View replaceLinearLayout = fragmentView.findViewById(R.id.replaceLinearLayout);
        replaceLinearLayout.setVisibility(View.GONE);
        view.findViewById(R.id.toggleImageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (replaceLinearLayout == null) {
                    return;
                }

                toggleFindReplaceLayout(fragmentView);
            }
        });

        view.findViewById(R.id.findInSelectionImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
            }
        });

        view.findViewById(R.id.matchCaseImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
            }
        });

        view.findViewById(R.id.matchWholeWordImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
            }
        });

        view.findViewById(R.id.useRegexImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
            }
        });

        view.findViewById(R.id.useRegexImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
            }
        });

        view.findViewById(R.id.preserveCaseImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
            }
        });
    }

    private boolean toggleViewCheckedState(View view, boolean checked) {
        if (checked) {
            view.getBackground().clearColorFilter();
        } else {
            view.getBackground().setColorFilter(CHECKED_COLOR, PorterDuff.Mode.DARKEN);
        }
        return !checked;
    }

    private void setReplaceLayoutVisibility(View parent, boolean visible) {
        View replaceLinearLayout = parent.findViewById(R.id.replaceLinearLayout);
        ImageButton imageButton = parent.findViewById(R.id.toggleImageButton);
        if (visible) {
            replaceLinearLayout.setVisibility(View.VISIBLE);
            imageButton.setImageResource(R.drawable.ic_chevron_down);
        } else {
            replaceLinearLayout.setVisibility(View.GONE);
            imageButton.setImageResource(R.drawable.ic_chevron_right);
        }
    }

    private void toggleFindReplaceLayout(View parent) {
        if (parent.findViewById(R.id.replaceLinearLayout).getVisibility() == View.VISIBLE) {
            setReplaceLayoutVisibility(parent, false);
        } else {
            setReplaceLayoutVisibility(parent, true);
        }
    }

    private static void requestEditTextFocus(View parent) {
        View view = parent.findViewById(R.id.searchEditText);
        if (view instanceof EditText) {
            view.requestFocus();
            ((EditText) view).selectAll();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return getLayoutInflater().inflate(R.layout.search_dialog_fragment, container, false);
    }

    public static FragmentTransaction newTransaction(FragmentActivity activity) {
        return activity.getSupportFragmentManager().beginTransaction();
    }

    public static void show(FragmentActivity activity, final EditText text) {
        SearchDialog fragment = (SearchDialog) activity.getSupportFragmentManager().findFragmentByTag(TAG);

        if (fragment == null) {
            try {
                fragment = SearchDialog.class.newInstance();
                newTransaction(activity).add(R.id.root, fragment, TAG).commit();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (java.lang.InstantiationException e) {
                throw new RuntimeException(e);
            }
        } else if (!fragment.isVisible()) {
            newTransaction(activity).show(fragment).commit();
            requestEditTextFocus(fragment.getView());
        }
    }

    public static void hide(FragmentActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (fragment != null) {
            newTransaction(activity).hide(fragment).commit();
        }
    }

    public static void close(FragmentActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (fragment != null) {
            newTransaction(activity).remove(fragment).commit();
        }
    }
}

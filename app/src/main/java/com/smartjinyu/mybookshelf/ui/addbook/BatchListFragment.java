package com.smartjinyu.mybookshelf.ui.addbook;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.smartjinyu.mybookshelf.R;
import com.smartjinyu.mybookshelf.adapter.BatchAddedAdapter;
import com.smartjinyu.mybookshelf.callback.BookFetchedCallback;
import com.smartjinyu.mybookshelf.model.BookLab;
import com.smartjinyu.mybookshelf.model.BookShelfLab;
import com.smartjinyu.mybookshelf.model.LabelLab;
import com.smartjinyu.mybookshelf.model.bean.Book;
import com.smartjinyu.mybookshelf.model.bean.BookShelf;
import com.smartjinyu.mybookshelf.model.bean.Label;
import com.smartjinyu.mybookshelf.support.CoverDownloader;
import com.smartjinyu.mybookshelf.util.AnswersUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * fragment to show list of books added
 * Created by smartjinyu on 2017/2/8.
 */

public class BatchListFragment extends Fragment implements BatchAddedAdapter.RecyclerViewOnItemLongClickListener {
    private static final String TAG = "BatchListFragment";

    private LinearLayout mNoBooksLL;
    private TextView mNoBooksText;
    private RecyclerView mRecyclerView;
    private BatchAddedAdapter mRecyclerViewAdapter;
    private BatchAddActivity mContext;
    private List<Book> mBooks;// books added
    private BookFetchedCallback mCallback = new BookFetchedCallback() {
        @Override
        public void onBookFetched(Book book) {
            boolean isAdded = containsInBooks(book);

            if (isAdded) {
                Snackbar.make(mContext.findViewById(R.id.batch_add_view_pager),
                        String.format(getString(R.string.batch_add_book_existed_in_added_list),
                                book.getTitle()), Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(mContext.findViewById(R.id.batch_add_view_pager),
                        String.format(getString(R.string.batch_add_added_snack_bar),
                                book.getTitle()), Snackbar.LENGTH_SHORT).show();
                if (book.getImgUrl() != null) {
                    CoverDownloader coverDownloader = new CoverDownloader(mContext, book, 1);
                    String path = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + book.getCoverPhotoFileName();
                    coverDownloader.downloadAndSaveImg(book.getImgUrl(), path);
                } else book.setHasCover(false);
                mBooks.add(book);
                ((BatchAddActivity) (mContext)).notifyTabTitle(false);
                refreshRecyclerView();
            }
        }
    };

    private boolean containsInBooks(Book book) {
        for (Book b : mBooks)
            if (b.getIsbn().equals(book.getIsbn())) return true;
        return false;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = (BatchAddActivity) getActivity();
        mBooks = new ArrayList<>();
        mRecyclerViewAdapter = new BatchAddedAdapter(mBooks, mContext);
        mRecyclerViewAdapter.setOnItemLongClickListener(this);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_batch_list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.bachlist_recycler_view);
        mNoBooksLL = (LinearLayout) view.findViewById(R.id.no_books);
        mNoBooksText = (TextView) view.findViewById(R.id.no_books_text);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return view;
    }
//
//    @Override
//    public void setUserVisibleHint(boolean isVisibleToUser) {
//        super.setUserVisibleHint(isVisibleToUser);
//        if (isVisibleToUser) {
//            updateUI();
//        }
//    }
//
//    public void updateUI() {
//        if (mRecyclerViewAdapter == null) {
//            mRecyclerViewAdapter = new BatchAddedAdapter(mBooks, mContext);
//            mRecyclerView.setAdapter(mRecyclerViewAdapter);
//        } else {
//            mRecyclerViewAdapter.notifyDataSetChanged();
//        }
//    }

    public void chooseBookshelf() {
        final BookShelfLab bookShelfLab = BookShelfLab.get(mContext);
        final List<BookShelf> bookShelves = bookShelfLab.getBookShelves();
        new MaterialDialog.Builder(mContext)
                .title(R.string.move_to_dialog_title)
                .items(bookShelves).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                List<BookShelf> bookShelves = bookShelfLab.getBookShelves();
                for (BookShelf bookShelf : bookShelves) {
                    if (bookShelf.getTitle().equals(text)) {
                        // selected bookshelf
                        for (Book book : mBooks) book.setBookshelfID(bookShelf.getId());
                        break;
                    }
                }
                dialog.dismiss();
                addLabel();
                // add label
            }
        }).neutralText(R.string.move_to_dialog_neutral).onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull final MaterialDialog listdialog, @NonNull DialogAction which) {
                // create new bookshelf
                new MaterialDialog.Builder(mContext)
                        .title(R.string.custom_book_shelf_dialog_title)
                        .inputRange(1, getResources().getInteger(R.integer.bookshelf_name_max_length)).input(R.string.custom_book_shelf_dialog_edit_text, 0, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        // nothing to do here
                    }
                }).onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        EditText etBookShelf = dialog.getInputEditText();
                        if (etBookShelf == null) return;
                        BookShelf bookShelfToAdd = new BookShelf();
                        bookShelfToAdd.setTitle(etBookShelf.getText().toString());
                        bookShelfLab.addBookShelf(bookShelfToAdd);
                        Log.i(TAG, "New bookshelf created " + bookShelfToAdd.getTitle());
                        List<CharSequence> itemList = listdialog.getItems();
                        if (itemList == null) return;
                        itemList.add(bookShelfToAdd.getTitle());
                        listdialog.notifyItemInserted(itemList.size() - 1);
                    }
                }).negativeText(android.R.string.cancel).onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }).autoDismiss(false).show();
        // if autoDismiss = false, the list dialog will dismiss when a new bookshelf is added
    }

    private void addLabel() {
        final LabelLab labelLab = LabelLab.get(mContext);
        final List<Label> labels = labelLab.getLabels();
        new MaterialDialog.Builder(mContext)
                .title(R.string.add_label_dialog_title)
                .items(labels).itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                List<Label> labels = labelLab.getLabels();
                // must refresh labels here because if user add label, the list won't update,
                // and select the newly add label won't take effect
                for (int i = 0; i < which.length; i++) {
                    for (Label label : labels) {
                        if (label.getTitle().equals(text[i])) {
                            // selected label
                            for (Book book : mBooks) book.addLabel(label);
                            break;
                        }
                    }
                }
                dialog.dismiss();
                BookLab.get(mContext).addBooks(mBooks);
                AnswersUtil.logContentView(TAG, "ADD", "1202", "ADD Succeeded", mBooks.size() + "");
                mContext.finish();
                return true;

            }
        }).neutralText(R.string.label_choice_dialog_neutral).onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull final MaterialDialog listDialog, @NonNull DialogAction which) {
                // create new label
                new MaterialDialog.Builder(mContext)
                        .title(R.string.label_add_new_dialog_title)
                        .inputRange(1, getResources().getInteger(R.integer.label_name_max_length)).input(R.string.label_add_new_dialog_edit_text, 0, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog1, CharSequence input) {
                        // nothing to do here
                    }
                }).positiveText(android.R.string.ok).onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog inputDialog, @NonNull DialogAction which) {
                        EditText etLabel = inputDialog.getInputEditText();
                        if (etLabel == null) return;
                        Label labelToAdd = new Label();
                        labelToAdd.setTitle(etLabel.getText().toString());
                        labelLab.addLabel(labelToAdd);
                        Log.i(TAG, "New label created " + labelToAdd.getTitle());
                        ArrayList<CharSequence> itemList = listDialog.getItems();
                        if (itemList == null) return;
                        itemList.add(labelToAdd.getTitle());
                        listDialog.notifyItemInserted(listDialog.getItems().size() - 1);
                    }
                }).negativeText(android.R.string.cancel).onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog inputDialog, @NonNull DialogAction which) {
                        inputDialog.dismiss();
                    }
                }).show();
            }
        }).autoDismiss(false).show();
    }

    public BookFetchedCallback getCallback() {
        return mCallback;
    }

    @Override
    public boolean onItemLongClick(final int position) {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.batch_add_delete_book_dialog_title).content(R.string.batch_add_delete_book_dialog_content)
                .positiveText(R.string.batch_add_delete_book_dialog_positive).onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                if (mBooks.get(position).isHasCover()) {
                    File file = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            + "/" + mBooks.get(position).getCoverPhotoFileName());
                    boolean succeeded = file.delete();
                    Log.i(TAG, "Remove cover result = " + succeeded);
                }
                mBooks.remove(position);
                refreshRecyclerView();
                mContext.notifyTabTitle(true);
            }
        }).negativeText(android.R.string.cancel).onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dialog.dismiss();
            }
        }).show();
        return false;
    }

    private void refreshRecyclerView() {
        mRecyclerViewAdapter.notifyDataSetChanged();
        if (mBooks == null || mBooks.size() <= 0) {
            mNoBooksText.setText(R.string.batch_add_no_books);
            mNoBooksLL.setVisibility(View.VISIBLE);
        } else {
            mNoBooksLL.setVisibility(View.GONE);
        }
    }
}

/*
* Copyright (C) 2012,2013 Renard Wellnitz.
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/
package com.agentwaj.speedread;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import android.app.AlertDialog;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.OCR;
import com.renard.documentview.DocumentActivity;
import com.renard.ocr.DocumentContentProvider.Columns;
import com.renard.ocr.LayoutQuestionDialog.LayoutChoseListener;
import com.renard.ocr.LayoutQuestionDialog.LayoutKind;
import com.renard.ocr.SdWarningDialogFragment.SdReadyListener;
import com.renard.ocr.cropimage.MonitoredActivity;
import com.renard.util.Util;

/**
* this activity is shown during the ocr process
*
* @author renard
*/
public class OCRActivity extends MonitoredActivity implements SdReadyListener {

    private static final String TAG = OCRActivity.class.getSimpleName();

    public static final String EXTRA_PARENT_DOCUMENT_ID = "parent_id";
    private static final String OCR_LANGUAGE = "ocr_language";

    private Button mButtonStartOCR;
    private OCRImageView mImageView;
    private int mOriginalHeight = 0;
    private int mOriginalWidth = 0;
    private Pix mFinalPix;
    private String mOcrLanguage; // is set by dialog in
    private int mAccuracy;

    // askUserAboutDocumentLayout
    private OCR mOCR;
    private Messenger mMessageReceiver = new Messenger(new ProgressActivityHandler()); // receives
    // messages
    // from
    // background
    // task
    private int mParentId = -1; // if >=0 its the id of the parentdocument to
    // which the current page shall be added

    /**
* receives progress status messages from the background ocr task and
* displays them in the current activity
*
* @author renard
*/
    private class ProgressActivityHandler extends Handler {

        private String hocrString;
        private String utf8String;
        private int layoutPix;
        private int mPreviewWith;
        private int mPreviewHeight;

        public void handleMessage(Message msg) {
            switch (msg.what) {

                case OCR.MESSAGE_EXPLANATION_TEXT: {
                    getSupportActionBar().setTitle(msg.arg1);
                    break;
                }
                case OCR.MESSAGE_TESSERACT_PROGRESS: {
                    int percent = msg.arg1;
                    Bundle data = msg.getData();
                    mImageView.setProgress(percent, (RectF) data.getParcelable(OCR.EXTRA_WORD_BOX), (RectF) data.getParcelable(OCR.EXTRA_OCR_BOX));
                    break;
                }
                case OCR.MESSAGE_PREVIEW_IMAGE: {
                    int nativePix = msg.arg1;

                    if (nativePix != 0) {
                        Pix pix = new Pix(nativePix);
                        final Bitmap preview = WriteFile.writeBitmap(pix);
                        pix.recycle();

                        mImageView.setImageBitmapResetBase(preview, true, 0);
                    }
                    // mTextView.setText(msg.arg2);
                    break;
                }
                case OCR.MESSAGE_FINAL_IMAGE: {
                    int nativePix = msg.arg1;

                    if (nativePix != 0) {
                        mFinalPix = new Pix(nativePix);
                    }
                    break;
                }
                case OCR.MESSAGE_LAYOUT_PIX: {
                    layoutPix = msg.arg1;
                    if (layoutPix != 0) {
                        Pix pix = new Pix(layoutPix);
                        final Bitmap preview = WriteFile.writeBitmap(pix);
                        mPreviewHeight = pix.getHeight();
                        mPreviewWith = pix.getWidth();
                        mImageView.setImageBitmapResetBase(preview, true, 0);
                        pix.recycle();
                    }
                    break;
                }

                case OCR.MESSAGE_LAYOUT_ELEMENTS: {
                    int nativePixaText = msg.arg1;
                    int nativePixaImages = msg.arg2;
                    final Pixa texts = new Pixa(nativePixaText, 0, 0);
                    final Pixa images = new Pixa(nativePixaImages, 0, 0);
                    ArrayList<Rect> boxes = images.getBoxRects();
                    ArrayList<RectF> scaledBoxes = new ArrayList<RectF>(boxes.size());
                    float xScale = (1.0f * mPreviewWith) / mOriginalWidth;
                    float yScale = (1.0f * mPreviewHeight) / mOriginalHeight;
                    // scale the to the preview image space
                    for (Rect r : boxes) {
                        scaledBoxes.add(new RectF(r.left * xScale, r.top * yScale, r.right * xScale, r.bottom * yScale));
                    }
                    mImageView.setImageRects(scaledBoxes);
                    boxes = texts.getBoxRects();
                    scaledBoxes = new ArrayList<RectF>(boxes.size());
                    for (Rect r : boxes) {
                        scaledBoxes.add(new RectF(r.left * xScale, r.top * yScale, r.right * xScale, r.bottom * yScale));
                    }
                    mImageView.setTextRects(scaledBoxes);

                    mButtonStartOCR.setVisibility(View.VISIBLE);
                    mButtonStartOCR.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            int[] selectedTexts = mImageView.getSelectedTextIndexes();
                            int[] selectedImages = mImageView.getSelectedImageIndexes();
                            if (selectedTexts.length > 0 || selectedImages.length > 0) {
                                mImageView.clearAllProgressInfo();

                                mOCR.startOCRForComplexLayout(OCRActivity.this, determineOcrLanguage(mOcrLanguage), texts, images, selectedTexts, selectedImages);
                                mButtonStartOCR.setVisibility(View.GONE);
                            } else {
                                Toast.makeText(getApplicationContext(), R.string.please_tap_on_column, Toast.LENGTH_LONG).show();
                            }

                        }
                    });

                    // CharSequence text =
                    // getText(R.string.progress_choose_columns);
                    // mFairyText.setText(text);
                    getSupportActionBar().setTitle(R.string.progress_choose_columns);

                    break;
                }
                case OCR.MESSAGE_HOCR_TEXT: {
                    this.hocrString = (String) msg.obj;
                    mAccuracy = msg.arg1;
                    break;
                }
                case OCR.MESSAGE_UTF8_TEXT: {
                    this.utf8String = (String) msg.obj;
                    break;
                }
                case OCR.MESSAGE_END: {
                    saveDocument(mFinalPix, hocrString, utf8String, true,mAccuracy);
                    break;
                }
                case OCR.MESSAGE_ERROR: {
                    Toast.makeText(getApplicationContext(), getText(msg.arg1), Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

    private void saveDocument(final Pix pix, final String hocrString, final String utf8String, final boolean checkSd, final int accuracy) {
// if (checkSd && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
// waitForSdCard(pix, hocrString, utf8String);
// } else {

        Util.startBackgroundJob(OCRActivity.this, "", getText(R.string.saving_document).toString(), new Runnable() {

            @Override
            public void run() {
                File imageFile = null;
                Uri documentUri = null;

                try {
                    imageFile = saveImage(pix);
                } catch (IOException ignore) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getText(R.string.error_create_file), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                try {

                    documentUri = saveDocumentToDB(imageFile, hocrString, utf8String);
                    Util.createThumbnail(OCRActivity.this, imageFile, Integer.valueOf(documentUri.getLastPathSegment()));
                } catch (RemoteException e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getText(R.string.error_create_file), Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    if (pix != null) {
                        pix.recycle();
                    }
                    if (documentUri != null) {
                        Intent i = new Intent(OCRActivity.this, DocumentActivity.class);
                        i.putExtra(DocumentActivity.EXTRA_ACCURACY, accuracy);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.setData(documentUri);
                        startActivity(i);
                        finish();
                    }
                }
            }
        }, new Handler());
        // }

    }

// private void sendBugReport(Exception e) {
// ByteArrayOutputStream baos = new ByteArrayOutputStream();
// PrintStream ps = new PrintStream(baos);
//
//
// try {
// e.printStackTrace(ps);
// String content = baos.toString("UTF-8"); // e.g. ISO-8859-1
// Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
// sharingIntent.setType("text/plain");
// sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, R.string.share_subject);
// sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, content);
// sharingIntent.putExtra(Intent.EXTRA_EMAIL, "renard.wellnitz@googlemail.com");
// startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_chooser_title)));
// } catch (UnsupportedEncodingException e1) {
// e1.printStackTrace();
// }
// }

    @Override
    public void onSdReady() {
        //saveDocument(pix, hocrString, utf8String, true);
    }

    @Override
    public void onSdNotReady() {
        //saveDocument(pix, hocrString, utf8String, false);
    }


    private void waitForSdCard(final Pix pix, final String hocrString, final String utf8String) {

        SdWarningDialogFragment warningDialog = new SdWarningDialogFragment();
        FragmentManager fm = getSupportFragmentManager();
        warningDialog.show(fm, OCRActivity.class.getSimpleName());
    }

    private File saveImage(Pix p) throws IOException {
        CharSequence id = DateFormat.format("ssmmhhddMMyy", new Date(System.currentTimeMillis()));
        return Util.savePixToSD(p, id.toString());
    }

    private Uri saveDocumentToDB(File imageFile, String hocr, String plainText) throws RemoteException {
        ContentProviderClient client = null;
        try {
            ContentValues v = new ContentValues();
            if (imageFile != null) {
                v.put(com.renard.ocr.DocumentContentProvider.Columns.PHOTO_PATH, imageFile.getPath());
            }
            if (hocr != null) {
                v.put(Columns.HOCR_TEXT, hocr);
            }
            if (plainText != null) {
                v.put(Columns.OCR_TEXT, plainText);
            }
            v.put(Columns.OCR_LANG,mOcrLanguage);

            if (mParentId > -1) {
                v.put(Columns.PARENT_ID, mParentId);
            }
            client = getContentResolver().acquireContentProviderClient(DocumentContentProvider.CONTENT_URI);
            return client.insert(DocumentContentProvider.CONTENT_URI, v);
        } finally {
            if (client != null) {
                client.release();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mOCR = new OCR(this, mMessageReceiver);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.ocr_visualize);
        mImageView = (OCRImageView) findViewById(R.id.progress_image);

        mParentId = getIntent().getIntExtra(EXTRA_PARENT_DOCUMENT_ID, -1);
        int nativePix = getIntent().getExtras().getInt(DocumentGridActivity.EXTRA_NATIVE_PIX);

        Pix pixOrg = new Pix(nativePix);
        mOriginalHeight = pixOrg.getHeight();
        mOriginalWidth = pixOrg.getWidth();

        askUserAboutDocumentLayout(pixOrg);

        mButtonStartOCR = (Button) findViewById(R.id.column_pick_completed);
        initAppIcon(this, -1);

    }

    private void askUserAboutDocumentLayout(final Pix pixOrg) {
        AlertDialog alertDialog = LayoutQuestionDialog.createDialog(this, new LayoutChoseListener() {

            @Override
            public void onLayoutChosen(final LayoutKind layoutKind, final String ocrLanguage) {
                if (layoutKind == LayoutKind.DO_NOTHING) {
                    saveDocument(pixOrg, null, null, true,0);
                } else {
                    mOcrLanguage = ocrLanguage;

                    getSupportActionBar().show();
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                    // mFairyText.setText(R.string.progress_start);
                    getSupportActionBar().setTitle(R.string.progress_start);

                    if (layoutKind == LayoutKind.SIMPLE) {
                        mOCR.startOCRForSimpleLayout(OCRActivity.this, determineOcrLanguage(ocrLanguage), pixOrg);
                    } else if (layoutKind == LayoutKind.COMPLEX) {
                        mAccuracy = 0;
                        mOCR.startLayoutAnalysis(pixOrg);
                    }
                }
            }
        });
        alertDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        alertDialog.show();
    }

    private String determineOcrLanguage(String ocrLanguage) {
        final String english = "eng";
        if (!ocrLanguage.equals(english)&& addEnglishData(ocrLanguage)) {
            return ocrLanguage + "+" + english;
        } else {
            return ocrLanguage;
        }

    }
    //when combining languages that have multi byte characters with english training data the ocr text gets corrupted
    //but adding english will improve overall accuracy for the other languages
    private static boolean addEnglishData(String mLanguage) {
        if (mLanguage.startsWith("chi")||
                mLanguage.equalsIgnoreCase("tha")||
                mLanguage.equalsIgnoreCase("kor")||
                mLanguage.equalsIgnoreCase("hin")||
                mLanguage.equalsIgnoreCase("heb")||
                mLanguage.equalsIgnoreCase("ell")||
                mLanguage.equalsIgnoreCase("bel")||
                mLanguage.equalsIgnoreCase("ara")||
                mLanguage.equalsIgnoreCase("grc")||
                mLanguage.equalsIgnoreCase("rus")||
                mLanguage.equalsIgnoreCase("vie")){
            return false;

        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mOcrLanguage != null) {
            outState.putString(OCR_LANGUAGE, mOcrLanguage);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mOcrLanguage == null) {
            mOcrLanguage = savedInstanceState.getString(OCR_LANGUAGE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mFinalPix != null) {
            mFinalPix.recycle();
            mFinalPix = null;
        }

        BitmapDrawable bd = (BitmapDrawable) mImageView.getDrawable();
        if (bd != null) {
            bd.getBitmap().recycle();
        }
        super.onDestroy();
    }
}
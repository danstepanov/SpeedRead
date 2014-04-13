package com.agentwaj.speedread;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.CameraManager;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.GestureDetector.BaseListener;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

public class SpeedRead extends Activity {
	
	// Shared prefs for local storage of saved stories
	SharedPreferences mPrefs;
	
	// Holds the different cards of this immersion
	private List<Card> mCards;
	
	// Allows for horizontal scrolling between cards
	private CardScrollView mCardScrollView;
	
	private CustomCardScrollAdapter mAdapter;
	
	// Handles gesture input
	private GestureDetector mGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Retrieve all saved stories
		mPrefs = getSharedPreferences("mPrefs", MODE_PRIVATE);
		List<String> savedStories = new ArrayList<String>();
		for (Map.Entry<String, ?> entry : mPrefs.getAll().entrySet()) {
			savedStories.add(entry.getKey());
		}
		
		// Create cards for this immersion
		createCards(savedStories);
		
		// Link the cards to the scroll view then display the scroll view
		mCardScrollView = new CardScrollView(this);
		mAdapter = new CustomCardScrollAdapter();
		mCardScrollView.setAdapter(mAdapter);
		mCardScrollView.activate();
		// Capture is initial screen, settings to left and stories to right
		mCardScrollView.setSelection(1);
		setContentView(mCardScrollView);

		// Creates the gesture detector to listen for gesture events
		mGestureDetector = createGestureDetector(this);
	}
	
	/*
	 * Creates the array of cards to be used for this immersion
	 */
	private void createCards(List<String> savedStories) {
		mCards = new ArrayList<Card>();
		
		Card card = new Card(this);
		card.setText("Settings");
		mCards.add(card);
		
		card = new Card(this);
		card.setText("Capture");
		mCards.add(card);
		
		for (String story : savedStories) {
			card = new Card(this);
			card.setText(story);
			card.setFootnote("Story");
			mCards.add(card);
		}
	}

	/*
	 * The gesture detector that listens for gesture events
	 */
	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		// Create a base listener for generic gestures
		gestureDetector.setBaseListener(new BaseListener() {

			@Override
			public boolean onGesture(Gesture gesture) {
				int position = mCardScrollView.getSelectedItemPosition();
				switch (position) {
				case 0: // Settings
					Toast.makeText(SpeedRead.this, "Coming soon!", Toast.LENGTH_SHORT).show();
					break;
				case 1: // Capture
					Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					startActivityForResult(i, 1);
					break;
				default: // Stories
					// Remove a card from the immersion
					mPrefs.edit().remove(mCards.get(position).getText()).commit();
					mCards.remove(mCards.get(position));
					mAdapter.notifyDataSetChanged();
				}
				return true;
			}
		});
		
		return gestureDetector;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Process a picture once it is ready
		if (requestCode == 1 && resultCode == RESULT_OK && mPrefs != null) {
			String picturePath = data.getStringExtra(CameraManager.EXTRA_PICTURE_FILE_PATH);
			processPictureWhenReady(picturePath);
			mAdapter.notifyDataSetChanged();
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/*
	 * Process a picture once it is ready
	 */
	private void processPictureWhenReady(final String picturePath) {
		final File pictureFile = new File(picturePath);
		
		if (pictureFile.exists()) {
			// The picture is ready; process it.
			
			// Save a card
			Calendar c = Calendar.getInstance();
			String time = c.get(Calendar.MONTH) + "/" + 
					c.get(Calendar.DATE) + ", " + 
					c.get(Calendar.HOUR_OF_DAY) + ":" + 
					c.get(Calendar.MINUTE);
			mPrefs.edit().putString(time, "Rest of text").commit();
			mCards.add(new Card(this).setText(time).setFootnote("Story"));
			mAdapter.notifyDataSetChanged();
		} else {
			// The file does not exist yet.
			final File parentDirectory = pictureFile.getParentFile();
			FileObserver observer = new FileObserver(parentDirectory.getPath()) {
				
				// Protect against additional pending events after CLOSE_WRITE is handled.
				private boolean isFileWritten;
				
				@Override
				public void onEvent(int event, String path) {
					if (!isFileWritten) {
						// For safety, make sure that the file was created in 
						// the directory is actually the one we're expecting.
						File affectedFile = new File(parentDirectory, path);
						isFileWritten = (event == FileObserver.CLOSE_WRITE 
								&& affectedFile.equals(pictureFile));
						
						if (isFileWritten) {
							stopWatching();
							
							// Now that the file is ready, recursively call 
							// processPictureWhenReady again (on the UI thread).
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									processPictureWhenReady(picturePath);
								}
							});
						}
					}
				}
			};
			observer.startWatching();
		}
	}
	
	/*
	 * Bridge between the cards data set and the scroll view
	 */
	private class CustomCardScrollAdapter extends CardScrollAdapter {

		@Override
		public int findIdPosition(Object id) {
			return -1;
		}

		@Override
		public int findItemPosition(Object item) {
			return mCards.indexOf(item);
		}

		@Override
		public int getCount() {
			return mCards.size();
		}

		@Override
		public Object getItem(int position) {
			return mCards.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return mCards.get(position).toView();
		}
	}

	/*
	 * Sends generic motion events to the gesture detector
	 */
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null)
			return mGestureDetector.onMotionEvent(event);
		return false;
	}
}

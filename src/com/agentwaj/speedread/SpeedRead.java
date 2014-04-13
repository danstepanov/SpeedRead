package com.agentwaj.speedread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.GestureDetector.BaseListener;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

public class SpeedRead extends Activity {
	
	//
	SharedPreferences mPrefs;
	
	// Holds the different cards of this immersion
	private List<Card> mCards;
	
	// Allows for horizontal scrolling between cards
	private CardScrollView mCardScrollView;
	
	// Handles gesture input
	private GestureDetector mGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mPrefs = getSharedPreferences("mPrefs", MODE_PRIVATE);
		List<String> savedStories = new ArrayList<String>();
		for (Map.Entry<String, ?> entry : mPrefs.getAll().entrySet()) {
			savedStories.add(entry.getKey());
		}
		
		// Create cards for this immersion
		createCards(savedStories);
		
		// Link the cards to the scroll view then display the scroll view
		mCardScrollView = new CardScrollView(this);
		CustomCardScrollAdapter adapter = new CustomCardScrollAdapter();
		mCardScrollView.setAdapter(adapter);
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
					Toast.makeText(SpeedRead.this, "Story " + (position - 1), Toast.LENGTH_SHORT).show();
				}
				return true;
			}
		});

		return gestureDetector;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && resultCode == RESULT_OK) {
			mPrefs.edit().putString(System.currentTimeMillis() + "", "test").commit();
			Toast.makeText(SpeedRead.this, "SUCCESS BUDDY!", Toast.LENGTH_SHORT).show();
		}
		
		super.onActivityResult(requestCode, resultCode, data);
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

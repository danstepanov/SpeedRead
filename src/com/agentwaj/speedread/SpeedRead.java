package com.agentwaj.speedread;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
	
	// Holds the different cards of this immersion
	private List<Card> mCards;
	
	// Allows for horizontal scrolling between cards
	private CardScrollView mCardScrollView;
	
	// Handles gesture input
	private GestureDetector mGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Create cards for this immersion
		createCards();
		
		// Link the cards to the scroll view then display the scroll view
		mCardScrollView = new CardScrollView(this);
		CustomCardScrollAdapter adapter = new CustomCardScrollAdapter();
		mCardScrollView.setAdapter(adapter);
		mCardScrollView.activate();
		setContentView(mCardScrollView);

		// Creates the gesture detector to listen for gesture events
		mGestureDetector = createGestureDetector(this);
	}
	
	private void createCards() {
		mCards = new ArrayList<Card>();
		
		for (int i = 0; i < 5; i++) {
			Card card = new Card(this);
			card.setText("Num " + (i +  1));
			mCards.add(card);
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
	 * The gesture detector that listens for gesture events
	 */
	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		// Create a base listener for generic gestures
		gestureDetector.setBaseListener(new BaseListener() {

			@Override
			public boolean onGesture(Gesture gesture) {
				Toast.makeText(SpeedRead.this, "Toasty.", Toast.LENGTH_SHORT).show();
				return true;
			}
		});

		return gestureDetector;
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

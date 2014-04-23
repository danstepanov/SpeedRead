package com.agentwaj.speedread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
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
	
	public static final String PREFS = "com.agentwaj.speedread.preferences";
	
	// Shared prefs for local storage of saved stories
	SharedPreferences mPrefs;
	
	// Holds the different cards of this immersion
	private List<Card> mCards;
	
	// Allows for horizontal scrolling between cards
	private CardScrollView mCardScrollView;
	
	// Links cards array to scroll view
	private CustomCardScrollAdapter mAdapter;
	
	// Handles gesture input
	private GestureDetector mGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Retrieve all saved stories
		mPrefs = getSharedPreferences(PREFS, MODE_PRIVATE);
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
		card.setText("ENG -> LIT");
		mCards.add(card);
		
		
		card = new Card(this);
		card.setText("LIT -> ENG");
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
					Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
					i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
					startActivityForResult(i, 1);
					break;
				case 2: // Capture
					i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
					i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "lt");
					startActivityForResult(i, 2);
					break;
				default: // Stories
//					Intent intent = new Intent(SpeedRead.this, ReadStory.class);
//					intent.putExtra("title", mCards.get(position).getText());
//					startActivity(intent);
					
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
		if (resultCode == RESULT_OK && mPrefs != null) {
			ArrayList<String> words = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			String[] y = words.get(0).split(" ");
			String result = "";
			for (String word : y) {
				try {
					String x = requestCode == 1 ? 
							new TranslateTask().execute("eng", "lit", word).get() :
								new TranslateTask().execute("lit", "eng", word).get();
					result += x + " ";
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			mCards.add(new Card(this).setText(result));
			mAdapter.notifyDataSetChanged();
//			mPrefs.edit().putString(result, "").commit();
//			mCards.add(new Card(this).setText(result));
//			mAdapter.notifyDataSetChanged();
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private class TranslateTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet();
				URI website = new URI("http://glosbe.com/gapi/translate?format=json&pretty=true&from=" + params[0] + "&dest=" + params[1] + "&phrase=" + params[2]);
				request.setURI(website);
				HttpResponse response = client.execute(request);
				InputStream in = response.getEntity().getContent();
				
				String result = convertStreamToString(in);
				
				JSONObject obj = new JSONObject(result);
				try {
				JSONObject arr = obj.getJSONArray("tuc").getJSONObject(0).getJSONObject("phrase");
				result = arr.getString("text");
				} catch (Exception e) {
					result = "";
				}
				
				return result;
			} catch (Exception e) {
				Log.i("my-error", "Goof: " + e.getMessage());
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
		}
	}
	
	private static String convertStreamToString(InputStream is) {
	    /*
	     * To convert the InputStream to String we use the BufferedReader.readLine()
	     * method. We iterate until the BufferedReader return null which means
	     * there's no more data to read. Each line will appended to a StringBuilder
	     * and returned as String.
	     */
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();

	    String line = null;
	    try {
	        while ((line = reader.readLine()) != null) {
	            sb.append(line + "\n");
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            is.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    return sb.toString();
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

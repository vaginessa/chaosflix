package de.nicidienase.chaosflix.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v17.leanback.app.PlaybackFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Row;
import android.util.Log;

import de.nicidienase.chaosflix.CardPresenter;
import de.nicidienase.chaosflix.PlaybackHelper;
import de.nicidienase.chaosflix.R;
import de.nicidienase.chaosflix.activities.DetailsActivity;
import de.nicidienase.chaosflix.entities.recording.Event;
import de.nicidienase.chaosflix.entities.recording.Recording;
import de.nicidienase.chaosflix.entities.streaming.Room;
import de.nicidienase.chaosflix.entities.streaming.StreamUrl;

import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS;

/**
 * Created by felix on 26.03.17.
 */

public class OverlayFragment extends PlaybackFragment{

	private static final String TAG = OverlayFragment.class.getSimpleName();

	private Recording mSelectedRecording;
	private Event mSelectedEvent;

	private Room mSelectedRoom;
	private PlaybackHelper mHelper;
	private PlaybackControlListener mCallback;
	private ArrayObjectAdapter mRowsAdapter;
	private MediaSession mSession;
	private boolean mHasAudioFocus;
	private boolean mPauseTransient;
	private AudioManager mAudioManager;
	private MediaController mMediaControler;
	private int eventType;
	private StreamUrl mSelectedStream;

	private MediaController.Callback mMediaControllerCallback;

	private final AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            abandonAudioFocus();
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (mHelper.isMediaPlaying()) {
                                pause();
                                mPauseTransient = true;
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            mCallback.mute(true);
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                            if (mPauseTransient) {
                                play();
                            }
                            mCallback.mute(false);
                            break;
                    }
                }
            };

	public interface PlaybackControlListener {
		void play();
		void pause();
		void playPause();
		void setVideoSource(String source);
		void skipForward(int sec);
		void skipBackward(int sec);
		void seekTo(long sec);
		boolean isMediaPlaying();
		long getCurrentPosition();
		void releasePlayer();
		long getPosition();
		long getBufferedPosition();
		void mute(boolean state);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG,"OnCreate");

		Intent intent = getActivity()
				.getIntent();
		eventType = intent.getIntExtra(DetailsActivity.TYPE, -1);
		if(eventType == DetailsActivity.TYPE_RECORDING){
			mSelectedEvent = intent.getParcelableExtra(DetailsActivity.EVENT);
			mSelectedRecording = intent.getParcelableExtra(DetailsActivity.RECORDING);
			mHelper = new PlaybackHelper(getActivity(),this,mSelectedEvent,mSelectedRecording);
		} else if(eventType == DetailsActivity.TYPE_STREAM){
			mSelectedRoom = intent.getParcelableExtra(DetailsActivity.ROOM);
			mSelectedStream = intent.getParcelableExtra(DetailsActivity.STREAM_URL);
			mHelper = new PlaybackHelper(getActivity(),this,mSelectedRoom,mSelectedStream);
		} else {
			Log.d(TAG,"No Media found, finishing");
			getActivity().finish();
		}
		mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

		setBackgroundType(PlaybackFragment.BG_LIGHT);
		setFadingEnabled(false);
		mHelper.setFadingEnabled(true);
	}

	@Override
	public synchronized void onStart() {
		super.onStart();
		Log.d(TAG,"OnStart");

		PlaybackControlsRowPresenter playbackControlsRowPresenter = mHelper.createControlsRowAndPresenter();
		PlaybackControlsRow controlsRow = mHelper.getControlsRow();
		mMediaControllerCallback = mHelper.createMediaControllerCallback();
		requestAudioFocus();
		mMediaControler = getActivity().getMediaController();
		if(mMediaControler != null){
			mMediaControler.registerCallback(mMediaControllerCallback);
		}
		ClassPresenterSelector ps = new ClassPresenterSelector();
		ps.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
		ps.addClassPresenter(ListRow.class, new ListRowPresenter());
		mRowsAdapter = new ArrayObjectAdapter(ps);
		mRowsAdapter.add(controlsRow);
//		mRowsAdapter.add(getRelatedItems());
		setAdapter(mRowsAdapter);

		if(mCallback != null && eventType == DetailsActivity.TYPE_STREAM){
			mCallback.setVideoSource(mSelectedStream.getUrl());
		} else if(mCallback != null && eventType == DetailsActivity.TYPE_RECORDING){
			mCallback.setVideoSource(mSelectedRecording.getRecordingUrl());
		} else {
			Log.d(TAG,"Callback not set or not event/stream");
		}
		requestAudioFocus();
	}

	private Row getRelatedItems() {
		ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
//		TODO Add related items
		HeaderItem header = new HeaderItem(0, getString(R.string.related_talks));
		return new ListRow(header, listRowAdapter);
	}

	public boolean isMediaPlaying() {
		if(mCallback != null){
			return mCallback.isMediaPlaying();
		}
		return false;
	}

	public int getCurrentPosition() {
		if(mCallback != null){
			return (int) mCallback.getCurrentPosition();
		}
		return 0;
	}

	private long getCurrentPositionLong(){
		if(mCallback != null){
			return mCallback.getCurrentPosition();
		}
		return 0;
	}

	public long getCurrentBufferedPosition(){
		if(mCallback != null){
			return mCallback.getBufferedPosition();
		}
		return 0;
	}

	@Override
	public void onStop() {
		super.onStop();
		mSession.release();
		abandonAudioFocus();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mSession.release();
		mCallback.releasePlayer();
		mHelper.onStop();
	}

	@SuppressWarnings("WrongConstant")
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG,"OnAttach");
		if(context instanceof PlaybackControlListener){
			mCallback = (PlaybackControlListener) context;
		} else {
			throw(new RuntimeException("Activity must implement PlaybackControlListener"));
		}

		if(mSession == null){
			mSession = new MediaSession(getActivity(),"chaosflix");
			mSession.setCallback(new ChaosflixSessionCallback());
			mSession.setFlags(FLAG_HANDLES_MEDIA_BUTTONS| FLAG_HANDLES_TRANSPORT_CONTROLS);
			mSession.setActive(true);

			setPlaybackState(PlaybackState.STATE_NONE);
//			mSession.setPlaybackState(state);

			getActivity().setMediaController(
					new MediaController(getActivity(),mSession.getSessionToken()));
		}
	}

	private void setPlaybackState(int state){
		long currentPosition = getCurrentPositionLong();
		PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
				.setActions(getAvailableActions(state))
				.setState(PlaybackState.STATE_PLAYING, currentPosition, 0);

		mSession.setPlaybackState(stateBuilder.build());
	}

	private int getPlaybackState() {
		Activity activity = getActivity();

		if (activity != null) {
			PlaybackState state = activity.getMediaController().getPlaybackState();
			if (state != null) {
				return state.getState();
			} else {
				return PlaybackState.STATE_NONE;
			}
		}
		return PlaybackState.STATE_NONE;
	}

	private long getAvailableActions(int nextState) {
		long actions = PlaybackState.ACTION_PLAY |
				PlaybackState.ACTION_SKIP_TO_NEXT |
				PlaybackState.ACTION_SKIP_TO_PREVIOUS |
				PlaybackState.ACTION_FAST_FORWARD |
				PlaybackState.ACTION_REWIND |
				PlaybackState.ACTION_PAUSE;

		if (nextState == PlaybackState.STATE_PLAYING) {
			actions |= PlaybackState.ACTION_PAUSE;
		}

		return actions;
	}

	private void requestAudioFocus() {
		if (mHasAudioFocus) {
			return;
		}
		int result = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
				AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			mHasAudioFocus = true;
		} else {
			pause();
		}
	}

	private void abandonAudioFocus() {
		mHasAudioFocus = false;
		mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
	}

	private void play() {
		setPlaybackState(PlaybackState.STATE_PLAYING);
		mCallback.play();
	}

	private void pause() {
		setPlaybackState(PlaybackState.STATE_PAUSED);
		mCallback.pause();
	}

	private void rewind() {
		int prevState = getPlaybackState();
		setPlaybackState(PlaybackState.STATE_FAST_FORWARDING);
		mCallback.skipBackward(30);
		setPlaybackState(prevState);
	}

	private void fastForward() {
		int prevState = getPlaybackState();
		setPlaybackState(PlaybackState.STATE_FAST_FORWARDING);
		mCallback.skipForward(30);
		setPlaybackState(prevState);
	}

	private class ChaosflixSessionCallback extends MediaSession.Callback {
		@Override
		public void onPlay() {
			play();
		}

		@Override
		public void onPause() {
			pause();
		}

		@Override
		public void onFastForward() {
			fastForward();
		}

		@Override
		public void onRewind() {
			rewind();
		}

		@Override
		public void onSkipToNext() {
			mCallback.skipForward(5*60);
		}

		@Override
		public void onSkipToPrevious() {
			mCallback.skipBackward(5*60);
		}

		@Override
		public void onSeekTo(long pos) {
			mCallback.seekTo(pos);
		}
	}
}
package com.example.Dive;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.Dive.viewmodel.MainActivityViewModel;
import com.example.Dive.adapter.FavTrackAdapter;
import com.example.Dive.adapter.TrackAdapter;
import com.example.Dive.model.Track;
import com.example.Dive.service.MediaPlayerService;
import java.util.ArrayList;
import java.util.Objects;

import static com.example.Dive.service.MediaPlayerService.player;

public class MainActivity extends AppCompatActivity {
    private ArrayList<Track> trackList = new ArrayList<>();

    private TrackAdapter trackAdapter;
    private FavTrackAdapter favTrackAdapter;

    private MainActivityViewModel mainActivityViewModel;
    private RecyclerView favoriteRecycler;
    private TextView artistName, trackName, trackDuration;

    private ImageView btnPlay, btnNext, btnPrev;
    public static int currPosition, nextPosition, prevPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        favoriteRecycler = findViewById(R.id.recycler_favorite);
        RecyclerView trackRecycler = findViewById(R.id.recycler_tracks);

        artistName = findViewById(R.id.artist_name);
        trackName = findViewById(R.id.track_name);
        trackDuration = findViewById(R.id.track_duration);

        btnPlay = findViewById(R.id.btn_play);
        btnNext = findViewById(R.id.btn_next);
        btnPrev = findViewById(R.id.btn_prev);

        // Adapter for favoriteList.
        favoriteRecycler.setLayoutManager(new GridLayoutManager(getApplicationContext(), 1,
                                        GridLayoutManager.HORIZONTAL, false));
        favTrackAdapter = new FavTrackAdapter();
        favoriteRecycler.setAdapter(favTrackAdapter);

        mainActivityViewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);
        mainActivityViewModel.getFavTracks().observe(this, tracks -> {
            favTrackAdapter.setFavTracks(tracks);
            checkIfFavEmpty();
        });

        if (mainActivityViewModel.getTrack() != null) {
            artistName.setText(mainActivityViewModel.getTrack().getArtistName());
            trackName.setText(mainActivityViewModel.getTrack().getTrackName());
            trackDuration.setText(String.valueOf(mainActivityViewModel.getTrack().getTrackDuration()));
        }

        favTrackAdapter.setOnTrackSelectListener(new FavTrackAdapter.OnFavTrackSelectListener() {
            @Override
            public void deleteTrack(View view, int position, Track track) {
                mainActivityViewModel.deleteTrack(track);
            }

            @Override
            public void selectTrack(View view, int position, Track track) {
                startForegroundService(track);
                mainActivityViewModel.setTrack(track);

                artistName.setText(mainActivityViewModel.getTrack().getArtistName());
                trackName.setText(mainActivityViewModel.getTrack().getTrackName());
                trackDuration.setText(String.valueOf(mainActivityViewModel.getTrack().getTrackDuration()));

                mainActivityViewModel.setBtnPlay(getResources().getDrawable(R.drawable.ic_pause));
                btnPlay.setImageDrawable(mainActivityViewModel.getBtnPlay());
            }
        });

        // Adapter for trackList
        getTracksFromStorage();
        trackRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        trackAdapter = new TrackAdapter(getApplicationContext(), trackList);
        trackRecycler.setAdapter(trackAdapter);
        trackAdapter.setOnTrackSelectListener(new TrackAdapter.onTrackSelectListener() {
            @Override
            public void onTrackSelect(View view, int position, Track track) {
                startForegroundService(track);
                mainActivityViewModel.setTrack(track);
                mainActivityViewModel.setBtnPlay(getResources().getDrawable(R.drawable.ic_pause));

                btnPlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_pause));
                artistName.setText(mainActivityViewModel.getTrack().getArtistName());
                trackName.setText(mainActivityViewModel.getTrack().getTrackName());
                trackDuration.setText(String.valueOf(mainActivityViewModel.getTrack().getTrackDuration()));

                btnNext.setClickable(true);
                btnPrev.setClickable(true);
            }

            @Override
            public void addToFavorites(View view, int position, Track track) {
                currPosition = position;
                mainActivityViewModel.insertTrack(track);
            }

            @Override
            public void deleteTrack(View view, int position, Track track) {
                deleteSelectedTrack(track.getId());
                trackAdapter.notifyItemRemoved(position);
            }
        });

        if (mainActivityViewModel.getBtnPlay() != null) {
            mainActivityViewModel.setBtnPlay(mainActivityViewModel.getBtnPlay());
            btnPlay.setImageDrawable(mainActivityViewModel.getBtnPlay());
        }

        btnNext.setClickable(true);
        btnPrev.setClickable(true);

        btnPlay.setOnClickListener(view -> {
            if(mainActivityViewModel.getTrack()==null) {
                Toast.makeText(getApplicationContext(), "Select track to begin", Toast.LENGTH_SHORT).show();
            }else {
                controlTrack();

                artistName.setText(mainActivityViewModel.getTrack().getArtistName());
                trackName.setText(mainActivityViewModel.getTrack().getTrackName());
                trackDuration.setText(String.valueOf(mainActivityViewModel.getTrack().getTrackDuration()));
            }
        });

            btnNext.setOnClickListener(view -> {
                nextPosition = currPosition++;
                Track nextTrack = trackList.get(nextPosition);
                if (nextPosition < trackList.size()-1){
                    startForegroundService(nextTrack);
                    mainActivityViewModel.setTrack(nextTrack);

                    artistName.setText(mainActivityViewModel.getTrack().getArtistName());
                    trackName.setText(mainActivityViewModel.getTrack().getTrackName());
                    trackDuration.setText(String.valueOf(mainActivityViewModel.getTrack().getTrackDuration()));

                    mainActivityViewModel.setBtnPlay(getResources().getDrawable(R.drawable.ic_pause));
                    btnPlay.setImageDrawable(mainActivityViewModel.getBtnPlay());
                }else {
                    nextPosition = 1;
                    startForegroundService(nextTrack);
                    mainActivityViewModel.setTrack(nextTrack);

                    artistName.setText(mainActivityViewModel.getTrack().getArtistName());
                    trackName.setText(mainActivityViewModel.getTrack().getTrackName());
                    trackDuration.setText(String.valueOf(mainActivityViewModel.getTrack().getTrackDuration()));

                    mainActivityViewModel.setBtnPlay(getResources().getDrawable(R.drawable.ic_pause));
                    btnPlay.setImageDrawable(mainActivityViewModel.getBtnPlay());
                }
            });

            btnPrev.setOnClickListener(view -> {
                prevPosition = currPosition--;
                Track prevTrack = trackList.get(prevPosition);
                if (prevPosition > 0){
                    startForegroundService(prevTrack);
                    mainActivityViewModel.setTrack(prevTrack);

                    artistName.setText(mainActivityViewModel.getTrack().getArtistName());
                    trackName.setText(mainActivityViewModel.getTrack().getTrackName());
                    trackDuration.setText(String.valueOf(mainActivityViewModel.getTrack().getTrackDuration()));

                    mainActivityViewModel.setBtnPlay(getResources().getDrawable(R.drawable.ic_pause));
                    btnPlay.setImageDrawable(mainActivityViewModel.getBtnPlay());
                }else {
                    prevPosition = 1;
                    startForegroundService(prevTrack);
                    mainActivityViewModel.setTrack(prevTrack);

                    artistName.setText(mainActivityViewModel.getTrack().getArtistName());
                    trackName.setText(mainActivityViewModel.getTrack().getTrackName());
                    trackDuration.setText(String.valueOf(mainActivityViewModel.getTrack().getTrackDuration()));

                    mainActivityViewModel.setBtnPlay(getResources().getDrawable(R.drawable.ic_pause));
                    btnPlay.setImageDrawable(mainActivityViewModel.getBtnPlay());
                }
            });

    }

    private void checkIfFavEmpty(){
        if(Objects.requireNonNull(mainActivityViewModel.getFavTracks().getValue()).isEmpty()){
            favoriteRecycler.setVisibility(View.GONE);
        }else {
            favoriteRecycler.setVisibility(View.VISIBLE);
        }
    }

    private void controlTrack(){
        if(player.isPlaying()){
            player.pause();
            mainActivityViewModel.setBtnPlay(getResources().getDrawable(R.drawable.ic_play));
            btnPlay.setImageDrawable(mainActivityViewModel.getBtnPlay());
        }else {
            player.start();
            mainActivityViewModel.setBtnPlay(getResources().getDrawable(R.drawable.ic_pause));
            btnPlay.setImageDrawable(mainActivityViewModel.getBtnPlay());
        }
    }

    private void startForegroundService(Track track){
        Intent serviceIntent = new Intent(this, MediaPlayerService.class);
        serviceIntent.putExtra("track", track);
        startService(serviceIntent);
    }

    private void stopForegroundService(){
        Intent serviceIntent = new Intent(this, MediaPlayerService.class);
        stopService(serviceIntent);
    }

    // Get tracks from storage
    private void getTracksFromStorage(){
        trackList = new ArrayList<Track>();
        ContentResolver contentResolver = Objects.requireNonNull(getApplicationContext()).getContentResolver();
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.IS_MUSIC, null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        String trackName, artistName, trackDuration;
        int trackId;

        if(cursor!=null && cursor.moveToFirst()){
            do{
                trackId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                trackName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                trackDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                String duration = trackDuration;
                int converted = Integer.parseInt(duration);
                int mns = (converted / 60000) % 60000;
                int scs = converted % 60000 / 1000;

                @SuppressLint("DefaultLocale") String songDuration = String.format("%02d:%02d",  mns, scs);

                trackList.add(new Track(trackId, trackName, artistName, songDuration));
            }while (cursor.moveToNext());
            cursor.close();
        }else {
            Toast.makeText(getApplicationContext(), "You've got 0 tracks", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSelectedTrack(long id){
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        getApplicationContext().getContentResolver().delete(uri, null, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

package com.wolcano.musicplayer.music.mvp.interactor;

import android.Manifest;
import android.app.Activity;

import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.wolcano.musicplayer.music.R;
import com.wolcano.musicplayer.music.mvp.DisposableManager;
import com.wolcano.musicplayer.music.mvp.interactor.interfaces.PlaylistInteractor;
import com.wolcano.musicplayer.music.mvp.models.Playlist;
import com.wolcano.musicplayer.music.utils.PermissionUtils;
import com.wolcano.musicplayer.music.utils.SongUtils;
import com.wolcano.musicplayer.music.utils.ToastUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.wolcano.musicplayer.music.constants.Constants.SONG_LIBRARY;

public class PlaylistInteractorImpl implements PlaylistInteractor {


    @Subscribe(tags = {@Tag(SONG_LIBRARY)})
    @Override
    public void getPlaylists(Activity activity, String sort, OnGetPlaylistListener onGetPlaylistListener) {

        PermissionUtils.with(activity)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .result(new PermissionUtils.PermInterface() {
                    @Override
                    public void onPermGranted() {
                        Observable<List<Playlist>> observable =
                                Observable.fromCallable(() -> SongUtils.scanPlaylist(activity)).throttleFirst(500, TimeUnit.MILLISECONDS);

                        Disposable disposable = observable.
                                subscribeOn(Schedulers.io()).
                                observeOn(AndroidSchedulers.mainThread()).
                                subscribe(playlists -> onGetPlaylistListener.sendPlaylists(playlists));

                        DisposableManager.add(disposable);
                    }

                    @Override
                    public void onPermUnapproved() {
                        onGetPlaylistListener.controlIfEmpty();
                        ToastUtils.show(activity.getApplicationContext(), R.string.no_perm_storage);
                    }
                })
                .reqPerm();


    }
}

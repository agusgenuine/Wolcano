package com.wolcano.musicplayer.music.mvp.interactor;

import android.Manifest;
import android.app.Activity;

import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.wolcano.musicplayer.music.R;
import com.wolcano.musicplayer.music.mvp.interactor.interfaces.ArtistInteractor;
import com.wolcano.musicplayer.music.mvp.interactor.interfaces.GenreInteractor;
import com.wolcano.musicplayer.music.mvp.models.Artist;
import com.wolcano.musicplayer.music.utils.Perms;
import com.wolcano.musicplayer.music.utils.SongUtils;
import com.wolcano.musicplayer.music.utils.ToastUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.wolcano.musicplayer.music.Constants.SONG_LIBRARY;

public class ArtistInteractorImpl implements ArtistInteractor {

    private Disposable disposable1;

    @Subscribe(tags = {@Tag(SONG_LIBRARY)})
    @Override
    public void getArtist(Activity activity, Disposable disposable, String sort, OnGetArtistListener onGetArtistListener) {

        disposable1 = disposable;
        Perms.with(activity)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .result(new Perms.PermInterface() {
                    @Override
                    public void onPermGranted() {
                        Observable<List<Artist>> observable =
                                Observable.fromCallable(() -> SongUtils.scanArtists(activity)).throttleFirst(500, TimeUnit.MILLISECONDS);

                        disposable1 = observable.
                                subscribeOn(Schedulers.io()).
                                observeOn(AndroidSchedulers.mainThread()).
                                subscribe(artistList -> onGetArtistListener.sendArtist(artistList));
                    }

                    @Override
                    public void onPermUnapproved() {
                        onGetArtistListener.controlIfEmpty();
                        ToastUtils.show(activity.getApplicationContext(), R.string.no_perm_storage);
                    }
                })
                .reqPerm();


    }
}
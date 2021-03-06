package com.github.axet.torrentclient.fragments;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.axet.androidlibrary.app.MainLibrary;
import com.github.axet.androidlibrary.widgets.ThemeUtils;
import com.github.axet.torrentclient.R;
import com.github.axet.torrentclient.activities.MainActivity;
import com.github.axet.torrentclient.app.MainApplication;
import com.github.axet.torrentclient.app.Storage;
import com.github.axet.torrentclient.widgets.Pieces;

import java.io.File;

import libtorrent.InfoTorrent;
import libtorrent.Libtorrent;
import libtorrent.StatsTorrent;

public class DetailsFragment extends Fragment implements MainActivity.TorrentFragmentInterface {
    View v;

    Pieces pview;
    TextView size;
    TextView hash;
    TextView pieces;
    TextView creator;
    TextView createdon;
    TextView comment;
    TextView status;
    TextView progress;
    TextView added;
    TextView completed;
    TextView downloading;
    TextView seeding;
    TextView name;
    View pathButton;
    View renameButton;
    ImageButton pathImage;
    ImageView check;
    TextView downloaded;
    TextView uploaded;
    TextView ratio;
    View meta;
    View parts;

    KeyguardManager myKM;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.torrent_status, container, false);

        pview = (Pieces) v.findViewById(R.id.torrent_status_pieces);
        size = (TextView) v.findViewById(R.id.torrent_size);
        hash = (TextView) v.findViewById(R.id.torrent_hash);
        pieces = (TextView) v.findViewById(R.id.torrent_pieces);
        creator = (TextView) v.findViewById(R.id.torrent_creator);
        createdon = (TextView) v.findViewById(R.id.torrent_created_on);
        comment = (TextView) v.findViewById(R.id.torrent_comment);
        status = (TextView) v.findViewById(R.id.torrent_status);
        progress = (TextView) v.findViewById(R.id.torrent_progress);
        added = (TextView) v.findViewById(R.id.torrent_added);
        completed = (TextView) v.findViewById(R.id.torrent_completed);
        downloading = (TextView) v.findViewById(R.id.torrent_downloading);
        seeding = (TextView) v.findViewById(R.id.torrent_seeding);
        check = (ImageView) v.findViewById(R.id.torrent_status_check);
        meta = v.findViewById(R.id.torrent_status_metadata);
        parts = v.findViewById(R.id.torrent_status_parts);

        final long t = getArguments().getLong("torrent");

        final String h = Libtorrent.torrentHash(t);
        hash.setText(h);

        View hashCopy = v.findViewById(R.id.torrent_hash_copy);
        hashCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("hash", h);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), R.string.hash_copied, Toast.LENGTH_SHORT).show();
            }
        });

        meta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Libtorrent.downloadMetadata(t)) {
                    ((MainActivity) getActivity().getApplicationContext()).Error(Libtorrent.error());
                    return;
                }
            }
        });

        final String p = ((MainApplication) getContext().getApplicationContext()).getStorage().path(t);

        TextView path = (TextView) v.findViewById(R.id.torrent_path);
        path.setText(p);

        pathButton = v.findViewById(R.id.torrent_path_open);
        pathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).openFolder(new File(p));
            }
        });

        name = (TextView) v.findViewById(R.id.torrent_name);

        renameButton = v.findViewById(R.id.torrent_status_rename);
        renameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).renameDialog(t);
            }
        });

        pathImage = (ImageButton) v.findViewById(R.id.torrent_path_image);

        myKM = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);

        downloaded = (TextView) v.findViewById(R.id.torrent_downloaded);
        uploaded = (TextView) v.findViewById(R.id.torrent_uploaded);
        ratio = (TextView) v.findViewById(R.id.torrent_ratio);

        update();

        return v;
    }

    @Override
    public void update() {
        final long t = getArguments().getLong("torrent");

        if (myKM.inKeyguardRestrictedInputMode()) {
            pathButton.setEnabled(false);
            pathImage.setColorFilter(Color.GRAY);
        } else {
            pathButton.setEnabled(true);
            pathImage.setColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent));
        }

        if (Libtorrent.metaTorrent(t)) {
            meta.setVisibility(View.GONE);
            parts.setVisibility(View.VISIBLE);
        } else {
            meta.setVisibility(View.VISIBLE);
            parts.setVisibility(View.GONE);
        }

        final Runnable checkUpdate = new Runnable() {
            @Override
            public void run() {
                int s = Libtorrent.torrentStatus(t);
                switch (s) {
                    case Libtorrent.StatusChecking:
                        check.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_stop_black_24dp));
                        check.setColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent));
                        check.setEnabled(true);
                        break;
                    case Libtorrent.StatusDownloading:
                    case Libtorrent.StatusQueued:
                    case Libtorrent.StatusSeeding:
                        check.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_done_all_black_24dp));
                        check.setColorFilter(Color.GRAY);
                        check.setEnabled(false);
                        break;
                    default:
                        check.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_done_all_black_24dp));
                        check.setColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent));
                        check.setEnabled(true);
                        break;
                }
            }
        };
        checkUpdate.run();
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).checkTorrent(t);
                checkUpdate.run();
            }
        });

        pview.setTorrent(t);

        name.setText(Libtorrent.torrentName(t));

        MainApplication.setText(size, !Libtorrent.metaTorrent(t) ? "" : MainLibrary.formatSize(getContext(), Libtorrent.torrentBytesLength(t)));

        MainApplication.setText(pieces, !Libtorrent.metaTorrent(t) ? "" : Libtorrent.torrentPiecesCount(t) + " / " + MainLibrary.formatSize(getContext(), Libtorrent.torrentPieceLength(t)));

        InfoTorrent i = Libtorrent.torrentInfo(t);

        MainApplication.setText(creator, i.getCreator());

        MainApplication.setDate(createdon, i.getCreateOn());

        final String c = i.getComment().trim();
        MainApplication.setText(comment, c);
        comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!c.startsWith("http"))
                    return;

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.open_url);

                builder.setMessage(c + "\n\n" + getContext().getString(R.string.are_you_sure));
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(c));
                        startActivity(browserIntent);
                    }
                });
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });

        switch (Libtorrent.torrentStatus(t)) {
            case Libtorrent.StatusQueued:
                status.setText(R.string.status_queue);
                break;
            case Libtorrent.StatusDownloading:
                status.setText(R.string.status_downloading);
                break;
            case Libtorrent.StatusPaused:
                status.setText(R.string.status_paused);
                break;
            case Libtorrent.StatusSeeding:
                status.setText(R.string.status_seeding);
                break;
            case Libtorrent.StatusChecking:
                status.setText(R.string.status_checking);
                break;
        }

        progress.setText(String.format("%d%%", Storage.Torrent.getProgress(t)));

        StatsTorrent b = Libtorrent.torrentStats(t);
        downloaded.setText(MainLibrary.formatSize(getContext(), b.getDownloaded()));

        uploaded.setText(MainLibrary.formatSize(getContext(), b.getUploaded()));

        float r = 0;
        if (Libtorrent.metaTorrent(t)) {
            if (b.getDownloaded() >= Libtorrent.torrentBytesLength(t)) {
                r = b.getUploaded() / (float) b.getDownloaded();
            } else {
                r = b.getUploaded() / (float) Libtorrent.torrentBytesLength(t);
            }
        }
        ratio.setText(String.format("%.2f", r));

        InfoTorrent info = Libtorrent.torrentInfo(t);

        MainApplication.setDate(added, info.getDateAdded());

        MainApplication.setDate(completed, info.getDateCompleted());

        downloading.setText(MainLibrary.formatDuration(getContext(), b.getDownloading() / 1000000));

        seeding.setText(MainLibrary.formatDuration(getContext(), b.getSeeding() / 1000000));
    }

    @Override
    public void close() {

    }
}
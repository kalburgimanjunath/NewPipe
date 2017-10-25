package org.schabi.newpipe.playlist;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.schabi.newpipe.R;
import org.schabi.newpipe.playlist.events.AppendEvent;
import org.schabi.newpipe.playlist.events.ErrorEvent;
import org.schabi.newpipe.playlist.events.MoveEvent;
import org.schabi.newpipe.playlist.events.PlayQueueEvent;
import org.schabi.newpipe.playlist.events.RemoveEvent;
import org.schabi.newpipe.playlist.events.SelectEvent;

import java.util.List;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

/**
 * Created by Christian Schabesberger on 01.08.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * InfoListAdapter.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class PlayQueueAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = PlayQueueAdapter.class.toString();

    private static final int ITEM_VIEW_TYPE_ID = 0;
    private static final int FOOTER_VIEW_TYPE_ID = 1;

    private final PlayQueueItemBuilder playQueueItemBuilder;
    private final PlayQueue playQueue;
    private boolean showFooter = false;
    private View footer = null;

    private Disposable playQueueReactor;

    public class HFHolder extends RecyclerView.ViewHolder {
        public HFHolder(View v) {
            super(v);
            view = v;
        }
        public View view;
    }

    public PlayQueueAdapter(final PlayQueue playQueue) {
        this.playQueueItemBuilder = new PlayQueueItemBuilder();
        this.playQueue = playQueue;

        startReactor();
    }

    public void setSelectedListener(final PlayQueueItemBuilder.OnSelectedListener listener) {
        playQueueItemBuilder.setOnSelectedListener(listener);
    }

    private void startReactor() {
        final Observer<PlayQueueEvent> observer = new Observer<PlayQueueEvent>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                if (playQueueReactor != null) playQueueReactor.dispose();
                playQueueReactor = d;
            }

            @Override
            public void onNext(@NonNull PlayQueueEvent playQueueMessage) {
                onPlayQueueChanged(playQueueMessage);
            }

            @Override
            public void onError(@NonNull Throwable e) {}

            @Override
            public void onComplete() {
                dispose();
            }
        };

        playQueue.getBroadcastReceiver()
                .toObservable()
                .subscribe(observer);
    }

    private void onPlayQueueChanged(final PlayQueueEvent message) {
        switch (message.type()) {
            case RECOVERY:
                // Do nothing.
                break;
            case SELECT:
                final SelectEvent selectEvent = (SelectEvent) message;
                notifyItemChanged(selectEvent.getOldIndex());
                notifyItemChanged(selectEvent.getNewIndex());
                break;
            case APPEND:
                final AppendEvent appendEvent = (AppendEvent) message;
                notifyItemRangeInserted(playQueue.size(), appendEvent.getAmount());
                break;
            case ERROR:
                final ErrorEvent errorEvent = (ErrorEvent) message;
                if (!errorEvent.isSkippable()) {
                    notifyItemRemoved(errorEvent.index());
                }
                notifyItemChanged(errorEvent.index());
                break;
            case REMOVE:
                final RemoveEvent removeEvent = (RemoveEvent) message;
                notifyItemRemoved(removeEvent.index());
                notifyItemChanged(removeEvent.index());
                break;
            case MOVE:
                final MoveEvent moveEvent = (MoveEvent) message;
                notifyItemMoved(moveEvent.getFromIndex(), moveEvent.getToIndex());
                break;
            case INIT:
            case REORDER:
            default:
                notifyDataSetChanged();
                break;
        }
    }

    public void dispose() {
        if (playQueueReactor != null) playQueueReactor.dispose();
        playQueueReactor = null;
    }

    public void setFooter(View footer) {
        this.footer = footer;
        notifyItemChanged(playQueue.size());
    }

    public void showFooter(final boolean show) {
        showFooter = show;
        notifyItemChanged(playQueue.size());
    }

    public List<PlayQueueItem> getItems() {
        return playQueue.getStreams();
    }

    @Override
    public int getItemCount() {
        int count = playQueue.getStreams().size();
        if(footer != null && showFooter) count++;
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if(footer != null && position == playQueue.getStreams().size() && showFooter) {
            return FOOTER_VIEW_TYPE_ID;
        }

        return ITEM_VIEW_TYPE_ID;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        switch(type) {
            case FOOTER_VIEW_TYPE_ID:
                return new HFHolder(footer);
            case ITEM_VIEW_TYPE_ID:
                return new PlayQueueItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.play_queue_item, parent, false));
            default:
                Log.e(TAG, "Attempting to create view holder with undefined type: " + type);
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof PlayQueueItemHolder) {
            // Build the list item
            playQueueItemBuilder.buildStreamInfoItem((PlayQueueItemHolder) holder, playQueue.getStreams().get(position));
            // Check if the current item should be selected/highlighted
            holder.itemView.setSelected(playQueue.getIndex() == position);
        } else if(holder instanceof HFHolder && position == playQueue.getStreams().size() && footer != null && showFooter) {
            ((HFHolder) holder).view = footer;
        }
    }
}

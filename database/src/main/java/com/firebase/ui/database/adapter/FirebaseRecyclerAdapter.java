package com.firebase.ui.database.adapter;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseArray;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This class is a generic way of backing an {@link RecyclerView} with a Firebase location. It
 * handles all of the child events at the given Firebase location. It marshals received data into
 * the given class type.
 * <p>
 * To use this class in your app, subclass it passing in all required parameters and implement the
 * {@link #populateViewHolder(RecyclerView.ViewHolder, Object, int)} method.
 * <p>
 * <pre>
 *     private static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
 *         TextView messageText;
 *         TextView nameText;
 *
 *         public ChatMessageViewHolder(View itemView) {
 *             super(itemView);
 *             nameText = (TextView)itemView.findViewById(android.R.id.text1);
 *             messageText = (TextView) itemView.findViewById(android.R.id.text2);
 *         }
 *     }
 *
 *     FirebaseRecyclerAdapter<ChatMessage, ChatMessageViewHolder> adapter;
 *     DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
 *
 *     RecyclerView recycler = (RecyclerView) findViewById(R.id.messages_recycler);
 *     recycler.setHasFixedSize(true);
 *     recycler.setLayoutManager(new LinearLayoutManager(this));
 *
 *     adapter = new FirebaseRecyclerAdapter<ChatMessage, ChatMessageViewHolder>(
 *           ChatMessage.class, android.R.layout.two_line_list_item, ChatMessageViewHolder.class,
 * ref) {
 *         public void populateViewHolder(ChatMessageViewHolder chatMessageViewHolder,
 *                                        ChatMessage chatMessage,
 *                                        int position) {
 *             chatMessageViewHolder.nameText.setText(chatMessage.getName());
 *             chatMessageViewHolder.messageText.setText(chatMessage.getMessage());
 *         }
 *     };
 *     recycler.setAdapter(mAdapter);
 * </pre>
 *
 * @param <T>  The Java class that maps to the type of objects stored in the Firebase location.
 * @param <VH> The {@link RecyclerView.ViewHolder} class that contains the Views in the layout that
 *             is shown for each object.
 */
public abstract class FirebaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements ChangeEventListener {
    private static final String TAG = "FirebaseRecyclerAdapter";

    protected FirebaseArray mSnapshots;
    protected Class<T> mModelClass;
    protected Class<VH> mViewHolderClass;
    protected int mModelLayout;

    /**
     * @param modelClass      Firebase will marshall the data at a location into an instance of a
     *                        class that you provide
     * @param modelLayout     This is the layout used to represent a single item in the list. You
     *                        will be responsible for populating an instance of the corresponding
     *                        view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance
     *                        modelLayout.
     * @param snapshots       The data used to populate the adapter
     */
    public FirebaseRecyclerAdapter(FirebaseArray snapshots,
                                   Class<T> modelClass,
                                   Class<VH> viewHolderClass,
                                   @LayoutRes int modelLayout) {
        mSnapshots = snapshots;
        mModelClass = modelClass;
        mViewHolderClass = viewHolderClass;
        mModelLayout = modelLayout;

        startListening();
    }

    /**
     * @param ref The Firebase location to watch for data changes. Can also be a slice of a
     *            location, using some combination of {@code limit()}, {@code startAt()}, and {@code
     *            endAt()}.
     * @see #FirebaseRecyclerAdapter(FirebaseArray, Class, Class, int)
     */
    public FirebaseRecyclerAdapter(Class<T> modelClass,
                                   Class<VH> viewHolderClass,
                                   @LayoutRes int modelLayout,
                                   Query ref) {
        this(new FirebaseArray(ref), modelClass, viewHolderClass, modelLayout);
    }

    /**
     * If you need to do some setup before we start listening for change events in the database
     * (such as setting a custom {@link JoinResolver}), do so it here and then call {@code
     * super.startListening()}.
     */
    protected void startListening() {
        if (!mSnapshots.isListening()) {
            mSnapshots.addChangeEventListener(this);
        }
    }

    public void cleanup() {
        mSnapshots.removeChangeEventListener(this);
    }

    @Override
    public void onChildChanged(ChangeEventListener.EventType type, int index, int oldIndex) {
        switch (type) {
            case ADDED:
                notifyItemInserted(index);
                break;
            case CHANGED:
                notifyItemChanged(index);
                break;
            case REMOVED:
                notifyItemRemoved(index);
                break;
            case MOVED:
                notifyItemMoved(oldIndex, index);
                break;
            default:
                throw new IllegalStateException("Incomplete case statement");
        }
    }

    @Override
    public void onDataChanged() {
    }

    @Override
    public void onCancelled(DatabaseError error) {
        Log.w(TAG, error.toException());
    }

    public T getItem(int position) {
        return parseSnapshot(mSnapshots.get(position));
    }

    /**
     * This method parses the DataSnapshot into the requested type. You can override it in
     * subclasses to do custom parsing.
     *
     * @param snapshot the DataSnapshot to extract the model from
     * @return the model extracted from the DataSnapshot
     */
    protected T parseSnapshot(DataSnapshot snapshot) {
        return snapshot.getValue(mModelClass);
    }

    public DatabaseReference getRef(int position) {
        return mSnapshots.get(position).getRef();
    }

    @Override
    public int getItemCount() {
        return mSnapshots.size();
    }

    @Override
    public long getItemId(int position) {
        // http://stackoverflow.com/questions/5100071/whats-the-purpose-of-item-ids-in-android-listview-adapter
        return mSnapshots.get(position).getKey().hashCode();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        try {
            Constructor<VH> constructor = mViewHolderClass.getConstructor(View.class);
            return constructor.newInstance(view);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mModelLayout;
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        T model = getItem(position);
        populateViewHolder(viewHolder, model, position);
    }

    /**
     * Each time the data at the given Firebase location changes,
     * this method will be called for each item that needs to be displayed.
     * The first two arguments correspond to the mLayout and mModelClass given to the constructor of
     * this class. The third argument is the item's position in the list.
     * <p>
     * Your implementation should populate the view using the data contained in the model.
     *
     * @param viewHolder The view to populate
     * @param model      The object containing the data used to populate the view
     * @param position   The position in the list of the view being populated
     */
    protected abstract void populateViewHolder(VH viewHolder, T model, int position);
}

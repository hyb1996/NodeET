package com.stareating.nodeet;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by Stardust on 2017/9/14.
 */

public class PostListActivity extends AppCompatActivity {

    //如何获取帖子列表：访问 http://39.108.231.37/api/category/cid
    //其中cid是对应板块的id

    private static final String LOG_TAG = "PostListActivity";
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateTimeInstance();
    public static final String CATEGORY_ID = "categoryId";
    public static final String CATEGORY_NAME = "categoryName";
    private PostListAdapter mPostListAdapter = new PostListAdapter();
    private List<Posts.PostItem> mPosts = new ArrayList<>();
    private int cid;
    private String categoryName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cid = getIntent().getIntExtra(CATEGORY_ID, -1);
        categoryName = getIntent().getStringExtra(CATEGORY_NAME);
        setUpViews();
        fetchPosts();
    }

    private void fetchPosts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                doFetchPosts();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPostListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    private void doFetchPosts() {
        Log.d(LOG_TAG, "cid = " + cid + ", category = " + categoryName);
        try {
            OkHttpClient mHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://www.autojs.org/api/category/" + String.valueOf(cid))
                    .build();
            Response response = mHttpClient.newCall(request).execute();
            Log.d(LOG_TAG, response.toString());
            ResponseBody body = response.body();
            if (body == null)
                return;
            String json = body.string();
            Log.d(LOG_TAG, "body = " + json);
            Posts posts = new Gson().fromJson(json, new TypeToken<Posts>() {
            }.getType());
            mPosts = posts.getPostItems();
            removeDeletedPosts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeDeletedPosts() {
        Iterator<Posts.PostItem> iterator = mPosts.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().deleted) {
                iterator.remove();
            }
        }
    }

    private void setUpViews() {
        setContentView(R.layout.activity_post_list);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.post_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mPostListAdapter);
        setUpToolbar();
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(categoryName);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private class PostViewHolder extends RecyclerView.ViewHolder {

        TextView title, view_count, post_count, description, iconText;
        RoundedImageView icon;
        GradientDrawable iconTextBackground;


        public PostViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            view_count = (TextView) itemView.findViewById(R.id.view_count);
            post_count = (TextView) itemView.findViewById(R.id.post_count_item_post);
            description = (TextView) itemView.findViewById(R.id.description_item_post);
            iconText = (TextView) itemView.findViewById(R.id.icon_text);
            iconText.setTypeface(CategoryListFragment.TYPEFACE_ICON);
            iconTextBackground = (GradientDrawable) iconText.getBackground();
            icon = (RoundedImageView) itemView.findViewById(R.id.icon);
        }
    }

    private class PostListAdapter extends RecyclerView.Adapter<PostViewHolder> {

        @Override
        public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_post, parent, false));
        }

        @Override
        public void onBindViewHolder(final PostViewHolder holder, int position) {
            final Posts.PostItem postItem = mPosts.get(position);
            holder.title.setText(postItem.title);
            holder.description.setText(DATE_FORMAT.format(new Date(postItem.timestamp)));
            holder.view_count.setText(String.valueOf(postItem.viewcount));
            holder.post_count.setText(String.valueOf(postItem.postcount));
            if (TextUtils.isEmpty(postItem.user.picture)) {
                holder.icon.setVisibility(View.GONE);
                holder.iconText.setVisibility(View.VISIBLE);
                holder.iconTextBackground.setColor(Color.parseColor(postItem.user.iconBgColor));
                holder.iconText.setText(postItem.user.iconText);
            } else {
                holder.icon.setVisibility(View.VISIBLE);
                holder.iconText.setVisibility(View.GONE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Drawable d = loadImageFromWebOperations("http://www.autojs.org" + postItem.user.picture);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                holder.icon.setImageDrawable(d);
                            }
                        });
                    }
                }).start();
            }
        }

        @Override
        public int getItemCount() {
            return mPosts == null ? 0 : mPosts.size();
        }
    }

    public static Drawable loadImageFromWebOperations(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            return Drawable.createFromStream(is, "src name");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

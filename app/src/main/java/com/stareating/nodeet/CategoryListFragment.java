package com.stareating.nodeet;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.stareating.nodeet.network.NodeBBService;
import com.stareating.nodeet.network.api.CategoryApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.stareating.nodeet.PostListActivity.CATEGORY_ID;
import static com.stareating.nodeet.PostListActivity.CATEGORY_NAME;

/**
 * Created by Stardust on 2017/9/16.
 */
public class CategoryListFragment extends Fragment {

    private static final String LOG_TAG = "CategoryListFragment";
    static Typeface TYPEFACE_ICON;

    private CategoryListAdapter mCategoryListAdapter = new CategoryListAdapter();
    private List<Categories.CategoryItem> mCategories = new ArrayList<>();
    private Retrofit mRetrofit = NodeBBService.getInstance().getRetrofit();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initIconTypeFaceIfNeeded();
        setUpCategoryList(view);
        fetchCategories();

    }


    private void initIconTypeFaceIfNeeded() {
        if (TYPEFACE_ICON != null)
            return;
        TYPEFACE_ICON = Typeface.createFromAsset(getActivity().getAssets(), "fonts/fontawesome-webfont.ttf");
    }

    private void fetchCategories() {
        //网络访问等耗时操作不能在主线程(UI线程)中执行，否则会造成界面卡顿。
        //这里直接让他返回一个Call，然后会有一个获取的回调。这样的话这个Call会异步执行并在获取后执行回调。就不用开线程。
        //其实这里的Call也可以直接和之前那样执行execute。只不过这样的话就是同步执行的，需要开新线程。

        /*
        retrofit2.Response<Categories> response = mRetrofit.create(CategoryApi.class)
                .getCategories()
                .execute();
        //这样和之前newCall().execute是一样的，只不过，之前要自己用Gson把他解析成对象。
        //而现在就不用了会自动解析。
        //现在是调用enqueue的话就会有一个回调函数会在请求执行完成后被调用并把结果传递到函数参数、
        //这样就是异步的了（enqueue函数会自动创建新线程来完成，并在获取结果后切换回主线程执行回调函数）
         */

        mRetrofit.create(CategoryApi.class)
                .getCategories()
                .enqueue(new Callback<Categories>() {
                    @Override
                    public void onResponse(Call<Categories> call, retrofit2.Response<Categories> response) {
                        mCategories = response.body().getCategoryItems();
                        mCategoryListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(Call<Categories> call, Throwable t) {
                        Toast.makeText(getContext(), R.string.fetch_failed, Toast.LENGTH_SHORT).show();
                        t.printStackTrace();
                    }

                });
    }


    private void setUpCategoryList(View view) {
        RecyclerView categoryListView = (RecyclerView) view.findViewById(R.id.category_list);
        categoryListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        categoryListView.setAdapter(mCategoryListAdapter);
    }


    //这个ViewHolder的代码看起来就很烦了，一堆的findViewById(还容易忘了加itemView造成蜜汁Bug。）以及冗长的setOnClickListener
    //emmmm 这样子看起来就比较干净一点点啦 更关键的是，维护起来更让人舒心一点
    class CategoryViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.name)
        TextView name;

        @BindView(R.id.description)
        TextView description;

        @BindView(R.id.topic_count)
        TextView topic_count;

        @BindView(R.id.post_count)
        TextView post_count;

        @BindView(R.id.icon)
        TextView icon;

        GradientDrawable iconBackground;

        CategoryViewHolder(View itemView) {
            super(itemView);
            //第一次参数是注解所在的对象。第二个参数是View。
            ButterKnife.bind(this, itemView);
            icon.setTypeface(TYPEFACE_ICON);
            iconBackground = (GradientDrawable) icon.getBackground();
        }

        @OnClick(R.id.item)
        void showCategoryContent() {
            int pos = getAdapterPosition();
            Categories.CategoryItem item = mCategories.get(pos);
            startActivity(new Intent(getActivity(), PostListActivity.class)
                    .putExtra(CATEGORY_ID, item.cid)
                    .putExtra(CATEGORY_NAME, item.name));
        }
    }

    private class CategoryListAdapter extends RecyclerView.Adapter<CategoryViewHolder> {

        @Override
        public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new CategoryViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_category, parent, false));
        }

        @Override
        public void onBindViewHolder(CategoryViewHolder holder, int position) {
            Categories.CategoryItem category = mCategories.get(position);
            holder.name.setText(category.name);
            holder.description.setText(category.description);
            holder.topic_count.setText(String.valueOf(category.topic_count));
            holder.post_count.setText(String.valueOf(category.post_count));
            holder.iconBackground.setColor(Color.parseColor(category.bgColor));
            holder.icon.setText(getCharForFontAwesome(category.icon));
        }

        @Override
        public int getItemCount() {
            return mCategories == null ? 0 : mCategories.size();
        }
    }

    private String getCharForFontAwesome(String icon) {
        int resId = getResources().getIdentifier(icon.replace('-', '_'), "string", getActivity().getPackageName());
        if (resId == 0)
            return "";
        return getString(resId);
    }

}

package com.lactozily.addict;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.lactozily.addict.model.ProductObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    JobManager mJobManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);

        AddictPagerAdapter addictPagerAdapter = new AddictPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager)findViewById(R.id.viewPager);
        viewPager.setAdapter(addictPagerAdapter);
        TabLayout tabLayout = (TabLayout)findViewById(R.id.tab);
        tabLayout.setupWithViewPager(viewPager);

        mJobManager = JobManager.instance();

        Log.i(TAG, "Create Activity");

        if (!AddictUtility.isMyServiceRunning(this, AddictMonitorService.class)) {
            Intent intent = new Intent(this, AddictMonitorService.class);
            startService(intent);
            Log.i(TAG, "Service not running");
        }

        Log.i(TAG, "Service running");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mJobManager.cancelAll();
    }

    @Override
    protected void onStop() {
        super.onStop();

        new JobRequest.Builder(AddictServiceChecker.TAG)
                .setPeriodic(60_000L)
                .setPersisted(true)
                .build()
                .schedule();
    }

    static class AddictPagerAdapter extends FragmentStatePagerAdapter {

        public AddictPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return AddictFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return AddictFragment.TAB_NAME[position];
        }
    }

    public static class AddictFragment extends ListFragment {
        private static final String[] TAB_NAME = {"DAILY", "MONTHLY", "ALL TIME"};
        private static final String TAB_POSITION = "tab_position";
        Realm realm;
        RealmResults<ProductObject> query;

        public AddictFragment() {
        }

        public static AddictFragment newInstance(int tabPosition) {
            AddictFragment fragment = new AddictFragment();
            Bundle args = new Bundle();
            args.putInt(TAB_POSITION, tabPosition);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);

            realm = Realm.getDefaultInstance();

            query = realm.where(ProductObject.class).findAll();

            setListAdapter(new AddictStatsAdapter(this.getContext(), query, true, tabPosition, realm));
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);

            realm = Realm.getDefaultInstance();
            Log.i(TAG, "onCreateView " + String.valueOf(tabPosition));

            View rootView = inflater.inflate(R.layout.addict_stats, container, false);
            TextView date_txt = (TextView)rootView.findViewById(R.id.date_txt);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
            String dt = "";

            switch (tabPosition) {
                case 0:
                    dt = sdf.format(Calendar.getInstance().getTime());
                    break;
                case 1:
                    sdf = new SimpleDateFormat("MMMM yyyy");
                    dt = "1 - " + Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) + " " + sdf.format(Calendar.getInstance().getTime()) ;
                    break;
                case 2:
                    dt = "All time";
                    break;
                default:
                    dt = sdf.format(Calendar.getInstance().getTime());
            }

            date_txt.setText(dt);
            return rootView;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getListView().setDivider(null);
        }

        @Override
        public void onResume() {
            super.onResume();
            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);

            realm = Realm.getDefaultInstance();

            query = realm.where(ProductObject.class).findAll();

            setListAdapter(new AddictStatsAdapter(this.getContext(), query, true, tabPosition, realm));
        }

        @Override
        public void onPause() {
            super.onPause();

            realm.close();
            realm.removeAllChangeListeners();
        }
    }
}

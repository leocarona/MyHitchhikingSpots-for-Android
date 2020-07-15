package com.myhitchhikingspots;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.myhitchhikingspots.databinding.MyRoutesActivityLayoutBinding;
import com.myhitchhikingspots.interfaces.ListListener;
import com.myhitchhikingspots.model.Spot;

import java.util.List;


public class MyRoutesActivity extends Fragment {

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private MyRoutesActivityLayoutBinding binding;
    private Snackbar snackbar;

    static final String LAST_TAB_OPENED_KEY = "last-tab-opened-key";
    static final String TAG = "main-activity";
    ListListener spotsListListener = null;
    private boolean isHandlingRequestToOpenSpotForm = false;

    int indexOfLastOpenTab = 0;

    /**
     * Set shouldGoBackToPreviousActivity to true if instead of opening a new map, the action bar option should just finish current activity
     */
    Boolean shouldGoBackToPreviousActivity = false;
    SharedPreferences prefs;

    SpotsListViewModel spotsListViewModel;
    MyRoutesViewModel myRoutesViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Important: setHasOptionsMenu must be called so that onOptionsItemSelected works
        setHasOptionsMenu(true);

        updateTitle(getString(R.string.main_activity_title));

        prefs = requireContext().getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        spotsListViewModel = new ViewModelProvider(requireActivity()).get(SpotsListViewModel.class);
        myRoutesViewModel = new ViewModelProvider(requireActivity()).get(MyRoutesViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.my_routes_activity_layout, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //savedInstanceState will be not null when a screen is rotated, for example. But will be null when activity is first created
        if (savedInstanceState != null && savedInstanceState.keySet().contains(LAST_TAB_OPENED_KEY))
            indexOfLastOpenTab = savedInstanceState.getInt(LAST_TAB_OPENED_KEY, 0);

        isHandlingRequestToOpenSpotForm = false;

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        //Note: using getFragmentManager() instead of getChildFragmentManager() would cause undesired behavior when navigating back.
        //For a little explanation see https://stackoverflow.com/a/27950670/1094261
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        // clear listeners first avoid adding duplicate listener upon calling notify update related code
        binding.container.clearOnPageChangeListeners();
        // Set up the ViewPager with the sections adapter.
        binding.container.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                requireActivity().invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        binding.tabs.setupWithViewPager(binding.container);
        binding.container.setAdapter(mSectionsPagerAdapter);

        spotsListListener = new ListListener() {
            @Override
            public void onListOfSelectedSpotsChanged() {
                showSpotDeletedSnackbar();
                requireActivity().invalidateOptionsMenu();
                prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, true).apply();
            }

            @Override
            public void onSpotClicked(Spot spot) {
                indexOfLastOpenTab = binding.container.getCurrentItem();
                //onSaveInstanceState will be executed right after onSpotClicked because when a spot is clicked, the fragment starts SpotFormActivity
            }
        };

        spotsListViewModel.getSpots().observe(requireActivity(), this::notifySpotListChanged);
    }

    private void notifySpotListChanged(List<Spot> spotList) {
        myRoutesViewModel.notifySpotListChanged(spotList);
    }

    public void selectTab(int tab_index) {
        binding.container.setCurrentItem(tab_index);
    }

    void showSnackbar(@NonNull CharSequence text, CharSequence action, View.OnClickListener listener) {
        snackbar = Snackbar.make(binding.coordinatorLayout, text.toString().toUpperCase(), Snackbar.LENGTH_LONG)
                .setAction(action, listener);

        // get snackbar view
        View snackbarView = snackbar.getView();

        // set action button color
        snackbar.setActionTextColor(Color.BLACK);

        // change snackbar text color
        int snackbarTextId = com.google.android.material.R.id.snackbar_text;
        TextView textView = (TextView) snackbarView.findViewById(snackbarTextId);
        if (textView != null) textView.setTextColor(Color.WHITE);

        // change snackbar background
        snackbarView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ic_regular_spot_color));

        snackbar.show();
    }

    void showSpotSavedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_saved_successfuly),
                String.format(getString(R.string.action_button_label), getString(R.string.view_map_button_label)), v -> {
                    if (shouldGoBackToPreviousActivity)
                        navigateUp();
                    else
                        navigateToMyMap();
                });
    }

    void showSpotDeletedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_deleted_successfuly),
                null, null);
    }

    public void updateTitle(String title) {
        ActionBar actionBar = ((MainActivity) requireActivity()).getSupportActionBar();
        if (actionBar == null)
            return;

        actionBar.setTitle(title);
    }

    private void navigateUp() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp();
    }

    public void navigateToMyMap() {
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.nav_my_map);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (snackbar != null)
            snackbar.dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.container.clearOnPageChangeListeners();
        spotsListViewModel.getSpots().removeObserver(this::notifySpotListChanged);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /*boolean spotListWasChanged = false;
        if (resultCode == Constants.RESULT_OBJECT_ADDED || resultCode == Constants.RESULT_OBJECT_EDITED) {
            spotListWasChanged = true;
            showSpotSavedSnackbar();
        } else if (resultCode == Constants.RESULT_OBJECT_DELETED) {
            spotListWasChanged = true;
            showSpotDeletedSnackbar();
        }

        isHandlingRequestToOpenSpotForm = false;

        if (mSectionsPagerAdapter != null)
            mSectionsPagerAdapter.onActivityResultFromSpotForm();

        if (spotListWasChanged) {
            viewModel.reloadSpots(requireContext());

            //Set this flag so that MainActivity knows that reloadSpots should be called to update their viewModel.
            prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, true).apply();
        }*/
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.my_routes_menu, menu);

        MenuItem item = menu.findItem(R.id.action_select_all);

        boolean isEditModeOn = mSectionsPagerAdapter.getIsEditMode(binding.container.getCurrentItem());
        item.setVisible(isEditModeOn);

        if (isEditModeOn) {
            String itemTitle = getString(R.string.general_select_all);
            if (mSectionsPagerAdapter.getIsAllSpotsSelected(binding.container.getCurrentItem()))
                itemTitle = getString(R.string.general_deselect_all);
            item.setTitle(itemTitle);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_new_spot:
                if (!isHandlingRequestToOpenSpotForm)
                    saveSpotButtonHandler();
                break;
            case R.id.action_select_all:
                if (mSectionsPagerAdapter != null) {
                    if (mSectionsPagerAdapter.getIsAllSpotsSelected(binding.container.getCurrentItem())) {
                        mSectionsPagerAdapter.deselectAllSpots(binding.container.getCurrentItem());
                    } else {
                        mSectionsPagerAdapter.selectAllSpots(binding.container.getCurrentItem());
                    }
                }
                break;
            case R.id.action_edit_list:
                if (mSectionsPagerAdapter != null) {
                    switch (binding.container.getCurrentItem()) {
                        case SectionsPagerAdapter.TAB_ROUTES_INDEX:
                            mSectionsPagerAdapter.toggleRoutesListEditMode();
                            break;
                        case SectionsPagerAdapter.TAB_SPOTS_INDEX:
                            mSectionsPagerAdapter.toggleSpotsListEditMode();
                            break;
                    }

                    //Call invalidateOptionsMenu so that it fires onCreateOptionsMenu and "Select all" option will be displayed.
                    requireActivity().invalidateOptionsMenu();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        indexOfLastOpenTab = binding.container.getCurrentItem();
        savedInstanceState.putInt(LAST_TAB_OPENED_KEY, indexOfLastOpenTab);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private SpotListFragment tab_route_spots_list;
        private SpotListFragment tab_single_spots_list;

        public SectionsPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        //Called before instantiateItem(..)
        @NonNull
        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == TAB_SPOTS_INDEX) {
                SpotListFragment singleSpotsListFrag = new SpotListFragment();
                Bundle args2 = new Bundle();
                singleSpotsListFrag.setArguments(args2);
                singleSpotsListFrag.subscribeTo(SpotListFragment.MyRoutesSpotsType.SINGLESPOTS);
                singleSpotsListFrag.setOnOneOrMoreSpotsDeleted(spotsListListener);
                return singleSpotsListFrag;
            }

            SpotListFragment listFrag = new SpotListFragment();
            Bundle args1 = new Bundle();
            listFrag.setArguments(args1);
            listFrag.subscribeTo(SpotListFragment.MyRoutesSpotsType.ROUTESPOTS);
            listFrag.setOnOneOrMoreSpotsDeleted(spotsListListener);
            return listFrag;
        }

       /* // Here we can finally safely save a reference to the created
        // Fragment, no matter where it came from (either getItem() or
        // FragmentManger). Simply save the returned Fragment from
        // super.instantiateItem() into an appropriate reference depending
        // on the ViewPager position. This solution was copied from:
        // http://stackoverflow.com/a/29288093/1094261
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Crashlytics.log(Log.INFO, TAG, "SectionsPagerAdapter.instantiateItem called for position " + position);

            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            // save the appropriate reference depending on position
            switch (position) {
                case TAB_ROUTES_INDEX:
                    SpotListFragment tab_list = (SpotListFragment) createdFragment;
                    tab_list.subscribeTo(SpotListFragment.MyRoutesSpotsType.ROUTESPOTS);
                    this.tab_route_spots_list.setOnOneOrMoreSpotsDeleted(spotsListListener);
                    this.tab_route_spots_list = tab_list;
                    break;
                case TAB_SPOTS_INDEX:
                    SpotListFragment tab_single_spots_list = (SpotListFragment) createdFragment;
                    tab_single_spots_list.subscribeTo(SpotListFragment.MyRoutesSpotsType.SINGLESPOTS);
                    this.tab_single_spots_list.setOnOneOrMoreSpotsDeleted(spotsListListener);
                    this.tab_single_spots_list = tab_single_spots_list;
                    break;
            }

            return createdFragment;
        }*/

        @Override
        public int getCount() {
            return 2;
        }

        public final static int TAB_ROUTES_INDEX = 0;
        public final static int TAB_SPOTS_INDEX = 1;

        @Override
        public CharSequence getPageTitle(int position) {
            CharSequence res = null;
            switch (position) {
                case TAB_ROUTES_INDEX:
                    res = getString(R.string.main_activity_list_tab);
                    break;
                case TAB_SPOTS_INDEX:
                    res = getString(R.string.main_activity_single_spots_list_tab);
                    break;
            }
            return res;
        }

        public void setValues(List<Spot> lst) {
            /*Crashlytics.log(Log.INFO, TAG, "SectionsPagerAdapter.setValues called");
            try {
                routeSpots = new ArrayList<>();
                singleSpots = new ArrayList<>();
                for (Spot s : lst)
                    if (s.getIsPartOfARoute() == null || !s.getIsPartOfARoute())
                        singleSpots.add(s);
                    else
                        routeSpots.add(s);
            } catch (Exception ex) {
                Crashlytics.logException(ex);
            }

            try {
                if (tab_route_spots_list != null)
                    tab_route_spots_list.setValues(routeSpots);

                if (tab_single_spots_list != null)
                    tab_single_spots_list.setValues(singleSpots);
            } catch (Exception ex) {
                Crashlytics.logException(ex);
            }*/
        }

        public void toggleRoutesListEditMode() {
            if (tab_route_spots_list != null)
                tab_route_spots_list.setIsEditMode(!tab_route_spots_list.getIsEditMode());
            requireActivity().invalidateOptionsMenu();
        }

        public void toggleSpotsListEditMode() {
            if (tab_single_spots_list != null)
                tab_single_spots_list.setIsEditMode(!tab_single_spots_list.getIsEditMode());
            requireActivity().invalidateOptionsMenu();
        }

        /**
         * Selects all spots on the list.
         *
         * @param tabPosition index of the tab which all list items should be selected.
         **/
        private void selectAllSpots(int tabPosition) {
            switch (tabPosition) {
                case TAB_ROUTES_INDEX:
                    if (tab_route_spots_list != null)
                        tab_route_spots_list.selectAllSpots();
                    break;
                case TAB_SPOTS_INDEX:
                    if (tab_single_spots_list != null)
                        tab_single_spots_list.selectAllSpots();
                    break;
            }
        }

        private boolean getIsAllSpotsSelected(int tabPosition) {
            switch (tabPosition) {
                case TAB_ROUTES_INDEX:
                    if (tab_route_spots_list != null)
                        return tab_route_spots_list.getIsAllSpotsSelected();
                    break;
                case TAB_SPOTS_INDEX:
                    if (tab_single_spots_list != null)
                        return tab_single_spots_list.getIsAllSpotsSelected();
                    break;
            }
            return false;
        }

        /**
         * Deselects all spots on the list.
         *
         * @param tabPosition index of the tab which all list items should be deselected.
         **/
        private void deselectAllSpots(int tabPosition) {
            switch (tabPosition) {
                case TAB_ROUTES_INDEX:
                    if (tab_route_spots_list != null)
                        tab_route_spots_list.deselectAllSpots();
                    break;
                case TAB_SPOTS_INDEX:
                    if (tab_single_spots_list != null)
                        tab_single_spots_list.deselectAllSpots();
                    break;
            }
        }

        private boolean getIsEditMode(int tabPosition) {
            switch (tabPosition) {
                case TAB_ROUTES_INDEX:
                    if (tab_route_spots_list != null)
                        return tab_route_spots_list.getIsEditMode();
                    break;
                case TAB_SPOTS_INDEX:
                    if (tab_single_spots_list != null)
                        return tab_single_spots_list.getIsEditMode();
                    break;
            }
            return false;
        }
    }

    private void saveSpotButtonHandler() {
        isHandlingRequestToOpenSpotForm = true;
        ((MainActivity) requireActivity()).navigateToCreateOrEditSpotForm(null, Constants.KEEP_ZOOM_LEVEL, false);
    }
}

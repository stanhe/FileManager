package dev.dworks.apps.anexplorer.fragment;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileNotFoundException;

import dev.dworks.apps.anexplorer.BaseActivity;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.IntentUtils;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_BROWSE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_CREATE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_GET_CONTENT;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_OPEN;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_OPEN_TREE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_GRID;
import static dev.dworks.apps.anexplorer.BaseActivity.State.MODE_LIST;
import static dev.dworks.apps.anexplorer.fragment.DirectoryFragment.ANIM_SIDE;
import static dev.dworks.apps.anexplorer.misc.AnalyticsManager.FILE_TYPE;

/**
 * Created by uu on 2018/3/17.
 */

public class NewHomeFragment extends Fragment {
    public static final String TAG = "NewHomeFragment";
    private static final String EXTRA_STATE = "state";
    private static final String EXTRA_AUTHENTICATED = "authenticated";
    private static final String EXTRA_ACTIONMODE = "actionmode";
    private static final String EXTRA_SEARCH_STATE = "searchsate";
    private Button open,write;
    private TextView otgDevice,message;
    private RootsCache roots;
    private RootInfo mHomeRoot,mParentRoot;
    private BaseActivity.State mState;
    private RootsCache mRoots;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_otg, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle icicle) {
        super.onViewCreated(view, icicle);
        mRoots = DocumentsApplication.getRootsCache(getActivity());

        if (icicle != null) {
            mState = icicle.getParcelable(EXTRA_STATE);
        } else {
            buildDefaultState();
        }
        initView(view);
        initData();
    }

    private void buildDefaultState() {
        mState = new BaseActivity.State();

        final Intent intent = getActivity().getIntent();
        final String action = intent.getAction();
        if (IntentUtils.ACTION_OPEN_DOCUMENT.equals(action)) {
            mState.action = ACTION_OPEN;
        } else if (IntentUtils.ACTION_CREATE_DOCUMENT.equals(action)) {
            mState.action = ACTION_CREATE;
        } else if (IntentUtils.ACTION_GET_CONTENT.equals(action)) {
            mState.action = ACTION_GET_CONTENT;
        } else if (IntentUtils.ACTION_OPEN_DOCUMENT_TREE.equals(action)) {
            mState.action = ACTION_OPEN_TREE;
        } else if (DocumentsContract.ACTION_MANAGE_ROOT.equals(action)) {
            //mState.action = ACTION_MANAGE;
            mState.action = ACTION_BROWSE;
        } else{
            mState.action = ACTION_BROWSE;
        }

        if (mState.action == ACTION_OPEN || mState.action == ACTION_GET_CONTENT) {
            mState.allowMultiple = intent.getBooleanExtra(IntentUtils.EXTRA_ALLOW_MULTIPLE, false);
        }

        if (mState.action == ACTION_GET_CONTENT || mState.action == ACTION_BROWSE) {
            mState.acceptMimes = new String[] { "*/*" };
            mState.allowMultiple = true;
        }
        else if (intent.hasExtra(IntentUtils.EXTRA_MIME_TYPES)) {
            mState.acceptMimes = intent.getStringArrayExtra(IntentUtils.EXTRA_MIME_TYPES);
        } else {
            mState.acceptMimes = new String[] { intent.getType() };
        }

        mState.localOnly = intent.getBooleanExtra(IntentUtils.EXTRA_LOCAL_ONLY, true);
        mState.forceAdvanced = intent.getBooleanExtra(DocumentsContract.EXTRA_SHOW_ADVANCED	, false);
        mState.showAdvanced = mState.forceAdvanced
                | SettingsActivity.getDisplayAdvancedDevices(getActivity());

        mState.rootMode = SettingsActivity.getRootMode(getActivity());
    }

    private void initView(View view) {
        open = view.findViewById(R.id.open);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        message = view.findViewById(R.id.message);
        otgDevice = view.findViewById(R.id.otgDevice);
        write = view.findViewById(R.id.write);
        write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 2018/3/17
                createFile();
            }
        });
    }


    private void createFile() {
        CreateFileFragment.show(((DocumentsActivity) getActivity()).getSupportFragmentManager(), "text/plain", "File");
        Bundle params = new Bundle();
        params.putString(FILE_TYPE, "file");
        AnalyticsManager.logEvent("create_file", params);
    }

    private void initData() {
        roots = DocumentsApplication.getRootsCache(getActivity());
        mHomeRoot = roots.getHomeRoot();
        showData();
    }

    private void showData() {
        showOtherStorage();
    }

    private void showOtherStorage() {
        final RootInfo otgRoot = roots.getSecondaryRoot();
        if (null != otgRoot) {
            //message.setText("Find otg device : "+otgRoot.title+"\n");
            setInfo(otgRoot);
            open.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onRootPicked(otgRoot,mHomeRoot);
                }
            });
        }else{
            otgDevice.setText("null");
        }
    }

    public void onRootPicked(RootInfo root, RootInfo parentRoot) {
        mParentRoot = parentRoot;
        onRootPicked(root, true);
    }
    public void onRootPicked(RootInfo root, boolean closeDrawer) {

        if(null == root){
            return;
        }
        // Clear entire backstack and start in new root
        mState.stack.root = root;
        mState.stack.clear();
        mState.stackTouched = true;

        onCurrentDirectoryChanged(ANIM_SIDE);

    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void onCurrentDirectoryChanged(int anim) {
        Log.e("Stan", " ==== onCurrentDirectoryChanged ====");
        final FragmentManager fm = getFragmentManager();
        final RootInfo root = getCurrentRoot();
        DocumentInfo cwd = getCurrentDirectory();

        //TODO : this has to be done nicely
        if(cwd == null && (null != root && !root.isServerStorage())){
            final Uri uri = DocumentsContract.buildDocumentUri(
                    root.authority, root.documentId);
            DocumentInfo result;
            try {
                result = DocumentInfo.fromUri(getActivity().getContentResolver(), uri);
                if (result != null) {
                    mState.stack.push(result);
                    mState.stackTouched = true;
                    cwd = result;
                }
            } catch (FileNotFoundException e) {
                CrashReportingManager.logException(e);
            }
        }
        if(!SettingsActivity.getFolderAnimation(getActivity())){
            anim = 0;
        }
        if (cwd == null) {
            // No directory means recents
            if (mState.action == ACTION_CREATE || mState.action == ACTION_OPEN_TREE) {
                RecentsCreateFragment.show(fm);
            } else {
                if(root.isHome()){
                    Log.e("Stan","show newHome");
                    //HomeFragment.show(fm);
                    NewHomeFragment.show(fm);
                }
                else if(root.isConnections()){
                    ConnectionsFragment.show(fm);
                } else if(root.isServerStorage()){
                    ServerFragment.show(fm, root);
                } else {
                    DirectoryFragment.showRecentsOpen(fm, anim);

                    // Start recents in grid when requesting visual things
                    final boolean visualMimes = true;//MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, mState.acceptMimes);
                    mState.userMode = visualMimes ? MODE_GRID : MODE_LIST;
                    mState.derivedMode = mState.userMode;
                }
            }
        } else {
            {
                // TODO: 2018/3/17
                // Normal boring directory
                DirectoryFragment.showNormal(fm, root, cwd, anim);
            }
        }

        // Forget any replacement target
        if (mState.action == ACTION_CREATE) {
            final SaveFragment save = SaveFragment.get(fm);
            if (save != null) {
                save.setReplaceTarget(null);
            }
        }

        if (mState.action == ACTION_OPEN_TREE) {
            final PickFragment pick = PickFragment.get(fm);
            if (pick != null) {
                final CharSequence displayName = (mState.stack.size() <= 1) && null != root
                        ? root.title : cwd.displayName;
                pick.setPickTarget(cwd, displayName);
            }
        }

        final MoveFragment move = MoveFragment.get(fm);
        if (move != null) {
            move.setReplaceTarget(cwd);
        }

        final RootsFragment roots = RootsFragment.get(fm);
        if (roots != null) {
            roots.onCurrentRootChanged();
        }
        dumpStack();
    }

    private void dumpStack() {
        Log.d(TAG, "Current stack: ");
        Log.d(TAG, " * " + mState.stack.root);
        for (DocumentInfo doc : mState.stack) {
            Log.d(TAG, " +-- " + doc);
        }
    }
    public DocumentInfo getCurrentDirectory() {
        return mState.stack.peek();
    }

    public RootInfo getCurrentRoot() {
        if (mState.stack.root != null) {
            return mState.stack.root;
        } else {
            return mState.action == ACTION_BROWSE ? mRoots.getDefaultRoot() : mRoots.getStorageRoot();
        }
    }

    public void setInfo(RootInfo root) {
        otgDevice.setText(root.title);
        // Show available space if no summary
        String summaryText = root.summary;
        if (TextUtils.isEmpty(summaryText) && root.availableBytes >= 0) {
            summaryText = getActivity().getString(R.string.root_available_bytes,
                    Formatter.formatFileSize(getActivity(), root.availableBytes));
            otgDevice.append(" "+summaryText);
        }
    }


    public static void show(FragmentManager fm) {
        final NewHomeFragment fragment = new NewHomeFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }
    public static NewHomeFragment get(FragmentManager fm) {
        return (NewHomeFragment) fm.findFragmentByTag(TAG);
    }

    public void reloadData(){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showData();
            }
        }, 500);
    }

}

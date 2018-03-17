package dev.dworks.apps.anexplorer;

import android.app.LoaderManager;
import android.content.Context;

import android.content.Intent;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Environment;

import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Collection;
import java.util.List;

import dev.dworks.apps.anexplorer.adapter.RootsExpandableAdapter;
import dev.dworks.apps.anexplorer.loader.RootsLoader;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.misc.SystemBarTintManager;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentStack;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

/**
 * Created by uu on 2018/3/17.
 */

public class WriteOTGActivity extends BaseActivity{

    EditText etName;
    TextView message;
    private RootsCache mRoots;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Document);
        if(Utils.hasLollipop()){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        else if(Utils.hasKitKat()){
            setTheme(R.style.Theme_Document_Translucent);
        }
        setUpStatusBar();
        super.onCreate(savedInstanceState);
        mRoots = DocumentsApplication.getRootsCache(this);
        setContentView(R.layout.activity_otg);
        initView();
        new LoaderManager.LoaderCallbacks<Collection<RootInfo>>() {
            @Override
            public Loader<Collection<RootInfo>> onCreateLoader(int id, Bundle args) {
                return new RootsLoader(WriteOTGActivity.this, mRoots, getDisplayState());
            }

            @Override
            public void onLoadFinished(Loader<Collection<RootInfo>> loader, Collection<RootInfo> result) {
                Log.e("stan","==== onLoadFinished ===");
               /* final Intent includeApps = getArguments().getParcelable(EXTRA_INCLUDE_APPS);
                if (mAdapter == null) {
                    mAdapter = new RootsExpandableAdapter(context, result, includeApps);
                    Parcelable state = mList.onSaveInstanceState();
                    mList.setAdapter(mAdapter);
                    mList.onRestoreInstanceState(state);
                } else {
                    mAdapter.setData(result);
                }

                int groupCount = mAdapter.getGroupCount();
                if(group_size != 0 && group_size == groupCount){
                    if (expandedIds != null) {
                        restoreExpandedState(expandedIds);
                    }
                } else {
                    group_size = groupCount;
                    for (int i = 0; i < group_size; i++) {
                        mList.expandGroup(i);
                    }
                    expandedIds = getExpandedIds();
                    mList.setOnGroupExpandListener(mOnGroupExpandListener);
                    mList.setOnGroupCollapseListener(mOnGroupCollapseListener);
                }*/
            }

            @Override
            public void onLoaderReset(Loader<Collection<RootInfo>> loader) {
                Log.e("stan","==== onLoaderReset ===");
              /*  mAdapter = null;
                mList.setAdapter((RootsExpandableAdapter)null);*/
            }
        };
    }

    private void initView() {
        etName = findViewById(R.id.name);
        message = findViewById(R.id.message);
        message.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                message.setText("");
                return false;
            }
        });
    }

    private void ctxt(String messages) {
        message.append(messages);
    }

    @Override
    public String getTag() {
        return null;
    }

    @Override
    public State getDisplayState() {
        return null;
    }

    @Override
    public RootInfo getCurrentRoot() {
        return null;
    }

    @Override
    public void onStateChanged() {

    }

    @Override
    public void setRootsDrawerOpen(boolean open) {

    }

    @Override
    public void onDocumentPicked(DocumentInfo doc) {

    }

    @Override
    public void onDocumentsPicked(List<DocumentInfo> docs) {

    }

    @Override
    public DocumentInfo getCurrentDirectory() {
        return null;
    }

    @Override
    public void setPending(boolean pending) {

    }

    @Override
    public void onStackPicked(DocumentStack stack) {

    }

    @Override
    public void onPickRequested(DocumentInfo pickTarget) {

    }

    @Override
    public void onAppPicked(ResolveInfo info) {

    }

    @Override
    public void onRootPicked(RootInfo root, boolean closeDrawer) {

    }

    @Override
    public void onSaveRequested(DocumentInfo replaceTarget) {

    }

    @Override
    public void onSaveRequested(String mimeType, String displayName) {

    }

    @Override
    public boolean isCreateSupported() {
        return false;
    }

    @Override
    public RootInfo getDownloadRoot() {
        return null;
    }

    @Override
    public boolean getActionMode() {
        return false;
    }

    @Override
    public void setActionMode(boolean actionMode) {

    }

    @Override
    public void setUpStatusBar() {
        int color = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor(this));
        if(Utils.hasLollipop()){
            getWindow().setStatusBarColor(color);
        }
        else if(Utils.hasKitKat()){
            SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
            systemBarTintManager.setTintColor(color);
            systemBarTintManager.setStatusBarTintEnabled(true);
        }
    }

    @Override
    public void setUpDefaultStatusBar() {

    }

    @Override
    public boolean isShowAsDialog() {
        return false;
    }

    @Override
    public void upadateActionItems(AbsListView mCurrentView) {

    }

    @Override
    public void setInfoDrawerOpen(boolean open) {

    }

    @Override
    public void again() {

    }

    public void writeOtg(View view) {
        createNewFile();
    }
    public void createNewFile() {
        String text = etName.getText().toString();
        etName.setText("");
        message.append(System.currentTimeMillis()/1000 + " Creating new file with text " + text + "\n");
        String internalStorage = Environment.getExternalStorageDirectory().getAbsolutePath() + "/usbCommunication";
        File root = new File(internalStorage);
        if (!root.exists()) {
            root.mkdirs();
        }
        String fileName = "myCreatedFile.txt";
        File myNewFile = new File(internalStorage + "/" + fileName);
        try {
            message.append(System.currentTimeMillis()/1000 + " Creating file " + fileName + " in internal storage\n");
            FileWriter fileWriter = new FileWriter(myNewFile);
            fileWriter.write(text);
            fileWriter.flush();
            fileWriter.close();
            message.append(System.currentTimeMillis()/1000 + " Successfully created file " + myNewFile.getAbsolutePath() + "\n");
        } catch (IOException e) {
            message.append(System.currentTimeMillis()/1000 + " Couldn't create file: " + e.getMessage() + "\n");
        }
        copyFileToUsb(myNewFile);
    }

    private void copyFileToUsb(File myNewFile) {

    }
}

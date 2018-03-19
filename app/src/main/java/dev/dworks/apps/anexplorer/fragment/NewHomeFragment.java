package dev.dworks.apps.anexplorer.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.provider.DocumentFile;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import dev.dworks.apps.anexplorer.BaseActivity;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.AnalyticsManager;
import dev.dworks.apps.anexplorer.misc.AsyncTask;
import dev.dworks.apps.anexplorer.misc.ContentProviderClientCompat;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.IntentUtils;
import dev.dworks.apps.anexplorer.misc.ProviderExecutor;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.provider.RootedStorageProvider;
import dev.dworks.apps.anexplorer.provider.UsbStorageProvider;
import dev.dworks.apps.anexplorer.root.RootCommands;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_BROWSE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_CREATE;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_GET_CONTENT;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_OPEN;
import static dev.dworks.apps.anexplorer.BaseActivity.State.ACTION_OPEN_TREE;

/**
 * Created by uu on 2018/3/17.
 */

public class NewHomeFragment extends Fragment {
    public static final String TAG = "NewHomeFragment";
    private EditText etFileName,etContent;
    private Button open,write,create;
    private TextView otgDevice,message;
    private RootsCache roots;
    private RootInfo mHomeRoot;
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
        buildDefaultState();
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

        mState.showAdvanced = mState.forceAdvanced | SettingsActivity.getDisplayAdvancedDevices(getActivity());

        mState.rootMode = SettingsActivity.getRootMode(getActivity());
    }

    private void initView(View view) {
        open = view.findViewById(R.id.open);
        message = view.findViewById(R.id.message);
        otgDevice = view.findViewById(R.id.otgDevice);
        write = view.findViewById(R.id.write);
        etFileName = view.findViewById(R.id.et_file_name);
        etContent = view.findViewById(R.id.et_content);
        create = view.findViewById(R.id.create);
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
            setInfo(otgRoot);
            open.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openRoot(otgRoot);
                }
            });
            write.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mState.stack.root = otgRoot;
                    mState.stack.clear();
                    mState.stackTouched = true;
                    final RootInfo root = getCurrentRoot();
                    DocumentInfo cwd = getCurrentDirectory();
                    if(cwd == null && (null != root && !root.isServerStorage())){
                        final Uri uri = DocumentsContract.buildDocumentUri(root.authority, root.documentId);
                        DocumentInfo result;
                        try {
                            result = DocumentInfo.fromUri(getActivity().getContentResolver(), uri);
                            if (result != null) {
                                mState.stack.push(result);
                                mState.stackTouched = true;
                                cwd = result;
                                createFile(root,cwd);
                            }
                        } catch (FileNotFoundException e) {
                            CrashReportingManager.logException(e);
                        }
                    }

                }
            });
        }else{
            reset();
        }
    }

    private void openRoot(RootInfo rootInfo){
        DocumentsActivity activity = ((DocumentsActivity)getActivity());
        activity.onRootPicked(rootInfo, mHomeRoot);
        AnalyticsManager.logEvent("open_shortcuts", rootInfo ,new Bundle());
    }
    private void reset() {
        otgDevice.setText("null");
        message.setText("");
        open.setOnClickListener(null);
        write.setOnClickListener(null);
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

    public void createFile(RootInfo root, DocumentInfo cwd) {
        message.append(System.currentTimeMillis()/1000 + "      check root : "+cwd.path+" \n");
        message.append(System.currentTimeMillis()/1000 + "      get root documentId : "+cwd.documentId+" \n");
        Log.i("Stan", "root : " + root.title);
        Log.i("Stan", "cwd path : " + cwd.path);
        Log.i("Stan","cwd .documentId" +cwd.documentId);
        new CreateFileTask(getActivity(), cwd,"text/plain", TextUtils.isEmpty(etFileName.getText().toString())?"TestOtg":etFileName.getText().toString()).executeOnExecutor(ProviderExecutor.forAuthority(cwd.authority));
    }
    private class CreateFileTask extends AsyncTask<Void, Void, Uri> {
        private final Activity mActivity;
        private final DocumentInfo mCwd;
        private final String mMimeType;
        private final String mDisplayName;
        public CreateFileTask(Activity activity,
                              DocumentInfo cwd,
                              String mimeType,
                              String displayName) {
            mActivity = activity;
            mCwd = cwd;
            mMimeType = mimeType;
            mDisplayName = displayName;
        }

        @Override
        protected void onPreExecute() {
            message.append(System.currentTimeMillis()/1000 + "      prepare for create file! \n");
        }

        @Override
        protected Uri doInBackground(Void... params) {
            final ContentResolver resolver = mActivity.getContentResolver();
            ContentProviderClient client = null;
            Uri childUri = null;
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, mCwd.derivedUri.getAuthority());
                childUri = DocumentsContract.createDocument(
                        resolver, mCwd.derivedUri, mMimeType, mDisplayName);
            } catch (Exception e) {
                Log.w(DocumentsActivity.TAG, "Failed to create document", e);
                CrashReportingManager.logException(e);
            } finally {
                ContentProviderClientCompat.releaseQuietly(client);
            }
            return childUri;
        }

        @Override
        protected void onPostExecute(Uri result) {
            message.append(System.currentTimeMillis()/1000 + "      file "+mDisplayName+" created! \n\n");
            //message.append(" file Uri :  "+result+" \n");
            Uri newUri = Uri.parse(result.toString().replace("%20(1)",""));
            //message.append(" file Uri :  "+newUri+" \n");
            new SaveContent(newUri).execute();
        }
    }

    private class SaveContent extends AsyncTask<Void, Void, Void>{
        private Uri uri;
        private String errorMsg;
        private String content;

        SaveContent(Uri uri){
            this.uri = uri;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (TextUtils.isEmpty(etContent.getText().toString())) {
                content = " ";
            }else {
                content = etContent.getText().toString();
            }
            message.append(System.currentTimeMillis()/1000 + "      write content! \n");
        }

        @Override
        protected Void doInBackground(Void... params) {
            String authority = uri.getAuthority();
            if(authority.equalsIgnoreCase(RootedStorageProvider.AUTHORITY)){
                InputStream is = RootCommands.putFile(getPath(uri), content);
                if(null == is){
                    Log.e("Stan", "is == null ");
                    errorMsg = "Unable to save file";
                }
                return null;
            } else if(authority.equalsIgnoreCase(UsbStorageProvider.AUTHORITY)){
                final ContentProviderClient usbclient = ContentProviderClientCompat.acquireUnstableContentProviderClient(getActivity().getContentResolver(), UsbStorageProvider.AUTHORITY);
                String docId = DocumentsContract.getDocumentId(uri);
                try {
                    UsbFile file = ((UsbStorageProvider) usbclient.getLocalContentProvider()).getFileForDocId(docId);
                    UsbFileOutputStream ufos = new UsbFileOutputStream(file);
                    ufos.write(content.getBytes("UTF-8"));
                    ufos.close();
                } catch (IOException e) {
                    errorMsg = e.getLocalizedMessage();
                    CrashReportingManager.logException(e);
                }
                return null;
            } else {
                OutputStream os = getOutStream(uri);
                if (null == os) {
                    Log.e("Stan", "os == null ");
                    errorMsg = "Unable to save file";
                    return null;
                }
                try {
                    os.write(content.getBytes("UTF-8"));
                    os.close();
                } catch (IOException e) {
                    errorMsg = e.getLocalizedMessage();
                    CrashReportingManager.logException(e);
                }
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(!TextUtils.isEmpty(errorMsg)){
                message.append(System.currentTimeMillis()/1000 + "      "+errorMsg+"\n");
            }else {
                message.append(System.currentTimeMillis()/1000 + "      write content success! \n\n");
            }
        }
    }

    private  String getPath(Uri uri){
        String path = uri.getSchemeSpecificPart();
        String part = uri.getSchemeSpecificPart();
        final int splitIndex = part.indexOf(':', 1);
        if(splitIndex != -1) {
            path = part.substring(splitIndex + 1);
        }
        return path;
    }

    private OutputStream getOutStream(Uri uri){
        Log.e("Stan","uri: "+uri);
        String scheme = uri.getScheme();
        if (scheme.startsWith(ContentResolver.SCHEME_CONTENT)) {
            try {
                DocumentFile documentFile = DocumentsApplication.getSAFManager(getActivity().getApplicationContext()).getDocumentFile(uri);
                Uri finalUri = null == documentFile ? uri : documentFile.getUri();
                return getActivity().getContentResolver().openOutputStream(finalUri);
            } catch (Exception e) {
                CrashReportingManager.logException(e);
            }
        } else if (scheme.startsWith(ContentResolver.SCHEME_FILE)) {
            File f = new File(uri.getPath());
            if (f.exists()) {
                try {
                    return new FileOutputStream(f);
                } catch (Exception e) {
                    CrashReportingManager.logException(e);
                }
            }
        }
        return null;
    }
}

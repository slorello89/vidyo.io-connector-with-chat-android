/**
 {file:
 {name: MainActivity.java}
 {description: .}
 {copyright:
 (c) 2016-2018 Vidyo, Inc.,
 433 Hackensack Avenue, 7th Floor,
 Hackensack, NJ  07601.

 All rights reserved.

 The information contained herein is proprietary to Vidyo, Inc.
 and shall not be reproduced, copied (in whole or in part), adapted,
 modified, disseminated, transmitted, transcribed, stored in a retrieval
 system, or translated into any language in any form by any means
 without the express written consent of Vidyo, Inc.
 ***** CONFIDENTIAL *****
 }
 }
 */
package com.vidyo.vidyoconnector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ToggleButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Device.Device;
import com.vidyo.VidyoClient.Device.LocalCamera;
import com.vidyo.VidyoClient.Endpoint.LogRecord;
import com.vidyo.VidyoClient.NetworkInterface;
import com.vidyo.VidyoClient.Endpoint.Participant;
import android.support.v4.app.Fragment;

public class CallFragment extends Fragment implements
        View.OnClickListener,
        Connector.IConnect,
        Connector.IRegisterLogEventListener,
        Connector.IRegisterNetworkInterfaceEventListener,
        Connector.IRegisterLocalCameraEventListener,
        Connector.IRegisterParticipantEventListener,
        IVideoFrameListener, Connector.IRegisterMessageEventListener {

    // Define the various states of this application.
    enum VidyoConnectorState {
        Connecting,
        Connected,
        Disconnecting,
        Disconnected,
        DisconnectedUnexpected,
        Failure,
        FailureInvalidResource
    }

    // Map the application state to the status to display in the toolbar.
    private static final Map<VidyoConnectorState, String> mStateDescription = new HashMap<VidyoConnectorState, String>() {{
        put(VidyoConnectorState.Connecting, "Connecting...");
        put(VidyoConnectorState.Connected, "Connected");
        put(VidyoConnectorState.Disconnecting, "Disconnecting...");
        put(VidyoConnectorState.Disconnected, "Disconnected");
        put(VidyoConnectorState.DisconnectedUnexpected, "Unexpected disconnection");
        put(VidyoConnectorState.Failure, "Connection failed");
        put(VidyoConnectorState.FailureInvalidResource, "Invalid Resource ID");
    }};

    // Helps check whether app has permission to access what is declared in its manifest.
    // - Permissions from app's manifest that have a "protection level" of "dangerous".
    private static final String[] mPermissions = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };
    // - This arbitrary, app-internal constant represents a group of requested permissions.
    // - For simplicity, this app treats all desired permissions as part of a single group.
    private final int PERMISSIONS_REQUEST_ALL = 1988;

    private VidyoConnectorState mVidyoConnectorState = VidyoConnectorState.Disconnected;
    private boolean mVidyoClientInitialized = false;
    private Logger mLogger = Logger.getInstance();
    private Connector mVidyoConnector = null;
    private LocalCamera mLastSelectedCamera = null;
    private ToggleButton mToggleConnectButton;
    private ToggleButton mMicrophonePrivacyButton;
    private ToggleButton mCameraPrivacyButton;
    private ProgressBar mConnectionSpinner;
    private LinearLayout mControlsLayout;
    private LinearLayout mToolbarLayout;
    private EditText mHost;
    private EditText mDisplayName;
    private EditText mToken;
    private EditText mResourceId;
    private TextView mToolbarStatus;
    private TextView mClientVersion;
    private VideoFrameLayout mVideoFrame;
    private boolean mHideConfig = false;
    private boolean mAutoJoin = false;
    private boolean mAllowReconnect = true;
    private boolean mCameraPrivacy = false;
    private boolean mMicrophonePrivacy = false;
    private boolean mEnableDebug = false;
    private String mReturnURL = null;
    private String mExperimentalOptions = null;
    private boolean mRefreshSettings = true;
    private boolean mDevicesSelected = true;
    private boolean mVidyoCloudJoin = false;
    private String mPortal;  // Used for VidyoCloud systems, not Vidyo.io
    private String mRoomKey; // Used for VidyoCloud systems, not Vidyo.io
    private String mRoomPin; // Used for VidyoCloud systems, not Vidyo.io
    private static final List<ChatMessage> messages = new ArrayList<ChatMessage>();
    private HashMap<String, Participant> participants = new HashMap<String, Participant>();
    private boolean mVidyoConnectorInitalized = false;
    /*
     *  Operating System Events
     *  Operating System Events
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();
        Uri uri = intent.getData();
        // Inflate the layout for this fragment
        View V = inflater.inflate(R.layout.fragment_call, container, false);
        // Initialize the member variables
        mControlsLayout = (LinearLayout) V.findViewById(R.id.controlsLayout);
        mToolbarLayout = (LinearLayout) V.findViewById(R.id.toolbarLayout);
        mVideoFrame = (VideoFrameLayout) V.findViewById(R.id.videoFrame);
        mVideoFrame.Register(this);
        mHost = (EditText) V.findViewById(R.id.hostTextBox);
        mDisplayName = (EditText) V.findViewById(R.id.displayNameTextBox);
        mToken = (EditText) V.findViewById(R.id.tokenTextBox);
        mResourceId = (EditText) V.findViewById(R.id.resourceIdTextBox);
        mToolbarStatus = (TextView) V.findViewById(R.id.toolbarStatusText);
        mClientVersion = (TextView) V.findViewById(R.id.clientVersion);
        mConnectionSpinner = (ProgressBar) V.findViewById(R.id.connectionSpinner);

        // Set the onClick listeners for the buttons
        mToggleConnectButton = (ToggleButton) V.findViewById(R.id.connect);
        mToggleConnectButton.setOnClickListener(this);
        mMicrophonePrivacyButton = (ToggleButton) V.findViewById(R.id.microphone_privacy);
        mMicrophonePrivacyButton.setOnClickListener(this);
        mCameraPrivacyButton = (ToggleButton) V.findViewById(R.id.camera_privacy);
        mCameraPrivacyButton.setOnClickListener(this);
        ToggleButton button = (ToggleButton) V.findViewById(R.id.camera_switch);
        button.setOnClickListener(this);
        button = (ToggleButton) V.findViewById(R.id.toggle_debug);
        button.setOnClickListener(this);
        this.changeState(this.mVidyoConnectorState);


        if (!mVidyoConnectorInitalized){
            // Initialize the VidyoClient library - this should be done once in the lifetime of the application.
            if(!mVidyoClientInitialized){
                // Set the application's UI context to this activity.
                ConnectorPkg.setApplicationUIContext(getActivity());
                mVidyoClientInitialized = ConnectorPkg.initialize();
            }



            if (mVidyoClientInitialized) {
                // Construct Connector and register for events.
                try {
                    mLogger.Log("Constructing Connector");
                    mVidyoConnector = new Connector(mVideoFrame,
                            Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
                            15,
                            "info@VidyoClient info@VidyoConnector warning",
                            "",
                            0);

                    mVidyoConnector.setCpuTradeOffProfile(Connector.ConnectorTradeOffProfile.VIDYO_CONNECTORTRADEOFFPROFILE_High);
                    // Set the client version in the toolbar
                    mClientVersion.setText("VidyoClient-AndroidSDK " + mVidyoConnector.getVersion());

                    // Register for local camera events
                    if (!mVidyoConnector.registerLocalCameraEventListener(this)) {
                        mLogger.Log("registerLocalCameraEventListener failed");
                    }
                    // Register for network interface events
                    if (!mVidyoConnector.registerNetworkInterfaceEventListener(this)) {
                        mLogger.Log("registerNetworkInterfaceEventListener failed");
                    }
                    // Register for log events
                    if (!mVidyoConnector.registerLogEventListener(this, "info@VidyoClient info@VidyoConnector warning")) {
                        mLogger.Log("registerLogEventListener failed");
                    }
                    if (!mVidyoConnector.registerMessageEventListener(this)){
                        mLogger.Log("registerMessageEventListener Failed");
                    }
                    if (!mVidyoConnector.registerParticipantEventListener(this)){
                        mLogger.Log("registerParticipantEventListener failed");
                    }

                    // Beginning in Android 6.0 (API level 23), users grant permissions to an app while
                    // the app is running, not when they install the app. Check whether app has permission
                    // to access what is declared in its manifest.
                    if (Build.VERSION.SDK_INT > 22) {
                        List<String> permissionsNeeded = new ArrayList<>();
                        for (String permission : mPermissions) {
                            // Check if the permission has already been granted.
                            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED)
                                permissionsNeeded.add(permission);
                        }
                        if (permissionsNeeded.size() > 0) {
                            // Request any permissions which have not been granted. The result will be called back in onRequestPermissionsResult.
                            ActivityCompat.requestPermissions(getActivity(), permissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_ALL);
                        } else {
                            // Begin listening for video view size changes.
                            this.startVideoViewSizeListener();
                        }
                    } else {
                        // Begin listening for video view size changes.
                        this.startVideoViewSizeListener();
                    }
                    mVidyoConnectorInitalized = true;
                } catch (Exception e) {
                    mLogger.Log("Connector Construction failed");
                    mLogger.Log(e.getMessage());
                }
            }
            if (uri != null) {
                String param = uri.getQueryParameter("host");
                mHost.setText( param != null ? param : "prod.vidyo.io");

                param = uri.getQueryParameter("token");
                mToken.setText(param != null ? param : "");

                param = uri.getQueryParameter("displayName");
                mDisplayName.setText(param != null ? param : "Demo User");

                param = uri.getQueryParameter("resourceId");
                mResourceId.setText(param != null ? param : "");

                mReturnURL = uri.getQueryParameter("returnURL");
                mHideConfig = uri.getBooleanQueryParameter("hideConfig", false);
                mAutoJoin = uri.getBooleanQueryParameter("autoJoin", false);
                mAllowReconnect = uri.getBooleanQueryParameter("allowReconnect", true);
                mCameraPrivacy = uri.getBooleanQueryParameter("cameraPrivacy", false);
                mMicrophonePrivacy = uri.getBooleanQueryParameter("microphonePrivacy", false);
                mEnableDebug = uri.getBooleanQueryParameter("enableDebug", false);
                mExperimentalOptions = uri.getQueryParameter("experimentalOptions");

                ///////////////////////////////////////////////////////////////////////////////////////
                // Note: the following parameters are used to connect to VidyoCloud systems, not Vidyo.io.
                mVidyoCloudJoin = (uri.getHost() != null) && uri.getHost().equalsIgnoreCase("join");
                if (mVidyoCloudJoin) {
                    // Do not display the Vidyo.io form in VidyoCloud mode.
                    mHideConfig = true;

                    // Populate portal, roomKey, and roomPin
                    param = uri.getQueryParameter("portal");
                    mPortal = param != null ? param : "";
                    param = uri.getQueryParameter("roomKey");
                    mRoomKey = param != null ? param : "";
                    param = uri.getQueryParameter("roomPin");
                    mRoomPin = param != null ? param : "";
                }
                ///////////////////////////////////////////////////////////////////////////////////////
            } else {
                // If this app was launched by a different app, then get any parameters; otherwise use default settings.
                mHost.setText(intent.hasExtra("host") ? intent.getStringExtra("host") : "prod.vidyo.io");
                mToken.setText(intent.hasExtra("token") ? intent.getStringExtra("token") : "cHJvdmlzaW9uAFRhcmFzQDM3ZjBiZC52aWR5by5pbwA2MzczMTk0NDQ5NgAAMTZiOTY0MWZjMzQ3OTcxMmVjYjEyZjEwMGIwYjYyY2YxMzEyMzE2YmViN2RlOWIyYTZkMmIxMjAzY2Q4ZTllYzQ4ZjljMTIwYjU3MTRkMDYyMzc3Mzk3YjU0NjAxZjc3");
                mDisplayName.setText(intent.hasExtra("displayName") ? intent.getStringExtra("displayName") : "Android");
                mResourceId.setText(intent.hasExtra("resourceId") ? intent.getStringExtra("resourceId") : "DemoRoom");
                mReturnURL = intent.hasExtra("returnURL") ? intent.getStringExtra("returnURL") : null;
                mHideConfig = intent.getBooleanExtra("hideConfig", false);
                mAutoJoin = intent.getBooleanExtra("autoJoin", false);
                mAllowReconnect = intent.getBooleanExtra("allowReconnect", true);
                mCameraPrivacy = intent.getBooleanExtra("cameraPrivacy", false);
                mMicrophonePrivacy = intent.getBooleanExtra("microphonePrivacy", false);
                mEnableDebug = intent.getBooleanExtra("enableDebug", false);
                mExperimentalOptions = intent.hasExtra("experimentalOptions") ? intent.getStringExtra("experimentalOptions") : null;
                mVidyoCloudJoin = false;
            }
        }
        else{
            mVidyoConnector.assignViewToCompositeRenderer(mVideoFrame, Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default, 8);
            mVidyoConnector.showViewAt(mVideoFrame, 0, 0, mVideoFrame.getWidth(), mVideoFrame.getHeight());
        }
        return V;
    }


    /*
     * Private Utility Functions
     */

    // Callback containing the result of the permissions request. If permissions were not previously obtained,
    // wait until this is received until calling startVideoViewSizeListener where Connector is initially rendered.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mLogger.Log("onRequestPermissionsResult: number of requested permissions = " + permissions.length);

        // If the expected request code is received, begin rendering video.
        if (requestCode == PERMISSIONS_REQUEST_ALL) {
            for (int i = 0; i < permissions.length; ++i)
                mLogger.Log("permission: " + permissions[i] + " " + grantResults[i]);

            // Begin listening for video view size changes.
            this.startVideoViewSizeListener();
        } else {
            mLogger.Log("ERROR! Unexpected permission requested. Video will not be rendered.");
        }
    }

    // Listen for UI changes to the view where the video is rendered.
    private void startVideoViewSizeListener() {
        mLogger.Log("startVideoViewSizeListener");

        // Render the video each time that the video view (mVideoFrame) is resized. This will
        // occur upon activity creation, orientation changes, and when foregrounding the app.
        ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Specify the width/height of the view to render to.
                    mLogger.Log("showViewAt: width = " + mVideoFrame.getWidth() + ", height = " + mVideoFrame.getHeight());
                    mVidyoConnector.showViewAt(mVideoFrame, 0, 0, mVideoFrame.getWidth(), mVideoFrame.getHeight());
                }
            });
        } else {
            mLogger.Log("ERROR in startVideoViewSizeListener! Video will not be rendered.");
        }
    }

    // Apply some of the app settings
    private void applySettings() {
        if (mVidyoConnector != null) {
            // If enableDebug is configured then enable debugging
            if (mEnableDebug) {
                mVidyoConnector.enableDebug(7776, "warning info@VidyoClient info@VidyoConnector");
                mClientVersion.setVisibility(View.VISIBLE);
            } else {
                mVidyoConnector.disableDebug();
            }

            // If cameraPrivacy is configured then mute the camera
            mCameraPrivacyButton.setChecked(false); // reset state
            if (mCameraPrivacy) {
                mCameraPrivacyButton.performClick();
            }

            // If microphonePrivacy is configured then mute the microphone
            mMicrophonePrivacyButton.setChecked(false); // reset state
            if (mMicrophonePrivacy) {
                mMicrophonePrivacyButton.performClick();
            }

            // Set experimental options if any exist
            if (mExperimentalOptions != null) {
                ConnectorPkg.setExperimentalOptions(mExperimentalOptions);
            }

            // If configured to auto-join, then simulate a click of the toggle connect button
            if (mAutoJoin) {
                mToggleConnectButton.performClick();
            }
        }
    }

    // The state of the VidyoConnector connection changed, reconfigure the UI.
    // If connected, dismiss the controls layout
    private void changeState(VidyoConnectorState state) {
        mLogger.Log("changeState: " + state.toString());

        mVidyoConnectorState = state;

        // Execute this code on the main thread since it is updating the UI layout.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Set the status text in the toolbar.
                mToolbarStatus.setText(mStateDescription.get(mVidyoConnectorState));

                // Depending on the state, do a subset of the following:
                // - update the toggle connect button to either start call or end call image: mToggleConnectButton
                // - display toolbar in case it is hidden: mToolbarLayout
                // - show/hide the connection spinner: mConnectionSpinner
                // - show/hide the input form: mControlsLayout
                switch (mVidyoConnectorState) {
                    case Connecting:
                        mToggleConnectButton.setChecked(true);
                        mConnectionSpinner.setVisibility(View.VISIBLE);
                        break;

                    case Connected:
                        mToggleConnectButton.setChecked(true);
                        mControlsLayout.setVisibility(View.GONE);
                        mConnectionSpinner.setVisibility(View.INVISIBLE);

                        // Keep the device awake if connected.
                        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        break;

                    case Disconnecting:
                        // The button just switched to the callStart image.
                        // Change the button back to the callEnd image because do not want to assume that the Disconnect
                        // call will actually end the call. Need to wait for the callback to be received
                        // before swapping to the callStart image.
                        mToggleConnectButton.setChecked(true);
                        break;

                    case Disconnected:
                    case DisconnectedUnexpected:
                    case Failure:
                    case FailureInvalidResource:
                        mToggleConnectButton.setChecked(false);
                        mToolbarLayout.setVisibility(View.VISIBLE);
                        mConnectionSpinner.setVisibility(View.INVISIBLE);

                        // If a return URL was provided as an input parameter, then return to that application
                        if (mReturnURL != null) {
                            // Provide a callstate of either 0 or 1, depending on whether the call was successful
                            Intent returnApp = getActivity().getPackageManager().getLaunchIntentForPackage(mReturnURL);
                            returnApp.putExtra("callstate", (mVidyoConnectorState == VidyoConnectorState.Disconnected) ? 1 : 0);
                            startActivity(returnApp);
                        }

                        // If the allow-reconnect flag is set to false and a normal (non-failure) disconnect occurred,
                        // then disable the toggle connect button, in order to prevent reconnection.
                        if (!mAllowReconnect && (mVidyoConnectorState == VidyoConnectorState.Disconnected)) {
                            mToggleConnectButton.setEnabled(false);
                            mToolbarStatus.setText("Call ended");
                        }

                        if (!mHideConfig ) {
                            // Display the form.
                            mControlsLayout.setVisibility(View.VISIBLE);
                        }

                        // Allow the device to sleep if disconnected.
                        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        break;
                }
            }
        });
    }

    /*
     * Button Event Callbacks
     */

    @Override
    public void onClick(View v) {
        if (mVidyoConnector != null) {
            switch (v.getId()) {
                case R.id.connect:
                    // Connect or disconnect.
                    this.toggleConnect();
                    break;

                case R.id.camera_switch:
                    // Cycle the camera.
                    mVidyoConnector.cycleCamera();
                    break;

                case R.id.camera_privacy:
                    // Toggle the camera privacy.
                    mCameraPrivacy = mCameraPrivacyButton.isChecked();
                    mVidyoConnector.setCameraPrivacy(mCameraPrivacy);
                    break;

                case R.id.microphone_privacy:
                    // Toggle the microphone privacy.
                    mMicrophonePrivacy = mMicrophonePrivacyButton.isChecked();
                    mVidyoConnector.setMicrophonePrivacy(mMicrophonePrivacy);
                    break;

                case R.id.toggle_debug:
                    // Toggle debugging.
                    mEnableDebug = !mEnableDebug;
                    if (mEnableDebug) {
                        mVidyoConnector.enableDebug(7776, "warning info@VidyoClient info@VidyoConnector");
                        mClientVersion.setVisibility(View.VISIBLE);
                    } else {
                        mVidyoConnector.disableDebug();
                        mClientVersion.setVisibility(View.INVISIBLE);
                    }
                    break;

                default:
                    mLogger.Log("onClick: Unexpected click event, id=" + v.getId());
                    break;
            }
        } else {
            mLogger.Log("ERROR: not processing click event because Connector is null.");
        }
    }

    // The Connect button was pressed.
    // If not in a call, attempt to connect to the backend service.
    // If in a call, disconnect.
    public void toggleConnect() {
        if (mToggleConnectButton.isChecked()) {
            // Connect to either a Vidyo.io resource or a VidyoCloud Vidyo room.
            if (!mVidyoCloudJoin) {
                // Connect to a Vidyo.io resource.

                // Abort the Connect call if resource ID is invalid. It cannot contain empty spaces or "@".
                String resourceId = mResourceId.getText().toString().trim(); // trim leading and trailing white space
                if (resourceId.contains(" ") || resourceId.contains("@")) {
                    this.changeState(VidyoConnectorState.FailureInvalidResource);
                } else {
                    this.changeState(VidyoConnectorState.Connecting);

                    if (!mVidyoConnector.connect(
                            mHost.getText().toString().trim(),
                            mToken.getText().toString().trim(),
                            mDisplayName.getText().toString().trim(),
                            resourceId,
                            this)) {
                        // Connect failed.
                        this.changeState(VidyoConnectorState.Failure);
                    }
                }
            } else {
                // Connect to a VidyoCloud Vidyo system, not Vidyo.io.
                this.changeState(VidyoConnectorState.Connecting);

                if (!mVidyoConnector.connectToRoomAsGuest(
                        mPortal,
                        mDisplayName.getText().toString().trim(),
                        mRoomKey,
                        mRoomPin,
                        this)) {
                    // Connect failed.
                    this.changeState(VidyoConnectorState.Failure);
                }
            }
            mLogger.Log("VidyoConnectorConnect status = " + (mVidyoConnectorState == VidyoConnectorState.Connecting));
        } else {
            // The user is either connected to a resource or is in the process of connecting to a resource;
            // Call VidyoConnectorDisconnect to either disconnect or abort the connection attempt.
            this.changeState(VidyoConnectorState.Disconnecting);
            mVidyoConnector.disconnect();
        }
    }

    // Toggle visibility of the toolbar
    @Override
    public void onVideoFrameClicked() {
        if (mVidyoConnectorState == VidyoConnectorState.Connected) {
            if (mToolbarLayout.getVisibility() == View.VISIBLE) {
                mToolbarLayout.setVisibility(View.INVISIBLE);
            } else {
                mToolbarLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    /*
     *  Connector Events
     */

    // Handle successful connection.
    @Override
    public void onSuccess() {
        mLogger.Log("onSuccess: successfully connected.");
        this.changeState(VidyoConnectorState.Connected);
    }

    // Handle attempted connection failure.
    @Override
    public void onFailure(Connector.ConnectorFailReason reason) {
        mLogger.Log("onFailure: connection attempt failed, reason = " + reason.toString());
        this.changeState(VidyoConnectorState.Failure);
    }

    // Handle an existing session being disconnected.
    @Override
    public void onDisconnected(Connector.ConnectorDisconnectReason reason) {
        if (reason == Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected) {
            mLogger.Log("onDisconnected: successfully disconnected, reason = " + reason.toString());
            this.changeState(VidyoConnectorState.Disconnected);
        } else {
            mLogger.Log("onDisconnected: unexpected disconnection, reason = " + reason.toString());
            this.changeState(VidyoConnectorState.DisconnectedUnexpected);
        }
    }

    // Handle local camera events.
    @Override
    public void onLocalCameraAdded(LocalCamera localCamera) {
        mLogger.Log("onLocalCameraAdded: " + localCamera.getName());
    }

    @Override
    public void onLocalCameraRemoved(LocalCamera localCamera) {
        mLogger.Log("onLocalCameraRemoved: " + localCamera.getName());
    }

    @Override
    public void onLocalCameraSelected(LocalCamera localCamera) {
        mLogger.Log("onLocalCameraSelected: " + (localCamera == null ? "none" : localCamera.getName()));

        // If a camera is selected, then update mLastSelectedCamera.
        if (localCamera != null) {
            mLastSelectedCamera = localCamera;
        }
    }
    @Override
    public void onStop() {
        mLogger.Log("onStop");
        super.onStop();

        if (mVidyoConnector != null) {
            if (mVidyoConnectorState != VidyoConnectorState.Connected &&
                mVidyoConnectorState != VidyoConnectorState.Connecting) {
                // Not connected/connecting to a resource.
                // Release camera, mic, and speaker from this app while backgrounded.
                mVidyoConnector.selectLocalCamera(null);
                mVidyoConnector.selectLocalMicrophone(null);
                mVidyoConnector.selectLocalSpeaker(null);
                mDevicesSelected = false;
            }
            mVidyoConnector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Background);
        }
    }

    @Override
    public void onResume() {
        mLogger.Log("onRestart");
        super.onResume();

        if (mVidyoConnector != null) {
            mVidyoConnector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Foreground);

            if (!mDevicesSelected) {
                // Devices have been released when backgrounding (in onStop). Re-select them.
                mDevicesSelected = true;

                // Select the previously selected local camera and default mic/speaker
                mVidyoConnector.selectLocalCamera(mLastSelectedCamera);
                mVidyoConnector.selectDefaultMicrophone();
                mVidyoConnector.selectDefaultSpeaker();

                // Reestablish camera and microphone privacy states
                mVidyoConnector.setCameraPrivacy(mCameraPrivacy);
                mVidyoConnector.setMicrophonePrivacy(mMicrophonePrivacy);
            }
        }
    }

    @Override
    public void onLocalCameraStateUpdated(LocalCamera localCamera, Device.DeviceState state) {
        mLogger.Log("onLocalCameraStateUpdated: name=" + localCamera.getName() + " state=" + state);
    }

    // Handle a message being logged.
    @Override
    public void onLog(LogRecord logRecord) {
        // No need to log to console here, since that is implicitly done when calling registerLogEventListener.
    }

    // Handle network interface events
    @Override
    public void onNetworkInterfaceAdded(NetworkInterface vidyoNetworkInterface) {
        mLogger.Log("onNetworkInterfaceAdded: name=" + vidyoNetworkInterface.getName() + " address=" + vidyoNetworkInterface.getAddress() + " type=" + vidyoNetworkInterface.getType() + " family=" + vidyoNetworkInterface.getFamily());
    }

    @Override
    public void onNetworkInterfaceRemoved(NetworkInterface vidyoNetworkInterface) {
        mLogger.Log("onNetworkInterfaceRemoved: name=" + vidyoNetworkInterface.getName() + " address=" + vidyoNetworkInterface.getAddress() + " type=" + vidyoNetworkInterface.getType() + " family=" + vidyoNetworkInterface.getFamily());
    }

    @Override
    public void onNetworkInterfaceSelected(NetworkInterface vidyoNetworkInterface, NetworkInterface.NetworkInterfaceTransportType vidyoNetworkInterfaceTransportType) {
        mLogger.Log("onNetworkInterfaceSelected: name=" + vidyoNetworkInterface.getName() + " address=" + vidyoNetworkInterface.getAddress() + " type=" + vidyoNetworkInterface.getType() + " family=" + vidyoNetworkInterface.getFamily());
    }

    @Override
    public void onNetworkInterfaceStateUpdated(NetworkInterface vidyoNetworkInterface, NetworkInterface.NetworkInterfaceState vidyoNetworkInterfaceState) {
        mLogger.Log("onNetworkInterfaceStateUpdated: name=" + vidyoNetworkInterface.getName() + " address=" + vidyoNetworkInterface.getAddress() + " type=" + vidyoNetworkInterface.getType() + " family=" + vidyoNetworkInterface.getFamily() + " state=" + vidyoNetworkInterfaceState);
    }
    public void sendMessage(String msg_text){
        mVidyoConnector.sendChatMessage(msg_text);
    }

    @Override
    public void onChatMessageReceived(Participant participant, com.vidyo.VidyoClient.Endpoint.ChatMessage chatMessage){
        if (! (chatMessage.type == com.vidyo.VidyoClient.Endpoint.ChatMessage.ChatMessageType.VIDYO_CHATMESSAGETYPE_Chat))
            return;
        ChatFragment cf = ((ChatFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Chat"));

        if (cf != null){
            getActivity().runOnUiThread(() -> cf.receiveChatMessage(participant, chatMessage));
        }
        messages.add(new ChatMessage(ChatMessage.ParticipantType.Remote, participant.name, chatMessage.body));

    }

    @Override
    public void onParticipantJoined(Participant participant){
        ParticipantFragment pf = ((ParticipantFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Participant"));
        if (pf != null){
            getActivity().runOnUiThread(() -> pf.addParticipant(participant));
        }
        participants.put(participant.id,participant);

    }
    @Override
    public void onParticipantLeft(Participant participant){
        ParticipantFragment pf = ((ParticipantFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Participant"));
        if (pf != null){
            getActivity().runOnUiThread(() -> pf.removeParticipant(participant));
        }
        participants.remove(participant.id);

    }
    @Override
    public void onDynamicParticipantChanged(ArrayList participants){

    }
    @Override
    public void onLoudestParticipantChanged(Participant participant, boolean audioOnly){

    }

    public HashMap<String,Participant> getParticipants(){
        return participants;
    }
    public void addLocalChatMessage(String message, String name){
        messages.add(new ChatMessage(ChatMessage.ParticipantType.Local,name,message));
    }
    public List<ChatMessage> getMessages() {
        return messages;
    }
}

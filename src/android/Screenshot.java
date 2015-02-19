package fr.oyez.cordova.screenshot;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.lang.StringBuilder;
import java.util.concurrent.FutureTask;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.view.View;
import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Screenshot extends CordovaPlugin {
    private int width;
    private int height;
    private ByteBuffer buffer;
    private Bitmap transferBmp;
    private String base64Header = "data:image/jpeg;base64,";

    @Override
    protected void pluginInitialize() {
        width = webView.getWidth();
        height = webView.getHeight();
        buffer = ByteBuffer.allocateDirect(width * height * 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        transferBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("takeScreenshot")){
            final Integer quality = (Integer) args.get(0);
            final Integer scaleDown = (Integer) args.get(1);

            FutureTask future = new FutureTask(new Runnable() {
                @Override
                public void run() {
                    buffer.clear();
                    GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                }
            }, null);

            cordova.getActivity().runOnUiThread(future);
            try {
                future.get();
            }
            catch (Exception e) {
                callbackContext.error(e.getMessage());
                return false;
            }

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        transferBmp.copyPixelsFromBuffer(buffer);

                        int tgtWidth = width / scaleDown;
                        int tgtHeight = height / scaleDown;

                        Bitmap scaled = Bitmap.createScaledBitmap(transferBmp, tgtWidth, tgtHeight, true);

                        Matrix matrix = new Matrix();
                        matrix.preScale(1.0f, -1.0f);

                        Bitmap bitmap = Bitmap.createBitmap(scaled, 0, 0, tgtWidth, tgtHeight, matrix, false);
                        ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();
                        Base64OutputStream raw_jpeg = new Base64OutputStream(jpeg_data, Base64.NO_WRAP);
                        if (bitmap.compress(CompressFormat.JPEG, quality, raw_jpeg)) {
                            StringBuilder sb = new StringBuilder(base64Header.length() + jpeg_data.size());
                            sb.append(base64Header);
                            sb.append(jpeg_data.toString());
                            PluginResult result = new PluginResult(PluginResult.Status.OK, sb.toString());
                            callbackContext.sendPluginResult(result);
                        }
                    } catch (Exception e) {
                        callbackContext.error(e.getMessage());
                    }
                }
            });

            return true;
        }
        callbackContext.error("Action not found");
        return false;
    }
}

/**
 * Copyright (C) 2012 30ideas (http://30ide.as)
 * MIT licensed
 * 
 * @author Josemando Sobral
 * @created Jul 2nd, 2012.
 * improved by Hongbo LU
 */
package com.darktalker.cordova.screenshot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Base64;
import android.view.View;
import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Screenshot extends CordovaPlugin {

	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
	 	// starting on ICS, some WebView methods
		// can only be called on UI threads

		if (action.equals("saveScreenshot")) {
			final String format = (String) args.get(0);
			final Integer quality = (Integer) args.get(1);
			final String fileName = (String)args.get(2);

			super.cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						if(format.equals("png") || format.equals("jpg")){
                            int width = webView.getWidth();
                            int height = webView.getHeight();
                         
                            ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
                            buf.order(ByteOrder.LITTLE_ENDIAN);
                            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
                            
                            Bitmap src = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            src.copyPixelsFromBuffer(buf);
                            
                            Matrix matrix = new Matrix();
                            matrix.preScale(1.0f, -1.0f);
                            
                            Bitmap bitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
							
                            File folder = new File(Environment.getExternalStorageDirectory(), "Pictures");
							if (!folder.exists()) {
								folder.mkdirs();
							}

							File f = new File(folder, fileName + "."+format);

							FileOutputStream fos = new FileOutputStream(f);
							if(format.equals("png")){
								bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
							}
							if(format.equals("jpg")){
								bitmap.compress(Bitmap.CompressFormat.JPEG, quality == null?100:quality, fos);
							}
							JSONObject jsonRes = new JSONObject();
							jsonRes.put("filePath",f.getAbsolutePath());
				                        PluginResult result = new PluginResult(PluginResult.Status.OK, jsonRes);
				                        callbackContext.sendPluginResult(result);
						}else{
							callbackContext.error("format "+format+" not found");
							
						}

					} catch (JSONException e) {
						callbackContext.error(e.getMessage());
						
					} catch (IOException e) {
						callbackContext.error(e.getMessage());
						
					}
				}
			});
			return true;
		}else if(action.equals("getScreenshotAsURI")){
			final Integer quality = (Integer) args.get(0);
			
			super.cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					View view = webView.getRootView();
					try {
						view.setDrawingCacheEnabled(true);
						Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
						view.setDrawingCacheEnabled(false);

						ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();
						
						if (bitmap.compress(CompressFormat.JPEG, quality, jpeg_data)) {
						   byte[] code = jpeg_data.toByteArray();
						   byte[] output = Base64.encode(code, Base64.NO_WRAP);
						   String js_out = new String(output);
						   js_out = "data:image/jpeg;base64," + js_out;
						   JSONObject jsonRes = new JSONObject();
						   jsonRes.put("URI", js_out);
				                   PluginResult result = new PluginResult(PluginResult.Status.OK, jsonRes);
				                   callbackContext.sendPluginResult(result);
							
						   js_out = null;
						   output = null;
						   code = null;
						}
						
						jpeg_data = null;

					} catch (JSONException e) {
						callbackContext.error(e.getMessage());
						
					} catch (Exception e) {
						callbackContext.error(e.getMessage());
						
					}
				}
			});

			return true;		
		}
		callbackContext.error("action not found");
		return false;
	}
}

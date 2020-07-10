package org.wakin.flexlayout;
/**
 * Created by matthew on 23/06/2020.
 */
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.AttributeSet;
import android.util.LruCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.graphics.drawable.BitmapDrawable;

import androidx.appcompat.widget.AppCompatImageView;

public class NetImageView extends AppCompatImageView {

    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static ThreadPoolExecutor dlQueue = new ThreadPoolExecutor(0, 6, 4, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    public static final int IMAGE_LOAD_SUCCESS = 1;
    public static final int IMAGE_LOAD_NETWORK_ERROR = 2;
    public static final int IMAGE_LOAD_SERVER_ERROR = 3;

    public static final int NET_IMAGE_VIEW_STATE_FAILED = 0;
    public static final int NET_IMAGE_VIEW_STATE_DOWNLOADED = 1;
    public static final int NET_IMAGE_VIEW_STATE_LOADED = 2;

    public String urlPending;

    /*
    int maxMemory = (int) Runtime.getRuntime().maxMemory();//获取系统分配给应用的总内存大小
    int mCacheSize = maxMemory / 8;//设置图片内存缓存占用八分之一
    mMemoryCache = new LruCache(mCacheSize) {
        //必须重写此方法，来测量Bitmap的大小
        @Override
        protected int sizeOf(String key, Drawable value) {
            if (value instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) value).getBitmap();
                return bitmap == null ? 0 : bitmap.getByteCount();
            }
            return super.sizeOf(key, value);
        }
    };
    */

    private static LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private static LruCache<String, BitmapDrawable> drawableCache = new LruCache<String, BitmapDrawable>(4 * 1024 * 1024) {
        protected int sizeOf(String key, BitmapDrawable drawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            return bitmap == null ? 0 : bitmap.getByteCount();
        }
    };

    public static interface StateChangeCallback {
        void onStateChanged(NetImageView imageView, int state, BitmapDrawable bitmap, Exception ex);

    }

    protected Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case IMAGE_LOAD_SUCCESS:
                    Bundle data = msg.getData();
                    if (null != data) {
                        String url = data.getString("url");
                        if (url.equalsIgnoreCase(NetImageView.this.urlPending)) {
                            BitmapDrawable drawable = (BitmapDrawable) msg.obj;
                            // setImageBitmap(bitmap);
                            setImageDrawable(drawable);
                        }

                    }
                    break;
                case IMAGE_LOAD_NETWORK_ERROR:
                    Toast.makeText(getContext(),"网络连接失败",Toast.LENGTH_SHORT).show();
                    break;
                case IMAGE_LOAD_SERVER_ERROR:
                    Toast.makeText(getContext(),"服务器发生错误",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    protected static void changeStateToLoaded(final NetImageView netImageView, final BitmapDrawable drawable, final StateChangeCallback stateChangeCallback) {

    }

    protected abstract static class ImageRequestRunnable implements Runnable {

        protected URL url;
        // protected HttpFile httpFile;
        // protected int options;
        protected Map<String, String> requestHeaders;

        public ImageRequestRunnable(URL url, Map<String, String> requestHeaders) {
            this.url = url;
            // this.httpFile = httpFile;
            // this.options = options;
            this.requestHeaders = requestHeaders;
            // this.responseHandler = responseHandler;
        }

    }

    public NetImageView(Context context) {
        super(context);

        urlPending = null;
    }


    public NetImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public NetImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /*
    public NetImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

     */

    public static boolean loadImage(String urlString, final StateChangeCallback stateChangeCallback) {
        return loadImageImpl(null, urlString, stateChangeCallback);
    }

    protected static boolean loadImageImpl(final NetImageView netImageView, final String urlString, final StateChangeCallback stateChangeCallback) {
        if (null == urlString) {
            return false;
        }

        // For DEBUG
        Map<String, String> requestHeaders = new HashMap<>();
        // final String urlStringNew = urlString.replace(ModelUrls.API_HOST, ModelUrls.API_HOST_IP);
        // requestHeaders.put("Host", ModelUrls.API_HOST);

        URL url = null;
        try {
            url = new URL(urlString);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        if (null == url) {
            return false;
        }

        netImageView.urlPending = urlString;

        BitmapDrawable cachedBitmap = drawableCache.get(urlString);
        if (null != cachedBitmap) {
            Message msg = Message.obtain();
            msg.obj = cachedBitmap;
            Bundle data = new Bundle();
            data.putString("url", urlString);
            msg.setData(data);
            msg.what = IMAGE_LOAD_SUCCESS;
            netImageView.handler.sendMessage(msg);

            if (null != stateChangeCallback) {
                stateChangeCallback.onStateChanged(netImageView, NET_IMAGE_VIEW_STATE_LOADED, cachedBitmap, null);
            }

            return true;
        }

        dlQueue.execute(new ImageRequestRunnable(url, requestHeaders) {
            @Override
            public void run() {

                this.requestHeaders.put("Accept", "*/*");
                // requestHeaders.put("User-Agent", buildUserAgent());
                // requestHeaders.put("Accept-Encoding", "gzip,deflate");

                // HttpResponse response = null;
                // NSError *error = nil;

                // NSLog(@"URL: %@", request.URL.absoluteString);

                HttpURLConnection connection = null;

                try {
                    connection = (HttpURLConnection)this.url.openConnection();

                    if (null == connection) {
                        // send error to main thread
                        if (null != stateChangeCallback) {
                            stateChangeCallback.onStateChanged(netImageView, NET_IMAGE_VIEW_STATE_FAILED, null, null);
                        }
                        return;
                    }

                    for (String key : requestHeaders.keySet()) {
                        connection.addRequestProperty(key, requestHeaders.get(key));
                    }

                    connection.connect();

                    int responseCode = connection.getResponseCode();
                    // response = new HttpResponse(connection);

                    if (HttpURLConnection.HTTP_OK == responseCode) {
                        InputStream inputStream = connection.getInputStream();
                        // Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Bitmap bitmap = decodeSampledBitmapFromResource(inputStream, netImageView.getWidth(), netImageView.getHeight());
                        // BitmapDrawable bitmapDrawable = new BitmapDrawable(inputStream);

                        inputStream.close();

                        BitmapDrawable drawable = new BitmapDrawable(netImageView.getResources(), bitmap);

                        if (null != stateChangeCallback) {
                            stateChangeCallback.onStateChanged(netImageView, NET_IMAGE_VIEW_STATE_DOWNLOADED, drawable, null);
                        }

                        if (null != netImageView) {
                            Message msg = Message.obtain();
                            msg.obj = drawable;
                            Bundle data = new Bundle();
                            data.putString("url", urlString);
                            msg.setData(data);
                            msg.what = IMAGE_LOAD_SUCCESS;
                            netImageView.handler.sendMessage(msg);
                        }

                        drawableCache.put(urlString, drawable);

                        if (null != stateChangeCallback) {
                            stateChangeCallback.onStateChanged(netImageView, NET_IMAGE_VIEW_STATE_LOADED, drawable, null);
                        }

                    }

                }
                catch (Exception ex) {
                    if (null != stateChangeCallback) {
                        stateChangeCallback.onStateChanged(netImageView, NET_IMAGE_VIEW_STATE_FAILED, null, ex);
                    }
                    ex.printStackTrace();
                }

            }
        });



        return true;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight, int width, int height) {
        // final int height = options.outHeight;
        // final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(InputStream is,
                                                         int reqWidth, int reqHeight) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inJustDecodeBounds = true;
        // BitmapFactory.decodeStream(is, null, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight, 480, 480);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(is, null, options);
    }

    public boolean loadUrl(String urlString) {
        return loadUrl(urlString, null);
    }

    public boolean loadUrl(String urlString, final StateChangeCallback stateChangeCallback) {
        return loadImageImpl(this, urlString, stateChangeCallback);
    }

    public int realImageViewWith() {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        //如果ImageView设置了宽度就可以获取实在宽带
        int width = getWidth();
        if (width <= 0) {
            //如果ImageView没有设置宽度，就获取父级容器的宽度
            width = layoutParams.width;
        }
        if (width <= 0) {
            //获取ImageView宽度的最大值
            // width = getMaxWidth();
        }
        if (width <= 0) {
            //获取屏幕的宽度
            width = displayMetrics.widthPixels;
        }
        Log.e("ImageView实际的宽度", String.valueOf(width));
        return width;
    }

    /**
     * 获取ImageView实际的高度
     * @return 返回ImageView实际的高度
     */
    public int realImageViewHeight() {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        //如果ImageView设置了高度就可以获取实在宽度
        int height = getHeight();
        if (height <= 0) {
            //如果ImageView没有设置高度，就获取父级容器的高度
            height = layoutParams.height;
        }
        if (height <= 0) {
            //获取ImageView高度的最大值
            // height = getMaxHeight();
        }
        if (height <= 0) {
            //获取ImageView高度的最大值
            height = displayMetrics.heightPixels;
        }
        Log.e("ImageView实际的高度", String.valueOf(height));
        return height;
    }

    /**
     * 获得需要压缩的比率
     *
     * @param options 需要传入已经BitmapFactory.decodeStream(is, null, options);
     * @return 返回压缩的比率，最小为1
     */
    public int getInSampleSize(BitmapFactory.Options options) {
        int inSampleSize = 1;
        int realWith = realImageViewWith();
        int realHeight = realImageViewHeight();

        int outWidth = options.outWidth;
        Log.e("网络图片实际的宽度", String.valueOf(outWidth));
        int outHeight = options.outHeight;
        Log.e("网络图片实际的高度", String.valueOf(outHeight));

        //获取比率最大的那个
        if (outWidth > realWith || outHeight > realHeight) {
            int withRadio = Math.round(outWidth / realWith);
            int heightRadio = Math.round(outHeight / realHeight);
            inSampleSize = withRadio > heightRadio ? withRadio : heightRadio;
        }
        Log.e("压缩比率", String.valueOf(inSampleSize));
        return inSampleSize;
    }

    /**
     * 根据输入流返回一个压缩的图片
     * @param input 图片的输入流
     * @return 压缩的图片
     */
    public Bitmap getCompressBitmap(InputStream input) {
        //因为InputStream要使用两次，但是使用一次就无效了，所以需要复制两个
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1 ) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //复制新的输入流
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        InputStream is2 = new ByteArrayInputStream(baos.toByteArray());

        //只是获取网络图片的大小，并没有真正获取图片
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        //获取图片并进行压缩
        options.inSampleSize = getInSampleSize(options);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(is2, null, options);
    }





}

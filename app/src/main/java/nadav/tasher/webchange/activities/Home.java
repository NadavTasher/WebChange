package nadav.tasher.webchange.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import nadav.tasher.lightool.communication.network.Download;
import nadav.tasher.lightool.graphics.views.ExpandingView;
import nadav.tasher.lightool.graphics.views.Utils;
import nadav.tasher.lightool.graphics.views.appview.AppView;
import nadav.tasher.lightool.graphics.views.appview.navigation.corner.Corner;
import nadav.tasher.lightool.info.Device;
import nadav.tasher.webchange.R;
import nadav.tasher.webchange.architecture.Site;
import nadav.tasher.webchange.services.Refresh;

import static nadav.tasher.lightool.info.Device.isOnline;

public class Home extends Activity {

    public static final String prefName = "prefs", sitesPref = "sites";
    private final int topColor = 0xFFAB45C9, bottomColor = 0x000000, uiColor = 0xFF123456;
    private AppView mAppView;
    private SharedPreferences sp;

    public static void saveSite(Context c, Site site) {
        SharedPreferences sp = c.getSharedPreferences(prefName, MODE_PRIVATE);
        try {
            JSONArray jsonArray = new JSONArray(sp.getString(sitesPref, new JSONArray().toString()));
            boolean found = false;
            for (int i = 0; i < jsonArray.length() && !found; i++) {
                if (Site.fromJSON(jsonArray.getJSONObject(i)).getId().equals(site.getId())) {
                    jsonArray.put(i, site.toJSON());
                    found = true;
                }
            }
            if (!found) jsonArray.put(site.toJSON());
            sp.edit().putString(sitesPref, jsonArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void removeSite(Context c, Site site) {
        SharedPreferences sp = c.getSharedPreferences(prefName, MODE_PRIVATE);
        try {
            JSONArray jsonArray = new JSONArray(sp.getString(sitesPref, new JSONArray().toString()));
            for (int i = 0; i < jsonArray.length(); i++) {
                if (Site.fromJSON(jsonArray.getJSONObject(i)).getId().equals(site.getId())) {
                    jsonArray.remove(i);
                }
            }
            sp.edit().putString(sitesPref, jsonArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String getSumForFile(File file) {
        try {
            return new String(Hex.encodeHex(DigestUtils.md5(new FileInputStream(file))));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean check(Site site, File temp) {
        return !site.getSum().equals(getSumForFile(temp));
    }

    public static File getTempFile(Context context) {
        return new File(context.getFilesDir(), "temp");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        sp = getSharedPreferences(prefName, MODE_PRIVATE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Refresh.reschedule(getApplicationContext());
        mAppView = new AppView(this);
        mAppView.setDrawNavigation(false);
        mAppView.setBackgroundColor(new AppView.Gradient(topColor, bottomColor));
        mAppView.getDrawer().getDrawerView().setBackground(Utils.getCoaster(Color.GRAY, 32, 20));
        mAppView.getDrawer().getDrawerView().setPadding(20, 20, 20, 20);
        Corner createCorner = new Corner(getApplicationContext(), Device.screenX(getApplicationContext()) / 4, uiColor);
        createCorner.setColorAlpha(255);
        createCorner.setColor(uiColor);
        ImageView createIcon = new ImageView(getApplicationContext());
        createIcon.setImageDrawable(getDrawable(nadav.tasher.lightool.R.drawable.ic_add));
        createCorner.setView(createIcon, 0.7);
        createCorner.addOnState(new Corner.OnState() {
            @Override
            public void onOpen() {
            }

            @Override
            public void onClose() {
            }

            @Override
            public void onBoth(boolean b) {
                if (mAppView.getDrawer().isOpen()) {
                    mAppView.getDrawer().close();
                } else {
                    createNew();
                }
            }
        });
        mAppView.getCornerView().setBottomRight(createCorner);
        getWindow().setNavigationBarColor(uiColor);
        setContentView(mAppView);
        loadSites();
    }

    private void createNew() {
        LinearLayout dialog = new LinearLayout(getApplicationContext());
        dialog.setOrientation(LinearLayout.VERTICAL);
        dialog.setGravity(Gravity.CENTER);
        FrameLayout urlFrame = new FrameLayout(getApplicationContext());
        urlFrame.setPadding(20, 20, 20, 20);
        final EditText url = new EditText(getApplicationContext());
        Button generate = new Button(getApplicationContext());
        url.setHint("Site URL e.g. http://nockio.com");
        url.setSingleLine();
        generate.setText("Add");
        generate.setAllCaps(false);
        generate.setBackground(Utils.getCoaster(Color.LTGRAY, 16, 10));
        urlFrame.addView(url);
        dialog.addView(urlFrame);
        dialog.addView(generate);
        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!url.getText().toString().isEmpty()) {
                    if (Pattern.compile("(\\.[a-z]+(/.+)|(/))$").matcher(url.getText().toString()).find()) {
                        url.setError(null);
                        mAppView.getDrawer().close();
                        final Site site = Site.createNew(protocolify(url.getText().toString()));
                        saveSite(getApplicationContext(), site);
                        if (isOnline(getApplicationContext())) {
                            site.getDownload(getTempFile(getApplicationContext()), new Download.Callback() {
                                @Override
                                public void onSuccess(File file) {
                                    site.setSum(getSumForFile(file));
                                    saveSite(getApplicationContext(), site);
                                    loadSites();
                                }

                                @Override
                                public void onFailure(Exception e) {

                                }
                            }).execute();
                        }
                        loadSites();
                    } else {
                        url.setError("A URL Must End With A TLD");
                    }
                } else {
                    url.setError("A URL Can't Be Empty");
                }
            }
        });
        mAppView.getDrawer().setContent(dialog);
        mAppView.getDrawer().open(0.3);
    }

    private void loadSites() {
        LinearLayout sites = new LinearLayout(getApplicationContext());
        sites.setOrientation(LinearLayout.VERTICAL);
        sites.setGravity(Gravity.CENTER);
        try {
            JSONArray siteArray = new JSONArray(sp.getString(sitesPref, new JSONArray().toString()));
            for (int index = 0; index < siteArray.length(); index++) {
                JSONObject currentSite = siteArray.getJSONObject(index);
                sites.addView(new SiteView(getApplicationContext(), Site.fromJSON(currentSite)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mAppView.getScrolly().setView(sites);
    }

    private String protocolify(String url) {

        if (!Pattern.compile("^([a-z]+)://").matcher(url).find()) {
            return "http://" + url;
        }
        return url;
    }

    class SiteView extends FrameLayout {

        private Site site;

        public SiteView(Context context, Site site) {
            super(context);
            this.site = site;
            init();
        }

        private String unprotocolify(String url) {
            return url.replaceAll("^([a-z]+)://", "");
        }

        private ImageView getImageView(int resource) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageDrawable(getDrawable(resource));
            int size = Device.screenX(getContext()) / 8;
            imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            return imageView;
        }

        private void init() {
            LinearLayout top = new LinearLayout(getContext());
            top.setOrientation(LinearLayout.VERTICAL);
            top.setGravity(Gravity.CENTER);
            top.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            FrameLayout urlHolder = new FrameLayout(getContext());
            final TextView url = new TextView(getContext());
            final EditText urlEditor = new EditText(getContext());
            urlEditor.setText(site.getUrl());
            urlEditor.setHint("URL");
            urlEditor.setTextSize(34);
            urlEditor.setGravity(Gravity.CENTER);
            urlEditor.setSingleLine();
            urlEditor.setTextColor(Color.WHITE);
            final TextView sum = new TextView(getContext());
            url.setText(unprotocolify(site.getUrl()));
            url.setTextSize(34);
            url.setGravity(Gravity.CENTER);
            url.setSingleLine();
            url.setEllipsize(TextUtils.TruncateAt.END);
            url.setTextColor(Color.WHITE);
            sum.setText(site.getSum());
            sum.setTextSize(20);
            sum.setGravity(Gravity.CENTER);
            sum.setSingleLine();
            sum.setEllipsize(TextUtils.TruncateAt.END);
            sum.setTextColor(Color.WHITE);
            urlHolder.addView(url);
            urlEditor.setVisibility(View.GONE);
            urlHolder.addView(urlEditor);
            top.addView(urlHolder);
            top.addView(sum);
            LinearLayout bottom = new LinearLayout(getContext());
            bottom.setOrientation(LinearLayout.HORIZONTAL);
            bottom.setGravity(Gravity.CENTER);
            bottom.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ImageView remove = getImageView(nadav.tasher.lightool.R.drawable.ic_delete);
            ImageView refresh = getImageView(R.drawable.ic_refresh);
            final ImageView edit = getImageView(R.drawable.ic_edit);
            bottom.addView(edit);
            bottom.addView(refresh);
            bottom.addView(remove);
            edit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (url.getVisibility() == View.GONE) {
                        if (!urlEditor.getText().toString().isEmpty()) {
                            if (Pattern.compile("(\\.[a-z]+(/.+)|(/))$").matcher(urlEditor.getText().toString()).find()) {
                                url.setVisibility(View.VISIBLE);
                                urlEditor.setVisibility(View.GONE);
                                edit.setImageDrawable(getDrawable(R.drawable.ic_edit));
                                url.setText(unprotocolify(urlEditor.getText().toString()));
                                site.setUrl(protocolify(urlEditor.getText().toString()));
                                saveSite(getContext(), site);
                            } else {
                                urlEditor.setError("A URL Must End With A TLD");
                            }
                        } else {
                            urlEditor.setError("A URL Can't Be Empty");
                        }
                    } else {
                        urlEditor.setVisibility(View.VISIBLE);
                        url.setVisibility(View.GONE);
                        edit.setImageDrawable(getDrawable(R.drawable.ic_save));
                    }
                }
            });
            refresh.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isOnline(getContext())) {

                        site.getDownload(getTempFile(getApplicationContext()), new Download.Callback() {
                            @Override
                            public void onSuccess(File file) {
                                site.setSum(getSumForFile(file));
                                saveSite(getApplicationContext(), site);
                                sum.setText(site.getSum());
                            }

                            @Override
                            public void onFailure(Exception e) {

                            }
                        }).execute();
                    }
                }
            });
            remove.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    removeSite(getContext(), site);
                    setVisibility(View.GONE);
                }
            });
            ExpandingView expand = new ExpandingView(getContext(), Utils.getCoaster(0xFF884444, 20, 10), 500, (int) ((double) Device.screenY(getContext()) / 6.5), top, bottom);
            expand.setPadding(20, 20, 20, 20);
            addView(expand);
        }

    }
}

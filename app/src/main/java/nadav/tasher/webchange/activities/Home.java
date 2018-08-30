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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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
import static nadav.tasher.webchange.architecture.Center.bottomColor;
import static nadav.tasher.webchange.architecture.Center.getTempFile;
import static nadav.tasher.webchange.architecture.Center.md5;
import static nadav.tasher.webchange.architecture.Center.prefName;
import static nadav.tasher.webchange.architecture.Center.protocolPattern;
import static nadav.tasher.webchange.architecture.Center.removeSite;
import static nadav.tasher.webchange.architecture.Center.saveSite;
import static nadav.tasher.webchange.architecture.Center.sitesPref;
import static nadav.tasher.webchange.architecture.Center.textColor;
import static nadav.tasher.webchange.architecture.Center.tldPattern;
import static nadav.tasher.webchange.architecture.Center.topColor;
import static nadav.tasher.webchange.architecture.Center.uiColor;
import static nadav.tasher.webchange.architecture.Center.uifColor;
import static nadav.tasher.webchange.architecture.Center.unprotocolPattern;

public class Home extends Activity {


    private AppView mAppView;
    private SharedPreferences sp;


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
        mAppView.getDrawer().getDrawerView().setBackground(Utils.getCoaster(uiColor, 32, 20));
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
        url.setHint("URL");
        url.setTextColor(textColor);
        url.setHintTextColor(Color.LTGRAY);
        url.setSingleLine();
        generate.setText(R.string.add);
        generate.setAllCaps(false);
        generate.setTextColor(textColor);
        generate.setBackground(Utils.getCoaster(uifColor, 16, 10));
        urlFrame.addView(url);
        dialog.addView(urlFrame);
        dialog.addView(generate);
        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!url.getText().toString().isEmpty()) {
                    if (Pattern.compile(tldPattern).matcher(url.getText().toString()).find()) {
                        url.setError(null);
                        mAppView.getDrawer().close();
                        final Site site = Site.createNew(protocolify(url.getText().toString()));
                        saveSite(getApplicationContext(), site);
                        if (isOnline(getApplicationContext())) {
                            site.getDownload(getTempFile(getApplicationContext()), new Download.Callback() {
                                @Override
                                public void onSuccess(File file) {
                                    site.setSum(md5(file));
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

        if (!Pattern.compile(protocolPattern).matcher(url).find()) {
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
            return url.replaceAll(unprotocolPattern, "");
        }

        private ImageView getImageView(int resource) {
            ImageView imageView = new ImageView(getContext());
            imageView.setImageDrawable(getDrawable(resource));
            int size = Device.screenX(getContext()) / 8;
            imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size, 1));
            return imageView;
        }

        private View generateSized() {
            View v = new View(getContext());
            v.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 2));
            return v;
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
            urlEditor.setTextColor(textColor);
            final TextView sum = new TextView(getContext());
            url.setText(unprotocolify(site.getUrl()));
            url.setTextSize(34);
            url.setGravity(Gravity.CENTER);
            url.setSingleLine();
            url.setEllipsize(TextUtils.TruncateAt.END);
            url.setTextColor(textColor);
            sum.setText(site.getSum());
            sum.setTextSize(20);
            sum.setGravity(Gravity.CENTER);
            sum.setSingleLine();
            sum.setEllipsize(TextUtils.TruncateAt.END);
            sum.setTextColor(textColor);
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
            bottom.addView(generateSized());
            bottom.addView(edit);
            bottom.addView(refresh);
            bottom.addView(remove);
            bottom.addView(generateSized());
            edit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (url.getVisibility() == View.GONE) {
                        if (!urlEditor.getText().toString().isEmpty()) {
                            if (Pattern.compile(tldPattern).matcher(urlEditor.getText().toString()).find()) {
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
                                site.setSum(md5(file));
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
            top.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
//            Utils.measure(top);
            ExpandingView expand = new ExpandingView(getContext(), Utils.getCoaster(uifColor, 20, 10), 500, top.getMeasuredHeight() + 40, top, bottom);
            expand.setPadding(20, 20, 20, 40);
            addView(expand);
        }

    }
}

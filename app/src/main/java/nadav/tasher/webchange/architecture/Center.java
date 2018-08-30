package nadav.tasher.webchange.architecture;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class Center {

    public static final String prefName = "prefs", sitesPref = "sites", tldPattern = "(\\..+)", unprotocolPattern = "^([a-z]+)://", protocolPattern = "^([a-z]+)://";

    public static final int topColor = 0xFF92bbfc, bottomColor = 0xFFfc9835, uiColor = 0xFF72e0b0, uifColor = 0xFF992244, textColor = 0xFF000000;

    public static void saveSite(Context c, Site site) {
        SharedPreferences sp = c.getSharedPreferences(prefName, Context.MODE_PRIVATE);
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
        SharedPreferences sp = c.getSharedPreferences(prefName, Context.MODE_PRIVATE);
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

    public static String md5(File file) {
        try {
            return new String(Hex.encodeHex(DigestUtils.md5(new FileInputStream(file))));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean check(Site site, File temp) {
        return !site.getSum().equals(md5(temp));
    }

    public static File getTempFile(Context context) {
        return new File(context.getFilesDir(), "temp");
    }

}

package nadav.tasher.webchange.architecture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Random;

import nadav.tasher.lightool.communication.network.Download;
import nadav.tasher.lightool.communication.network.Requester;
import okhttp3.Request;

public class Site {
    private String url;
    private String sum;
    private String id;

    private Site(String url, String sum, String id) {
        this.url = url;
        this.sum = sum;
        this.id = id;
    }

    public static Site createNew(String url) {
        return new Site(url, "", String.valueOf(new Random().nextInt(100000)));
    }

    public static Site fromJSON(JSONObject jsonObject) {
        try {
            return new Site(jsonObject.getString("url"), jsonObject.getString("sum"), jsonObject.getString("id"));
        } catch (JSONException e) {
            e.printStackTrace();
            return new Site("", "", String.valueOf(new Random().nextInt(100000)));
        }
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("url", url);
            jsonObject.put("sum", sum);
            jsonObject.put("id", id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public Download getDownload(File destination, Download.Callback callback) {
        return new Download(url, destination, callback);
    }

    public Requester getExecutable(Requester.Callback callback) {
        return new Requester(new Request.Builder().url(url).get().build(), callback);
    }
}

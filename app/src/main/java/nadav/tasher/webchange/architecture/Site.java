package nadav.tasher.webchange.architecture;

import nadav.tasher.lightool.communication.network.Requester;
import okhttp3.Request;

public class Site {
    private String url;

    public Site(String url) {

    }

    public Requester getExecutable(Requester.Callback callback) {
        return new Requester(new Request.Builder().url(url).get().build(), callback);
    }
}

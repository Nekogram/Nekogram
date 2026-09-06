package tw.nekomimi.nekogram.streaming;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.collection.LruCache;

import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;

import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FileStreamLoadOperation;
import org.telegram.tgnet.TLRPC;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import fi.iki.elonen.NanoHTTPD;

public class MediaStreamingServer extends NanoHTTPD {
    private static final int PORT = 61579;
    private static final String HOST = "127.0.0.1";

    private static final class InstanceHolder {
        private static final MediaStreamingServer instance = new MediaStreamingServer();
    }

    public static MediaStreamingServer getInstance() {
        return InstanceHolder.instance;
    }

    public MediaStreamingServer() {
        super(PORT);
    }

    private final LruCache<String, Uri> files = new LruCache<>(6);
    private final AtomicInteger reqId = new AtomicInteger();

    private boolean started = false;

    public boolean addFile(String path, Uri file) {
        if (!started) {
            try {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
                started = true;
            } catch (IOException e) {
                FileLog.e(e);
                return false;
            }
        }
        files.put(path, file);
        return true;
    }

    @Override
    public Response serve(IHTTPSession session) {
        var reqId = this.reqId.incrementAndGet();
        FileLog.d("Request " + reqId + " " + session.getMethod() + " " + session.getUri() + " " + session.getHeaders().get("range"));
        try {
            return serveImpl(session);
        } catch (Throwable e) {
            FileLog.e("Error " + reqId, e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "error reading file");
        }
    }

    private Response serveImpl(IHTTPSession session) throws Exception {
        if (Method.OPTIONS.equals(session.getMethod())) {
            return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
        }
        var uri = Uri.parse("http://" + HOST + session.getUri());
        var path = uri.getPath();
        if (path == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "invalid path");
        }
        if (TextUtils.equals(path, "/")) {
            return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "nya");
        }
        var mediaFile = files.get(path);
        if (mediaFile == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "file not found");
        }
        return serveFileImpl(session, mediaFile);
    }

    private Response serveFileImpl(IHTTPSession session, Uri file) throws Exception {
        var headers = session.getHeaders();

        var source = new FileStreamLoadOperation();
        var dataSpecBuilder = new DataSpec.Builder().setUri(file);
        var fileSize = source.open(dataSpecBuilder.build());
        source.close();

        var range = parseRangeHeader(headers.get("range"), fileSize);
        var readSize = (range != null) ? (range.end - range.start + 1) : fileSize;

        if (range != null) {
            dataSpecBuilder.setPosition(range.start);
            dataSpecBuilder.setLength(readSize);
        }

        Response response;
        if (readSize != 0) {
            var dataInputStream = new DataSourceInputStream(source, dataSpecBuilder.build());
            response = newFixedLengthResponse(range != null ? Response.Status.PARTIAL_CONTENT : Response.Status.OK, file.getQueryParameter("mime"), dataInputStream, readSize);
        } else {
            response = newFixedLengthResponse(Response.Status.NO_CONTENT, file.getQueryParameter("mime"), "");
        }

        if (range != null) {
            response.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + fileSize);
        }

        return response;
    }

    private static Range parseRangeHeader(String rangeHeader, long fileLength) {
        if (TextUtils.isEmpty(rangeHeader)) {
            return null;
        }

        long start, end;
        var rangeValue = rangeHeader.trim().substring("bytes=".length());

        if (rangeValue.startsWith("-")) {
            end = fileLength - 1;
            start = fileLength - 1 - Long.parseLong(rangeValue.substring("-".length()));
        } else {
            var range = rangeValue.split("-");
            start = Long.parseLong(range[0]);
            end = range.length > 1 ? Long.parseLong(range[1]) : fileLength - 1;
        }
        if (end > fileLength - 1) {
            end = fileLength - 1;
        }

        return new Range(start, end);
    }

    private record Range(long start, long end) {
    }

    public static boolean openForStreaming(Activity activity, int currentAccount, TLRPC.Document document, Object parent) {
        var uri = FileStreamLoadOperation.prepareUri(currentAccount, document, parent);
        if (uri == null || !"tg".equals(uri.getScheme())) {
            return false;
        }
        var builder = new Uri.Builder();
        builder.scheme("http");
        builder.encodedAuthority(HOST + ":" + PORT);
        builder.appendPath(String.valueOf(currentAccount));
        builder.appendPath(String.valueOf(document.id));
        builder.appendPath(FileLoader.getDocumentFileName(document));
        var streamingUri = builder.build();
        if (!getInstance().addFile(streamingUri.getPath(), uri)) {
            return false;
        }

        var intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(streamingUri, document.mime_type);
        activity.startActivityForResult(intent, 500);
        return true;
    }
}

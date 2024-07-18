package sun.net.www.protocol.ipfs;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

    public synchronized URLConnection openConnection(URL u) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

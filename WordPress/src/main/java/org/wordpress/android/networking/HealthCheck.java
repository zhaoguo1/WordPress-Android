package org.wordpress.android.networking;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Created by hypest on 25/02/16.
 */
public class HealthCheck {
    public static boolean failsWith404(String baseUrl) {
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(URI.create(baseUrl + "/xmlrpc.php"), "", "");
        try {
            // just try to execute an xmlrpc method to see if xmlrpc.php is there
            client.call(ApiHelper.Method.LIST_METHODS);
        } catch (XMLRPCException e) {
            if (e.getMessage().startsWith("HTTP status code: 404 was returned.")) {
                return true;
            } else {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        // call did not fail with 404 so, return false
        return false;
    }
}

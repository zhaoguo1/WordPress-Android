package org.wordpress.android.networking;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;

import android.content.Context;

import java.io.IOException;
import java.net.URI;

/**
 * Created by hypest on 25/02/16.
 */
public class HealthCheck {
    public static String checkInSequence(Context context, String baseUrl) {
        if (HealthCheck.failsWith404(baseUrl)) {
            return context.getString(org.wordpress.android.R.string.health_check_error_xmlrpc_missing);
        }

        if (HealthCheck.failsWith403(baseUrl)) {
            return "xmlrpc.php access denied (perhaps by .htaccess)";
        }

        return "No detectable error";
    }

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

    public static boolean failsWith403(String baseUrl) {
        XMLRPCClientInterface client = XMLRPCFactory.instantiate(URI.create(baseUrl + "/xmlrpc.php"), "", "");
        try {
            // just try to execute an xmlrpc method to see if xmlrpc.php is there
            client.call(ApiHelper.Method.LIST_METHODS);
        } catch (XMLRPCException e) {
            if (e.getMessage().startsWith("HTTP status code: 403 was returned.")) {
                return true;
            } else {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        // call did not fail wihth 403 so, return false
        return false;
    }
}

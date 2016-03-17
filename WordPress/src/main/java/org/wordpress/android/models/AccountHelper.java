package org.wordpress.android.models;

import org.wordpress.android.datasets.AccountTable;

/**
 * The app supports only one WordPress.com account at the moment, so we might use getDefaultAccount() everywhere we
 * need the account data.
 */
public class AccountHelper {
    private static AccountLegacy sAccount;

    public static AccountLegacy getDefaultAccount() {
        if (sAccount == null) {
            sAccount = AccountTable.getDefaultAccount();
            if (sAccount == null) {
                sAccount = new AccountLegacy();
            }
        }
        return sAccount;
    }
}

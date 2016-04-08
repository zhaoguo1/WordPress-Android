import LoginPage from '../pages/login-page.js';

export default class LoginFlow {
	constructor( driver, account, { selfHostedURL = '' } = {} ) {
		this.driver = driver;

		if ( account ) {
			this.account = account;
		} else {
			this.account = 'defaultUser';
		}

		this.selfHostedURL = selfHostedURL;
	}

	login() {
		let testUserName, testPassword;
		const accountInfo = JSON.parse( process.env.ACCOUNT_INFO )[this.account];

		if ( accountInfo !== undefined ) {
			testUserName = accountInfo[0];
			testPassword = accountInfo[1];
		} else {
			throw new Error( `Account key '${this.account}' not found in environment variable ACCOUNT_INFO` );
		}

		let loginPage = new LoginPage( this.driver );

		if ( this.selfHostedURL !== '' ) {
			loginPage.addSelfHostedURL( this.selfHostedURL );
		}

		return loginPage.login( testUserName, testPassword );
	}

}

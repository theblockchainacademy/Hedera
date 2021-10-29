
require("dotenv").config();

const {
    Client,
    PrivateKey,
    AccountBalanceQuery,
    Hbar,
    AccountId,
	TokenCreateTransaction,
	TokenAssociateTransaction
} = require("@hashgraph/sdk");

//const accountId = "0.0.574363"; // set this to an account you want to find the balance of 
const accountId = "0.0.2084028"; // set this to an account you want to find the balance of

main();

async function main() {
    let client;
	const moperatorId = AccountId.fromString(process.env.OPERATOR_ID);
	
	console.log("Greetings earthling....");
	console.log("This is a JavaScript Hedera API application");
	
	console.log("Attempting to get token info from the Hedera API...");
	
	// from a pre-configured network
	client = Client.forTestnet();


    if (process.env.OPERATOR_KEY != null && process.env.OPERATOR_ID != null) {
        const operatorKey = PrivateKey.fromString(process.env.OPERATOR_KEY);
        const operatorId = AccountId.fromString(process.env.OPERATOR_ID);

        client.setOperator(operatorId, operatorKey);
    } else
	{
		throw new Error("Environment variables OPERATOR_KEY and OPERATOR_ID must be present");
	}

		//Create the query
	const query = new AccountBalanceQuery()
		.setAccountId(accountId);

	//Sign with the client operator private key and submit to a Hedera network
	const tokenBalance = await query.execute(client);

	console.log("The token balance(s) for this account: " +tokenBalance.tokens.toString());
	
	//Create new HTS Token
	
	console.log("\nCreating the new TBA Token\n");
	
	var createTokenTx = await new TokenCreateTransaction()
		.setTokenName("TBAToken")
		.setTokenSymbol("TBAT")
		.setDecimals(5)
		.setInitialSupply(500)
		//.setTreasuryAccountId("0.0.2084028")
		.setTreasuryAccountId(moperatorId)
		.execute(client);
	
	var createReceipt = await createTokenTx.getReceipt(client);
	var newTokenId = createReceipt.tokenId;
	
	console.log('new TBA token id: ', newTokenId.toString(), '  ;)');
	
	//new account 
	
}
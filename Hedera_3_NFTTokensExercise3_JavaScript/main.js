
require("dotenv").config();

const {
    Client,
    PrivateKey,
    AccountBalanceQuery,
    Hbar,
	NftId,
    AccountId,
	TokenCreateTransaction,
	TokenAssociateTransaction,
	TokenInfoQuery,
	TokenType,
	TokenSupplyType,
	TokenMintTransaction,
	TokenBurnTransaction,
	AccountUpdateTransaction,
	TokenGetNftInfoQuery,
	TokenNftInfoQuery
	
} = require("@hashgraph/sdk");

var needBurn = false;   //Flag for burn token if needed.

main();

async function main() {
    let client;
	const moperatorId = AccountId.fromString(process.env.OPERATOR_ID);
	
	console.log("Greetings earthling....");
	console.log("This is a JavaScript Hedera API application");
	
	console.log("Attempting to get token info from the Hedera API...");
	
	
	const operatorId = AccountId.fromString(process.env.OPERATOR_ID);
	const operatorKey = PrivateKey.fromString(process.env.OPERATOR_KEY);
	
	// from a pre-configured network
	client = Client.forTestnet();
    

    client.setOperator(operatorId, operatorKey);
    
	
	//Create new HTS Token
	
	console.log("\nCreating the new TBA Badge Asset\n");
	
	
	CID = ["390501f749aee349d486ed844fdd7f0b55aed4afc2e3bb35e62c709040a6c18c"];
	//CID = ["390501f749aee349d486ed844fdd7f0b55aed4afc2e3bb35e62c709040a6cz4b"];
	
	
	
	
	//Create NFT class
	let createTokenTx = await new TokenCreateTransaction()
		.setTokenName("TBAToken")
		.setTokenSymbol("TBAT")
		.setTokenType(TokenType.NonFungibleUnique)
		.setDecimals(0)
		.setInitialSupply(0)
		.setTreasuryAccountId(moperatorId)
		.setSupplyType(TokenSupplyType.Finite)
		.setMaxSupply(CID.length)
		.setAdminKey(operatorKey)
		.setSupplyKey(operatorKey)
		.freezeWith(client)
		.sign(operatorKey);
		
		let nftCreateSign = await createTokenTx.sign(operatorKey);
		let nftCreateSubmit = await createTokenTx.execute(client);
		let nftCreateRx = await nftCreateSubmit.getReceipt(client);
		let mytokenId = nftCreateRx.tokenId;
		console.log('Create NFT Id: ', mytokenId.toString() );
		
	console.log('');
	
	
	nftCert = [];
	for(var i = 0;i < CID.length;i++){
		nftCert[i] = await tokenMinterFcn(CID[i])
		console.log('created NFT of class type: ', mytokenId.toString(), ' serial number: ', nftCert[i].serials[0].low.toString());
	}
	
	var tokenInfo = await new TokenInfoQuery()
		.setTokenId(mytokenId)
		.execute(client);
	console.log('Current NFT type ID: ' ,mytokenId.toString(), ' ', tokenInfo.tokenType.toString(), ' supply with serial:', CID.length.toString(), ' is: ', tokenInfo.totalSupply.toString());
	
	
	const nftInfos = await new TokenNftInfoQuery()
		.setNftId(new NftId(mytokenId, CID.length))
		.execute(client);
	console.log('\nnftInfos \n' , nftInfos.toString(), ' ');
	
//v2.0.28
	
	//Burn NFT if needed
	if(needBurn == true){
		//Burn if needed
		let tokenBurnTx = await new TokenBurnTransaction()
			.setTokenId(mytokenId)
			.setSerials([CID.length])
			.freezeWith(client)
			.sign(operatorKey);
			
		let tokenBurnSubmit = await tokenBurnTx.execute(client);
		let tokenBurnRx = await tokenBurnSubmit.getReceipt(client);
		console.log('\nBurned token with serial ', CID.length.toString(), ':', tokenBurnRx.status.toString() );
	
		var tokenInfo = await new TokenInfoQuery()
			.setTokenId(mytokenId)
			.execute(client);
		console.log('Current NFT type ID: ' , mytokenId.toString(),' ',tokenInfo.tokenType.toString(), ' supply with serial ', CID.length.toString(), ': ', tokenInfo.totalSupply.toString());
	
	}
	
	async function tokenMinterFcn(CID){
		mintTx = await new TokenMintTransaction()
			.setTokenId(mytokenId)
			.setMetadata([Buffer.from(CID)]) //setting NFT metadata
			.freezeWith(client);
		let mintTxSign = await mintTx.sign(operatorKey);
		let mintTxSubmit = await mintTxSign.execute(client);
		let mintRx = await mintTxSubmit.getReceipt(client);
		return mintRx;
	}
	
}
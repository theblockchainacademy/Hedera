import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Collections;
import java.util.concurrent.TimeoutException;

public final class HederaTokenService {
    
    //	
    //private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
    //private static final AccountId OPERATOR_ID = AccountId.fromString("0.0.2084028");

    // HEDERA_NETWORK defaults to testnet if not specified in dotenv
    private static final AccountId OPERATOR_ID = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");


    private HederaTokenService() {
    }

    public static void main(String[] args) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        //create a client to interact with Hedera nodes on the specified network.
        Client client = Client.forName(HEDERA_NETWORK);

        // Defaults the operator account ID and key such that all generated transactions will be paid for
        // by this account and be signed by this key
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        // Create three accounts, Alice, Bob, and Charlie.  Alice will be the treasury for our example token.
        // Fees only apply in transactions not involving the treasury, so we need two other accounts.

        PrivateKey aliceKey = PrivateKey.generate();
        AccountId aliceId = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(10))
                .setKey(aliceKey)
                .freezeWith(client)
                .sign(aliceKey)
                .execute(client)
                .getReceipt(client)
                .accountId;

        PrivateKey bobKey = PrivateKey.generate();
        AccountId bobId = new AccountCreateTransaction()
                .setInitialBalance(new Hbar(10))
                .setKey(bobKey)
                .freezeWith(client)
                .sign(bobKey)
                .execute(client)
                .getReceipt(client)
                .accountId;

        System.out.println("Alice's account ID: " + aliceId);
        System.out.println("Bob's account ID: " + bobId);

        Hbar aliceHbarBefore = new AccountBalanceQuery()
                .setAccountId(aliceId)
                .execute(client)
                .hbars;

        System.out.println("Alice's Hbar balance before token transfers: " + aliceHbarBefore);

        // In this example the fee is in Hbar, but you can charge a fixed fee in a token if you'd like.
        // EG, you can make it so that each time an account transfers Foo tokens,
        // they must pay a fee in Bar tokens to the fee collecting account.
        // To charge a fixed fee in tokens, instead of calling setHbarAmount(), call
        // setDenominatingTokenId(tokenForFee) and setAmount(tokenFeeAmount).

        // Setting the feeScheduleKey to Alice's key will enable Alice to change the custom
        // fees list on this token later using the TokenFeeScheduleUpdateTransaction.
        // We will create an initial supply of 100 of these tokens.

        TokenId tokenId = new TokenCreateTransaction()
                .setTokenName("Example Token")
                .setTokenSymbol("EX")
                .setInitialSupply(20)
                //.setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setAdminKey(aliceKey) // so that we can delete later on
                .setSupplyKey(aliceKey) // so that we can mint later on
                .setTreasuryAccountId(aliceId) //initial supply and any additional tokens that are minted
                .freezeWith(client)
                .sign(aliceKey)
                .execute(client)
                .getReceipt(client)
                .tokenId;

        System.out.println("Token: " + tokenId); //Return the Token ID

        TokenInfo tokenInfo = new TokenInfoQuery()
                .setTokenId(tokenId)
                .execute(client);

        System.out.println(tokenInfo.tokenType);

        // We must associate the token with Bob before it can be traded.
        new TokenAssociateTransaction()
                .setAccountId(bobId)
                .setTokenIds(Collections.singletonList(tokenId))
                .freezeWith(client)
                .sign(bobKey)
                .execute(client)
                .getReceipt(client);

        System.out.println("Minting");

        TokenMintTransaction transaction = new TokenMintTransaction()
                .setTokenId(tokenId)
                .setAmount(100);

        //Get a response for transaction success or failure
        TransactionResponse txResponse = transaction.freezeWith(client)
                .sign(aliceKey)
                .execute(client);

        TransactionReceipt receipt = txResponse.getReceipt(client);
        Status transactionStatus = receipt.status;
        System.out.println("The transaction consensus status is " +transactionStatus);

        AccountBalance accountBalance = new AccountBalanceQuery()
                .setAccountId(aliceId)
                .execute(client);

        System.out.println("The Alice account balance before is: " +accountBalance);

        AccountBalance accountBalanceBob = new AccountBalanceQuery()
                .setAccountId(bobId)
                .execute(client);

        System.out.println("The Bob account balance before is: " +accountBalanceBob);

        // send the token to Bob
        System.out.println("Transfer");
        new TransferTransaction()
                .addTokenTransfer(tokenId, aliceId, -40)
                .addTokenTransfer(tokenId, bobId, 40)
                .addHbarTransfer(bobId, Hbar.from(-1))
                .addHbarTransfer(aliceId, Hbar.from(1))
                .freezeWith(client)
                .sign(aliceKey)
                .sign(bobKey)
                .execute(client)
                .getReceipt(client);

        AccountBalance accountBalanceAfter = new AccountBalanceQuery()
                .setAccountId(aliceId)
                .execute(client);

        System.out.println("The Alice account balance after is: " +accountBalanceAfter);

        AccountBalance accountBalanceBoba = new AccountBalanceQuery()
                .setAccountId(bobId)
                .execute(client);

        System.out.println("The Bob account balance after is: " +accountBalanceBoba);

        Hbar aliceHbarAfter = new AccountBalanceQuery()
                .setAccountId(aliceId)
                .execute(client)
                .hbars;

        System.out.println("Alice's Hbar balance after token: " + aliceHbarAfter);

        client.close();

    }

}
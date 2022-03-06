import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;

public final class TransactionSetup {

    /// see `.env.sample` in the repository root for how to specify these values
    // or set environment variables with the same names
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("MY_ACCOUNT_ID")));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("MY_PRIVATE_KEY")));
    // HEDERA_NETWORK defaults to testnet if not specified in dotenv
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");


    private TransactionSetup() {
    }

    public static void main(String[] args) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {
        Client client = Client.forName(HEDERA_NETWORK);

        // Defaults the operator account ID and key such that all generated transactions will be paid for
        // by this account and be signed by this key
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        // Create three accounts, Alice and Bob. Alice will be the treasury for our example token.
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

        System.out.println("Alice account ID: " + aliceId);
        System.out.println("Bob account ID: " + bobId);

        TokenId tokenId = new TokenCreateTransaction()
                .setTokenName("TBA NFT")
                .setTokenSymbol("TBA-NFT")
                .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
                .setAdminKey(aliceKey) // so that we can delete later on
                .setSupplyKey(aliceKey) // so that we can mint later on
                .setTreasuryAccountId(aliceId)
                .freezeWith(client)
                .sign(aliceKey)
                .execute(client)
                .getReceipt(client)
                .tokenId;

        System.out.println("NFT Token ID: " + tokenId);

        // We must associate the token with Bob and Charlie before they can trade in it.

        new TokenAssociateTransaction()
                .setAccountId(bobId)
                .setTokenIds(Collections.singletonList(tokenId))
                .freezeWith(client)
                .sign(bobKey)
                .execute(client)
                .getReceipt(client);

        AccountBalance accountBalance = new AccountBalanceQuery() //One method to get account balance
                .setAccountId(aliceId)
                .execute(client);

        System.out.println("The Alice account balance before is: " +accountBalance);

        AccountBalance accountBobBalance = new AccountBalanceQuery() //One method to get account balance
                .setAccountId(bobId)
                .execute(client);

        System.out.println("The Bob account balance before is: " +accountBobBalance);

        //Transaction record used to output transaction details.
        //Send the last part of the NFT to the receiver. Charge HBar fees and get a record.
        TransactionRecord record1 = new TransferTransaction()
                //.addNftTransfer(new NftId(tokenId, serials.size()), aliceId, bobId)
                .addHbarTransfer(bobId, Hbar.from(-4))
                .addHbarTransfer(aliceId, Hbar.from(4))
                .freezeWith(client)
                .sign(bobKey)
                .sign(aliceKey)
                .execute(client)
                .getRecord(client);

        System.out.println("Transaction record: tokenID: {[TokenNftTransfer{sender, receiver, serial}]} " + record1.tokenNftTransfers);

        Hbar aliceHbar2 = new AccountBalanceQuery()
                .setAccountId(aliceId)
                .execute(client)
                .hbars;


        Hbar bobHbar2 = new AccountBalanceQuery()
                .setAccountId(bobId)
                .execute(client)
                .hbars;

        System.out.println("Alices's HBar balance after token transfer to Bob: " + aliceHbar2);

        System.out.println("Bob's HBar balance after token transfer from Alice: " + bobHbar2);

        client.close();

    }
}

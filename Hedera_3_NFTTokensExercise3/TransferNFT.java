import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;

public final class TransferNFT {

    /// see `.env.sample` in the repository root for how to specify these values
    // or set environment variables with the same names
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("MY_ACCOUNT_ID")));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("MY_PRIVATE_KEY")));
    // HEDERA_NETWORK defaults to testnet if not specified in dotenv
    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK", "testnet");


    private TransferNFT() {
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

        TokenInfo tokenInfo = new TokenInfoQuery()
                .setTokenId(tokenId)
                .execute(client);

        System.out.println(tokenInfo.tokenType);

        // We must associate the token with Bob and Charlie before they can trade in it.

        new TokenAssociateTransaction()
                .setAccountId(bobId)
                .setTokenIds(Collections.singletonList(tokenId))
                .freezeWith(client)
                .sign(bobKey)
                .execute(client)
                .getReceipt(client);

        // create metadata for two NFTs
        byte[] metadata = "Jim Woz Here".getBytes(StandardCharsets.UTF_8);
        byte[] metadata1 = "Ryan Woz Here".getBytes(StandardCharsets.UTF_8);
        byte[] img0 = "https://theblockchainacademy.com/wp-content/uploads/sites/6/2021/03/the-blockchain-academy-logo.png".getBytes(StandardCharsets.UTF_8);

        // put the metadata for each NFT into an array
        List<byte[]> metadatas = new ArrayList<>();
        metadatas.add(metadata);
        metadatas.add(metadata1);
        metadatas.add(img0);

        // mint an nft
        System.out.println("Minting");
        List<Long> serials = new TokenMintTransaction()
                .setTokenId(tokenId)
                .setMetadata(metadatas) // set the metadata using the array above
                .freezeWith(client)
                .sign(aliceKey)
                .execute(client)
                .getReceipt(client)
                .serials;

        //serials.size() contains the number of parts that make up the NFT
        System.out.println("The NFT has " + serials.size() + " individual parts.");

        //serials is an array of longs, 0, 1, 2, 3 and so on ...

        // resulting nftIds tokenId.0, tokenId.1, ....

        // limit on transaction size is 6144 bytes -> TRANSACTION_OVERSIZE

        AccountBalance accountBalance = new AccountBalanceQuery() //One method to get account balance
                .setAccountId(aliceId)
                .execute(client);

        System.out.println("The Alice account balance before is: " +accountBalance);

        AccountBalance accountBobBalance = new AccountBalanceQuery() //One method to get account balance
                .setAccountId(bobId)
                .execute(client);

        System.out.println("The Bob account balance before is: " +accountBobBalance);

        // send the all NFTs to Bob
        //The NFT is in 3 parts, this loop operates on the first 2 of 3. This is why serials.size()-1 is used in the loop.
        // Send all but the last 1 to the receiver
        System.out.println("Transfer");
        long mySize = serials.size() -1;
        for (int i = 0;i<mySize;i++) {
            new TransferTransaction()
                    .addNftTransfer(new NftId(tokenId, serials.get(i)), aliceId, bobId)
                    .freezeWith(client)
                    .sign(bobKey)
                    .sign(aliceKey)
                    .execute(client);
        }

        //Transaction record used to output transaction details.
        //Send the last part of the NFT to the receiver. Charge HBar fees and get a record.
        TransactionRecord record1 = new TransferTransaction()
                .addNftTransfer(new NftId(tokenId, serials.size()), aliceId, bobId)
                .addHbarTransfer(bobId, Hbar.from(-4))
                .addHbarTransfer(aliceId, Hbar.from(4))
                .freezeWith(client)
                .sign(bobKey)
                .sign(aliceKey)
                .execute(client)
                .getRecord(client);

        System.out.println("Transaction record: tokenID: {[TokenNftTransfer{sender, receiver, serial}]} " + record1.tokenNftTransfers);

        //a slightly different way to return account balances
        Map<TokenId, Long> aliceToken = new AccountBalanceQuery()
                .setAccountId(aliceId)
                .execute(client)
                .tokens;

        Hbar aliceHbar2 = new AccountBalanceQuery()
                .setAccountId(aliceId)
                .execute(client)
                .hbars;

        Map<TokenId, Long> bobTokens = new AccountBalanceQuery()
                .setAccountId(bobId)
                .execute(client)
                .tokens;

        Hbar bobHbar2 = new AccountBalanceQuery()
                .setAccountId(bobId)
                .execute(client)
                .hbars;

        System.out.println("Alices's NFT balance after transfer to Bob: " + aliceToken);
        System.out.println("Alices's HBar balance after token transfer to Bob: " + aliceHbar2);

        System.out.println("Bob's NFT balance after transfer from Alice: " + bobTokens);
        System.out.println("Bob's HBar balance after token transfer from Alice: " + bobHbar2);

        client.close();

    }
}


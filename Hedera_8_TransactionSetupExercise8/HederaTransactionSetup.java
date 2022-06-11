import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class HederaTransactionSetup {

    public static void main(String[] args) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {

        //Grab your Hedera testnet account ID and private key
        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        PrivateKey myPrivateKey = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));

        //Create your Hedera testnet client
        Client client = Client.forTestnet();
        client.setOperator(myAccountId, myPrivateKey);

        // Generate a new key pair
        PrivateKey newAccountPrivateKey = PrivateKey.generate();
        PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();

        //Create new account and assign the public key
        TransactionResponse newAccount = new AccountCreateTransaction()
                .setKey(newAccountPublicKey)
                .setInitialBalance( Hbar.fromTinybars(1000))
                .execute(client);

        // Get the new account ID
        AccountId newAccountId = newAccount.getReceipt(client).accountId;

        System.out.println("The new account ID is: " +newAccountId);

        //Check the new account's balance
        AccountBalance accountBalance = new AccountBalanceQuery()
                .setAccountId(newAccountId)
                .execute(client);

        System.out.println("The new account balance is: " +accountBalance.hbars);

        //Transfer hbar
        TransactionResponse sendHbar = new TransferTransaction()
                .addHbarTransfer(myAccountId, Hbar.fromTinybars(-500))
                .addHbarTransfer(newAccountId, Hbar.fromTinybars(500))
                .execute(client);

        System.out.println("The transfer transaction was: " +sendHbar.getReceipt(client).status);

        //Request the cost of the query
        Hbar queryCost = new AccountBalanceQuery()
                .setAccountId(newAccountId)
                .getCost(client);

        System.out.println("The cost of this query is: " +queryCost);

        //Check the new account's balance
        AccountBalance accountBalanceNew = new AccountBalanceQuery()
                .setAccountId(newAccountId)
                .execute(client);

        System.out.println("The new account balance is: " +accountBalanceNew.hbars);

    }
}

import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.concurrent.TimeoutException;

public class HederaExamples {

    public static void main(String[] args) throws TimeoutException, PrecheckStatusException, ReceiptStatusException {

        //Grab your Hedera testnet account ID and private key
        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        PrivateKey myPrivateKey = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));

        //Create your Hedera testnet client
        Client client = Client.forTestnet();
        client.setOperator(myAccountId, myPrivateKey);

      
    }
}

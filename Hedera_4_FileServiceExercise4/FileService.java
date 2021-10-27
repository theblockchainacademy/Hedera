import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class FileService {

    public static void main(String[] args) throws TimeoutException, PrecheckStatusException, ReceiptStatusException, IOException, NoSuchAlgorithmException {

        String myFile = "accounts.txt";

        //pass the filename as a File object
        File f = new File(myFile);

        String content = Files.readString(Paths.get(myFile)); //file object is content

        //Grab your Hedera testnet account ID and private key
        AccountId myAccountId = AccountId.fromString(Dotenv.load().get("MY_ACCOUNT_ID"));
        PrivateKey myPrivateKey = PrivateKey.fromString(Dotenv.load().get("MY_PRIVATE_KEY"));

        //Create your Hedera testnet client
        Client client = Client.forTestnet();
        client.setOperator(myAccountId, myPrivateKey);

        // Generate a new key pair
        PrivateKey newAccountPrivateKey = PrivateKey.generate();
        PublicKey newAccountPublicKey = newAccountPrivateKey.getPublicKey();

        //Create the transaction
        FileCreateTransaction transaction = new FileCreateTransaction()
                .setKeys(myPrivateKey)   //set the file key
                .setFileMemo("accounts file") //add a file memo
                .setContents(content);  //set contents of the file with the content file object

        //Change the default max transaction fee to 2 hbars
        FileCreateTransaction modifyMaxTransactionFee = transaction.setMaxTransactionFee(new Hbar(2));

        //Prepare transaction for signing, sign with the key on the file, sign with the client operator key and submit to a Hedera network
        TransactionResponse txResponse = modifyMaxTransactionFee
                .freezeWith(client)
                .sign(myPrivateKey)
                .execute(client);

        //Request the receipt
        TransactionReceipt receipt = txResponse.getReceipt(client);

        //Get the file ID
        FileId newFileId = receipt.fileId;

        System.out.println("\nThe new file ID is: " + newFileId + "\n");

        //Get file contents, the file object is transaction. Get the file contents into thhe getContents object
        ByteString getContents = transaction.getContents();

        //Convert the contents of the getContents object to String data, and move string theFile
        String theFile = getContents.toString();

        //output the string to the console
        System.out.println("\nThe new file contents is: " + theFile + "\n");

        FileInfoQuery query = new FileInfoQuery()
                .setFileId(newFileId);

        FileInfo getInfo = query.execute(client);

        System.out.println("File response: " + getInfo);

    }
}

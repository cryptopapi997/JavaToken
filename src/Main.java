import com.javaToken.JavaToken;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {
        // Ganache port is 7545
        Web3j w3 = Web3j.build(new HttpService("HTTP://127.0.0.1:7545"));

        String privateKey = "YourPrivateKeyHere";
        Credentials credentials = Credentials.create(privateKey);

        String contractAddress = "ContractAddressHere";

        // Deploy the contract/Fetch an already deployed contract

        JavaToken javaToken = getDeployedContract(contractAddress, credentials, w3,
                BigInteger.valueOf(200000));

        // Do more smart contract things :)
        // ...
    }

    static JavaToken deploy(BigInteger gasLimit,
                            BigInteger initialSupply, Credentials credentials, Web3j w3)
            throws Exception {
        return JavaToken.deploy(w3, credentials, getContractGasProvider(getGasPrice(w3), gasLimit),
                initialSupply).send();
    }

    static JavaToken getDeployedContract(String address, Credentials credentials, Web3j w3,
                                         BigInteger gasLimit) throws IOException {
        return JavaToken.load(address, w3, credentials, getContractGasProvider(getGasPrice(w3), gasLimit));
    }

    static BigInteger getBalance(String address, JavaToken javaToken) throws Exception {
        return javaToken.balanceOf(address).send();
    }

    static TransactionReceipt transfer(String to, BigInteger value, JavaToken javaToken) throws Exception {
        return javaToken.transfer(to,value).send();
    }

    static BigInteger getBalanceWithoutWrapper(String address, String contractAddress, Web3j w3) throws IOException {

        // Define the function we want to invoke from the smart contract
        Function function = new Function("balanceOf", Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<Uint256>() {}));

        // Encode it for the contract to understand
        String encodedFunction = FunctionEncoder.encode(function);

        /*
         Send the request and wait for the response using eth call since
         it's a read only transaction with no cost associated
         */
        EthCall response = w3.ethCall(
                Transaction.createEthCallTransaction(address, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST).send();

        return Numeric.toBigInt(response.getValue());

    }

    static TransactionReceipt transferWithoutWrapper(String to, BigInteger value, String contractAddress,
                                                     Credentials credentials, BigInteger gasLimit,
                                                     Web3j w3) throws IOException {

        // Define the function we want to invoke from the smart contract
        Function function = new Function("transfer", Arrays.asList(new Address(to), new Uint256(value)),
                Collections.emptyList());

        // Encode it for the contract to understand
        String encodedFunction = FunctionEncoder.encode(function);

        /*
         Need to use a TransactionManager here since transfer actually alters the state of the blockchain
         and credentials are therefore relevant
         */
        TransactionManager transactionManager = new FastRawTransactionManager(w3, credentials);


        // Send the transaction off using transactionManager and wait for the hash
        String transactionHash = transactionManager.sendTransaction(getGasPrice(w3), gasLimit,
                contractAddress, encodedFunction, BigInteger.ZERO).getTransactionHash();

        // Fetch the transaction receipt
        Optional<TransactionReceipt> transactionReceipt =
                w3.ethGetTransactionReceipt(transactionHash).send().getTransactionReceipt();

        if(transactionReceipt.isEmpty())
            return null;
        return transactionReceipt.get();

    }

    // Helper functions
    
     /*
     For more customization you can generate a custom gas provider for your contract here to set different
     gas limits for each method call
     */
    static ContractGasProvider getContractGasProvider(BigInteger gasPrice, BigInteger gasLimit) {
        return new ContractGasProvider() {
            @Override
            public BigInteger getGasPrice(String s) {
                return gasPrice;
            }

            @Override
            public BigInteger getGasPrice() {
                return gasPrice;
            }

            @Override
            public BigInteger getGasLimit(String s) {
                return gasLimit;
            }

            @Override
            public BigInteger getGasLimit() {
                return gasLimit;
            }
        };
    }

     // Gets the gas price off the network instead of using the standard
    static BigInteger getGasPrice(Web3j w3) throws IOException {
        EthGasPrice ethGasPrice = w3.ethGasPrice().send();
        return ethGasPrice.getGasPrice();

    }
}

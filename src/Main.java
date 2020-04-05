import com.javaToken.JavaToken;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;

public class Main {

    public static void main(String[] args) throws Exception {
        // Ganache port is 7545
        Web3j w3 = Web3j.build(new HttpService("HTTP://127.0.0.1:7545"));
        // Private key taken from Ganache test network, NEVER share your private Key with anyone
        String privateKey = "91530b694c784cc79da98a19862932488521790d779a75b29744549e037d6e4a";
        Credentials credentials = Credentials.create(privateKey);

        EthGasPrice ethGasPrice = w3.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();

        // Deploy the contract
        JavaToken javaToken = deploy(gasPrice, BigInteger.valueOf(2000000),
                BigInteger.valueOf(100000),credentials,w3);

    }

    static JavaToken deploy(BigInteger gasPrice, BigInteger gasLimit,
                            BigInteger initialSupply, Credentials credentials, Web3j w3)
            throws Exception {

        return JavaToken.deploy(w3, credentials, new ContractGasProvider() {
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
        },initialSupply).send();
    }

}

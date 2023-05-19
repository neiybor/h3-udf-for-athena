package  com.aws.athena.udf.h3.infrastructure;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

/** The main application to deploy UDF stack using CDK. 
 *  The code assumes that the account and region are available in your environment. 
 *  
 */
public class DeployApp {
    public static void main(final String[] args) {
        final App app = new App();

        new AthenaUDFStack(app, "H3AthenaUDF", StackProps.builder()
            .env(Environment.builder().build())
            .build());
        app.synth();
    }
}

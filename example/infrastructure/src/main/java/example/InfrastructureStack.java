package example;

import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.PayloadFormatVersion;
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegrationProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Table exampleTable = new Table(this, "ExampleTable", TableProps.builder()
                .partitionKey(Attribute.builder()
                        .type(AttributeType.STRING)
                        .name("id").build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

        LayerVersion jre17LayerARM = new LayerVersion(this, "Java17LayerARM", LayerVersionProps.builder()
                .layerVersionName("JRE17LayerARM")
                .description("JRE 17 ARM")
                .compatibleArchitectures(singletonList(Architecture.ARM_64))
                .compatibleRuntimes(Arrays.asList(Runtime.PROVIDED_AL2))
                .code(Code.fromAsset("../../jre-17-layer-arm.zip"))
                .build());

        LayerVersion jre17LayerX86 = new LayerVersion(this, "Java17LayerX86", LayerVersionProps.builder()
                .layerVersionName("JRE17LayerX86")
                .description("JRE 17 X86")
                .compatibleArchitectures(singletonList(Architecture.X86_64))
                .compatibleRuntimes(Arrays.asList(Runtime.PROVIDED_AL2))
                .code(Code.fromAsset("../../jre-17-layer-x86.zip"))
                .build());

        Function jre17FunctionARM = new Function(this, "JRE17FunctionARM", FunctionProps.builder()
                .functionName("jre-17-function-arm")
                .description("jre-17-function-arm")
                .architecture(Architecture.ARM_64)
                .layers(singletonList(jre17LayerARM))
                .handler("example.ExampleDynamoDbHandler::handleRequest")
                .runtime(Runtime.PROVIDED_AL2)
                .code(Code.fromAsset("../software/ExampleFunction/target/function.jar"))
                .memorySize(512)
                .environment(mapOf("TABLE_NAME", exampleTable.getTableName()))
                .timeout(Duration.seconds(20))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

        Function jre17FunctionX86 = new Function(this, "JRE17FunctionX86", FunctionProps.builder()
                .functionName("jre-17-function-x86")
                .description("jre-17-function-x86")
                .architecture(Architecture.X86_64)
                .layers(singletonList(jre17LayerX86))
                .handler("example.ExampleDynamoDbHandler::handleRequest")
                .runtime(Runtime.PROVIDED_AL2)
                .code(Code.fromAsset("../software/ExampleFunction/target/function.jar"))
                .memorySize(512)
                .environment(mapOf("TABLE_NAME", exampleTable.getTableName()))
                .timeout(Duration.seconds(20))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

        exampleTable.grantWriteData(jre17FunctionARM);
        exampleTable.grantWriteData(jre17FunctionX86);

        HttpApi httpApi = new HttpApi(this, "JRE17LayerExampleApi", HttpApiProps.builder()
                .apiName("JRE17LayerExampleApi")
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/arm")
                .methods(singletonList(HttpMethod.GET))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(jre17FunctionARM)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/x86")
                .methods(singletonList(HttpMethod.GET))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(jre17FunctionX86)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());

        new CfnOutput(this, "api-endpoint", CfnOutputProps.builder()
                .value(httpApi.getApiEndpoint())
                .build());
    }

    private Map<String, String> mapOf(String... keyValues) {
        Map<String, String> map = new HashMap<>(keyValues.length/2);
        for (int i = 0; i < keyValues.length; i++) {
            if(i % 2 == 0) {
                map.put(keyValues[i], keyValues[i + 1]);
            }
        }
        return map;
    }
}

#!/bin/sh

cd example/software/ExampleFunction
mvn clean package

cd ../../infrastructure
cdk synth
cdk deploy --outputs-file target/outputs.json

curl -i $(cat target/outputs.json | jq -r '.InfrastructureJRE17LayerStack.apiendpoint')/arm
curl -i $(cat target/outputs.json | jq -r '.InfrastructureJRE17LayerStack.apiendpoint')/x86

import ballerina/http;

final http:Client pineValleyEp = check new ("http://localhost:9091/pineValley/");
final http:Client grandOakEp = check new ("http://localhost:9092/grandOak/");

type PineValleyPayload record {
    string doctorType;
};

service / on new http:Listener(9090) {
    resource function get doctor/[string doctorType]() returns json|error? {
        @display {
            label: "Node",
            templateId: "StartNode",
            xCord: 1000,
            yCord: 1000,
            metadata: "JTdCJTIyb3V0cHV0cyUyMiUzQSU1QiU3QiUyMm5hbWUlMjIlM0ElMjJvdXRWYXIlMjIlMkMlMjJ0eXBlJTIyJTNBJTIyKCklMjIlN0QlNUQlN0Q="
        }
        worker StartNode returns error? {
            _ = <- function;
        }

        @display {
            label: "Node",
            templateId: "TransformNode",
            xCord: 679,
            yCord: 868,
            metadata: "JTdCJTIyaW5wdXRzJTIyJTNBJTVCJTdCJTIybmFtZSUyMiUzQSUyMmluVmFyJTIyJTJDJTIydHlwZSUyMiUzQSUyMmpzb24lMjIlN0QlMkMlN0IlMjJuYW1lJTIyJTNBJTIyaW5WYXIxJTIyJTJDJTIydHlwZSUyMiUzQSUyMmpzb24lMjIlN0QlNUQlMkMlMjJvdXRwdXRzJTIyJTNBJTVCJTdCJTIybmFtZSUyMiUzQSUyMnBheWxvYWQlMjIlMkMlMjJ0eXBlJTIyJTNBJTIyanNvbiUyMiU3RCU1RCU3RA=="
        }
        worker TransformNode_1 returns error? {
            json transformnode_1_transformed = check transformnode_1_transform(inVar, inVar1);
        }

        () -> StartNode;
    }
}

function transformnode_1_transform(json inVar, json inVar1) returns json|error => ();

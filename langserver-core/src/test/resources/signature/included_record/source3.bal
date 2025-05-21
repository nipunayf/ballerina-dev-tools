import ballerina/http;

type Response record {
    json body;
    http:RetryConfig retryConfig;
};

public function main() {
    int val = getInt();
}

function getInt(*Response response) returns int {
    return 1;
}

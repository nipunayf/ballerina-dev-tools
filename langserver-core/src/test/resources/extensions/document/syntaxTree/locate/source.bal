import ballerina/log;

public function bar() {
    int x = 10;
    int y = 20;
    log:printInfo("sum", result = x + y);
}

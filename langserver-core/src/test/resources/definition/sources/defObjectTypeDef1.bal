import ballerina/http;

public function testStdlibGotoDefinition() returns http:ClientError? {
    http:Client cl = check new ("http://loaclhost:9090");
    http:Listener|http:ListenerError defaultListener = http:getDefaultListener();
    json response = check cl->post("/test", "Hello World");
}

public function testLangLig() {
    string[] stringArr = [];
    int length = stringArr.length();
}

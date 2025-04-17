import ballerina/ftp;
import ballerina/io;
import ballerina/log;
import ballerina/http;

listener ftp:Listener ftpListener = new ({
    protocol: ftp:FTP,
    host: "127.0.0.1",
    port: 21,
    pollingInterval: 5,
    fileNamePattern: "(.*).txt"
});

http:Client httpClient = check new ("http://localhost:8080");

service on ftpListener {
    remote function onFileChange(ftp:WatchEvent & readonly event, ftp:Caller caller) returns error? {
        foreach ftp:FileInfo fileInfo in event.addedFiles {
            log:printInfo("Added file path: " + fileInfo.path);

            stream<byte[] & readonly, io:Error?> fileStream = check caller->get(fileInfo.path);

            check caller->delete(fileInfo.path);
        }
    }
}

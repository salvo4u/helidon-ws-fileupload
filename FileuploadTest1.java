/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.webserver.examples.websocket;

import io.helidon.common.reactive.Single;
import io.helidon.webserver.WebServer;
import org.glassfish.tyrus.client.ClientManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.helidon.webserver.examples.websocket.Main.startWebServer;
import static org.junit.jupiter.api.Assertions.fail;

public class FileuploadTest1 {
    private static ClientManager websocketClient = ClientManager.createClient();
    private static WebServer server;
    //BUFFER_SIZE should be always less than 2700 in my local test and will effect time taken to upload.
    //This was tried with 4 different files of sizes 3MB,15MB,2GB and 5GB
    //At 2800 above the file size produced on the server was inconsistent for a 2.04GB file so this variable might have to adjusted as per the network.
    private static long FILE_SIZE, BUFFER_SIZE = 10000;

    @BeforeAll
    static void initClass() {
        server = startWebServer();
    }

    @AfterAll
    static void destroyClass() {
        server.shutdown();
    }

    //M and R
    @Test
    public void testUpload() throws IOException, DeploymentException, InterruptedException, ExecutionException {
        long t1 = System.currentTimeMillis();
        URI websocketUri = URI.create("ws://localhost:" + server.port() + "/websocket/fileupload/");
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
        //String path = "C:\\file.txt";
        String path = "C:\\pdf.zip";
        //String path = "C:\\file.zip";

        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);

        FILE_SIZE = file.length();
        // CountDownLatch messageLatch = new CountDownLatch((int) FILE_SIZE);
        CompletableFuture<Boolean> f = new CompletableFuture<Boolean>();
        websocketClient.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        long M = FILE_SIZE / BUFFER_SIZE;
                        long R = FILE_SIZE % BUFFER_SIZE;


                        if (!message.equals("SENDFILENOW"))
                            return;
                        try {
                            System.out.println("Starting File read ... " + path + "  " + FILE_SIZE + "  " + M + "  " + message);
                            byte[] buffer = new byte[(int) BUFFER_SIZE];

                            while (M > 0) {
                                fileInputStream.read(buffer);
                                ByteBuffer bytebuffer = ByteBuffer.wrap(buffer);
                                session.getBasicRemote().sendBinary(bytebuffer);
                                M--;
                                FILE_SIZE -= buffer.length;
                            }
                            buffer = new byte[(int) R];
                            fileInputStream.read(buffer, 0, (int) R);
                            FILE_SIZE -= buffer.length;
                            fileInputStream.close();
                            ByteBuffer bytebuffer = ByteBuffer.wrap(buffer);
                            session.getBasicRemote().sendBinary(bytebuffer);
                            System.out.println("CLIENT FILESIZE " + FILE_SIZE + "  " + BUFFER_SIZE);
                            //TODO:How to remove this sleep[THis needs more research]
                            Thread.sleep(2000);
                            f.complete(true);
                            session.getBasicRemote().sendText("ENDOFFILE");
                        } catch (Exception e) {
                            fail("Unexpected exception " + e);
                        }
                    }
                });
                try {
                    session.getBasicRemote().sendText("FILESIZE=" + FILE_SIZE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(Session session, CloseReason closeReason) {
                System.out.println("Client OnClose called '" + closeReason + "'");
            }

            @Override
            public void onError(Session session, Throwable thr) {
                System.out.println("Client OnError called '" + thr + "'");
            }
        }, config, websocketUri);

        // Wait until file is written
        Single.create(f).flatMapSingle(aBoolean -> {
                    System.out.println("Client >> FILE IS SEND !!");
                    return Single.just("");
                }
        ).await();
        long t2 = System.currentTimeMillis();
        System.out.println("testUpload4 time in ms >>" + (t2 - t1));
    }
}

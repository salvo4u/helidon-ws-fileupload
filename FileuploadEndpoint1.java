/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
/**
 * Class MessageBoardEndpoint.
 */
public class FileuploadEndpoint1 extends Endpoint {
    private static final Logger LOGGER = Logger.getLogger(FileuploadEndpoint1.class.getName());
    long FILE_SIZE;
    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        String fileName = "C:\\newfile.zip";
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(fileName);
           // Files.deleteIfExists(Path.of(fileName));
            CompletableFuture<Boolean> f = new CompletableFuture<Boolean>();
            FileOutputStream finalFileOutputStream = fileOutputStream;


            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    System.out.println("Server >> " + message);
                    try {
                        if (message.contains("FILESIZE")) {
                            FILE_SIZE = Long.parseLong(message.split("=")[1]);
                            System.out.println("FILE_SIZE "+FILE_SIZE);
                            session.getBasicRemote().sendText("SENDFILENOW");
                        }
                        if(message.contains("ENDOFFILE")) {
                            System.out.println("Server >> FILE_SIZE=" + FILE_SIZE);
                            finalFileOutputStream.close();
                            session.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public void onMessage(ByteBuffer b) {
                    try {
                        finalFileOutputStream.write(b.array(), 0, b.array().length);
                        finalFileOutputStream.flush();
                        FILE_SIZE-=b.array().length;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }catch (Exception e){

        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("Server OnClose called '" + closeReason + "'");
        super.onClose(session, closeReason);
    }

    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }

}

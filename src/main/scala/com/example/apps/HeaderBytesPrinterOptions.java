/*
 * Copyright 2022 ksilin
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

package com.example.apps;

import picocli.CommandLine;

public class HeaderBytesPrinterOptions {
    @CommandLine.Option(names = "-t", description = "topic")
    String topic;

    @CommandLine.Option(names = { "-c", "--config-file" }, description = "the consumer config file")
    String configFile;


    @CommandLine.Option(names = "-d", description = "printDecimalBytes")
    boolean printDecimalBytes;

    @CommandLine.Option(names = "-h", description = "printHexBytes")
    boolean printHexBytes;

    @Override
    public String toString() {
        return "HeaderBytesPrinterOptions{" +
                "topic='" + topic + '\'' +
                ", configFile='" + configFile + '\'' +
                ", printDecimalBytes=" + printDecimalBytes +
                ", printHexBytes=" + printHexBytes +
                '}';
    }
}

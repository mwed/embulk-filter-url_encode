/*
 * Copyright 2016 Minnano Wedding Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.filter.url_encode;

import com.google.common.base.Throwables;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.PageTestUtils;
import org.embulk.spi.Schema;
import org.embulk.spi.TestPageBuilderReader;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.embulk.spi.type.Types.STRING;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestUrlEncodeFilterPlugin
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void testOpenWithDefault()
    {
        final Schema inputSchema = Schema.builder()
                .add("url", STRING)
                .build();

        final UrlEncodeFilterPlugin plugin = new UrlEncodeFilterPlugin();
        final ConfigSource configSource = loadConfigSource("default.yml");

        plugin.transaction(configSource, inputSchema, new FilterPlugin.Control()
        {
            @Override
            public void run(TaskSource taskSource, Schema outputSchema)
            {
                TestPageBuilderReader.MockPageOutput mockPageOutput = new TestPageBuilderReader.MockPageOutput();
                PageOutput pageOutput = plugin.open(taskSource, inputSchema, outputSchema, mockPageOutput);

                List<Page> pages = PageTestUtils.buildPage(runtime.getBufferAllocator(), inputSchema, "?q= aあ");
                for (Page page : pages) {
                    pageOutput.add(page);
                }

                pageOutput.finish();
                pageOutput.close();

                PageReader pageReader = new PageReader(outputSchema);
                for (Page page : mockPageOutput.pages) {
                    pageReader.setPage(page);

                    assertThat(pageReader.getString(0), is("%3Fq%3D%20a%E3%81%82"));
                }
            }
        });
    }

    @Test
    public void testOpenWithOnlyNonAscii()
    {
        final Schema inputSchema = Schema.builder()
                .add("url", STRING)
                .build();

        final UrlEncodeFilterPlugin plugin = new UrlEncodeFilterPlugin();
        final ConfigSource configSource = loadConfigSource("non_ascii_only_true.yml");

        plugin.transaction(configSource, inputSchema, new FilterPlugin.Control()
        {
            @Override
            public void run(TaskSource taskSource, Schema outputSchema)
            {
                TestPageBuilderReader.MockPageOutput mockPageOutput = new TestPageBuilderReader.MockPageOutput();
                PageOutput pageOutput = plugin.open(taskSource, inputSchema, outputSchema, mockPageOutput);

                List<Page> pages = PageTestUtils.buildPage(runtime.getBufferAllocator(), inputSchema, "?q= aあ");
                for (Page page : pages) {
                    pageOutput.add(page);
                }

                pageOutput.finish();
                pageOutput.close();

                PageReader pageReader = new PageReader(outputSchema);
                for (Page page : mockPageOutput.pages) {
                    pageReader.setPage(page);

                    assertThat(pageReader.getString(0), is("?q=%20a%E3%81%82"));
                }
            }
        });
    }

    private ConfigSource loadConfigSource(String yamlPath)
    {
        try {
            ConfigLoader loader = new ConfigLoader(Exec.getModelManager());
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(yamlPath);

            return loader.fromYaml(stream);
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}

package org.ebyhr.trino.storage.operator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Streams;
import io.trino.hive.$internal.org.json.JSONObject;
import io.trino.spi.TrinoException;
import org.ebyhr.trino.storage.StorageColumn;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.trino.spi.StandardErrorCode.TYPE_MISMATCH;
import static io.trino.spi.type.VarcharType.VARCHAR;

public class CohereApiPlugin implements FilePlugin {

    private static final String DELIMITER = ",";

    private static final CacheLoader<String, String> loader = new CacheLoader<String, String>() {
        @Override
        public @NotNull String load(String key) {
            return key.toUpperCase();
        }
    };

    private static LoadingCache<String, String> cache = CacheBuilder.newBuilder().maximumSize(50).expireAfterAccess(Duration.ofHours(1)).build(loader);

    @Override
    public List<StorageColumn> getFields(String path, Function<String, InputStream> streamProvider)
    {

        try {
            Splitter splitter = Splitter.on(DELIMITER).trimResults();
            String[] csvString = getCSVStringFull(path).split("##@@");
            List<String> fields = Arrays.stream(csvString).findFirst().map(splitter::splitToList).orElse(List.of(""));
            System.out.println("getFields fields:  " + path +" "+ fields);
            return fields.stream().map(field -> new StorageColumn(field, VARCHAR))
                    .collect(toImmutableList());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

//    @Override
//    public List<StorageColumn> getFields(String path, Function<String, InputStream> streamProvider)
//    {
//
//        try {
//            Splitter splitter = Splitter.on(DELIMITER).trimResults();
//            String csvString = getCSVStringFull(path);
//            List<String> fields = Arrays.stream(csvString).findFirst().map(splitter::splitToList).orElse(List.of(""));
//            System.out.println("getFields fields:  " + path +" "+ fields);
//            return fields.stream().map(field -> new StorageColumn(field, VARCHAR))
//                    .collect(toImmutableList());
//        }
//        catch (IOException e) {
//            throw new UncheckedIOException(e);
//        }
//    }

    public String callBot(String prompt) throws IOException {
        String csvString;
        System.out.println("====================== callBot ====================" + prompt + " " +cache.getIfPresent(prompt));
        if (cache.getIfPresent(prompt)==null) {
            String response = CohereApiHelper.getCSVFileFromGPT(prompt);
            JSONObject responseJson = new JSONObject(response);
            csvString = responseJson.getJSONArray("generations").getJSONObject(0).getString("text");
            System.out.println("====================== callBot ====================");
            System.out.println(csvString);
            cache.put(prompt,csvString);
        }
        else{
            System.out.println("Getting the results  from cache ................&&&& ...........");
            csvString = cache.getIfPresent(prompt);
        }
        return csvString.strip();
    }
    public String getCSVColumnFields(String prompt) throws IOException {
        String output = callBot(prompt);
        System.out.println(output);
        return output;
    }

    public String getCSVStringFull(String prompt) throws IOException {
        String newPrompt = prompt;
        return callBot(newPrompt);
    }


    @Override
    public Stream<List<?>> getRecordsIterator(String path, Function<String, InputStream> streamProvider) throws IOException {
        System.out.println("getRecordsIterator =================================");
        System.out.println(path);

        Splitter splitter = Splitter.on(DELIMITER).trimResults();
        String[] csvString = getCSVStringFull(path).split("##@@");
        System.out.println(Arrays.toString(csvString));
//        String[] csvString = "tn,dd qwq,refe".split(" ");
        return Arrays.stream(csvString).skip(1).map(splitter::splitToList);
//        return reader.lines()
//                .skip(1)
//                .map(splitter::splitToList);
    }

}

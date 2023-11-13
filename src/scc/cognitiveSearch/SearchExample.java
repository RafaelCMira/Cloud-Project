package scc.cognitiveSearch;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.SearchMode;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.util.SearchPagedIterable;
import com.azure.search.documents.util.SearchPagedResponse;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Based on the code from:
 * https://docs.microsoft.com/en-us/azure/search/search-get-started-java
 */
public class SearchExample {

    public static final String PROP_SERVICE_NAME = "SearchServiceName";
    public static final String PROP_SERVICE_URL = "SearchServiceUrl";
    public static final String PROP_INDEX_NAME = "IndexName";
    public static final String PROP_QUERY_KEY = "SearchServiceQueryKey";

    public static void main(String[] args) {

        try {
            // WHEN READING THE PROPERTIES SET IN AZURE
            SearchClient searchClient = new SearchClientBuilder()
                    .credential(new AzureKeyCredential(System.getenv(PROP_QUERY_KEY)))
                    .endpoint(System.getenv(PROP_SERVICE_URL))
                    .indexName(System.getenv(PROP_INDEX_NAME))
                    .buildClient();

            // SIMPLE QUERY
            // Check parameters at:
            // https://docs.microsoft.com/en-us/rest/api/searchservice/search-documents
            String queryText = "laboriosam";
            SearchOptions options = new SearchOptions().setIncludeTotalCount(true).setTop(5);

            SearchPagedIterable searchPagedIterable = searchClient.search(queryText, options, null);
            System.out.println("Number of results : " + searchPagedIterable.getTotalCount());

            for (SearchPagedResponse resultResponse : searchPagedIterable.iterableByPage()) {
                resultResponse.getValue().forEach(searchResult -> {
                    for (Map.Entry<String, Object> res : searchResult.getDocument(SearchDocument.class).entrySet()) {
                        System.out.printf("%s -> %s\n", res.getKey(), res.getValue());
                    }
                    System.out.println();
                });
            }

            System.out.println();
            System.out.println("=============== Second query ======================");
            queryText = "laboriosam";
            options = new SearchOptions().setIncludeTotalCount(true)
                    .setFilter("owner eq 'Margie.Prosacco'")
                    .setSelect("id", "name", "owner", "description")
                    .setSearchFields("name")
                    .setTop(5);

            searchPagedIterable = searchClient.search(queryText, options, null);
            System.out.println("Number of results : " + searchPagedIterable.getTotalCount());

            for (SearchPagedResponse resultResponse : searchPagedIterable.iterableByPage()) {
                resultResponse.getValue().forEach(searchResult -> {
                    for (Map.Entry<String, Object> res : searchResult.getDocument(SearchDocument.class).entrySet()) {
                        System.out.printf("%s -> %s\n", res.getKey(), res.getValue());
                    }
                    System.out.println();
                });
            }

            System.out.println();
            System.out.println("=============== Third query ======================");
            queryText = "laboriosam";
            options = new SearchOptions().setIncludeTotalCount(true)
                    .setSelect("id", "owner", "name", "location", "description")
                    .setSearchFields("name", "description")
                    .setSearchMode(SearchMode.ALL)
                    .setTop(5);

            searchPagedIterable = searchClient.search(queryText, options, null);
            System.out.println("Number of results : " + searchPagedIterable.getTotalCount());

            for (SearchPagedResponse resultResponse : searchPagedIterable.iterableByPage()) {
                resultResponse.getValue().forEach(searchResult -> {
                    for (Map.Entry<String, Object> res : searchResult.getDocument(SearchDocument.class).entrySet()) {
                        System.out.printf("%s -> %s\n", res.getKey(), res.getValue());
                    }
                    System.out.println();
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

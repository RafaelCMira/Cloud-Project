package scc.cognitiveSearch;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.util.SearchPagedIterable;

public class CognitiveSearchLayer {

    private static CognitiveSearchLayer instance;
    private final SearchClient client;
    public static final String PROP_SERVICE_NAME = "SearchServiceName";
    public static final String PROP_SERVICE_URL = "SearchServiceUrl";
    public static final String PROP_INDEX_NAME = "IndexName";
    public static final String PROP_QUERY_KEY = "SearchServiceQueryKey";

    public CognitiveSearchLayer(SearchClient client) { this.client = client; }

    public static synchronized CognitiveSearchLayer getInstance() {
        if (instance != null)
            return instance;

        SearchClient searchClient = new SearchClientBuilder()
                .credential(new AzureKeyCredential(System.getenv(PROP_QUERY_KEY)))
                .endpoint(System.getenv(PROP_SERVICE_URL))
                .indexName(System.getenv(PROP_INDEX_NAME))
                .buildClient();
        instance = new CognitiveSearchLayer(searchClient);
        return instance;
    }

    public SearchPagedIterable search(String queryText, SearchOptions options) throws Exception {
           return client.search(queryText, options, null);
    }
}

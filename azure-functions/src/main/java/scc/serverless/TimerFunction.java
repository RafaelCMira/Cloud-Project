package scc.serverless;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import scc.data.HouseDAO;
import scc.db.CosmosDBLayer;

import java.util.ArrayList;
import java.util.List;


public class TimerFunction {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private static final String TIMER = "0 */10 * * * *";
    private static final int THRESHOLD = 0;
    private static final int DISCOUNT = 10;

    @FunctionName("periodic-discount")
    public void cosmosFunction( @TimerTrigger(name = "periodic-discount",
                                schedule = TIMER)
                                String timerInfo,
                                ExecutionContext context)
    {

        var houses = db.getAll(CosmosDBFunction.HOUSES_COLLECTION, HouseDAO.class);
        for (HouseDAO h: houses) {
            if (h.getRentalsCounter() <= THRESHOLD ) {
                h.setDiscount(DISCOUNT);
                db.update(h,CosmosDBFunction.HOUSES_COLLECTION,h.getId());
            }
        }

    }
}

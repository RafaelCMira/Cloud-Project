package scc.serverless;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import scc.data.HouseDAO;
import scc.db.CosmosDBLayer;

public class TimerFunction {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private static final String TIMER = "*/30 * * * * *";
    private static final int THRESHOLD = 0;
    private static final int DISCOUNT = 10;
    private static final int LIMITS = 5;

    @FunctionName("periodicDiscount")
    public void cosmosFunction( @TimerTrigger(name = "periodicDiscount",
                                schedule = TIMER)
                                String timerInfo,
                                ExecutionContext context)
    {


        try {
            var res = db.getAll(CosmosDBFunction.HOUSES_COLLECTION, HouseDAO.class);
            int counter = 0;
            for (HouseDAO h: res) {
                counter++;
                if (h.getRentalsCounter() <= THRESHOLD && h.getDiscount() == 0) {
                    h.setDiscount(DISCOUNT);
                    db.update(h,CosmosDBFunction.HOUSES_COLLECTION,h.getId());
                    context.getLogger().info("Timer function changed house:" + h.getId() + " time:"+ timerInfo);
                }
                if (counter > LIMITS)
                    break;
            }

        } catch (Exception e) {
            return;
        }

    }
}

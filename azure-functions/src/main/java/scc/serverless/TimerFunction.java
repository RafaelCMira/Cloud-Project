package scc.serverless;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import scc.data.House;
import scc.data.HouseDAO;
import scc.db.CosmosDBLayer;
import scc.db.MongoDBLayer;

public class TimerFunction {

    private static final MongoDBLayer db = MongoDBLayer.getInstance();
    //private static final String TIMER = "* */5 * * * *";
    private static final int THRESHOLD = 0;
    private static final int DISCOUNT = 10;
    private static final int LIMITS = 5;

    private static final String HOUSES_COLLECTION = "houses";


    public static void main(String[] args) {
        timerFunction();
    }

    public static void timerFunction()
    {
        try {
            //context.getLogger().info("Timer function:"+ timerInfo);
            var housesDocuments = db.getAll(HOUSES_COLLECTION);
            int counter = 0;

            for (var doc : housesDocuments) {
                HouseDAO h = HouseDAO.fromDocument(doc);

                if (h.getRentalsCounter() <= THRESHOLD && h.getDiscount() == 0) {
                    counter++;
                    h.setDiscount(DISCOUNT);
                    db.update(h.getId(),h,HOUSES_COLLECTION);
                }
                if (counter > LIMITS)
                    break;
            }

        } catch (Exception e) {
            return;
        }

    }


}

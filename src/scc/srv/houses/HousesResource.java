package scc.srv.houses;

import scc.data.House;
import scc.data.HouseDAO;
import scc.db.CosmosDBLayer;

public class HousesResource implements HousesService {

    private static final String CONTAINER_NAME = "houses";
    private final CosmosDBLayer db = CosmosDBLayer.getInstance(CONTAINER_NAME);

    @Override
    public String createHouse(HouseDAO houseDAO) throws Exception {
        /*
        var res = db.createHouse(houseDAO);
        int statusCode = res.getStatusCode();
        if (statusCode < 300) {
            return houseDAO.toHouse().toString();
        } else {
            throw new Exception("Error: " + statusCode);
        }
        */
        return null;
    }

    @Override
    public House deleteHouse(String id) throws Exception {
        return null;
    }

    @Override
    public House getHouse(String id) throws Exception {
        return null;
    }

    @Override
    public House updateHouse(String id, HouseDAO houseDAO) throws Exception {
        return null;
    }
}

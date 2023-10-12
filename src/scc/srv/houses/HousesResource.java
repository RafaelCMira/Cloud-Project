package scc.srv.houses;

import com.azure.core.util.BinaryData;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import scc.data.House;
import scc.data.HouseDAO;
import scc.db.CosmosDBLayer;
import scc.srv.Checks;
import scc.srv.media.MediaService;
import scc.utils.Hash;

import java.util.Optional;

public class HousesResource implements HousesService {
    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public String createHouse(HouseDAO houseDAO) throws Exception {
        if (Checks.badParams(houseDAO.getId(), houseDAO.getName(), houseDAO.getLocation(),
                houseDAO.getPhotoId()) && !hasPricesByPeriod(houseDAO)) {
            throw new Exception("Error: 400 Bad Request");
        }

        // TODO - Replace this blob access with a Media Resource method
        // Get container client
        BlobContainerClient containerClient = new BlobContainerClientBuilder()
                .connectionString(MediaService.storageConnectionString)
                .containerName("images")
                .buildClient();

        // Get client to blob
        BlobClient blob = containerClient.getBlobClient(houseDAO.getPhotoId());

        // Download contents to BinaryData (check documentation for other alternatives)
        BinaryData data = blob.downloadContent();
        if (data == null) throw new Exception("Error: 404 Image not found");


        var res = db.createHouse(houseDAO);
        int statusCode = res.getStatusCode();
        if (statusCode < 300) {
            return houseDAO.toHouse().toString();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public String deleteHouse(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");

        CosmosItemResponse<Object> res = db.delHouseById(id);
        int statusCode = res.getStatusCode();

        if (isStatusOk(statusCode)) {
            return String.format("StatusCode: %d \nHouse %s was delete", statusCode, id);
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public House getHouse(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");

        CosmosPagedIterable<HouseDAO> res = db.getHouseById(id);
        Optional<HouseDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            return result.get().toHouse();
        } else {
            throw new Exception("Error: 404");
        }
    }

    @Override
    public House updateHouse(String id, HouseDAO houseDAO) throws Exception {
        HouseDAO updatedHouse = refactorHouse(id, houseDAO);
        var res = db.updateHouseById(id, updatedHouse);
        int statusCode = res.getStatusCode();
        if (isStatusOk(res.getStatusCode())) {
            return updatedHouse.toHouse();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }


    /**
     * Checks if the given House has prices valid for all months.
     * @param house - house to be checked
     * @return true if all month has a valid price, false otherwise.
     */
    private boolean hasPricesByPeriod(HouseDAO house) {
        int[][] p = house.getPriceByPeriod();
        for(int i=0; i<12; i++) {
            if(p[0][i] <= 0) return false;
        }
        return true;
    }

    /**
     * Returns updated houseDAO to the method who's making the request to the database
     *
     * @param id      of the house being accessed
     * @param houseDAO new house attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws Exception If id is null or if the user does not exist
     */
    private HouseDAO refactorHouse(String id, HouseDAO houseDAO) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        CosmosPagedIterable<HouseDAO> res = db.getHouseById(id);
        Optional<HouseDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            HouseDAO h = result.get();

            String houseDAOName = houseDAO.getName();
            if (!h.getName().equals(houseDAOName))
                h.setName(houseDAOName);

            String houseDAOloc = Hash.of(houseDAO.getLocation());
            if (!h.getLocation().equals(houseDAOloc))
                h.setLocation(houseDAOloc);

            String houseDAOPhotoId = houseDAO.getPhotoId();
            if (!h.getPhotoId().equals(houseDAOPhotoId))
                h.setPhotoId(houseDAOPhotoId);

            // TODO - finish refactor

            return h;

        } else {
            throw new Exception("Error: 404");
        }
    }

    // Verifies if HTTP code is OK
    private boolean isStatusOk(int statusCode) {
        return statusCode > 200 && statusCode < 300;
    }
}

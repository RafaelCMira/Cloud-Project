package scc.srv.rentals;

import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.HouseDAO;
import scc.data.Rental;
import scc.data.RentalDAO;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.Checks;
import scc.utils.Hash;

import java.time.LocalDate;
import java.util.Optional;

public class RentalResource implements RentalService {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public String createRental(RentalDAO rentalDAO) throws Exception {
        if(Checks.badParams(rentalDAO.getId(), rentalDAO.getHouseID(), rentalDAO.getUserID()))
            throw new Exception("Error: 400 Bad Request");

        // Verify if house exists
        var houseRes = db.getHouseById(rentalDAO.getHouseID());
        Optional<HouseDAO> hResult = houseRes.stream().findFirst();
        if (hResult.isEmpty())
            throw new Exception("Error: 404 House Not Found ");

        // Verify if user exists
        var userRes = db.getUserById(rentalDAO.getUserID());
        Optional<UserDAO> uResult = userRes.stream().findFirst();
        if (uResult.isEmpty())
            throw new Exception("Error: 404 User Not Found ");

        var createRental = db.createRental(rentalDAO);
        int statusCode = createRental.getStatusCode();

        if (Checks.isStatusOk(statusCode))
            return rentalDAO.toRental().toString();
        else
            throw new Exception("Error: " + statusCode);
    }

    @Override
    public Rental getRental(String id) throws Exception {
        if(id == null) throw new Exception("Error: 400 Bad Request (Null ID)");

        CosmosPagedIterable<RentalDAO> res = db.getRentalById(id);
        Optional<RentalDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            return result.get().toRental();
        } else {
            throw new Exception("Error: 404 Rental Not Found");
        }
    }

    @Override
    public Rental updateRental(String id, RentalDAO rentalDAO) throws Exception {
        RentalDAO updatedRental = refactorRental(id, rentalDAO);
        var res = db.updateRentalById(id, updatedRental);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(res.getStatusCode())) {
            return updatedRental.toRental();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    private RentalDAO refactorRental(String id, RentalDAO rentalDAO) throws Exception{
        if(id == null) throw new Exception("Error: 400 Bad Request (Null ID)");

        CosmosPagedIterable<RentalDAO> res = db.getRentalById(id);
        Optional<RentalDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            RentalDAO r = result.get();

            String rentalDAOId = rentalDAO.getId();
            if (!r.getId().equals(rentalDAOId))
                r.setId(rentalDAOId);

            String rentalDAOHouseId = rentalDAO.getHouseID();
            if (!r.getHouseID().equals(rentalDAOHouseId))
                r.setHouseID(rentalDAOHouseId);

            String rentalDAOUserId = rentalDAO.getUserID();
            if (!r.getUserID().equals(rentalDAOUserId))
                r.setUserID(rentalDAOUserId);

            double rentalDAOPrice = rentalDAO.getPrice();
            if (r.getPrice() != (rentalDAOPrice))
                r.setPrice(rentalDAOPrice);

            LocalDate rentalDAOInitialDate = rentalDAO.getInitialDate();
            if (!r.getInitialDate().equals(rentalDAOInitialDate))
                r.setInitialDate(rentalDAOInitialDate);

            LocalDate rentalDAOEndDate = rentalDAO.getEndDate();
            if (!r.getEndDate().equals(rentalDAOEndDate))
                r.setEndDate(rentalDAOEndDate);

            return r;

        } else {
            throw new Exception("Error: 404");
        }
    }
}

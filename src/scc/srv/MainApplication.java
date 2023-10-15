package scc.srv;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import scc.srv.houses.HousesResource;
import scc.srv.media.MediaResource;
import scc.srv.question.QuestionResource;
import scc.srv.users.UsersResource;

public class MainApplication extends Application {
    private Set<Object> singletons = new HashSet<Object>();
    private Set<Class<?>> resources = new HashSet<Class<?>>();

    public MainApplication() {
        // TODO: isto é para adicionar em qual? (nos singletons ou nos resources)
        resources.add(UsersResource.class);
        resources.add(HousesResource.class);
        //resources.add(RentalResource.class);
        resources.add(QuestionResource.class);
        singletons.add(new MediaResource());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}

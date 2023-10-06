package scc.srv;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import scc.srv.media.MediaResource;
import scc.srv.users.UsersResource;

public class MainApplication extends Application {
    private Set<Object> singletons = new HashSet<Object>();
    private Set<Class<?>> resources = new HashSet<Class<?>>();

    public MainApplication() {
        //  resources.add(ControlResource.class);
        singletons.add(new MediaResource());
        singletons.add(new UsersResource()); // TODO: isto é para adicionar em qual? (nos singletons ou nos resources)
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

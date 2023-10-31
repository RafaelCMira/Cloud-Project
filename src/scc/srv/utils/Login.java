package scc.srv.utils;

import scc.utils.Hash;

public class Login {

    private String id;
    private String pwd;

    public Login() {
    }

    public Login(String id, String pwd) {
        this.id = id;
        this.pwd = Hash.of(pwd);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }
}

package com.example.collector.model;

public class ServiceEntity {
    private final int id;
    private final String name;
    private final String url;
    private final String type;
    private final String serverIp;
    private final String sshUser;
    private final String sshKeyPath;

    public ServiceEntity(int id, String name, String url, String type, String serverIp, String sshUser, String sshKeyPath) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.type = type;
        this.serverIp = serverIp;
        this.sshUser = sshUser;
        this.sshKeyPath = sshKeyPath;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getType() { return type; }
    public String getServerIp() { return serverIp; }
    public String getSshUser() { return sshUser; }
    public String getSshKeyPath() { return sshKeyPath; }
}

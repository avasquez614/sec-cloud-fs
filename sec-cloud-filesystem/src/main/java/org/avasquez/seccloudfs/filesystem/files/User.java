package org.avasquez.seccloudfs.filesystem.files;

/**
 * Created by alfonsovasquez on 25/01/14.
 */
public class User {

    private long uid;
    private long gid;

    /**
     * Private no-arg constructor, for use by frameworks like Jongo/Jackson.
     */
    private User() {
    }

    public User(long uid, long gid) {
        this.uid = uid;
        this.gid = gid;
    }

    public long getUid() {
        return uid;
    }

    public long getGid() {
        return gid;
    }

    @Override
    public String toString() {
        return "User{" +
                "uid=" + uid +
                ", gid=" + gid +
                '}';
    }

}

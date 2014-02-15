package org.avasquez.seccloudfs.filesystem.fuse;

import net.fusejna.types.TypeMode;

/**
 * Created by alfonsovasquez on 25/01/14.
 */
public class Constants {

    /**
     * Test for read permission.
     * */
    public static final int R_OK =  4;
    /**
     * Test for write permission.
     * */
    public static final int W_OK =  2;
    /**
     * Test for execute or search permission.
     * */
    public static final int X_OK =  1;
    /**
     * Test for existence of file.
     * */
    public static final int F_OK =  0;

    /**
     * The mask for the permissions bits of the file mode.
     */
    public static final long PERMISSIONS_MASK = TypeMode.S_IRUSR | TypeMode.S_IWUSR | TypeMode.S_IXUSR |
            TypeMode.S_IRGRP | TypeMode.S_IWGRP | TypeMode.S_IXGRP |
            TypeMode.S_IROTH | TypeMode.S_IWOTH | TypeMode.S_IXOTH;


    /**
     * The path separator
     */
    public static final String PATH_SEPARATOR = "/";

    private Constants() {
    }

}

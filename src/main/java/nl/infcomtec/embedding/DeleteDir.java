/*
 *  Copyright (c) 2018 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.embedding;

import java.io.File;

/**
 * rm -Rf
 *
 * @author walter
 */
public class DeleteDir {

    private DeleteDir() {
    }

    /**
     * rm -Rf
     *
     * @param f Directory with children to delete.
     */
    public static void rmMinRF(File f) {
        try {
            File[] sub = f.listFiles();
            if (null != sub) {
                for (File f2 : sub) {
                    if (f2.isDirectory()) {
                        rmMinRF(f2);
                    } else {
                        f2.delete();
                    }
                }
            }
        } catch (Exception any) {
            return;
        }
        f.delete();
    }
}

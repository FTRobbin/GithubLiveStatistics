package Cloner;

import Data.GithubRepo;

/**
 * Created by RobbinNi on 7/8/16.
 */
public class ClonerMultiLevelDirectory {

    public static String getDirectory(int ownerId) {
        int tmp = ownerId;
        String ret = "";
        for (int i = 0; i < 7; ++i) {
            ret += tmp % 10;
            tmp /= 10;
            if (i == 1 || i == 3 || i == 6) {
                ret += "/";
            }
        }
        return ret;
    }
}

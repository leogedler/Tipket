package com.tipket.app;

/**
 * Created by leonardogedler on 09/04/2014.
 */
public class StringHelper {


    public static String capitalize(String s) {

        String[] array = s.split("\\s+");

        if (array.length > 1) {

            StringBuffer result = new StringBuffer();
            for (int i = 0; i < array.length; i++) {

                result.append( array[i].substring(0, 1).toUpperCase() + array[i].substring(1)+" ");

            }
            String s1 = result.toString();
            return s1;

        } else {

            String s2 = array[0].substring(0, 1).toUpperCase() + s.substring(1);

            return s2;
        }
    }

}

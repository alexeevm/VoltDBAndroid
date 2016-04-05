/* This file is part of VoltDB.
 * Copyright (C) 2008-2016 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.restclient.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class for authenticating
 *
 */

public class VoltConnectionUtil {

    public static final Charset UTF8ENCODING = Charset.forName("UTF-8");
    private static final int caseDiff = ('a' - 'A');

    /**
     * Get a hex-encoded hashed password using SHA-1 in a consistent way.
     * @param scheme hashing scheme for password.
     * @param password The password to encode.
     * @return The hex-encoded string of bytes of the hashed password.
     */
    public static String getHexEncodedHashedPassword(ClientAuthScheme scheme, String password) {
        return hexEncode(getHashedPassword(scheme, password));
    }

    /**
     * Get a hashed password using SHA-1 in a consistent way.
     *
     * @param scheme   hashing scheme for password.
     * @param password The password to encode.
     * @return The bytes of the hashed password.
     */
    public static byte[] getHashedPassword(ClientAuthScheme scheme, String password) {
        if (password == null)
            return null;

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(ClientAuthScheme.getDigestScheme(scheme));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        byte hashedPassword[] = null;
        hashedPassword = md.digest(password.getBytes(UTF8ENCODING));
        return hashedPassword;
    }

    /**
     *
     * @param data A binary array of bytes.
     * @return A hex-encoded string with double length.
     */
    public static String hexEncode(byte[] data) {
        if (data == null)
            return null;

        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            // hex encoding same way as java.net.URLEncoder.
            char ch = Character.forDigit((b >> 4) & 0xF, 16);
            // to uppercase
            if (Character.isLetter(ch)) {
                ch -= caseDiff;
            }
            sb.append(ch);
            ch = Character.forDigit(b & 0xF, 16);
            if (Character.isLetter(ch)) {
                ch -= caseDiff;
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}

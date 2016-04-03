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

package org.voltdb.restclient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * VoltCall declares a method to call VoltDB over its JSON HTTP interface. The interface has a package visibility
 * to shields users from specifying VoltDB base URL (like /api/1.0/) which is defined in the project's
 * property file. The clients should use VoltClient methods to make actuall calls
 */
interface VoltCall {

    /**
     * See <a href="https://docs.voltdb.com/UsingVoltDB/ProgLangJson.php#ftn.fnJsonPortNum">VoltDB JSON HTTTP Interface</a>
     * for more information.
     *
     * @param voltBaseUrl VoltDB JSON HTTP base url
     * @param params JSON parameters
     * @return
     */
    @FormUrlEncoded
    @Headers({
         "Accept: application/json",
    })
    @POST()
    Call<VoltResponse> callProcedure(@Url String voltBaseUrl, @FieldMap Map<String, Object> params);
}

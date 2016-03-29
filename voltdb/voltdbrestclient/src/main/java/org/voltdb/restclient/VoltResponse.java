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

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by mikealexeev on 3/23/16.
 */
//{       appstatus           (integer, boolean)
//        appstatusstring     (string)
//        exception           (integer)
//        results             (array)
//          [                 (object, VoltTable)
//              {  data       (array)
//                  [        (any type)
//                  ]
//                 schema     (array)
//                  [   name  (string)
//                      type  (integer, enumerated)
//                  ]
//                  status     (integer, boolean)
//              }
//          ]
//        status              (integer)
//        statusstring        (string)
// }
public class VoltResponse {

    public static class Schema {
        @SerializedName("name")
        private String mName;

        @SerializedName("type")
        private Integer mType;

        public String getName() {
            return mName;
        }

        public void setName(String mName) {
            this.mName = mName;
        }

        public Integer getType() {
            return mType;
        }

        public void setType(int mType) {
            this.mType = mType;
        }

        public Schema(String name, Integer type) {
            mName = name;
            mType = type;
        }

        public Schema()   {
            this(null, null);
        }
    }
    public static class VoltTable {

        @SerializedName("data")
        private List<Object> mData;

        @SerializedName("schema")
        private List<Schema> mSchema;

        @SerializedName("status")
        private Integer mStatus;

        public List<Object> getData() {
            return mData;
        }

        public void setData(List<Object> mData) {
            this.mData = mData;
        }

        public List<Schema> getSchema() {
            return mSchema;
        }

        public void setSchema(List<Schema> mSchema) {
            this.mSchema = mSchema;
        }

        public Integer getStatus() {
            return mStatus;
        }

        public void setStatus(int mStatus) {
            this.mStatus = mStatus;
        }

        public VoltTable(List<Object> data, List<Schema> schema, Integer status) {
            mData = data;
            mSchema = schema;
            mStatus = status;
        }

        public VoltTable() {
            this(null, null, null);
        }
    }

    @SerializedName("appstatus")
    private Integer mAppsstatus;

    @SerializedName("appstatusstring")
    private String mAppstatusstring;

    @SerializedName("exception")
    private Integer mException;

    @SerializedName("status")
    private Integer mStatus;

    @SerializedName("statusstring")
    private String mStatusstring;

    @SerializedName("results")
    private List<VoltTable> mResults;

    private transient Throwable mThrowable;

    public Integer getAppsstatus() {
        return mAppsstatus;
    }

    public void setAppsstatus(int mAppsstatus) {
        this.mAppsstatus = mAppsstatus;
    }

    public String getAppstatusstring() {
        return mAppstatusstring;
    }

    public void setAppstatusstring(String mAppstatusstring) {
        this.mAppstatusstring = mAppstatusstring;
    }

    public Integer getException() {
        return mException;
    }

    public void setException(int mException) {
        this.mException = mException;
    }

    public Integer getStatus() {
        return mStatus;
    }

    public void setStatus(int mStatus) {
        this.mStatus = mStatus;
    }

    public String getStatusstring() {
        return mStatusstring;
    }

    public void setStatusstring(String mStatusstring) {
        this.mStatusstring = mStatusstring;
    }

    public List<VoltTable> getResults() {
        return mResults;
    }

    public void setResults(List<VoltTable> mResults) {
        this.mResults = mResults;
    }

    public Throwable getCallError() {
        return mThrowable;
    }

    public VoltResponse(Integer appstatus, String appstatusstring, Integer exception,
                        Integer status, String statusstring, List<VoltTable> results) {
        mAppsstatus = appstatus;
        mAppstatusstring = appstatusstring;
        mException = exception;
        mStatus = status;
        mStatusstring = statusstring;
        mResults = results;
    }

    public VoltResponse() {
        this(null, null, null, null, null, null);
    }

    public VoltResponse(Throwable t) {
        mThrowable = t;
    }
}

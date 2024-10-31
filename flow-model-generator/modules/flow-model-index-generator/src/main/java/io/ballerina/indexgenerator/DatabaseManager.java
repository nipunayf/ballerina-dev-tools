/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.indexgenerator;

import io.ballerina.compiler.api.symbols.ParameterKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final String DB_URL =
            "jdbc:sqlite:flow-model-generator/modules/flow-model-generator-ls-extension/src/main/resources/" +
                    "central-index.sqlite";
    private static final String CENTRAL_INDEX_SQL =
            "/Users/nipunaf/projects/ballerina/ballerina-dev-tools/flow-model-generator/modules/" +
                    "flow-model-index-generator/src/main/resources/central-index.sql";

    private static int insertEntry(String sql, Object[] params) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating package failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
            return -1;
        }
    }

    private static void executeQuery(String sql) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Query executed successfully.");
            stmt.getGeneratedKeys();
        } catch (SQLException e) {
            Logger.getGlobal().severe("Error executing query: " + e.getMessage());
        }
    }

    public static void createDatabase() {
        try {
            String sql = new String(Files.readAllBytes(Paths.get(CENTRAL_INDEX_SQL)));
            executeQuery(sql);
        } catch (IOException e) {
            Logger.getGlobal().severe("Error reading SQL file: " + e.getMessage());
        }
    }

    public static int insertPackage(String org, String name, String version, List<String> keywords) {
        String sql = "INSERT INTO Package (org, name, version, keywords) VALUES (?, ?, ?, ?)";
        return insertEntry(sql, new Object[]{org, name, version, keywords == null ? "" : String.join(",", keywords)});
    }

    public static int insertFunction(int packageId, String name, String description, String returnType, String kind) {
        String sql = "INSERT INTO Function (package_id, name, description, return_type, kind) VALUES (?, ?, ?, ?, ?)";
        return insertEntry(sql, new Object[]{packageId, name, description, returnType, kind});
    }

    public static void insertFunctionParameter(int functionId, String paramName, String paramDescription,
                                               String paramType, ParameterKind parameterKind) {
        String sql = "INSERT INTO Parameter (function_id, name, description, type, kind) VALUES (?, ?, ?, ?, ?)";
        insertEntry(sql, new Object[]{functionId, paramName, paramDescription, paramType, parameterKind.name()});
    }

    public static void mapConnectorAction(int actionId, int connectorId) {
        String sql = "INSERT INTO FunctionConnector (function_id, connector_id) VALUES (?, ?)";
        insertEntry(sql, new Object[]{actionId, connectorId});
    }
}
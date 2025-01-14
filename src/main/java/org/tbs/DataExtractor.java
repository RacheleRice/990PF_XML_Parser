package org.tbs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class DataExtractor {
    //initialize connection object to be used throughout the class methods
    private Connection conn;

    public void createGrantsTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS grants (" +
                "id SERIAL PRIMARY KEY," +
                "tax_period_end_date DATE," +
                "return_type_code VARCHAR(10)," +
                "tax_period_begin_date DATE," +
                "ein VARCHAR(15)," +
                "filer_name VARCHAR(255)," +
                "filer_street VARCHAR(255)," +
                "filer_city VARCHAR(100)," +
                "filer_state VARCHAR(10)," +
                "filer_zip VARCHAR(15)," +
                "total_rev BIGINT," +
                "total_assets_boy BIGINT," +
                "total_assets_eoy BIGINT," +
                "distributable_amount BIGINT," +
                "remaining_distribution BIGINT," +
                "recipient_name VARCHAR(255)," +
                "grant_amount BIGINT," +
                "recipient_street VARCHAR(255)," +
                "recipient_city VARCHAR(100)," +
                "recipient_state VARCHAR(10)," +
                "recipient_zip VARCHAR(15)," +
                "recipient_relationship VARCHAR(50)," +
                "recipient_foundation_status VARCHAR(50)," +
                "grant_purpose TEXT," +
                "standardized_recipient_name VARCHAR(255)" +
                ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            System.out.println("Error creating grants table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public DataExtractor() {
        //initialize connection to the database
        try {
            conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/grants", "postgres", "postgres1");
            createGrantsTable();
        } catch (SQLException e) {
            System.out.println("Error initializing a database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void insertGrantInfo(GrantInfo grantInfo) {
        //Insert grantInfo into database table grants
        String insertSQL = "INSERT INTO grants (" +
                "tax_period_end_date," +
                "return_type_code," +
                "tax_period_begin_date," +
                "ein," +
                "filer_name," +
                "filer_street," +
                "filer_city," +
                "filer_state," +
                "filer_zip," +
                "total_rev," +
                "total_assets_boy," +
                "total_assets_eoy," +
                "distributable_amount," +
                "remaining_distribution," +
                "recipient_name," +
                "grant_amount," +
                "recipient_street," +
                "recipient_city," +
                "recipient_state," +
                "recipient_zip," +
                "recipient_relationship," +
                "recipient_foundation_status," +
                "grant_purpose," +
                "standardized_recipient_name" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
        //Insert grantInfo into the grants table
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setDate(1, grantInfo.getTaxPeriodEndDate());
            pstmt.setString(2, grantInfo.getReturnTypeCode());
            pstmt.setDate(3, grantInfo.getTaxPeriodBeginDate());
            pstmt.setString(4, grantInfo.getEin());
            pstmt.setString(5, grantInfo.getFilerName());
            pstmt.setString(6, grantInfo.getFilerStreet());
            pstmt.setString(7, grantInfo.getFilerCity());
            pstmt.setString(8, grantInfo.getFilerState());
            pstmt.setString(9, grantInfo.getFilerZip());
            pstmt.setLong(10, grantInfo.getTotalRev());
            pstmt.setLong(11, grantInfo.getTotalAssetsBOY());
            pstmt.setLong(12, grantInfo.getTotalAssetsEOY());
            pstmt.setLong(13, grantInfo.getDistributableAmount());
            pstmt.setLong(14, grantInfo.getRemainingDistribution());
            pstmt.setString(15, grantInfo.getRecipientName());
            pstmt.setLong(16, grantInfo.getGrantAmount());
            pstmt.setString(17, grantInfo.getRecipientStreet());
            pstmt.setString(18, grantInfo.getRecipientCity());
            pstmt.setString(19, grantInfo.getRecipientState());
            pstmt.setString(20, grantInfo.getRecipientZip());
            pstmt.setString(21, grantInfo.getRecipientRelationship());
            pstmt.setString(22, grantInfo.getRecipientFoundationStatus());
            pstmt.setString(23, grantInfo.getGrantPurpose());
            pstmt.setString(24, grantInfo.getStandardizedRecipientName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error inserting grant info into database: " + e.getMessage());
            e.printStackTrace();
        }

    }
    //Construct a query from the CSV file
    public String constructQueryFromCSV() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("grant_external_configuration_file.csv");

        if (inputStream == null) {
            throw new FileNotFoundException("File grant_external_configuration_file.csv not found");
        }
        //StringBuilder to build the query from the CSV file records (Field and Pattern)
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM grants WHERE ");

        try (
            //Read the CSV file using Apache Commons CSV library and parse it into CSVRecords
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            //parse the CSV file into CSVRecords using Apache Commons CSV library
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            //boolean to check if there are any records in the CSV file
            boolean hasRecords = false;
            //Iterate through each record in the CSV file and construct the query
            for (CSVRecord record : records) {
                //Get the Field and Pattern values from the CSV record
            String field = record.get("Field");
            String pattern = record.get("Pattern");
            //Append the Field and Pattern to the query
            queryBuilder.append(field).append(" ILIKE '").append(pattern).append("' OR ");
            //Set hasRecords to true if there are any records in the CSV file
            hasRecords = true;
        }
        // Remove the last " OR " from the query
        if (hasRecords) {
            //removes the last " OR " only if there are no records
            queryBuilder.setLength(queryBuilder.length() - 4);
            } else {
            //if there are no records, remove the " WHERE " clause
            queryBuilder.setLength(queryBuilder.length() - 7);
             }
        }
        //return the constructed query from the CSV file
        return queryBuilder.toString();
    }


    public void executeQueryFromCSV() throws Exception {

        String query = constructQueryFromCSV();
        System.out.println("Executing query from CSV...");

        //Execute the query and insert the results into the matching_grants table
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/grants", "postgres", "postgres1");
             Statement stmt = conn.createStatement(); //create a statement object
                ResultSet rs = stmt.executeQuery(query)) { //execute the query and get the result set

            createMatchingGrantsTable(conn); //assuming this method creates the table if it doesn't exist
                //Create a DataTransformer object to clean and standardize the data
            DataTransformer transformer = new DataTransformer();

            while (rs.next()) { //iterate through the result set
                System.out.println(rs.getString("standardized_recipient_name"));
                //Create a GrantInfo object from the ResultSet
                GrantInfo grantInfo = new GrantInfo();
                //Populate grantInfo with data from rs
                grantInfo.setTaxPeriodEndDate(rs.getDate("tax_period_end_date"));
                grantInfo.setReturnTypeCode(rs.getString("return_type_code"));
                grantInfo.setTaxPeriodBeginDate(rs.getDate("tax_period_begin_date"));
                grantInfo.setEin(rs.getString("ein"));
                grantInfo.setFilerName(rs.getString("filer_name"));
                grantInfo.setFilerStreet(rs.getString("filer_street"));
                grantInfo.setFilerCity(rs.getString("filer_city"));
                grantInfo.setFilerState(rs.getString("filer_state"));
                grantInfo.setFilerZip(rs.getString("filer_zip"));
                grantInfo.setTotalRev(rs.getLong("total_rev"));
                grantInfo.setTotalAssetsBOY(rs.getLong("total_assets_boy"));
                grantInfo.setTotalAssetsEOY(rs.getLong("total_assets_eoy"));
                grantInfo.setDistributableAmount(rs.getLong("distributable_amount"));
                grantInfo.setRemainingDistribution(rs.getLong("remaining_distribution"));
                grantInfo.setRecipientName(rs.getString("recipient_name"));
                grantInfo.setGrantAmount(rs.getLong("grant_amount"));
                grantInfo.setRecipientStreet(rs.getString("recipient_street"));
                grantInfo.setRecipientCity(rs.getString("recipient_city"));
                grantInfo.setRecipientState(rs.getString("recipient_state"));
                grantInfo.setRecipientZip(rs.getString("recipient_zip"));
                grantInfo.setRecipientRelationship(rs.getString("recipient_relationship"));
                grantInfo.setRecipientFoundationStatus(rs.getString("recipient_foundation_status"));
                grantInfo.setGrantPurpose(rs.getString("grant_purpose"));
                grantInfo.setStandardizedRecipientName(rs.getString("standardized_recipient_name"));

                //clean the data
                String cleanedFilerName = transformer.cleanFilerName(grantInfo.getFilerName());
                String cleanedRecipientName = transformer.cleanRecipientName(grantInfo.getRecipientName());
                String standardizedFilerName = transformer.standardizeFilerName(cleanedFilerName);
                String originalRecipientName = grantInfo.getRecipientName(); //store the original recipient name
                String standardizedRecipientName = transformer.getStandardizedRecipientNameByPattern(originalRecipientName); //use the transformer method to get the standardized recipient name
                //set cleaned/standardized data back into grantInfo object
                grantInfo.setRecipientName(cleanedRecipientName);
                grantInfo.setFilerName(standardizedFilerName);
                grantInfo.setStandardizedRecipientName(standardizedRecipientName); //set the standardized recipient name


                //Extract data from result set and insert into the matching_grants table (which includes standardized recipient name)
                insertIntoMatchingGrantsTable(conn, rs, grantInfo);
                System.out.println("Inserted into matching_grants table");
            }
        }

    }

    private void createMatchingGrantsTable(Connection Conn) throws SQLException {
         System.out.println("Creating matching_grants table...");
        //SQL to create the matching_grants table if it doesn't exist
        String createTableSQL = "CREATE TABLE IF NOT EXISTS matching_grants (" +
                "id SERIAL PRIMARY KEY," +
                "standardized_recipient_name VARCHAR(255)," +
                "tax_period_end_date DATE," +
                "return_type_code VARCHAR(10)," +
                "tax_period_begin_date DATE," +
                "ein VARCHAR(15)," +
                "filer_name VARCHAR(255)," +
                "filer_street VARCHAR(255)," +
                "filer_city VARCHAR(100)," +
                "filer_state VARCHAR(10)," +
                "filer_zip VARCHAR(15)," +
                "total_rev BIGINT," +
                "total_assets_boy BIGINT," +
                "total_assets_eoy BIGINT," +
                "distributable_amount BIGINT," +
                "remaining_distribution BIGINT," +
                "recipient_name VARCHAR(255)," +
                "grant_amount BIGINT," +
                "recipient_street VARCHAR(255)," +
                "recipient_city VARCHAR(100)," +
                "recipient_state VARCHAR(10)," +
                "recipient_zip VARCHAR(15)," +
                "recipient_relationship VARCHAR(50)," +
                "recipient_foundation_status VARCHAR(50)," +
                "grant_purpose TEXT" +
                ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            System.out.println("Error creating matching_grants table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertIntoMatchingGrantsTable(Connection conn, ResultSet rs, GrantInfo grantInfo) throws SQLException {
        String insertSQL = "INSERT INTO matching_grants (" +
                "standardized_recipient_name," +
                "tax_period_end_date," +
                "return_type_code," +
                "tax_period_begin_date," +
                "ein," +
                "filer_name," +
                "filer_street," +
                "filer_city," +
                "filer_state," +
                "filer_zip," +
                "total_rev," +
                "total_assets_boy," +
                "total_assets_eoy," +
                "distributable_amount," +
                "remaining_distribution," +
                "recipient_name," +
                "grant_amount," +
                "recipient_street," +
                "recipient_city," +
                "recipient_state," +
                "recipient_zip," +
                "recipient_relationship," +
                "recipient_foundation_status," +
                "grant_purpose" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
//                "ON CONFLICT (reipient_name, grant_purpose) DO UPDATE SET"
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, grantInfo.getStandardizedRecipientName());
            pstmt.setDate(2, grantInfo.getTaxPeriodEndDate());
            pstmt.setString(3, grantInfo.getReturnTypeCode());
            pstmt.setDate(4, grantInfo.getTaxPeriodBeginDate());
            pstmt.setString(5, grantInfo.getEin());
            pstmt.setString(6, grantInfo.getFilerName());
            pstmt.setString(7, grantInfo.getFilerStreet());
            pstmt.setString(8, grantInfo.getFilerCity());
            pstmt.setString(9, grantInfo.getFilerState());
            pstmt.setString(10, grantInfo.getFilerZip());
            pstmt.setLong(11, grantInfo.getTotalRev());
            pstmt.setLong(12, grantInfo.getTotalAssetsBOY());
            pstmt.setLong(13, grantInfo.getTotalAssetsEOY());
            pstmt.setLong(14, grantInfo.getDistributableAmount());
            pstmt.setLong(15, grantInfo.getRemainingDistribution());
            pstmt.setString(16, grantInfo.getRecipientName());
            pstmt.setLong(17, grantInfo.getGrantAmount());
            pstmt.setString(18, grantInfo.getRecipientStreet());
            pstmt.setString(19, grantInfo.getRecipientCity());
            pstmt.setString(20, grantInfo.getRecipientState());
            pstmt.setString(21, grantInfo.getRecipientZip());
            pstmt.setString(22, grantInfo.getRecipientRelationship());
            pstmt.setString(23, grantInfo.getRecipientFoundationStatus());
            pstmt.setString(24, grantInfo.getGrantPurpose());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error inserting matching_grants into database: " + e.getMessage());
            e.printStackTrace();
        }

    }



    public void closeConnection() {
        try {
            conn.close();
        } catch (SQLException e) {
            System.out.println("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }


}

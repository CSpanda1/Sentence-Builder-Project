package database;

/****************************************************************
 * DatabaseConnection.java
 *
 * This program tests the connection to the AWS RDS database using JDBC.
 * It retrieves the database URL, username, and password from environment variables and attempts to establish a connection.
 * If successful, it prints a success message; otherwise, it prints an error message along with the stack trace.
 *
 * Written by: Ahmed Sherwani(AJS170005) at the University of Texas at Dallas starting October 17th, 2026.
 ****************************************************************/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection
{
    public static Connection getConnection() throws SQLException
    {
        // Retrieve database connection details from environment variables
        String url = System.getenv("DATABASE_URL");

        // Retrieve the username from environment variables
        String username = System.getenv("DATABASE_USERNAME");
        // Retrieve the password from environment variables
        String password = System.getenv("DATABASE_PASSWORD");

        // Establish and return the database connection
        return DriverManager.getConnection(url, username, password);

    }
}

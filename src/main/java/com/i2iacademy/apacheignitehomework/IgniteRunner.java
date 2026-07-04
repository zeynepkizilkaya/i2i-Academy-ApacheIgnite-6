 package com.i2iacademy.apacheignitehomework;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.IgniteSql;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

    public class IgniteRunner {

        public static void main(String[] args) {

            try (IgniteClient client = IgniteClient.builder()
                    .addresses("127.0.0.1:10800")
                    .build()) {

                System.out.println("Connected to Ignite cluster.");

                IgniteSql sql = client.sql();

                sql.execute(null,
                        "CREATE TABLE IF NOT EXISTS Subscriber (" +
                                "customerId VARCHAR PRIMARY KEY, " +
                                "dataUsage DOUBLE, " +
                                "smsUsage INT, " +
                                "callUsage INT)"
                );
                System.out.println("Table 'Subscriber' is ready.");

                sql.execute(null, "DELETE FROM Subscriber");
                System.out.println("Table cleared.");

                List<Subscriber> subscribers = new ArrayList<>();
                for (int i = 1; i <= 5; i++) {
                    subscribers.add(new Subscriber("C00" + i, 0.0, 0, 0));
                }

                for (Subscriber s : subscribers) {
                    sql.execute(null,
                            "INSERT INTO Subscriber (customerId, dataUsage, smsUsage, callUsage) VALUES (?, ?, ?, ?)",
                            s.getCustomerId(), s.getDataUsage(), s.getSmsUsage(), s.getCallUsage());
                }
                System.out.println("Inserted 5 dummy subscribers with baseline values.");

                // 4. Simulation: retrieve from DB, update fields in Java, write back
                Random random = new Random();

                for (String id : List.of("C001", "C002", "C003", "C004", "C005")) {

                    // Retrieve current subscriber from the database
                    Subscriber current = null;
                    try (ResultSet<SqlRow> rs = sql.execute(null,
                            "SELECT * FROM Subscriber WHERE customerId = ?", id)) {
                        if (rs.hasNext()) {
                            SqlRow row = rs.next();
                            current = new Subscriber(
                                    row.stringValue("customerId"),
                                    row.doubleValue("dataUsage"),
                                    row.intValue("smsUsage"),
                                    row.intValue("callUsage")
                            );
                        }
                    }

                    if (current != null) {
                        // Add random usage amounts to the retrieved object
                        current.setDataUsage(current.getDataUsage() + (1 + random.nextDouble() * 10));
                        current.setSmsUsage(current.getSmsUsage() + (1 + random.nextInt(50)));
                        current.setCallUsage(current.getCallUsage() + (1 + random.nextInt(120)));

                        // Write the updated object back to the database
                        sql.execute(null,
                                "UPDATE Subscriber SET dataUsage = ?, smsUsage = ?, callUsage = ? WHERE customerId = ?",
                                current.getDataUsage(), current.getSmsUsage(), current.getCallUsage(), current.getCustomerId());
                    }
                }

                System.out.println("Usage updated for all subscribers.");

                System.out.println("\n--- Final Subscriber States ---");
                try (ResultSet<SqlRow> resultSet = sql.execute(null, "SELECT * FROM Subscriber ORDER BY customerId")) {
                    while (resultSet.hasNext()) {
                        SqlRow row = resultSet.next();
                        System.out.println("customerId=" + row.stringValue("customerId") +
                                ", dataUsage=" + row.doubleValue("dataUsage") +
                                ", smsUsage=" + row.intValue("smsUsage") +
                                ", callUsage=" + row.intValue("callUsage"));
                    }
                }
            }

            System.out.println("Program finished.....");
        }
    }


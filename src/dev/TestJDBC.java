package dev;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
public class TestJDBC {
	public static void main(String[] args) {
		
		String url="jdbc:postgresql://simpg.cds.unistra.fr/simbad" ;
        String user = "guest";
        
        try (Connection conn = DriverManager.getConnection(url, user, null)) {
			DatabaseMetaData dbMeta = conn.getMetaData();
            System.out.println("Connected to database!" + dbMeta.supportsSchemasInTableDefinitions());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        }
}

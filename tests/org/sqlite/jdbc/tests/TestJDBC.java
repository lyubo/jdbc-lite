package org.sqlite.jdbc.tests;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJDBC
{

  private Connection conn;

  private String readFile(String filename)
  {
    File f = new File(filename);
    InputStreamReader rd;
    try {
      rd = new InputStreamReader(new FileInputStream(f), "UTF-8");
      char[] buf = new char[(int) f.length()];
      rd.read(buf);
      return new String(buf);
    } catch (IOException e) {
      return null;
    }
  }

  @BeforeClass
  public static void setup() throws ClassNotFoundException, SQLException
  {
    Class.forName("org.sqlite.jdbc.Driver");

    // Create attached table
    Connection c = DriverManager.getConnection("jdbc:sqlite:jdbctest-meta-attached.db");
    Statement s = null;
    try {
      s = c.createStatement();
      s.execute("CREATE TABLE AT1(ID INTEGER NOT NULL)");
      s.execute("CREATE TABLE AT2(ID INTEGER NOT NULL)");
      s.execute("CREATE VIEW AV1 AS SELECT * FROM AT1");
    } finally {
      if (s != null) s.close();
      c.close();
    }

    // Create main table
    c = DriverManager.getConnection("jdbc:sqlite:jdbctest-meta-main.db");
    s = null;
    try {
      s = c.createStatement();
      s.execute("CREATE TABLE MT1(ID INTEGER NOT NULL)");
      s.execute("CREATE TABLE MT2(ID INTEGER NOT NULL)");
      s.execute("CREATE VIEW MV2 AS SELECT * FROM MT2");
    } finally {
      if (s != null) s.close();
      c.close();
    }    
  }

  @AfterClass
  public static void cleanup() throws ClassNotFoundException
  {
    new File("jdbctest.db").delete();
    new File("jdbctest-meta-attached.db").delete();
    new File("jdbctest-meta-main.db").delete();
  }

  @Before
  public void openConnection() throws SQLException
  {
    conn = DriverManager.getConnection("jdbc:sqlite:jdbctest.db");
  }

  @After
  public void closeConnection() throws SQLException
  {
    conn.close();
  }

  @Test
  public void testGetConnection() throws SQLException
  {
    Assert.assertNotNull(conn);
  }

  @Test
  public void testSetAutoCommit() throws SQLException
  {
    // Test if default mode is auto-commit
    Assert.assertTrue(conn.getAutoCommit());

    // Test if we switched to non-auto-commit mode
    conn.setAutoCommit(false);
    Assert.assertFalse(conn.getAutoCommit());

    // Test if we stayed to non-auto-commit mode
    conn.commit();
    Assert.assertFalse(conn.getAutoCommit());
    conn.rollback();
    Assert.assertFalse(conn.getAutoCommit());

    // Test if we switched back to auto-commit mode
    conn.setAutoCommit(true);
    Assert.assertTrue(conn.getAutoCommit());
  }

  @Test
  public void testGetMetaData_getCatalogs() throws SQLException
  {
    Connection c = DriverManager.getConnection("jdbc:sqlite:jdbctest-meta-main.db");
    try {
      Statement s = c.createStatement();

      ResultSet rs = c.getMetaData().getCatalogs();
      String str = "";
      while (rs.next()) {
        str = str.concat(rs.getString(1)).concat(",");
      }

      assertEquals("main,", str);

      s.execute("ATTACH DATABASE 'jdbctest-meta-attached.db' AS at");

      rs = c.getMetaData().getCatalogs();
      str = "";
      while (rs.next()) {
        str = str.concat(rs.getString(1)).concat(",");
      }
      assertEquals("at,main,", str);
    } finally {
      c.close();
    }
  }

  @Test
  public void testGetMetaData_getTables() throws SQLException
  {
    Connection c = DriverManager.getConnection("jdbc:sqlite:jdbctest-meta-main.db");
    try {
      Statement s = c.createStatement();
      s.execute("ATTACH DATABASE 'jdbctest-meta-attached.db' AS at");
      
      // All records
      ResultSet rs = c.getMetaData().getTables(null, null, null, null);
      String str = "";
      while (rs.next()) {
        str = String.format("%s,%s.%s", str, rs.getString("TABLE_CAT"), rs.getString("TABLE_NAME"));
      }
      assertEquals(",at.AT1,at.AT2,main.MT1,main.MT2,at.AV1,main.MV2", str);
      
      // Filter by one parameter
      rs = c.getMetaData().getTables(null,null, "M%",null);
      str = "";
      while (rs.next()) {
        str = String.format("%s,%s.%s", str, rs.getString("TABLE_CAT"), rs.getString("TABLE_NAME"));
      }
      assertEquals(",main.MT1,main.MT2,main.MV2", str);

      // Filter by two parameters
      rs = c.getMetaData().getTables(null,null, "%1%",new String[] { "VIEW" });
      str = "";
      while (rs.next()) {
        str = String.format("%s,%s.%s", str, rs.getString("TABLE_CAT"), rs.getString("TABLE_NAME"));
      }
      assertEquals(",at.AV1", str);
    } finally {
      c.close();
    }
  }
  
  @Test
  public void testExecuteUpdate() throws SQLException
  {

    Statement s = conn.createStatement();
    try {
      s.execute("CREATE TABLE X(ID INTEGER NOT NULL, CONSTRAINT X_PK PRIMARY KEY (ID));");
      s.execute("CREATE TABLE TIMES(ID INTEGER NOT NULL, DT DATE,TM TIME,TS TIMESTAMP, CONSTRAINT X_PK PRIMARY KEY (ID));");
    } finally {
      s.close();
    }
    PreparedStatement ps = conn.prepareStatement("INSERT INTO X(ID) VALUES(?);");
    try {
      ps.setObject(1, new Integer(25342));
      ps.execute();
    } finally {
      ps.close();
    }
  }

  @Test
  public void testExecuteQuery() throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement("SELECT id FROM X WHERE ID = ?;");

    try {
      ps.setObject(1, new Integer(25342));
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        int i = rs.getInt(1);
        assertEquals(25342, i);
        int j = rs.getInt("id");
        assertEquals(i, j);
      }
    } finally {
      ps.close();
    }
  }

  @Test
  public void testExecuteBatch() throws SQLException
  {

    Statement s = conn.prepareStatement("CREATE TABLE A1(ID INTEGER NOT NULL);CREATE TABLE A2(ID INTEGER NOT NULL);INSERT INTO A1(ID) VALUES(1); INSERT INTO A2(ID) VALUES(2);");
    try {
      s.executeBatch();
    } finally {
      s.close();
    }
    PreparedStatement ps = conn.prepareStatement("SELECT A1.ID ID1,A2.ID ID2 FROM A1,A2");
    try {
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        assertEquals(1, rs.getInt("ID1"));
        assertEquals(2, rs.getInt("ID2"));
      }
    } finally {
      ps.close();
    }
  }

  @Test
  public void testEmptyStatement() throws SQLException
  {
    PreparedStatement s = conn.prepareStatement("\n");
    try {
      s.execute();
    } catch (SQLException e) {
      Assert.assertTrue("Empty statement returrned exception", true);
    } finally {
      s.close();
    }
  }
  
  @Test
  public void testDateTimeConversion() throws SQLException
  {
    PreparedStatement ps = conn.prepareStatement("INSERT INTO TIMES(ID,DT,TM,TS) VALUES(?,?,?,?);");
    Calendar c = new GregorianCalendar(TimeZone.getTimeZone("CET"));
    c.set(1970, Calendar.JANUARY, 1);
    c.set(Calendar.HOUR_OF_DAY, 10);
    c.set(Calendar.MINUTE,54);
    c.set(Calendar.SECOND, 1);
    c.set(Calendar.MILLISECOND,0);
    try {
      ps.setObject(1, new Integer(1));
      ps.setObject(2, Date.valueOf("2008-10-12"));
      ps.setTime(3, new Time(c.getTimeInMillis()));
      ps.setObject(4, Timestamp.valueOf("2008-10-20 08:19:01.123"));
      ps.executeUpdate();
    } finally {
      ps.close();
    }

    ps = conn.prepareStatement("SELECT DT,TM,TS FROM TIMES WHERE ID = ?;");
   try {
      ps.setObject(1, new Integer(1));
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        Date dt = rs.getDate("dt");
        assertEquals(Date.valueOf("2008-10-12"), dt);
        Time tm = rs.getTime("tm");
        assertEquals(Time.valueOf("11:54:01").getTime(),tm.getTime());
      }
    } finally {
      ps.close();
    }

  }

}

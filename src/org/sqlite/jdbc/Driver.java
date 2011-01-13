package org.sqlite.jdbc;

import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

public class Driver implements java.sql.Driver
{
  private static final String URL_PREFIX      = "jdbc:sqlite:";
  private static final int    VERSION_MAJOR   = 0;
  private static final int    VERSION_MINOR   = 1;

  private static final String PROP_AUTOCOMMIT = "autocommit";

  static {
    try {
      java.sql.DriverManager.registerDriver(new Driver());
    } catch (SQLException E) {
      throw new RuntimeException("Can't register driver!");
    }
  }

  public Driver() throws SQLException
  {
    // Required for Class.forName().newInstance()
  }

  public boolean jdbcCompliant()
  {
    return false;
  }

  public boolean acceptsURL(String url) throws SQLException
  {
    return url != null && url.toLowerCase().startsWith(URL_PREFIX);
  }

  public int getMajorVersion()
  {
    return VERSION_MAJOR;
  }

  public int getMinorVersion()
  {
    return VERSION_MINOR;
  }

  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException
  {
    return null;
  }

  public Connection connect(String url, Properties info) throws SQLException
  {
    if (!acceptsURL(url)) {
      return null;
    }

    String[] urlParts = url.substring(URL_PREFIX.length()).split("\\?");
    SQLiteConnection result = new SQLiteConnection(urlParts[0]);
    if (urlParts.length > 1) {
      String[] properties = urlParts[1].toLowerCase().split("&");
      for (String p : properties) {
        String[] pp = p.split("=");
        if (PROP_AUTOCOMMIT.equals(pp[0]) && pp.length > 1) {
          result.setAutoCommit(pp[1].equals("1") || pp[1].equals("true") || pp[1].equals("yes"));
        }
      }
    }
    return result;
  }

}

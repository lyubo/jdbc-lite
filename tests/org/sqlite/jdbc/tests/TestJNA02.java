package org.sqlite.jdbc.tests;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sqlite.SQLite;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public class TestJNA02
{
  private static final String MSG_NO_DATABASE_CONNECTION = "No active database connection";
  Pointer                     db                         = null;

  private String readFile(String filename)
  {
    File f = new File(filename);
    InputStreamReader rd;
    try {
      rd = new InputStreamReader(new FileInputStream(f),"UTF-8");
      char[] buf = new char[(int) f.length()];
      rd.read(buf);
      return new String(buf);
    } catch (IOException e) {
      return null;
    }
  }
  
  @Before
  public void dbOpen()
  {
    File dbFile = new File("test.db");
    // Open database
    PointerByReference pdb = new PointerByReference();
    SQLite.open(dbFile.getAbsolutePath(), pdb);
    db = pdb.getValue();
  }

  @After
  public void dbClose()
  {
    SQLite.close(db);
    db = null;
  }

  @Test
  public void sqlite3_errmsg()
  {
    assertNotNull(MSG_NO_DATABASE_CONNECTION, db);
    assertEquals(SQLite.SQLITE_OK, SQLite.exec(db, "BEGIN", null, null, null));
    assertEquals("not an error", SQLite.errmsg(db));
  }
  
  @Test
  public void sqlite3_errmsg16()
  {
    assertNotNull(MSG_NO_DATABASE_CONNECTION, db);
    assertEquals(SQLite.SQLITE_OK, SQLite.exec(db, "BEGIN;", null, null, null));
    assertEquals("not an error", SQLite.errmsg16(db).toString());
  }

  @Test
  public void sqlite3_exec()
  {
    assertNotNull(MSG_NO_DATABASE_CONNECTION, db);
    String script = readFile("tests/test-jna.sql");
    assertNotNull("Schema creation script not found",script);
    int result = SQLite.exec(db, script, null, null, null);
    assertEquals(SQLite.errmsg(db),SQLite.SQLITE_OK,result);
  }

  @Test
  public void sqlite3_prepare()
  {
    assertNotNull(MSG_NO_DATABASE_CONNECTION, db);
    
    PointerByReference pstmt = new PointerByReference();
    int result = SQLite.prepare_v2(db, "SELECT * FROM ORDERS",-1,pstmt, null);
    assertEquals(SQLite.errmsg(db),SQLite.SQLITE_OK,result);
    Pointer stmt = pstmt.getValue();
    assertEquals(SQLite.SQLITE_OK, SQLite.reset(stmt));
    try {
      while ((result = SQLite.step(stmt)) == SQLite.SQLITE_ROW) {
      }
      assertEquals(SQLite.errmsg(db),SQLite.SQLITE_DONE,result);
    }
    finally {
      result = SQLite.finalize(stmt);
      assertEquals(SQLite.errmsg(db),SQLite.SQLITE_OK,result);
    }
  }

}

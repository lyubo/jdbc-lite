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

public class TestJNA01
{
  Pointer db = null;

  @Test
  public void sqlite3_libversion()
  {
    int vn = SQLite.libversion_number();
    String[] v = SQLite.libversion().split("\\.");
    assertEquals("Version check api:", vn, Integer.parseInt(v[0]) * 1000000 + Integer.parseInt(v[1]) * 1000 + Integer.parseInt(v[2]));
    assertEquals("Major version 3 check:", "3", v[0]);
  }

  @Test
  public void sqlite3_open()
  {
    // database file check
    File dbFile = new File("test.db");
    assertFalse(String.format("File '%s' already exists. Cannot test database file creation.",dbFile.getAbsolutePath()), dbFile.exists());

    // Open database
    PointerByReference pdb = new PointerByReference();
    assertEquals(SQLite.SQLITE_OK, SQLite.open(dbFile.getAbsolutePath(), pdb));
    db = pdb.getValue();
    assertNotNull(db);
    assertTrue(dbFile.exists());
  }

  @Test
  public void sqlite3_close()
  {
    int result = SQLite.close(db);
    assertEquals(SQLite.SQLITE_OK, result);
  }

  @Test
  public void sqlite3_open16()
  {
    // database file check
    File dbFile = new File("test16.db");
    assertFalse(String.format("File '%s' already exists. Cannot test database file creation.",dbFile.getAbsolutePath()), dbFile.exists());

    // Open database
    PointerByReference pdb = new PointerByReference();
    assertEquals(SQLite.SQLITE_OK, SQLite.open16(new SQLite.String16(dbFile.getAbsolutePath()), pdb));
    Pointer db16 = pdb.getValue();
    assertNotNull(db16);
    assertTrue(dbFile.exists());
    assertEquals(SQLite.SQLITE_OK, SQLite.close(db16));
  }

}

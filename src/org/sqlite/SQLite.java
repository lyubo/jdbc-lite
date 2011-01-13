package org.sqlite;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Callback;
import com.sun.jna.FromNativeContext;
import com.sun.jna.FromNativeConverter;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeMapped;
import com.sun.jna.Pointer;
import com.sun.jna.ToNativeContext;
import com.sun.jna.ToNativeConverter;
import com.sun.jna.TypeMapper;
import com.sun.jna.ptr.PointerByReference;

public class SQLite
{

  // Fundamental Data Types
  public final static int SQLITE_INTEGER    = 1;
  public final static int SQLITE_FLOAT      = 2;
  public final static int SQLITE_BLOB       = 4;
  public final static int SQLITE_NULL       = 5;
  public final static int SQLITE_TEXT       = 3;

  //Result Codes
  public final static int SQLITE_OK         = 0;
  public final static int SQLITE_ERROR      = 1;  // SQL error or missing database
  public final static int SQLITE_INTERNAL   = 2;  // Internal logic error in SQLite
  public final static int SQLITE_PERM       = 3;  // Access permission denied
  public final static int SQLITE_ABORT      = 4;  // Callback routine requested an abort
  public final static int SQLITE_BUSY       = 5;  // The database file is locked
  public final static int SQLITE_LOCKED     = 6;  // A table in the database is locked
  public final static int SQLITE_NOMEM      = 7;  // A malloc() failed
  public final static int SQLITE_READONLY   = 8;  // Attempt to write a readonly database
  public final static int SQLITE_INTERRUPT  = 9;  // Operation terminated by sqlite3_interrupt()
  public final static int SQLITE_IOERR      = 10; // Some kind of disk I/O error occurred
  public final static int SQLITE_CORRUPT    = 11; // The database disk image is malformed
  public final static int SQLITE_NOTFOUND   = 12; // NOT USED. Table or record not found
  public final static int SQLITE_FULL       = 13; // Insertion failed because database is full
  public final static int SQLITE_CANTOPEN   = 14; // Unable to open the database file
  public final static int SQLITE_PROTOCOL   = 15; // NOT USED. Database lock protocol error
  public final static int SQLITE_EMPTY      = 16; // Database is empty
  public final static int SQLITE_SCHEMA     = 17; // The database schema changed
  public final static int SQLITE_TOOBIG     = 18; // String or BLOB exceeds size limit
  public final static int SQLITE_CONSTRAINT = 19; // Abort due to constraint violation
  public final static int SQLITE_MISMATCH   = 20; // Data type mismatch
  public final static int SQLITE_MISUSE     = 21; // Library used incorrectly
  public final static int SQLITE_NOLFS      = 22; // Uses OS features not supported on host
  public final static int SQLITE_AUTH       = 23; // Authorization denied
  public final static int SQLITE_FORMAT     = 24; // Auxiliary database format error
  public final static int SQLITE_RANGE      = 25; // 2nd parameter to sqlite3_bind out of range
  public final static int SQLITE_NOTADB     = 26; // File opened that is not a database file
  public final static int SQLITE_ROW        = 100; // sqlite3_step() has another row ready
  public final static int SQLITE_DONE       = 101; // sqlite3_step() has finished executing

  public interface SQLiteAPI extends Library
  {
    public int sqlite3_libversion_number();

    public String sqlite3_libversion();

    public int sqlite3_open(String filename, PointerByReference ppDb);

    public int sqlite3_open16(String16 filename, PointerByReference ppDb);

    public int sqlite3_open_v2(String filename, PointerByReference handle, int flags, String zVfs);

    public int sqlite3_close(Pointer sqlite3);

    public int sqlite3_errcode(Pointer sqlite3);

    public String sqlite3_errmsg(Pointer sqlite3);

    public String16 sqlite3_errmsg16(Pointer sqlite3);

    public int sqlite3_get_autocommit(Pointer sqlite3);

    public int sqlite3_changes(Pointer sqlite3);

    public long sqlite3_last_insert_rowid(Pointer sqlite3);

    public int sqlite3_exec(Pointer sqlite3, String sql, ExecCallback callback, Pointer data, PointerByReference errmsg);

    public int sqlite3_bind_parameter_count(Pointer sqlite3_stmt);

    public int sqlite3_bind_null(Pointer sqlite3_stmt, int index);

    public int sqlite3_bind_int(Pointer sqlite3_stmt, int index, int value);

    public int sqlite3_bind_int64(Pointer sqlite3_stmt, int index, long value);

    //int sqlite3_bind_double(Pointer sqlite3_stmt, int index, double value);
    // Last parameter in the next 3 functions is useless in Java because of GC.
    public int sqlite3_bind_text(Pointer sqlite3_stmt, int index, String value, int length, Pointer dummy);

    //int sqlite3_bind_text16(Pointer sqlite3_stmt, int index, const void*, int, void(*)(void*));
    //int sqlite3_bind_blob(Pointer sqlite3_stmt, int index, const void*, int n, void(*)(void*));
    //int sqlite3_bind_value(Pointer sqlite3_stmt index, int, const sqlite3_value*);
    //int vbind_zeroblob(Pointer sqlite3_stmt index, int, int n);

    public int sqlite3_complete(String zSql);

    public int sqlite3_complete16(String16 zSql);

    public int sqlite3_prepare(Pointer sqlite3, String zSql, int nByte, PointerByReference ppStmt, PointerByReference pzTail);

    public int sqlite3_prepare_v2(Pointer sqlite3, String zSql, int nByte, PointerByReference ppStmt, PointerByReference pzTail);

    public int sqlite3_prepare16(Pointer sqlite3, String16 zSql, int nByte, PointerByReference ppStmt, PointerByReference pzTail);

    public int sqlite3_prepare16_v2(Pointer sqlite3, String16 zSql, int nByte, PointerByReference ppStmt, PointerByReference pzTail);

    public int sqlite3_reset(Pointer sqlite3_stmt);

    public int sqlite3_step(Pointer sqlite3_stmt);

    public int sqlite3_finalize(Pointer sqlite3_stmt);

    public int sqlite3_column_count(Pointer sqlite3_stmt);

    public String sqlite3_column_name(Pointer sqlite3_stmt, int N);

    public String16 sqlite3_column_name16(Pointer sqlite3_stmt, int N);

    public int sqlite3_column_type(Pointer sqlite3_stmt, int iCol);

    public int sqlite3_column_int(Pointer sqlite3_stmt, int iCol);

    public long sqlite3_column_int64(Pointer sqlite3_stmt, int iCol);

    public double sqlite3_column_double(Pointer sqlite3_stmt, int iCol);

    public String sqlite3_column_text(Pointer sqlite3_stmt, int iCol);

    public String16 sqlite3_column_text16(Pointer sqlite3_stmt, int iCol);

    public Pointer sqlite3_column_blob(Pointer sqlite3_stmt, int iCol);

    public int sqlite3_column_bytes(Pointer sqlite3_stmt, int iCol);

    public int sqlite3_column_bytes16(Pointer sqlite3_stmt, int iCol);

    public String sqlite3_column_table_name(Pointer sqlite3_stmt, int iCol);

    public String16 sqlite3_column_table_name16(Pointer sqlite3_stmt, int iCol);

    public String16 sqlite3_value_text16(Pointer sqlite3_value);

    public String16LE sqlite3_value_text16le(Pointer sqlite3_value);

    public String16BE sqlite3_value_text16be(Pointer sqlite3_value);

  }

  interface ExecCallback extends Callback
  {
    void callback(Pointer data, int columnCount, String[] values, String[] names);
  }

  interface ParameterDestroyer extends Callback
  {
    void destroy();
  }

  private static final String          LIBRARY_NAME    = "sqlite3";
  private static SQLiteAPI             api;
  private static Map<String, Object>   options         = new HashMap<String, Object>();
  private static SQLiteStringConverter stringConverter = new SQLiteStringConverter();

  static {

    options.put(Library.OPTION_TYPE_MAPPER, new TypeMapper() {

      @SuppressWarnings("unchecked")
      public FromNativeConverter getFromNativeConverter(Class javaType)
      {
        return String.class.equals(javaType) ? stringConverter : null;
      }

      @SuppressWarnings("unchecked")
      public ToNativeConverter getToNativeConverter(Class javaType)
      {
        return String.class.equals(javaType) ? stringConverter : null;
      }
    });

    api = (SQLiteAPI) Native.loadLibrary(LIBRARY_NAME, SQLiteAPI.class, options);
  }

  static class SQLiteStringConverter implements FromNativeConverter, ToNativeConverter
  {
    private static final String MSG_USUPPORTED_CHARSET = "'%s' is not suported by this VM";
    private static final String encoding               = "UTF-8";

    @SuppressWarnings("unchecked")
    public Class nativeType()
    {
      return Pointer.class;
    }

    public Object fromNative(Object nativeValue, FromNativeContext ctx)
    {
      Pointer p = (Pointer) nativeValue;
      if (p == null) return null;

      long len = p.indexOf(0, (byte) 0);
      if (len != -1 && (len <= Integer.MAX_VALUE)) {
        try {
          return new String(p.getByteArray(0, (int) len), encoding);
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(String.format(MSG_USUPPORTED_CHARSET, encoding));
        }
      }
      return null;
    }

    public Object toNative(Object value, ToNativeContext ctx)
    {
      try {
        byte[] buf = ((String) value).getBytes(encoding);
        Pointer p = new Memory(buf.length + 1);
        p.write(0, buf, 0, buf.length);
        p.setByte(buf.length, (byte) 0);
        return p;

      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(String.format(MSG_USUPPORTED_CHARSET, encoding));
      }

    }
  }

  private static abstract class String16Base implements NativeMapped
  {

    private static final String MSG_USUPPORTED_CHARSET = "'%s' is not suported by this VM";
    protected String            value;

    public Object fromNative(Object nativeValue, FromNativeContext context)
    {
      Pointer p = (Pointer) nativeValue;
      int len = 0;
      while (++len <= Integer.MAX_VALUE)
        if (p.getByte(len - 1) == 0 && p.getByte(len) == 0) break;
      try {
        value = new String(p.getByteArray(0, len), getCharset());
        return this;
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(String.format(MSG_USUPPORTED_CHARSET, getCharset()));
      }
    }

    @SuppressWarnings("unchecked")
    public Class nativeType()
    {
      return Pointer.class;
    }

    public Object toNative()
    {
      try {
        byte[] buf = value.getBytes(getCharset());
        Pointer p = new Memory(buf.length + 2);
        p.write(0, buf, 0, buf.length);
        p.setMemory(buf.length, 2, (byte) 0);
        return p;

      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(String.format(MSG_USUPPORTED_CHARSET, getCharset()));
      }
    }

    protected abstract String getCharset();

    public String toString()
    {
      return value;
    }

  }

  public static final class String16 extends String16Base
  {
    final private static String CHARSET = "UTF-16LE";

    public String16()
    {
      value = new String();
    }

    public String16(String s)
    {
      value = s;
    }

    @Override
    protected String getCharset()
    {
      return CHARSET;
    }

  }

  public static final class String16LE extends String16Base
  {
    final private static String CHARSET = "UTF-16LE";

    public String16LE()
    {
      value = new String();
    }

    public String16LE(String s)
    {
      value = s;
    }

    @Override
    protected String getCharset()
    {
      return CHARSET;
    }

  }

  public static final class String16BE extends String16Base
  {
    final private static String CHARSET = "UTF-16BE";

    public String16BE()
    {
      value = new String();
    }

    public String16BE(String s)
    {
      value = s;
    }

    @Override
    protected String getCharset()
    {
      return CHARSET;
    }

  }

  static public int bind_int(Pointer sqlite3_stmt, int index, int value)
  {
    return api.sqlite3_bind_int(sqlite3_stmt, index, value);
  }

  static public int bind_int64(Pointer sqlite3_stmt, int index, long value)
  {
    return api.sqlite3_bind_int64(sqlite3_stmt, index, value);
  }

  static public int bind_null(Pointer sqlite3_stmt, int index)
  {
    return api.sqlite3_bind_null(sqlite3_stmt, index);
  }

  static public int bind_parameter_count(Pointer sqlite3_stmt)
  {
    return api.sqlite3_bind_parameter_count(sqlite3_stmt);
  }

  static public int bind_text(Pointer sqlite3_stmt, int index, String value, int length, Pointer dummy)
  {
    return api.sqlite3_bind_text(sqlite3_stmt, index, value, length, dummy);
  }

  static public int changes(Pointer sqlite3)
  {
    return api.sqlite3_changes(sqlite3);
  }

  static public long last_insert_rowid(Pointer sqlite3)
  {
    return api.sqlite3_last_insert_rowid(sqlite3);
  }
  
  static public int close(Pointer sqlite3)
  {
    return api.sqlite3_close(sqlite3);
  }

  static public Pointer column_blob(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_blob(sqlite3_stmt, iCol);
  }

  static public int column_bytes(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_bytes(sqlite3_stmt, iCol);
  }

  static public int column_bytes16(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_bytes16(sqlite3_stmt, iCol);
  }

  static public int column_count(Pointer sqlite3_stmt)
  {
    return api.sqlite3_column_count(sqlite3_stmt);
  }

  static public double column_double(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_double(sqlite3_stmt, iCol);
  }

  static public int column_int(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_int(sqlite3_stmt, iCol);
  }

  static public long column_int64(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_int64(sqlite3_stmt, iCol);
  }

  static public String column_name(Pointer sqlite3_stmt, int N)
  {
    return api.sqlite3_column_name(sqlite3_stmt, N);
  }

  static public String16 column_name16(Pointer sqlite3_stmt, int N)
  {
    return api.sqlite3_column_name16(sqlite3_stmt, N);
  }

  static public String column_table_name(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_table_name(sqlite3_stmt, iCol);
  }

  static public String16 column_table_name16(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_table_name16(sqlite3_stmt, iCol);
  }

  static public String column_text(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_text(sqlite3_stmt, iCol);
  }

  static public String16 column_text16(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_text16(sqlite3_stmt, iCol);
  }

  static public int column_type(Pointer sqlite3_stmt, int iCol)
  {
    return api.sqlite3_column_type(sqlite3_stmt, iCol);
  }

  static public int complete(String zSql)
  {
    return api.sqlite3_complete(zSql);
  }

  static public int complete16(String16 zSql)
  {
    return api.sqlite3_complete16(zSql);
  }

  static public int errcode(Pointer sqlite3)
  {
    return api.sqlite3_errcode(sqlite3);
  }

  static public String errmsg(Pointer sqlite3)
  {
    return api.sqlite3_errmsg(sqlite3);
  }

  static public String16 errmsg16(Pointer sqlite3)
  {
    return api.sqlite3_errmsg16(sqlite3);
  }

  static public int exec(Pointer sqlite3, String sql, ExecCallback callback, Pointer data, PointerByReference errmsg)
  {
    return api.sqlite3_exec(sqlite3, sql, callback, data, errmsg);
  }

  static public int finalize(Pointer sqlite3_stmt)
  {
    return api.sqlite3_finalize(sqlite3_stmt);
  }

  static public int get_autocommit(Pointer sqlite3_stmt)
  {
    return api.sqlite3_get_autocommit(sqlite3_stmt);
  }

  static public String libversion()
  {
    return api.sqlite3_libversion();
  }

  static public int libversion_number()
  {
    return api.sqlite3_libversion_number();
  }

  static public int open(String filename, PointerByReference ppDb)
  {
    return api.sqlite3_open(filename, ppDb);
  }

  static public int open16(String16 filename, PointerByReference ppDb)
  {
    return api.sqlite3_open16(filename, ppDb);
  }

  static public int open_v2(String filename, PointerByReference handle, int flags, String zVfs)
  {
    return api.sqlite3_open_v2(filename, handle, flags, zVfs);
  }

  static public int prepare(Pointer sqlite3, String zSql, int nByte, PointerByReference ppStmt, PointerByReference pzTail)
  {
    return api.sqlite3_prepare(sqlite3, zSql, nByte, ppStmt, pzTail);
  }

  static public int prepare16(Pointer sqlite3, String16 zSql, int nByte, PointerByReference ppStmt, PointerByReference pzTail)
  {
    return api.sqlite3_prepare16(sqlite3, zSql, nByte, ppStmt, pzTail);
  }

  static public int prepare16_v2(Pointer sqlite3, String16 zSql, int nByte, PointerByReference ppStmt, PointerByReference pzTail)
  {
    return api.sqlite3_prepare16_v2(sqlite3, zSql, nByte, ppStmt, pzTail);
  }

  static public int prepare_v2(Pointer sqlite3, String zSql, int nByte, PointerByReference ppStmt, PointerByReference pzTail)
  {
    return api.sqlite3_prepare_v2(sqlite3, zSql, nByte, ppStmt, pzTail);
  }

  static public int reset(Pointer sqlite3_stmt)
  {
    return api.sqlite3_reset(sqlite3_stmt);
  }

  static public int step(Pointer sqlite3_stmt)
  {
    return api.sqlite3_step(sqlite3_stmt);
  }

  static public String16 value_text16(Pointer sqlite3_value)
  {
    return api.sqlite3_value_text16(sqlite3_value);
  }

  static public String16BE value_text16be(Pointer sqlite3_value)
  {
    return api.sqlite3_value_text16be(sqlite3_value);
  }

  static public String16LE value_text16le(Pointer sqlite3_value)
  {
    return api.sqlite3_value_text16le(sqlite3_value);
  }

}

package org.sqlite.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sqlite.SQLite;

import com.sun.jna.Pointer;

public class SQLiteResultSet implements ResultSet,ResultSetMetaData
{
  
  private SQLiteStatement statement;
  private int lastResult;
  private List<ColumnDef> columnDefs;
  private Map<String,ColumnDef> columnNameMap;
  private boolean wasNull;
  private int currentRow;
  

  public SQLiteResultSet(SQLiteStatement pStatement)
  {
    super();
    statement = pStatement;
    lastResult = SQLite.SQLITE_OK;
    currentRow = 0;
    wasNull = false;
    columnDefs = new ArrayList<ColumnDef>();
    columnNameMap = new HashMap<String,ColumnDef>();
    for (int i = 0; i < SQLite.column_count(statement.stmt); i++) {
      ColumnDef cd = new ColumnDef(i + 1,SQLite.column_name(statement.stmt,i).toString());
      columnDefs.add(i,cd);
      columnNameMap.put(cd.name.toLowerCase(),cd);
    }
  }

  // ***************************************************************************
  // *** ResultSet implementation
  // ***************************************************************************
  public void close() throws SQLException
  {
    statement = null;
  }

  public boolean next() throws SQLException
  {
    checkIfClosed();
    if (lastResult == SQLite.SQLITE_ROW || lastResult == SQLite.SQLITE_OK) {
      statement.connection.check(lastResult = SQLite.step(statement.stmt));
      currentRow = (lastResult == SQLite.SQLITE_ROW) ? currentRow + 1 : 0;
    }
    return lastResult == SQLite.SQLITE_ROW;
  }

  public int findColumn(String columnName) throws SQLException
  {
   ColumnDef cd = columnNameMap.get(columnName.toLowerCase());
   if (cd == null) throw new SQLException("No such column");
   return cd.index;
  }
  
  public boolean wasNull() throws SQLException
  {
    return wasNull;
  }
  
  public int getInt(int columnIndex) throws SQLException
  {
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    return SQLite.column_int(statement.stmt,columnIndex - 1);
  }

  public int getInt(String columnName) throws SQLException
  {
    return getInt(findColumn(columnName));
  }
  
  public long getLong(int columnIndex) throws SQLException
  {
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    return SQLite.column_int64(statement.stmt, columnIndex - 1);
  }

  public long getLong(String columnName) throws SQLException
  {
    return getLong(findColumn(columnName));
  }

  public short getShort(int columnIndex) throws SQLException
  {
    return Integer.valueOf(getInt(columnIndex)).shortValue();
  }

  public short getShort(String columnName) throws SQLException
  {
    return getShort(findColumn(columnName));
  }

  public double getDouble(int columnIndex) throws SQLException
  {
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    return SQLite.column_double(statement.stmt, columnIndex - 1);
  }

  public double getDouble(String columnName) throws SQLException
  {
    return getDouble(findColumn(columnName));
  }

  public float getFloat(int columnIndex) throws SQLException
  {
    return Double.valueOf(getDouble(columnIndex)).floatValue();
  }

  public float getFloat(String columnName) throws SQLException
  {
    return getFloat(findColumn(columnName));
  }

  public byte[] getBytes(int columnIndex) throws SQLException
  {
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    if (wasNull) {
      return null;
    }
    Pointer data = SQLite.column_blob(statement.stmt, columnIndex - 1);
    int dataSize = SQLite.column_bytes(statement.stmt,columnIndex - 1);
    byte[] result = new byte[dataSize];
    data.read(0, result,0,dataSize);
    return result;
  }

  public byte[] getBytes(String columnName) throws SQLException
  {
    return getBytes(findColumn(columnName));
  }

  public String getString(int columnIndex) throws SQLException
  {
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    return SQLite.column_text(statement.stmt, columnIndex - 1);
  }

  public String getString(String columnName) throws SQLException
  {
    return getString(findColumn(columnName));
  }


  public Date getDate(int columnIndex) throws SQLException
  {
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    return wasNull?null:new Date(SQLite.column_int64(statement.stmt, columnIndex - 1));
  }

  public Date getDate(String columnName) throws SQLException
  {
    return getDate(findColumn(columnName));
  }

  public Date getDate(int columnIndex, Calendar cal) throws SQLException
  {
    if (cal == null) {
      return getDate(columnIndex);
    }
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    if (wasNull) {
      return null;
    } else {
      cal.setTimeInMillis(SQLite.column_int64(statement.stmt, columnIndex - 1));
      return new Date(cal.getTime().getTime());
    }
  }

  public Date getDate(String columnName, Calendar cal) throws SQLException
  {
    return getDate(findColumn(columnName),cal);
  }

  public Time getTime(int columnIndex) throws SQLException
  {
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    return wasNull?null:new Time(SQLite.column_int64(statement.stmt, columnIndex - 1));
  }

  public Time getTime(String columnName) throws SQLException
  {
    return getTime(findColumn(columnName));
  }

  public Time getTime(int columnIndex, Calendar cal) throws SQLException
  {
    if (cal == null) {
      return getTime(columnIndex);
    }
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    if (wasNull) {
      return null;
    } else {
      cal.setTimeInMillis(SQLite.column_int64(statement.stmt, columnIndex - 1));
      return new Time(cal.getTime().getTime());
    }
  }

  public Time getTime(String columnName, Calendar cal) throws SQLException
  {
    return getTime(findColumn(columnName),cal);
  }

  public Timestamp getTimestamp(int columnIndex) throws SQLException
  {
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    return wasNull?null:new Timestamp(SQLite.column_int64(statement.stmt, columnIndex - 1));
  }

  public Timestamp getTimestamp(String columnName) throws SQLException
  {
    return getTimestamp(findColumn(columnName));
  }

  public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException
  {
    if (cal == null) {
      return getTimestamp(columnIndex);
    }
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    if (wasNull) {
      return null;
    } else {
      cal.setTimeInMillis(SQLite.column_int64(statement.stmt, columnIndex - 1));
      return new Timestamp(cal.getTime().getTime());
    }
  }

  public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException
  {
    return getTimestamp(findColumn(columnName),cal);
  }

  public boolean getBoolean(int columnIndex) throws SQLException
  {
    checkIfClosed();
    wasNull = (SQLite.column_type(statement.stmt, columnIndex - 1) == SQLite.SQLITE_NULL);
    return SQLite.column_int(statement.stmt, columnIndex - 1) == 0 ? Boolean.FALSE.booleanValue() : Boolean.TRUE.booleanValue();
  }

  public boolean getBoolean(String columnName) throws SQLException
  {
    return getBoolean(findColumn(columnName));
  }

  public Object getObject(int columnIndex) throws SQLException
  {
    checkIfClosed();
    int columnType = SQLite.column_type(statement.stmt, columnIndex - 1);
    wasNull = (columnType == SQLite.SQLITE_NULL);

    switch (columnType) {
      
      case SQLite.SQLITE_NULL:
        return null;
      
      case SQLite.SQLITE_INTEGER:
        return SQLite.column_int64(statement.stmt,columnIndex - 1);
      
      case SQLite.SQLITE_FLOAT:
        return SQLite.column_double(statement.stmt,columnIndex - 1);
      
      case SQLite.SQLITE_BLOB:
        Pointer data = SQLite.column_blob(statement.stmt, columnIndex - 1);
        int dataSize = SQLite.column_bytes(statement.stmt,columnIndex - 1);
        byte[] result = new byte[dataSize];
        data.read(0,result,0,dataSize);
        return result;
      
      default:
        return SQLite.column_text(statement.stmt,columnIndex - 1);
    }
  }

  public Object getObject(String columnName) throws SQLException
  {
    return getObject(findColumn(columnName));
  }

  public ResultSetMetaData getMetaData() throws SQLException
  {
    return this;
  }

  // ***************************************************************************
  // *** ResultSetMetaData implementation
  // ***************************************************************************
  public int getColumnCount() throws SQLException
  {
    return columnDefs.size();
  }
  
  public String getColumnName(int column) throws SQLException
  {
    return columnDefs.get(column - 1).name;
  }

  public String getTableName(int column) throws SQLException
  {
    String result = columnDefs.get(column - 1).tableName;
    if (result == null) {
      result = SQLite.column_table_name(statement.stmt, column - 1);
      if (result == null) result = "";
      columnDefs.get(column - 1).tableName = result;
    }
    return result;
  }

  // ***************************************************************************
  // *** Private/protected methods
  // ***************************************************************************
  private void checkIfClosed() throws SQLException
  {
    if (statement == null) throw new SQLException("ResultSet is closed");
  }
  

   
  // ***************************************************************************
  // *** Unimplemented methods
  // ***************************************************************************
  public boolean absolute(int row) throws SQLException
  {
    throw new SQLException("Not implemented: absolute(int row)");
  }

  public void afterLast() throws SQLException
  {
    throw new SQLException("Not implemented:  afterLast()");
  }

  public void beforeFirst() throws SQLException
  {
    throw new SQLException("Not implemented:beforeFirst() ");
  }

  public void cancelRowUpdates() throws SQLException
  {
    throw new SQLException("Not implemented: cancelRowUpdates()");
  }

  public void clearWarnings() throws SQLException
  {
    throw new SQLException("Not implemented: clearWarnings()");
  }

  public void deleteRow() throws SQLException
  {
    throw new SQLException("Not implemented: deleteRow()");
  }

  public boolean first() throws SQLException
  {
    throw new SQLException("Not implemented: first()");
  }

  public Array getArray(int i) throws SQLException
  {
    throw new SQLException("Not implemented:  getArray(int i)");
  }

  public Array getArray(String colName) throws SQLException
  {
    throw new SQLException("Not implemented: getArray(String colName)");
  }

  public InputStream getAsciiStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented:  InputStream getAsciiStream(int columnIndex)");
  }

  public InputStream getAsciiStream(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: getAsciiStream(String columnName)");
  }

  public BigDecimal getBigDecimal(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: BigDecimal getBigDecimal(int columnIndex)");
  }

  public BigDecimal getBigDecimal(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: BigDecimal getBigDecimal(String columnName)");
  }

  public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException
  {
    throw new SQLException("Not implemented: BigDecimal getBigDecimal(int columnIndex, int scale)");
  }

  public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException
  {
    throw new SQLException("Not implemented: getBigDecimal(String columnName, int scale)");
  }

  public InputStream getBinaryStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: getBinaryStream(int columnIndex)");
  }

  public InputStream getBinaryStream(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: getBinaryStream(String columnName)");
  }

  public Blob getBlob(int i) throws SQLException
  {
    throw new SQLException("Not implemented: etBlob(int i)");
  }

  public Blob getBlob(String colName) throws SQLException
  {
    throw new SQLException("Not implemented: getBlob(String colName)");
  }

  public byte getByte(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: getByte(int columnIndex)");
  }

  public byte getByte(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: getByte(String columnName)");
  }

  public Reader getCharacterStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: getCharacterStream(int columnIndex)");
  }

  public Reader getCharacterStream(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented:  getCharacterStream(String columnName)");
  }

  public Clob getClob(int i) throws SQLException
  {
    throw new SQLException("Not implemented: Clob getClob(int i)");
  }

  public Clob getClob(String colName) throws SQLException
  {
    throw new SQLException("Not implemented: getClob(String colName)");
  }

  public int getConcurrency() throws SQLException
  {
    throw new SQLException("Not implemented: getConcurrency()");
  }

  public String getCursorName() throws SQLException
  {
    throw new SQLException("Not implemented: getCursorName()");
  }

   public int getFetchDirection() throws SQLException
  {
     throw new SQLException("Not implemented: getFetchDirection()");
  }

  public int getFetchSize() throws SQLException
  {
    throw new SQLException("Not implemented: getFetchSize()");
  }

  public Object getObject(int i, Map<String, Class<?>> map) throws SQLException
  {
    throw new SQLException("Not implemented: Object getObject(int i, Map<String, Class<?>> map)");
  }

  public Object getObject(String colName, Map<String, Class<?>> map) throws SQLException
  {
    throw new SQLException("Not implemented: getObject(String colName, Map<String, Class<?>> map)");
  }

  public Ref getRef(int i) throws SQLException
  {
    throw new SQLException("Not implemented: getRef(int i)");
  }

  public Ref getRef(String colName) throws SQLException
  {
    throw new SQLException("Not implemented: Ref getRef(String colName)");
  }

  public int getRow() throws SQLException
  {
    throw new SQLException("Not implemented: getRow() ");
  }

  public Statement getStatement() throws SQLException
  {
    throw new SQLException("Not implemented: Statement getStatement()");
  }

  public int getType() throws SQLException
  {
    throw new SQLException("Not implemented:  getType()");
  }

  public URL getURL(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: URL getURL(int columnIndex)");
  }

  public URL getURL(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: URL getURL(String columnName)");
  }

  public InputStream getUnicodeStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: InputStream getUnicodeStream(int columnIndex)");
  }

  public InputStream getUnicodeStream(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: InputStream getUnicodeStream(String columnName)");
  }

  public SQLWarning getWarnings() throws SQLException
  {
    throw new SQLException("Not implemented: SQLWarning getWarnings()");
  }

  public void insertRow() throws SQLException
  {
    throw new SQLException("Not implemented: void insertRow()");
  }

  public boolean isAfterLast() throws SQLException
  {
    throw new SQLException("Not implemented:  isAfterLast()");
  }

  public boolean isBeforeFirst() throws SQLException
  {
    throw new SQLException("Not implemented: isBeforeFirst()");
  }

  public boolean isFirst() throws SQLException
  {
    throw new SQLException("Not implemented:  isFirst()");
  }

  public boolean isLast() throws SQLException
  {
    throw new SQLException("Not implemented: boolean isLast()");
  }

  public boolean last() throws SQLException
  {
    throw new SQLException("Not implemented: last()");
  }

  public void moveToCurrentRow() throws SQLException
  {
    throw new SQLException("Not implemented: moveToCurrentRow()");
  }

  public void moveToInsertRow() throws SQLException
  {
    throw new SQLException("Not implemented: moveToInsertRow()");
  }

  public boolean previous() throws SQLException
  {
    throw new SQLException("Not implemented: previous()");
  }

  public void refreshRow() throws SQLException
  {
    throw new SQLException("Not implemented: refreshRow()");
  }

  public boolean relative(int rows) throws SQLException
  {
    throw new SQLException("Not implemented: relative(int rows)");
  }

  public boolean rowDeleted() throws SQLException
  {
    throw new SQLException("Not implemented: rowDeleted()");
  }

  public boolean rowInserted() throws SQLException
  {
    throw new SQLException("Not implemented: rowInserted()");
  }

  public boolean rowUpdated() throws SQLException
  {
    throw new SQLException("Not implemented: rowUpdated()");
  }

  public void setFetchDirection(int direction) throws SQLException
  {
    throw new SQLException("Not implemented: setFetchDirection(int direction");
  }

  public void setFetchSize(int rows) throws SQLException
  {
    throw new SQLException("Not implemented: setFetchSize(int rows)");
  }

  public void updateArray(int columnIndex, Array x) throws SQLException
  {
    throw new SQLException("Not implemented: updateArray(int columnIndex, Array x)");
  }

  public void updateArray(String columnName, Array x) throws SQLException
  {
    throw new SQLException("Not implemented: updateArray(String columnName, Array x)");
  }

  public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: updateAsciiStream(int columnIndex, InputStream x, int length)");
  }

  public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: updateAsciiStream(String columnName, InputStream x, int length)");
  }

  public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException
  {
    throw new SQLException("Not implemented: pdateBigDecimal(int columnIndex, BigDecimal x)");
  }

  public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException
  {
    throw new SQLException("Not implemented:  updateBigDecimal(String columnName, BigDecimal x)");
  }

  public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: updateBinaryStream(int columnIndex, InputStream x, int length)");
  }

  public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: updateBinaryStream(String columnName, InputStream x, int length)");
  }

  public void updateBlob(int columnIndex, Blob x) throws SQLException
  {
    throw new SQLException("Not implemented:  updateBlob(int columnIndex, Blob x)");
  }

  public void updateBlob(String columnName, Blob x) throws SQLException
  {
    throw new SQLException("Not implemented: updateBlob(String columnName, Blob x)");
  }

  public void updateBoolean(int columnIndex, boolean x) throws SQLException
  {
    throw new SQLException("Not implemented: pdateBoolean(int columnIndex, boolean x)");
  }

  public void updateBoolean(String columnName, boolean x) throws SQLException
  {
    throw new SQLException("Not implemented: updateBoolean(String columnName, boolean x)");
  }

  public void updateByte(int columnIndex, byte x) throws SQLException
  {
    throw new SQLException("Not implemented: updateByte(int columnIndex, byte x)");
  }

  public void updateByte(String columnName, byte x) throws SQLException
  {
    throw new SQLException("Not implemented: updateByte(String columnName, byte x)");
  }

  public void updateBytes(int columnIndex, byte[] x) throws SQLException
  {
    throw new SQLException("Not implemented: updateBytes(int columnIndex, byte[] x)");
  }

  public void updateBytes(String columnName, byte[] x) throws SQLException
  {
    throw new SQLException("Not implemented: updateBytes(String columnName, byte[] x)");
  }

  public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: updateCharacterStream(int columnIndex, Reader x, int length)");
  }

  public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException
  {
    throw new SQLException("Not implemented: updateCharacterStream(String columnName, Reader reader, int length)");
  }

  public void updateClob(int columnIndex, Clob x) throws SQLException
  {
    throw new SQLException("Not implemented: updateClob(int columnIndex, Clob x)");
  }

  public void updateClob(String columnName, Clob x) throws SQLException
  {
    throw new SQLException("Not implemented: pdateClob(String columnName, Clob x)");
  }

  public void updateDate(int columnIndex, Date x) throws SQLException
  {
    throw new SQLException("Not implemented: updateDate(int columnIndex, Date x)");
  }

  public void updateDate(String columnName, Date x) throws SQLException
  {
    throw new SQLException("Not implemented: updateDate(String columnName, Date x)");
  }

  public void updateDouble(int columnIndex, double x) throws SQLException
  {
    throw new SQLException("Not implemented: updateDouble(int columnIndex, double x)");
  }

  public void updateDouble(String columnName, double x) throws SQLException
  {
    throw new SQLException("Not implemented:  updateDouble(String columnName, double x)");
  }

  public void updateFloat(int columnIndex, float x) throws SQLException
  {
    throw new SQLException("Not implemented: updateFloat(int columnIndex, float x)");
  }

  public void updateFloat(String columnName, float x) throws SQLException
  {
    throw new SQLException("Not implemented: updateFloat(String columnName, float x)");
  }

  public void updateInt(int columnIndex, int x) throws SQLException
  {
    throw new SQLException("Not implemented: updateInt(int columnIndex, int x)");
  }

  public void updateInt(String columnName, int x) throws SQLException
  {
    throw new SQLException("Not implemented: updateInt(String columnName, int x)");
  }

  public void updateLong(int columnIndex, long x) throws SQLException
  {
    throw new SQLException("Not implemented: updateLong(int columnIndex, long x)");
  }

  public void updateLong(String columnName, long x) throws SQLException
  {
    throw new SQLException("Not implemented: updateLong(String columnName, long x)");
  }

  public void updateNull(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: updateNull(int columnIndex)");
  }

  public void updateNull(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: updateNull(String columnName)");
  }

  public void updateObject(int columnIndex, Object x) throws SQLException
  {
    throw new SQLException("Not implemented: updateObject(int columnIndex, Object x)");
  }

  public void updateObject(String columnName, Object x) throws SQLException
  {
    throw new SQLException("Not implemented: updateObject(String columnName, Object x)");
  }

  public void updateObject(int columnIndex, Object x, int scale) throws SQLException
  {
    throw new SQLException("Not implemented: updateObject(int columnIndex, Object x, int scale)");
  }

  public void updateObject(String columnName, Object x, int scale) throws SQLException
  {
    throw new SQLException("Not implemented: updateObject(String columnName, Object x, int scale)");
  }

  public void updateRef(int columnIndex, Ref x) throws SQLException
  {
    throw new SQLException("Not implemented: updateRef(int columnIndex, Ref x)");
  }

  public void updateRef(String columnName, Ref x) throws SQLException
  {
    throw new SQLException("Not implemented: pdateRef(String columnName, Ref x)");
  }

  public void updateRow() throws SQLException
  {
    throw new SQLException("Not implemented: updateRow()");
  }

  public void updateShort(int columnIndex, short x) throws SQLException
  {
    throw new SQLException("Not implemented: updateShort(int columnIndex, short x)");
  }

  public void updateShort(String columnName, short x) throws SQLException
  {
    throw new SQLException("Not implemented: updateShort(String columnName, short x)");
  }

  public void updateString(int columnIndex, String x) throws SQLException
  {
    throw new SQLException("Not implemented: updateString(int columnIndex, String x)");
  }

  public void updateString(String columnName, String x) throws SQLException
  {
    throw new SQLException("Not implemented: updateString(String columnName, String x)");
  }

  public void updateTime(int columnIndex, Time x) throws SQLException
  {
    throw new SQLException("Not implemented: updateTime(int columnIndex, Time x)");
  }

  public void updateTime(String columnName, Time x) throws SQLException
  {
    throw new SQLException("Not implemented: updateTime(String columnName, Time x)");
  }

  public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException
  {
    throw new SQLException("Not implemented: updateTimestamp(int columnIndex, Timestamp x)");
  }

  public void updateTimestamp(String columnName, Timestamp x) throws SQLException
  {
    throw new SQLException("Not implemented: updateTimestamp(String columnName, Timestamp x)");
  }

  // ***************************************************************************
  // *** ResultSetMetaData implementation
  // ***************************************************************************
  
  public String getCatalogName(int column) throws SQLException
  {
    throw new SQLException("Not implemented: getCatalogName(int column)");
  }

  public String getColumnClassName(int column) throws SQLException
  {
    throw new SQLException("Not implemented: getColumnClassName(int column)");
  }

  public int getColumnDisplaySize(int column) throws SQLException
  {
    throw new SQLException("Not implemented: getColumnDisplaySize(int column)");
  }

  public String getColumnLabel(int column) throws SQLException
  {
    throw new SQLException("Not implemented:  getColumnLabel(int column)");
  }

  public int getColumnType(int column) throws SQLException
  {
    throw new SQLException("Not implemented:  getColumnType(int column)");
  }

  public String getColumnTypeName(int column) throws SQLException
  {
    throw new SQLException("Not implemented:  getColumnTypeName(int column)");
  }

  public int getPrecision(int column) throws SQLException
  {
    throw new SQLException("Not implemented: getPrecision(int column)");
  }

  public int getScale(int column) throws SQLException
  {
    throw new SQLException("Not implemented: getScale(int column)");
  }

  public String getSchemaName(int column) throws SQLException
  {
    throw new SQLException("Not implemented: getSchemaName(int column)");
  }

  public boolean isAutoIncrement(int column) throws SQLException
  {
    throw new SQLException("Not implemented: isAutoIncrement(int column)");
  }

  public boolean isCaseSensitive(int column) throws SQLException
  {
    throw new SQLException("Not implemented: isCaseSensitive(int column)");
  }

  public boolean isCurrency(int column) throws SQLException
  {
    throw new SQLException("Not implemented: isCurrency(int column)");
  }

  public boolean isDefinitelyWritable(int column) throws SQLException
  {
    throw new SQLException("Not implemented: isDefinitelyWritable(int column)");
  }

  public int isNullable(int column) throws SQLException
  {
    throw new SQLException("Not implemented: isNullable(int column)");
  }

  public boolean isReadOnly(int column) throws SQLException
  {
    throw new SQLException("Not implemented: isReadOnly(int column)");
  }

  public boolean isSearchable(int column) throws SQLException
  {
    throw new SQLException("Not implemented: isSearchable(int column)");
  }

  public boolean isSigned(int column) throws SQLException
  {
    throw new SQLException("Not implemented: isSigned(int column)");
 }

  public boolean isWritable(int column) throws SQLException
  {
    throw new SQLException("Not implemented:  isWritable(int column)");
  }

  // ***************************************************************************
  class ColumnDef
  {
    int        index;
    String     name;
    String     tableName;

    public ColumnDef(int index,String name)
    {
      super();
      this.index = index;
      this.name = name;
      this.tableName = null;
    }
      
  }

  public int getHoldability() throws SQLException
  {
    throw new SQLException("Not implemented: getHoldability()");
  }

  public Reader getNCharacterStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: getNCharacterStream(int columnIndex)");
  }

  public Reader getNCharacterStream(String columnLabel) throws SQLException
  {
    throw new SQLException("Not implemented: getNCharacterStream(String columnLabel)");
  }

  public String getNString(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: getNString(int columnIndex)");
  }

  public String getNString(String columnLabel) throws SQLException
  {
    throw new SQLException("Not implemented: getNString(String columnLabel");
  }

  public boolean isClosed() throws SQLException
  {
    throw new SQLException("Not implemented: isClosed()");
  }

  public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException
  {
    throw new SQLException("Not implemented: updateAsciiStream(int columnIndex, InputStream x)");
  }

  public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException
  {
    throw new SQLException("Not implemented: updateAsciiStream(String columnLabel, InputStream x)");
  }

  public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateAsciiStream(int columnIndex, InputStream x, long length)"); 
  }

  public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateAsciiStream(String columnLabel, InputStream x, long length)"); 
  }

  public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException
  {
    throw new SQLException("Not implemented: updateBinaryStream(int columnIndex, InputStream x)");
  }

  public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException
  {
    throw new SQLException("Not implemented: updateBinaryStream(String columnLabel, InputStream x)");
  }

  public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateBinaryStream(int columnIndex, InputStream x, long length)"); 
  }

  public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException
  {
    throw new SQLException("Not implemented:  updateBinaryStream(String columnLabel, InputStream x, long length)");  
  }

  public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException
  {
    throw new SQLException("Not implemented: updateBlob(int columnIndex, InputStream inputStream)");
  }

  public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException
  {
    throw new SQLException("Not implemented: updateBlob(String columnLabel, InputStream inputStream)");
  }

  public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateBlob(int columnIndex, InputStream inputStream, long length)");
  }

  public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateBlob(String columnLabel, InputStream inputStream, long length)"); 
  }

  public void updateCharacterStream(int columnIndex, Reader x) throws SQLException
  {
    throw new SQLException("Not implemented: updateCharacterStream(int columnIndex, Reader x)");
  }

  public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: updateCharacterStream(String columnLabel, Reader reader)");
  }

  public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateCharacterStream(int columnIndex, Reader x, long length)"); 
  }

  public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateCharacterStream(String columnLabel, Reader reader, long length)");
  }

  public void updateClob(int columnIndex, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: pdateClob(int columnIndex, Reader reader)");
  }

  public void updateClob(String columnLabel, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: updateClob(String columnLabel, Reader reader)"); 
  }

  public void updateClob(int columnIndex, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateClob(int columnIndex, Reader reader, long length)");  
  }

  public void updateClob(String columnLabel, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateClob(String columnLabel, Reader reader, long length)");
  }

  public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException
  {
    throw new SQLException("Not implemented: updateNCharacterStream(int columnIndex, Reader x)");  
  }

  public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: updateNCharacterStream(String columnLabel, Reader reader)"); 
  }

  public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateNCharacterStream(int columnIndex, Reader x, long length)"); 
  }

  public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateNCharacterStream(String columnLabel, Reader reader, long length)");
  }

  public void updateNClob(int columnIndex, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: updateNClob(int columnIndex, Reader reader)"); 
  }

  public void updateNClob(String columnLabel, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: updateNClob(String columnLabel, Reader reader)"); 
  }

  public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateNClob(int columnIndex, Reader reader, long length)");
  }

  public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: updateNClob(String columnLabel, Reader reader, long length)");
  }

  public void updateNString(int columnIndex, String nString) throws SQLException
  {
    throw new SQLException("Not implemented: updateNString(int columnIndex, String nString)");
  }

  public void updateNString(String columnLabel, String nString) throws SQLException
  {
    throw new SQLException("Not implemented: updateNString(String columnLabel, String nString)");
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException
  {
    throw new SQLException("Not implemented: isWrapperFor(Class<?> iface)");
  }

  public <T> T unwrap(Class<T> iface) throws SQLException
  {
    throw new SQLException("Not implemented: <T> T unwrap(Class<T> iface)");
  }

  public NClob getNClob(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: getNClob(int columnIndex)");
  }

  public NClob getNClob(String columnLabel) throws SQLException
  {
    throw new SQLException("Not implemented: getNClob(String columnLabel)");
  }

  public RowId getRowId(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: getRowId(int columnIndex)");
  }

  public RowId getRowId(String columnLabel) throws SQLException
  {
    throw new SQLException("Not implemented:getRowId(String columnLabel)");
  }

  public SQLXML getSQLXML(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: getSQLXML(int columnIndex)");
  }

  public SQLXML getSQLXML(String columnLabel) throws SQLException
  {
    throw new SQLException("Not implemented: getSQLXML(String columnLabel)");
  }

  public void updateNClob(int columnIndex, NClob nClob) throws SQLException
  {
    throw new SQLException("Not implemented: updateNClob(int columnIndex, NClob nClob)");
  }

  public void updateNClob(String columnLabel, NClob nClob) throws SQLException
  {
    throw new SQLException("Not implemented: updateNClob(String columnLabel, NClob nClob)");
  }

  public void updateRowId(int columnIndex, RowId x) throws SQLException
  {
    throw new SQLException("Not implemented: updateRowId(int columnIndex, RowId x)");
  }

  public void updateRowId(String columnLabel, RowId x) throws SQLException
  {
    throw new SQLException("Not implemented: updateRowId(String columnLabel, RowId x)");
  }

  public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException
  {
    throw new SQLException("Not implemented: updateSQLXML(int columnIndex, SQLXML xmlObject)");
  }

  public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException
  {
    throw new SQLException("Not implemented: updateSQLXML(String columnLabel, SQLXML xmlObject)");
  }

 
}

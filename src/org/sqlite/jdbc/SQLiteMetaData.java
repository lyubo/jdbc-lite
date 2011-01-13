package org.sqlite.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sqlite.SQLite;

public class SQLiteMetaData implements DatabaseMetaData
{

  private SQLiteConnection connection;

  public SQLiteMetaData(SQLiteConnection pConnection)
  {
    super();
    connection = pConnection;
  }

  // ***************************************************************************
  // *** Implementation of java.sql.DatabaseMetaData interface
  // ***************************************************************************

  public String getDatabaseProductName() throws SQLException
  {
    return "sqlite";
  }

  public int getDatabaseMajorVersion() throws SQLException
  {
    return SQLite.libversion_number() / 1000000;
  }

  public int getDatabaseMinorVersion() throws SQLException
  {
    return (SQLite.libversion_number() % 1000000) / 1000;
  }

  public String getDatabaseProductVersion() throws SQLException
  {
    return SQLite.libversion();
  }

  public String getSchemaTerm() throws SQLException
  {
    return "schema";
  }

  private final static List<ColumnDef> getSchemas_columnDefs = new ArrayList<ColumnDef>();
  static {
    getSchemas_columnDefs.add(new ColumnDef(1, "TABLE_SCHEM", "String"));
    getSchemas_columnDefs.add(new ColumnDef(2, "TABLE_CAT", "String"));
  }

  public ResultSet getSchemas() throws SQLException
  {
    connection.checkConnection();

    List<Object[]> rows = new ArrayList<Object[]>();
    List<String> databases = getDatabases();
    for (String s : databases) {
      rows.add(new Object[] { "sqlite", s });
    }
    return new ListResultSet(getSchemas_columnDefs, rows);
  }

  public String getCatalogTerm() throws SQLException
  {
    return "catalog";
  }

  private final static List<ColumnDef> getCatalogs_columnDefs = new ArrayList<ColumnDef>();
  static {
    getCatalogs_columnDefs.add(new ColumnDef(1, "TABLE_CAT", "String"));
  }

  public ResultSet getCatalogs() throws SQLException
  {
    connection.checkConnection();

    List<Object[]> rows = new ArrayList<Object[]>();
    List<String> databases = getDatabases();
    for (String s : databases) {
      rows.add(new Object[] { s });
    }
    return new ListResultSet(getCatalogs_columnDefs, rows);
  }

  private final static List<ColumnDef> getTables_columnDefs = new ArrayList<ColumnDef>();
  static {
    getTables_columnDefs.add(new ColumnDef(1, "TABLE_CAT", "String"));
    getTables_columnDefs.add(new ColumnDef(2, "TABLE_SCHEM", "String"));
    getTables_columnDefs.add(new ColumnDef(3, "TABLE_NAME", "String"));
    getTables_columnDefs.add(new ColumnDef(4, "TABLE_TYPE", "String"));
    getTables_columnDefs.add(new ColumnDef(5, "REMARKS", "String"));
    getTables_columnDefs.add(new ColumnDef(6, "TYPE_CAT", "String"));
    getTables_columnDefs.add(new ColumnDef(7, "TYPE_SCHEM", "String"));
    getTables_columnDefs.add(new ColumnDef(8, "TYPE_NAME", "String"));
    getTables_columnDefs.add(new ColumnDef(9, "SELF_REFERENCING_COL_NAME", "String"));
    getTables_columnDefs.add(new ColumnDef(10, "REF_GENERATION", "String"));
  }

  private final static String          SQL_TABLE_LIST_INNER = " UNION SELECT '%1$s',tbl_name, CASE WHEN type = 'table' THEN 'TABLE' WHEN type = 'view' THEN 'VIEW' END FROM %1$s.sqlite_master";

  private final static String          SQL_TABLE_LIST       = "SELECT TABLE_CAT,TABLE_NAME,TABLE_TYPE"
                                                                + "  FROM (SELECT '' TABLE_CAT,tbl_name TABLE_NAME,CASE WHEN type = 'table' THEN 'TEMPORARY TABLE' WHEN type = 'view' THEN 'TEMPORARY VIEW' END TABLE_TYPE"
                                                                + "          FROM sqlite_temp_master%s) T %s" + " ORDER BY TABLE_TYPE,TABLE_CAT,TABLE_NAME";

  List<Object[]>                       rows                 = new ArrayList<Object[]>();

  // SQL_TABLE_COND "WHERE TABLE_NAME like '%s";

  public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException
  {
    connection.checkConnection();

    List<Object[]> rows = new ArrayList<Object[]>();
    List<String> databases = getDatabases();
    String sql = "";
    for (String db : databases) {
      sql = sql.concat(String.format(SQL_TABLE_LIST_INNER, db));
    }
    String filter = "";
    // Filter by type
    if (types != null) {
      for (int i = 0; i < types.length; i++)
        filter = filter.concat(i == 0 ? "(TABLE_TYPE IN ('" : "','").concat(types[i]);
      filter = filter.concat("'))");
    }
    // Filter by table name
    if (tableNamePattern != null) {
      filter = filter.concat(filter.length() > 0 ? " AND " : "").concat("(TABLE_NAME LIKE '").concat(tableNamePattern).concat("')");
    }
    // Filter by catalog
    if (catalog != null) {
      filter = filter.concat(filter.length() > 0 ? " AND " : "").concat("(TABLE_CAT='").concat(catalog).concat("')");
    }
    // Filter by schema
    if (schemaPattern != null) {
      filter = filter.concat(filter.length() > 0 ? " AND " : "").concat("(TABLE_SCHEM LIKE '").concat(schemaPattern).concat("')");
    }

    sql = String.format(SQL_TABLE_LIST, sql, (filter.length() > 0) ? "WHERE ".concat(filter) : "");
    Statement s = connection.createStatement();
    try {
      ResultSet rs = s.executeQuery(sql);
      while (rs.next())
        rows.add(new Object[] { rs.getString(1), "sqlite", rs.getString(2), rs.getString(3), "", null, null, null, null, null });
    } finally {
      s.close();
    }
    return new ListResultSet(getTables_columnDefs, rows);
  }

  public boolean supportsGetGeneratedKeys() throws SQLException
  {
    return true;
  }

  public boolean supportsResultSetType(int type) throws SQLException
  {
    return type == ResultSet.TYPE_FORWARD_ONLY;
  }

  public boolean supportsResultSetHoldability(int holdability) throws SQLException
  {
    return holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT;
  }

  public boolean allProceduresAreCallable() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean allTablesAreSelectable() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean dataDefinitionCausesTransactionCommit() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean dataDefinitionIgnoredInTransactions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean deletesAreDetected(int type) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean doesMaxRowSizeIncludeBlobs() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getCatalogSeparator() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Connection getConnection() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getCrossReference(String primaryCatalog, String primarySchema, String primaryTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getDefaultTransactionIsolation() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getDriverMajorVersion()
  {
    throw new RuntimeException("Not implemented: getDriverMajorVersion");
  }

  public int getDriverMinorVersion()
  {
    throw new RuntimeException("Not implemented: getDriverMinorVersion");
  }

  public String getDriverName() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getDriverVersion() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getExtraNameCharacters() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getIdentifierQuoteString() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getJDBCMajorVersion() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getJDBCMinorVersion() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxBinaryLiteralLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxCatalogNameLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxCharLiteralLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxColumnNameLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxColumnsInGroupBy() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxColumnsInIndex() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxColumnsInOrderBy() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxColumnsInSelect() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxColumnsInTable() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxConnections() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxCursorNameLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxIndexLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxProcedureNameLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxRowSize() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxSchemaNameLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxStatementLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxStatements() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxTableNameLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxTablesInSelect() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getMaxUserNameLength() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getNumericFunctions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getProcedureTerm() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getResultSetHoldability() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getSQLKeywords() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getSQLStateType() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getSearchStringEscape() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getStringFunctions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getSystemFunctions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getTableTypes() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getTimeDateFunctions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getTypeInfo() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getURL() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getUserName() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean insertsAreDetected(int type) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isCatalogAtStart() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isReadOnly() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean locatorsUpdateCopy() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean nullPlusNonNullIsNull() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean nullsAreSortedAtEnd() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean nullsAreSortedAtStart() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean nullsAreSortedHigh() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean nullsAreSortedLow() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean othersDeletesAreVisible(int type) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean othersInsertsAreVisible(int type) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean othersUpdatesAreVisible(int type) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean ownDeletesAreVisible(int type) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean ownInsertsAreVisible(int type) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean ownUpdatesAreVisible(int type) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean storesLowerCaseIdentifiers() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean storesLowerCaseQuotedIdentifiers() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean storesMixedCaseIdentifiers() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean storesMixedCaseQuotedIdentifiers() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean storesUpperCaseIdentifiers() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean storesUpperCaseQuotedIdentifiers() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsANSI92EntryLevelSQL() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsANSI92FullSQL() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsANSI92IntermediateSQL() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsAlterTableWithAddColumn() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsAlterTableWithDropColumn() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsBatchUpdates() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsCatalogsInDataManipulation() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsCatalogsInIndexDefinitions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsCatalogsInProcedureCalls() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsCatalogsInTableDefinitions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsColumnAliasing() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsConvert() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsConvert(int fromType, int toType) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsCoreSQLGrammar() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsCorrelatedSubqueries() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsDataManipulationTransactionsOnly() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsDifferentTableCorrelationNames() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsExpressionsInOrderBy() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsExtendedSQLGrammar() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsFullOuterJoins() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsGroupBy() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsGroupByBeyondSelect() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsGroupByUnrelated() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsIntegrityEnhancementFacility() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsLikeEscapeClause() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsLimitedOuterJoins() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsMinimumSQLGrammar() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsMixedCaseIdentifiers() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsMultipleOpenResults() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsMultipleResultSets() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsMultipleTransactions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsNamedParameters() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsNonNullableColumns() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsOpenCursorsAcrossCommit() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsOpenCursorsAcrossRollback() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsOpenStatementsAcrossCommit() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsOpenStatementsAcrossRollback() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsOrderByUnrelated() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsOuterJoins() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsPositionedDelete() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsPositionedUpdate() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSavepoints() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSchemasInDataManipulation() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSchemasInIndexDefinitions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSchemasInProcedureCalls() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSchemasInTableDefinitions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSelectForUpdate() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsStatementPooling() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsStoredProcedures() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSubqueriesInComparisons() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSubqueriesInExists() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSubqueriesInIns() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsSubqueriesInQuantifieds() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsTableCorrelationNames() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsTransactionIsolationLevel(int level) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsTransactions() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsUnion() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsUnionAll() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean updatesAreDetected(int type) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean usesLocalFilePerTable() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean usesLocalFiles() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean autoCommitFailureClosesAllResultSets() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getClientInfoProperties() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getFunctionColumns(String arg0, String arg1, String arg2, String arg3) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getFunctions(String arg0, String arg1, String arg2) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSet getSchemas(String arg0, String arg1) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public <T> T unwrap(Class<T> iface) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public RowIdLifetime getRowIdLifetime() throws SQLException
  {
    throw new SQLException("Not implemented: getRowIdLifetime()");
  }

  //***************************************************************************
  //*** private / protected methods
  //***************************************************************************
  private List<String> getDatabases() throws SQLException
  {
    List<String> rows = new ArrayList<String>();
    Statement s = connection.createStatement();
    try {
      ResultSet rs = s.executeQuery("PRAGMA database_list");
      while (rs.next()) {
        if ("temp".equals(rs.getString(2))) continue;
        rows.add(rs.getString(2));
      }
    } finally {
      s.close();
    }
    Collections.sort(rows);
    return rows;
  }

  private static String trace(StackTraceElement e[], int level)
  {
    if (e != null && e.length >= level) {
      StackTraceElement s = e[level];
      if (s != null) {
        return s.getMethodName();
      }
    }
    return null;
  }

}

// ***************************************************************************
// *** ListResultSet
// ***************************************************************************
class ListResultSet implements ResultSet, ResultSetMetaData
{

  private List<Object[]>         records;
  private List<ColumnDef>        columnDefs;
  private Map<String, ColumnDef> columnNameMap;
  private int                    currentRow;
  private boolean                wasNull;

  public ListResultSet(List<ColumnDef> pColumnDefs, List<Object[]> pRecords)
  {
    super();
    records = pRecords;
    currentRow = -1;
    columnDefs = pColumnDefs;
    columnNameMap = new HashMap<String, ColumnDef>();
    for (int i = 0; i < pColumnDefs.size(); i++) {
      columnNameMap.put(columnDefs.get(i).name.toLowerCase(), columnDefs.get(i));
    }
  }

  // ***************************************************************************
  // *** ListResultSet.ResultSet implementation
  // ***************************************************************************
  public void close() throws SQLException
  {
    records = null;
  }

  public boolean next() throws SQLException
  {
    checkIfClosed();
    currentRow = (currentRow + 1 < records.size()) ? currentRow + 1 : -1;
    return (currentRow >= 0 && currentRow < records.size());
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
    Integer result = (Integer) records.get(currentRow)[columnIndex];
    wasNull = (result == null);
    return (result == null) ? 0 : result.intValue();
  }

  public int getInt(String columnName) throws SQLException
  {
    return getInt(findColumn(columnName));
  }

  public long getLong(int columnIndex) throws SQLException
  {
    checkIfClosed();
    Long result = (Long) records.get(currentRow)[columnIndex];
    wasNull = (result == null);
    return (result == null) ? 0 : result.longValue();
  }

  public long getLong(String columnName) throws SQLException
  {
    return getLong(findColumn(columnName));
  }

  public short getShort(int columnIndex) throws SQLException
  {
    Short result = (Short) records.get(currentRow)[columnIndex];
    wasNull = (result == null);
    return (result == null) ? 0 : result.shortValue();
  }

  public short getShort(String columnName) throws SQLException
  {
    return getShort(findColumn(columnName));
  }

  public double getDouble(int columnIndex) throws SQLException
  {
    checkIfClosed();
    Double result = (Double) records.get(currentRow)[columnIndex];
    wasNull = (result == null);
    return (result == null) ? 0 : result.doubleValue();
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
    byte[] result = (byte[]) records.get(currentRow)[columnIndex];
    wasNull = (result == null);
    return result;
  }

  public byte[] getBytes(String columnName) throws SQLException
  {
    return getBytes(findColumn(columnName));
  }

  public String getString(int columnIndex) throws SQLException
  {
    checkIfClosed();
    String result = (String) records.get(currentRow)[columnIndex - 1];
    wasNull = (result == null);
    return result;
  }

  public String getString(String columnName) throws SQLException
  {
    return getString(findColumn(columnName));
  }

  public Date getDate(int columnIndex) throws SQLException
  {
    checkIfClosed();
    Date result = (Date) records.get(currentRow)[columnIndex - 1];
    wasNull = (result == null);
    return result;
  }

  public Date getDate(String columnName) throws SQLException
  {
    return getDate(findColumn(columnName));
  }

  public Date getDate(int columnIndex, Calendar cal) throws SQLException
  {
    checkIfClosed();
    Date result = (Date) records.get(currentRow)[columnIndex - 1];
    wasNull = (result == null);
    if (cal == null) {
      return result;
    } else {
      cal.setTimeInMillis(result.getTime());
      return new Date(cal.getTime().getTime());
    }
  }

  public Date getDate(String columnName, Calendar cal) throws SQLException
  {
    return getDate(findColumn(columnName), cal);
  }

  public Time getTime(int columnIndex) throws SQLException
  {
    checkIfClosed();
    Time result = (Time) records.get(currentRow)[columnIndex - 1];
    wasNull = (result == null);
    return result;
  }

  public Time getTime(String columnName) throws SQLException
  {
    return getTime(findColumn(columnName));
  }

  public Time getTime(int columnIndex, Calendar cal) throws SQLException
  {
    checkIfClosed();
    Time result = (Time) records.get(currentRow)[columnIndex - 1];
    wasNull = (result == null);
    if (cal == null) {
      return result;
    } else {
      cal.setTimeInMillis(result.getTime());
      return new Time(cal.getTime().getTime());
    }
  }

  public Time getTime(String columnName, Calendar cal) throws SQLException
  {
    return getTime(findColumn(columnName), cal);
  }

  public Timestamp getTimestamp(int columnIndex) throws SQLException
  {
    checkIfClosed();
    Timestamp result = (Timestamp) records.get(currentRow)[columnIndex];
    wasNull = (result == null);
    return result;
  }

  public Timestamp getTimestamp(String columnName) throws SQLException
  {
    return getTimestamp(findColumn(columnName));
  }

  public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException
  {
    checkIfClosed();
    Timestamp result = (Timestamp) records.get(currentRow)[columnIndex - 1];
    wasNull = (result == null);
    if (cal == null) {
      return result;
    } else {
      cal.setTimeInMillis(result.getTime());
      return new Timestamp(cal.getTime().getTime());
    }
  }

  public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException
  {
    return getTimestamp(findColumn(columnName), cal);
  }

  public Object getObject(int columnIndex) throws SQLException
  {
    checkIfClosed();
    Object result = records.get(currentRow)[columnIndex - 1];
    wasNull = (result == null);
    return result;
  }

  public Object getObject(String columnName) throws SQLException
  {
    return getObject(findColumn(columnName));
  }

  // ***************************************************************************
  // *** ListResultSet.ResultSetMetaData implementation
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
    return "";
  }

  // ***************************************************************************
  // *** Private/protected methods
  // ***************************************************************************
  private void checkIfClosed() throws SQLException
  {
    if (records == null) throw new SQLException("ResultSet is closed");
  }

  public boolean absolute(int row) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void afterLast() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void beforeFirst() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void cancelRowUpdates() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void clearWarnings() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void deleteRow() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean first() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Array getArray(int i) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Array getArray(String colName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public InputStream getAsciiStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public InputStream getAsciiStream(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public BigDecimal getBigDecimal(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public BigDecimal getBigDecimal(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public InputStream getBinaryStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public InputStream getBinaryStream(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Blob getBlob(int i) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Blob getBlob(String colName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean getBoolean(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean getBoolean(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public byte getByte(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public byte getByte(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Reader getCharacterStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Reader getCharacterStream(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Clob getClob(int i) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Clob getClob(String colName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getConcurrency() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getCursorName() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getFetchDirection() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getFetchSize() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public ResultSetMetaData getMetaData() throws SQLException
  {
    return this;
  }

  public Object getObject(int i, Map<String, Class<?>> map) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Object getObject(String colName, Map<String, Class<?>> map) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Ref getRef(int i) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Ref getRef(String colName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getRow() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Statement getStatement() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getType() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public URL getURL(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public URL getURL(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public InputStream getUnicodeStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public InputStream getUnicodeStream(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public SQLWarning getWarnings() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void insertRow() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isAfterLast() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isBeforeFirst() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isFirst() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isLast() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean last() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void moveToCurrentRow() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void moveToInsertRow() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean previous() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void refreshRow() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean relative(int rows) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean rowDeleted() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean rowInserted() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean rowUpdated() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void setFetchDirection(int direction) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void setFetchSize(int rows) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateArray(int columnIndex, Array x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateArray(String columnName, Array x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBlob(int columnIndex, Blob x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBlob(String columnName, Blob x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBoolean(int columnIndex, boolean x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBoolean(String columnName, boolean x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateByte(int columnIndex, byte x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateByte(String columnName, byte x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBytes(int columnIndex, byte[] x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBytes(String columnName, byte[] x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateClob(int columnIndex, Clob x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateClob(String columnName, Clob x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateDate(int columnIndex, Date x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateDate(String columnName, Date x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateDouble(int columnIndex, double x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateDouble(String columnName, double x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateFloat(int columnIndex, float x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateFloat(String columnName, float x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateInt(int columnIndex, int x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateInt(String columnName, int x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateLong(int columnIndex, long x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateLong(String columnName, long x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNull(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNull(String columnName) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateObject(int columnIndex, Object x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateObject(String columnName, Object x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateObject(int columnIndex, Object x, int scale) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateObject(String columnName, Object x, int scale) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateRef(int columnIndex, Ref x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateRef(String columnName, Ref x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateRow() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateShort(int columnIndex, short x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateShort(String columnName, short x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateString(int columnIndex, String x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateString(String columnName, String x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateTime(int columnIndex, Time x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateTime(String columnName, Time x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateTimestamp(String columnName, Timestamp x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  // ***************************************************************************
  // *** ResultSetMetaData implementation
  // ***************************************************************************

  public String getCatalogName(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getColumnClassName(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getColumnDisplaySize(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getColumnLabel(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getColumnType(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getColumnTypeName(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getPrecision(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getScale(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getSchemaName(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isAutoIncrement(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isCaseSensitive(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isCurrency(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isDefinitelyWritable(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int isNullable(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isReadOnly(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isSearchable(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isSigned(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isWritable(int column) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public int getHoldability() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Reader getNCharacterStream(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public Reader getNCharacterStream(String columnLabel) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getNString(int columnIndex) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public String getNString(String columnLabel) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public boolean isClosed() throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateCharacterStream(int columnIndex, Reader x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateClob(int columnIndex, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateClob(String columnLabel, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateClob(int columnIndex, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateClob(String columnLabel, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNClob(int columnIndex, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNClob(String columnLabel, Reader reader) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNString(int columnIndex, String nString) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNString(String columnLabel, String nString) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }


  public boolean isWrapperFor(Class<?> iface) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public <T> T unwrap(Class<T> iface) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public NClob getNClob(int arg0) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public NClob getNClob(String arg0) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public RowId getRowId(int arg0) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public RowId getRowId(String arg0) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public SQLXML getSQLXML(int arg0) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public SQLXML getSQLXML(String arg0) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNClob(int arg0, NClob arg1) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateNClob(String arg0, NClob arg1) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateRowId(int arg0, RowId arg1) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateRowId(String arg0, RowId arg1) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateSQLXML(int arg0, SQLXML arg1) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  public void updateSQLXML(String arg0, SQLXML arg1) throws SQLException
  {
    throw new SQLException("Not implemented: " + trace(Thread.currentThread().getStackTrace(), 2));
  }

  private static String trace(StackTraceElement e[], int level)
  {
    if (e != null && e.length >= level) {
      StackTraceElement s = e[level];
      if (s != null) {
        return s.getMethodName();
      }
    }
    return null;
  }

}

// ***************************************************************************
class ColumnDef
{
  int    index;
  String name;
  String type;

  public ColumnDef(int index, String name, String type)
  {
    super();
    this.index = index;
    this.name = name;
    this.type = type;
  }

}

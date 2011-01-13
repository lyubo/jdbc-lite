package org.sqlite.jdbc.tests;

import org.sqlite.SQLite;

public class SingleTest
{

  
  public static void main(String[] args)
  {
    //Map options = new HashMap();
    //options.put(Library.OPTION_FUNCTION_MAPPER,new SQLiteFunctionMapper());
    System.out.println(SQLite.libversion());
    //System.out.println(SQLite.api.libversion());
  }

}
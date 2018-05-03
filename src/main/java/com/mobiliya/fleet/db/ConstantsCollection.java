package com.mobiliya.fleet.db;

@SuppressWarnings({"ALL", "unused"})
public class ConstantsCollection {
    public static final String DATABASE_NAME = "telemetry";
    public static final int DATABASE_VERSION = 1;

    public static final String ID = "ID";

    public static final String SQLITE_CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ";
    public static final String SQLITE_SELECT_ALL_FROM = "SELECT  * FROM ";
    public static final String SQLITE_CREATE_TABLE = "CREATE TABLE ";
    public static final String SQLITE_TEXT = " TEXT";
    public static final String SQLITE_INTEGER = "INTEGER";
    public static final String SQLITE_DOUBLE = "DOUBLE";
    public static final String SQLITE_BOOLEAN = "BOOLEAN";
    public static final String SQLITE_INTEGER_PRIMARY_KEY = " INTEGER PRIMARY KEY,";
    public static final String SQLITE_INTEGER_PRIMARY_KEY_AUTOINCREMENT = " INTEGER PRIMARY KEY AUTOINCREMENT ";
    public static final String SQLITE_OPENNING_BRACKET = "(";
    public static final String SQLITE_CLOSING_BRACKET = ")";
    public static final String SQLITE_CLOSING_SEMICOLUMN = ";";
    public static final String SQLITE_DOT = ".";
    public static final String SQLITE_COMMA = ",";
    public static final String SQLITE_SPACE = " ";
    public static final String SQLITE_EQUAL_SIGN = " = ";
    public static final String SQLITE_DELETE_FROM = "DELETE FROM ";
    public static final String SQLITE_WHERE = " WHERE ";

    public static int INDEX_NOT_DEFINED = -1;
    public static int STATEMENT_SUCCESS = 1;
}

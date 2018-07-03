package com.mobiliya.fleet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mobiliya.fleet.db.tables.DB_BASIC;
import com.mobiliya.fleet.utils.DateUtils;
import com.mobiliya.fleet.utils.ReflectionUtils;
import com.mobiliya.fleet.utils.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class DBHandler extends SQLiteOpenHelper {
    //TAG (Class name) for Log tagging
    private static final String LOG_TAG = "DBHandler";

    /**
     * Initialize databaser with its name and version
     *
     * @param context Application context
     */
    public DBHandler(Context context) {
        super(
                context,
                ConstantsCollection.DATABASE_NAME,
                null,
                ConstantsCollection.DATABASE_VERSION
        );
    }

    @Override

    public void onCreate(SQLiteDatabase db) {/*DO NOTHING*/}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    /**
     * Creates table using reflection of given object,
     * each member of given object will be converted to a row in a database table.
     * if used primitive convertible to Sqlite format types.
     *
     * @param object an object on which reflection will be based the table creation
     */
    public void CreateTable(DB_BASIC object) {
        if (object != null) {
            SQLiteDatabase db = null;
            try {
                db = getWritableDatabase();

                Class<? extends DB_BASIC> c = object.getClass();
                String tableName = c.getName();

                tableName = ReflectionUtils.GetClassName(c);

                //Fields of the object
                Field[] fields = c.getFields();

                StringBuilder sbCreateTable = new StringBuilder();

                //Beginning of the CREATE raw query
                sbCreateTable.append(ConstantsCollection.SQLITE_CREATE_TABLE_IF_NOT_EXISTS);
                sbCreateTable.append(tableName);
                sbCreateTable.append(ConstantsCollection.SQLITE_OPENNING_BRACKET);

                String lastFieldName = "";
                for (int i = fields.length - 1; i > 0; i--) {
                    if (!Modifier.isStatic(fields[i].getModifiers())) {
                        lastFieldName = fields[i].getName();
                        break;
                    }
                }

                //Iterates on the given object fields using reflection
                //and creates appropriate column definition
                for (Field field : fields) {
                    String fieldName = field.getName();

                    if (fieldName.startsWith("$") || Modifier.isStatic(field.getModifiers()))
                        continue;

                    if (fieldName.equalsIgnoreCase(ConstantsCollection.ID)) {//Creates an auto increament index named ID
                        sbCreateTable.append(fieldName);
                        sbCreateTable.append(ConstantsCollection.SQLITE_INTEGER_PRIMARY_KEY_AUTOINCREMENT);
                    } else {//Creates column declaration
                        String rowname = GetSqliteType(field.getType());

                        if (rowname != null) {
                            sbCreateTable.append(fieldName);
                            sbCreateTable.append(ConstantsCollection.SQLITE_SPACE);
                            sbCreateTable.append(rowname);
                        }
                    }

                    //if(i != fields.length - 1)
                    if (!Objects.equals(lastFieldName, fieldName)) {//Allways adds , in the end of each column declaration except the last one
                        sbCreateTable.append(ConstantsCollection.SQLITE_COMMA);
                        sbCreateTable.append(ConstantsCollection.SQLITE_SPACE);
                    }
                }

                //Closing raw CREATE Query with }; characters
                sbCreateTable.append(ConstantsCollection.SQLITE_CLOSING_BRACKET);
                sbCreateTable.append(ConstantsCollection.SQLITE_CLOSING_SEMICOLUMN);

                //Executes raw SQlite statement
                db.execSQL(sbCreateTable.toString());
            } catch (SecurityException e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "Exception in CreateTable: " + e.toString());
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "Exception in CreateTable: " + e.toString());
            } finally {
                //Closing the DB connection
                CloseDB(db);
            }
        }
    }

    /**
     * Finds appropriate Sqlite raw string class to given java class
     *
     * @return Sqlite row format
     */
    private String GetSqliteType(Class<?> c) {
        String type = "TEXT";

        if (c.equals(String.class)) {
            type = ConstantsCollection.SQLITE_TEXT;
        } else if (c.equals(Integer.class)
                || c.equals(int.class)
                || c.equals(Long.class)
                || c.equals(long.class)
                || c.equals(Number.class)
                || c.equals(java.util.Date.class)) {
            type = ConstantsCollection.SQLITE_INTEGER;
        } else if (c.equals(Boolean.class)
                || c.equals(boolean.class)) {
            type = ConstantsCollection.SQLITE_BOOLEAN;
        } else if (c.equals(double.class)
                || c.equals(Double.class)
                || c.equals(float.class)
                || c.equals(Float.class)) {
            type = ConstantsCollection.SQLITE_DOUBLE;
        }

        return type;
    }

    /**
     * Adds given object to the database, by its class name. Perform INSERT Sqlite operation
     *
     * @param object to be inserted
     * @return id of the inserted object
     */
    public long AddNewObject(DB_BASIC object) {
        long result = ConstantsCollection.INDEX_NOT_DEFINED;
        if (object != null) {
            SQLiteDatabase db = null;
            try {
                db = this.getWritableDatabase();

                ContentValues values = new ContentValues();
                Class<? extends DB_BASIC> c = object.getClass();
                Field[] fields = c.getFields();

                //Iterates on object's members
                for (Field field : fields) {
                    if (field.getName().startsWith("$") || Modifier.isStatic(field.getModifiers()))
                        continue;

                    Object val = GetValue(field, object);

                    if (val != null) {
                        String rawValue = null;
                        if (field.getType().equals(Date.class)) {
                            try {
                                rawValue = DateUtils.DateToValue((Date) val);
                            } catch (ParseException e) {
                                Log.e(LOG_TAG, e.toString());
                            }
                        } else if (field.getType().equals(Boolean.class) ||
                                field.getType().equals(boolean.class)) {
                            rawValue = ((Boolean) val) ? "1" : "0";
                        } else {
                            rawValue = val.toString();
                        }


                        /*if (c.equals(String.class))
                        {
                            type = ConstantsCollection.SQLITE_TEXT;
                        }
                        else if (  c.equals(Integer.class)
                                || c.equals(int.class)
                                || c.equals(Long.class)
                                || c.equals(long.class)
                                || c.equals(Number.class)
                                || c.equals(java.util.Date.class))
                        {
                            type = ConstantsCollection.SQLITE_INTEGER;
                        }
                        else if(c.equals(Boolean.class)
                                || c.equals(boolean.class))
                        {
                            type = ConstantsCollection.SQLITE_BOOLEAN;
                        }
                        else if(c.equals(double.class)
                                || c.equals(Double.class)
                                || c.equals(float.class)
                                || c.equals(Float.class))
                        {
                            type = ConstantsCollection.SQLITE_DOUBLE;
                        }*/


                        String name = field.getName();

                        values.put(name, rawValue);
                    }
                }

                String tableName = ReflectionUtils.GetClassName(object.getClass());

                if (values.size() > 0) {
                    result = db.insert(tableName, null, values);
                }
            } finally {
                //  CloseDB(db);
            }
        }

        return result;
    }

    public long DeleteTableRow(DB_BASIC object) {
        long result = ConstantsCollection.INDEX_NOT_DEFINED;

        Class<? extends DB_BASIC> c = object.getClass();
        String tableName = ReflectionUtils.GetClassName(c);

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            String[] values = new String[1];
            values[0] = object.ID.toString();
            result = db.delete(tableName, ConstantsCollection.ID, values);
        } finally {
            // CloseDB(db);
        }
        return result;
    }

    /**
     * Delete all data from table
     *
     * @param object
     * @return returns affected rows
     */
    public int DeleteAllTableData(DB_BASIC object) {
        int result = ConstantsCollection.INDEX_NOT_DEFINED;

        Class<? extends DB_BASIC> c = object.getClass();
        String tableName = ReflectionUtils.GetClassName(c);

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            result = db.delete(tableName, null, null);
        } finally {
            //   CloseDB(db);
        }
        return result;
    }

    /**
     * Delete Table row
     *
     * @param clazz
     * @param whereClause
     * @param values
     * @return
     */
    public long DeleteTableRow(Class<? extends DB_BASIC> clazz, String whereClause, String[] values) {
        int result = 0;
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();

            if (db != null && !db.isOpen()) {

                db = this.getWritableDatabase();
            }


            synchronized (db) {
                String tableName = ReflectionUtils.GetClassName(clazz);
                // List<DB_BASIC> db_basics = GetAllTableData(clazz);

                for (String item : values) {
                    String[] items = {item};
                    result += db.delete(tableName, whereClause, items);
                }

                Log.d("*****", "result :: " + result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (db != null && db.isOpen()) {
                //    db.close();
            }

        }


        return result;
    }

    /**
     * Delete Table row
     *
     * @param clazz
     * @param whereClause
     * @param values
     * @return
     */
    public List<DB_BASIC> GetTableRow(Class<? extends DB_BASIC> clazz, String whereClause, String[] values) {
        List<DB_BASIC> result = null;
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();

            if (db != null && !db.isOpen()) {
                db = this.getWritableDatabase();
            }

            synchronized (db) {
                String tableName = ReflectionUtils.GetClassName(clazz);
                Cursor cur = db.query(tableName, null, whereClause, values, null, null, null, null);

                result = ConvertCursorToObjects(cur, clazz);

                Log.d("*****", "result :: " + result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }


        return result;
    }

    /**
     * Delete Query
     *
     * @param query
     * @param params
     * @return
     */

    public long DeleteTableRowByQuery(String query, String[] params) {
        long result = ConstantsCollection.INDEX_NOT_DEFINED;

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();

            db.execSQL(query, params); //db.delete(tableName, ConstantsCollection.ID, params);
            result = ConstantsCollection.STATEMENT_SUCCESS;
        } catch (SQLException se) {
            Log.d("DBHandler", "Sql Exception: " + se);
        } catch (Exception e) {
            Log.d("DBHandler", "Exception: " + e);
        } finally {
            CloseDB(db);
        }
        return result;
    }

    /**
     * Gets the value of the fields in specified object using reflection
     *
     * @return the value of the field
     */
    private Object GetValue(Field field, DB_BASIC object) {
        Object result = null;
        try {
            result = field.get(object);
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
            Log.e(LOG_TAG, e1.toString());
        } catch (IllegalArgumentException e1) {
            e1.printStackTrace();
            Log.e(LOG_TAG, e1.toString());
        }
        return result;
    }

    /**
     * Deletes the table from database
     *
     * @param tblClass table object
     * @return the number of rows affected if a whereClause is passed in, 0 otherwise.
     */
    public int DeleteTable(Class<? extends DB_BASIC> tblClass) {
        int result = -1;
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            String tblName = ReflectionUtils.GetClassName(tblClass);
            result = db.delete(tblName, null, null);
        } finally {
            // CloseDB(db);
        }

        return result;
    }

    /**
     * Converts cursor to List of objects
     *
     * @param cursor database cursor
     * @param clazz  the desired clazz
     * @return converted cursor object to List collection
     **/
    @SuppressWarnings("unchecked")
    private <T> List<T> ConvertCursorToObjects(Cursor cursor, Class<? extends DB_BASIC> clazz) {
        List<T> list = new ArrayList<>();

        //moves the cursor to the first row
        if (cursor.moveToFirst()) {
            String[] ColumnNames = cursor.getColumnNames();
            do {
                Object obj = ReflectionUtils.GetInstance(clazz);

                //iterates on column names
                for (int i = 0; i < ColumnNames.length; i++) {
                    try {

                        Field field = obj.getClass().getField(ColumnNames[i]);
                        Object objectValue = null;
                        String str = cursor.getString(i);

                        if (str != null) {
                            //Converting stored Sqlite data to java objects
                            if (field.getType().equals(java.util.Date.class)) {
                                Date date = DateUtils.ValueToDate(str);
                                objectValue = date;
                                field.set(obj, objectValue);
                            } else if (field.getType().equals(Number.class)) {
                                objectValue = NumberFormat.getInstance().parse(str);
                            } else if (field.getType().equals(Long.class) ||
                                    field.getType().equals(long.class)) {
                                objectValue = NumberFormat.getInstance().parse(str);
                                long value = Long.parseLong(objectValue.toString());
                                field.set(obj, value);
                            } else if (field.getType().equals(Integer.class) ||
                                    field.getType().equals(int.class)) {
                                objectValue = NumberFormat.getInstance().parse(str);
                                int value = Integer.parseInt(str);
                                field.set(obj, value);
                            } else if (field.getType().equals(Double.class) ||
                                    field.getType().equals(double.class)) {
                                objectValue = NumberFormat.getInstance().parse(str);
                                double value = Double.parseDouble(objectValue.toString());
                                field.set(obj, value);
                            } else if (field.getType().equals(float.class) ||
                                    field.getType().equals(Float.class)) {
                                objectValue = NumberFormat.getInstance().parse(str);
                                float value = Float.parseFloat(objectValue.toString());
                                field.set(obj, value);
                            } else if (field.getType().equals(Boolean.class) ||
                                    field.getType().equals(boolean.class)) {
                                boolean value = Integer.parseInt(str) == 1;
                                field.set(obj, value);
                            } else {
                                objectValue = str;
                                field.set(obj, objectValue);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "Exception in ConvertCursorToObjects IllegalArgumentException: " + e.toString());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "Exception in ConvertCursorToObjects IllegalAccessException: " + e.toString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "Exception in ConvertCursorToObjects ParseException: " + e.toString());
                    } catch (SecurityException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "Exception in ConvertCursorToObjects SecurityException: " + e.toString());
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "Exception in ConvertCursorToObjects NoSuchFieldException: " + e.toString());
                    }
                }

                if (obj instanceof DB_BASIC) {
                    list.add((T) obj);
                }
            } while (cursor.moveToNext());
        }
        return list;
    }

    /**
     * Gets all data from specified table by class instance
     *
     * @return null if no objects are located, List<DB_BASIC> there is records
     */
    public List<DB_BASIC> GetAllTableData(Class<? extends DB_BASIC> clazz) {
        List<DB_BASIC> list;
        String tableName = ReflectionUtils.GetClassName(clazz);

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            String[] columns = null;

            Cursor cursor = db.query(tableName, columns, null, null, null, null, null);

            list = ConvertCursorToObjects(cursor, clazz);
        } finally {
            // CloseDB(db);
        }
        return list;
    }

    /**
     * Gets data from specified table by class instance
     *
     * @param queryString: SQL Query
     * @param params:      Dependent {arameters}
     * @return null if no objects are located, List<DB_BASIC> there is records
     */
    public List<DB_BASIC> GetTableDataByQuery(Class<? extends DB_BASIC> clazz, String queryString, String[] params) {
        List<DB_BASIC> list;
        String tableName = ReflectionUtils.GetClassName(clazz);

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            String[] columns = null;

            Cursor cursor = db.rawQuery(queryString, params);

            list = ConvertCursorToObjects(cursor, clazz);
        } finally {
            //CloseDB(db);
        }
        return list;
    }

    public int UpdateRow(Class<? extends DB_BASIC> clazz, ContentValues values, String whereClause) {
        int result = ConstantsCollection.INDEX_NOT_DEFINED;

        try {
            SQLiteDatabase db = null;

            db = this.getWritableDatabase();

            String tableName = ReflectionUtils.GetClassName(clazz);
            result = db.update(tableName, values, whereClause, null);

        } catch (Exception e) {

            e.printStackTrace();
        } finally {
        }


        return result;
    }

    /**
     * Updates row in relevant table, specified by class instance
     *
     * @return number of updated rows or -1 on fail
     */
    public int UpdateRow(DB_BASIC object) {
        int result = ConstantsCollection.INDEX_NOT_DEFINED;

        if (object != null && object.ID != null) {
            SQLiteDatabase db = null;

            db = this.getWritableDatabase();
            String tableName = ReflectionUtils.GetClassName(object.getClass());

            String sbWhereClause = ConstantsCollection.SQLITE_SPACE +
                    ConstantsCollection.ID +
                    ConstantsCollection.SQLITE_EQUAL_SIGN +
                    String.valueOf(object.ID);

            ContentValues values = new ContentValues();

            //iterates on fields
            for (Field f : object.getClass().getFields()) {
                if (f.getName().startsWith("$") || Modifier.isStatic(f.getModifiers())) continue;

                String fieldValue = GetStringValue(f, object);
                String name = f.getName();

                if (fieldValue != null
                        && !fieldValue.equals(StringUtils.EMPTY_STRING)
                        && !name.equals(ConstantsCollection.ID)) {
                    values.put(name, fieldValue);
                }
            }
            result = db.update(tableName, values, sbWhereClause, null);
        }

        return result;
    }

    /**
     * @return String value of the object
     */
    private String GetStringValue(Field field, DB_BASIC object) {
        Class<?> type = field.getType();
        String result = null;

        Object value = GetValue(field, object);

        if (value != null) {
            if (type.equals(Date.class)) {
                try {
                    result = DateUtils.DateToValue((Date) value);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                result = value.toString();
            }
        }

        return result;
    }

    /**
     * Closes database connection
     *
     * @param db database reference
     */
    private void CloseDB(SQLiteDatabase db) {
        try {
            if (db != null) {
                db.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.toString());
        }

    }
}

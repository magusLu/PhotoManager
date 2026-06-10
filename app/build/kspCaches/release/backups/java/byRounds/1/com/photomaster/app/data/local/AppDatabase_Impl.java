package com.photomaster.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile CustomFolderDao _customFolderDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `folder_media_mappings` (`folderId` INTEGER NOT NULL, `mediaId` INTEGER NOT NULL, `isCopy` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`folderId`, `mediaId`), FOREIGN KEY(`folderId`) REFERENCES `custom_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folder_media_mappings_folderId` ON `folder_media_mappings` (`folderId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folder_media_mappings_mediaId` ON `folder_media_mappings` (`mediaId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '56773bbf328db31c9e9c6462be0265e5')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `custom_folders`");
        db.execSQL("DROP TABLE IF EXISTS `folder_media_mappings`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCustomFolders = new HashMap<String, TableInfo.Column>(3);
        _columnsCustomFolders.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomFolders.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomFolders.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomFolders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomFolders = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCustomFolders = new TableInfo("custom_folders", _columnsCustomFolders, _foreignKeysCustomFolders, _indicesCustomFolders);
        final TableInfo _existingCustomFolders = TableInfo.read(db, "custom_folders");
        if (!_infoCustomFolders.equals(_existingCustomFolders)) {
          return new RoomOpenHelper.ValidationResult(false, "custom_folders(com.photomaster.app.data.local.entity.CustomFolderEntity).\n"
                  + " Expected:\n" + _infoCustomFolders + "\n"
                  + " Found:\n" + _existingCustomFolders);
        }
        final HashMap<String, TableInfo.Column> _columnsFolderMediaMappings = new HashMap<String, TableInfo.Column>(4);
        _columnsFolderMediaMappings.put("folderId", new TableInfo.Column("folderId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolderMediaMappings.put("mediaId", new TableInfo.Column("mediaId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolderMediaMappings.put("isCopy", new TableInfo.Column("isCopy", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolderMediaMappings.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFolderMediaMappings = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysFolderMediaMappings.add(new TableInfo.ForeignKey("custom_folders", "CASCADE", "NO ACTION", Arrays.asList("folderId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesFolderMediaMappings = new HashSet<TableInfo.Index>(2);
        _indicesFolderMediaMappings.add(new TableInfo.Index("index_folder_media_mappings_folderId", false, Arrays.asList("folderId"), Arrays.asList("ASC")));
        _indicesFolderMediaMappings.add(new TableInfo.Index("index_folder_media_mappings_mediaId", false, Arrays.asList("mediaId"), Arrays.asList("ASC")));
        final TableInfo _infoFolderMediaMappings = new TableInfo("folder_media_mappings", _columnsFolderMediaMappings, _foreignKeysFolderMediaMappings, _indicesFolderMediaMappings);
        final TableInfo _existingFolderMediaMappings = TableInfo.read(db, "folder_media_mappings");
        if (!_infoFolderMediaMappings.equals(_existingFolderMediaMappings)) {
          return new RoomOpenHelper.ValidationResult(false, "folder_media_mappings(com.photomaster.app.data.local.entity.FolderMediaMappingEntity).\n"
                  + " Expected:\n" + _infoFolderMediaMappings + "\n"
                  + " Found:\n" + _existingFolderMediaMappings);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "56773bbf328db31c9e9c6462be0265e5", "da6b5bdf8b74749b938b4006e00ee30b");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "custom_folders","folder_media_mappings");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `custom_folders`");
      _db.execSQL("DELETE FROM `folder_media_mappings`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(CustomFolderDao.class, CustomFolderDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public CustomFolderDao customFolderDao() {
    if (_customFolderDao != null) {
      return _customFolderDao;
    } else {
      synchronized(this) {
        if(_customFolderDao == null) {
          _customFolderDao = new CustomFolderDao_Impl(this);
        }
        return _customFolderDao;
      }
    }
  }
}

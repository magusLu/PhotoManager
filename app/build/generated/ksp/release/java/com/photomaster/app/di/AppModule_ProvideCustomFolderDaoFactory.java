package com.photomaster.app.di;

import com.photomaster.app.data.local.AppDatabase;
import com.photomaster.app.data.local.CustomFolderDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AppModule_ProvideCustomFolderDaoFactory implements Factory<CustomFolderDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideCustomFolderDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CustomFolderDao get() {
    return provideCustomFolderDao(dbProvider.get());
  }

  public static AppModule_ProvideCustomFolderDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideCustomFolderDaoFactory(dbProvider);
  }

  public static CustomFolderDao provideCustomFolderDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCustomFolderDao(db));
  }
}

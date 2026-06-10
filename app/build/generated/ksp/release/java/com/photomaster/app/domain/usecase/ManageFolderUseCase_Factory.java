package com.photomaster.app.domain.usecase;

import com.photomaster.app.data.MediaStoreRepository;
import com.photomaster.app.data.local.CustomFolderDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ManageFolderUseCase_Factory implements Factory<ManageFolderUseCase> {
  private final Provider<MediaStoreRepository> mediaStoreRepositoryProvider;

  private final Provider<CustomFolderDao> customFolderDaoProvider;

  public ManageFolderUseCase_Factory(Provider<MediaStoreRepository> mediaStoreRepositoryProvider,
      Provider<CustomFolderDao> customFolderDaoProvider) {
    this.mediaStoreRepositoryProvider = mediaStoreRepositoryProvider;
    this.customFolderDaoProvider = customFolderDaoProvider;
  }

  @Override
  public ManageFolderUseCase get() {
    return newInstance(mediaStoreRepositoryProvider.get(), customFolderDaoProvider.get());
  }

  public static ManageFolderUseCase_Factory create(
      Provider<MediaStoreRepository> mediaStoreRepositoryProvider,
      Provider<CustomFolderDao> customFolderDaoProvider) {
    return new ManageFolderUseCase_Factory(mediaStoreRepositoryProvider, customFolderDaoProvider);
  }

  public static ManageFolderUseCase newInstance(MediaStoreRepository mediaStoreRepository,
      CustomFolderDao customFolderDao) {
    return new ManageFolderUseCase(mediaStoreRepository, customFolderDao);
  }
}

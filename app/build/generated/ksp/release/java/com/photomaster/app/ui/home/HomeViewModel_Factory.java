package com.photomaster.app.ui.home;

import com.photomaster.app.domain.usecase.ClassifyMediaUseCase;
import com.photomaster.app.domain.usecase.ManageFolderUseCase;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<ClassifyMediaUseCase> classifyMediaUseCaseProvider;

  private final Provider<ManageFolderUseCase> manageFolderUseCaseProvider;

  public HomeViewModel_Factory(Provider<ClassifyMediaUseCase> classifyMediaUseCaseProvider,
      Provider<ManageFolderUseCase> manageFolderUseCaseProvider) {
    this.classifyMediaUseCaseProvider = classifyMediaUseCaseProvider;
    this.manageFolderUseCaseProvider = manageFolderUseCaseProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(classifyMediaUseCaseProvider.get(), manageFolderUseCaseProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<ClassifyMediaUseCase> classifyMediaUseCaseProvider,
      Provider<ManageFolderUseCase> manageFolderUseCaseProvider) {
    return new HomeViewModel_Factory(classifyMediaUseCaseProvider, manageFolderUseCaseProvider);
  }

  public static HomeViewModel newInstance(ClassifyMediaUseCase classifyMediaUseCase,
      ManageFolderUseCase manageFolderUseCase) {
    return new HomeViewModel(classifyMediaUseCase, manageFolderUseCase);
  }
}

package com.photomaster.app.ui.folder;

import androidx.lifecycle.SavedStateHandle;
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
public final class FolderDetailViewModel_Factory implements Factory<FolderDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ClassifyMediaUseCase> classifyMediaUseCaseProvider;

  private final Provider<ManageFolderUseCase> manageFolderUseCaseProvider;

  public FolderDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ClassifyMediaUseCase> classifyMediaUseCaseProvider,
      Provider<ManageFolderUseCase> manageFolderUseCaseProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.classifyMediaUseCaseProvider = classifyMediaUseCaseProvider;
    this.manageFolderUseCaseProvider = manageFolderUseCaseProvider;
  }

  @Override
  public FolderDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), classifyMediaUseCaseProvider.get(), manageFolderUseCaseProvider.get());
  }

  public static FolderDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ClassifyMediaUseCase> classifyMediaUseCaseProvider,
      Provider<ManageFolderUseCase> manageFolderUseCaseProvider) {
    return new FolderDetailViewModel_Factory(savedStateHandleProvider, classifyMediaUseCaseProvider, manageFolderUseCaseProvider);
  }

  public static FolderDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      ClassifyMediaUseCase classifyMediaUseCase, ManageFolderUseCase manageFolderUseCase) {
    return new FolderDetailViewModel(savedStateHandle, classifyMediaUseCase, manageFolderUseCase);
  }
}

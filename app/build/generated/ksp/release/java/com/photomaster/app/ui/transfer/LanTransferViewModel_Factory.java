package com.photomaster.app.ui.transfer;

import android.content.Context;
import com.photomaster.app.domain.usecase.ClassifyMediaUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class LanTransferViewModel_Factory implements Factory<LanTransferViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<ClassifyMediaUseCase> classifyMediaUseCaseProvider;

  public LanTransferViewModel_Factory(Provider<Context> contextProvider,
      Provider<ClassifyMediaUseCase> classifyMediaUseCaseProvider) {
    this.contextProvider = contextProvider;
    this.classifyMediaUseCaseProvider = classifyMediaUseCaseProvider;
  }

  @Override
  public LanTransferViewModel get() {
    return newInstance(contextProvider.get(), classifyMediaUseCaseProvider.get());
  }

  public static LanTransferViewModel_Factory create(Provider<Context> contextProvider,
      Provider<ClassifyMediaUseCase> classifyMediaUseCaseProvider) {
    return new LanTransferViewModel_Factory(contextProvider, classifyMediaUseCaseProvider);
  }

  public static LanTransferViewModel newInstance(Context context,
      ClassifyMediaUseCase classifyMediaUseCase) {
    return new LanTransferViewModel(context, classifyMediaUseCase);
  }
}

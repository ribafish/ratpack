/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.core.server.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBufAllocator;
import ratpack.config.ConfigObject;
import ratpack.core.error.ClientErrorHandler;
import ratpack.core.error.ServerErrorHandler;
import ratpack.core.error.internal.DefaultDevelopmentErrorHandler;
import ratpack.core.error.internal.DefaultProductionErrorHandler;
import ratpack.core.error.internal.ErrorHandler;
import ratpack.exec.ExecController;
import ratpack.exec.ExecInitializer;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.internal.ExecControllerInternal;
import ratpack.config.FileSystemBinding;
import ratpack.core.file.MimeTypes;
import ratpack.core.file.internal.ActivationBackedMimeTypes;
import ratpack.core.file.internal.FileRenderer;
import ratpack.core.form.internal.FormParser;
import ratpack.func.Function;
import ratpack.core.handling.Redirector;
import ratpack.core.handling.RequestId;
import ratpack.core.handling.internal.UuidBasedRequestIdGenerator;
import ratpack.core.health.internal.HealthCheckResultsRenderer;
import ratpack.core.http.client.HttpClient;
import ratpack.core.impose.Impositions;
import ratpack.core.jackson.internal.JsonParser;
import ratpack.core.jackson.internal.JsonRenderer;
import ratpack.exec.registry.Registry;
import ratpack.exec.registry.RegistryBuilder;
import ratpack.core.render.internal.*;
import ratpack.core.server.*;
import ratpack.core.sse.client.ServerSentEventClient;

import java.time.Clock;
import java.util.Optional;

import static ratpack.func.Exceptions.uncheck;

@SuppressWarnings("deprecation")
public abstract class ServerRegistry {

  public static Registry serverRegistry(RatpackServer ratpackServer, Impositions impositions, ExecControllerInternal execController, ServerConfig serverConfig, Function<? super Registry, ? extends Registry> userRegistryFactory) {
    Registry baseRegistry = buildBaseRegistry(ratpackServer, impositions, execController, serverConfig);
    Registry userRegistry = buildUserRegistry(userRegistryFactory, baseRegistry);

    execController.setInterceptors(ImmutableList.copyOf(userRegistry.getAll(ExecInterceptor.class)));
    execController.setInitializers(ImmutableList.copyOf(userRegistry.getAll(ExecInitializer.class)));

    return baseRegistry.join(userRegistry);
  }

  private static Registry buildUserRegistry(Function<? super Registry, ? extends Registry> userRegistryFactory, Registry baseRegistry) {
    Registry userRegistry;
    try {
      userRegistry = userRegistryFactory.apply(baseRegistry);
    } catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new StartupFailureException("Failed to build user registry", e);
    }
    return userRegistry;
  }

  public static Registry buildBaseRegistry(RatpackServer ratpackServer, Impositions impositions, ExecController execController, ServerConfig serverConfig) {
    ErrorHandler errorHandler = serverConfig.isDevelopment() ? new DefaultDevelopmentErrorHandler() : new DefaultProductionErrorHandler();

    RegistryBuilder baseRegistryBuilder;
    try {

      HttpClient httpClient = HttpClient.of(s -> s
        .poolSize(0)
        .byteBufAllocator(ByteBufAllocator.DEFAULT)
        .maxContentLength(serverConfig.getMaxContentLength())
        .execController(execController)
      );

      baseRegistryBuilder = Registry.builder()
        .add(ServerConfig.class, serverConfig)
        .add(Impositions.class, impositions)
        .add(ByteBufAllocator.class, ByteBufAllocator.DEFAULT)
        .add(ExecController.class, execController)
        .add(MimeTypes.class, ActivationBackedMimeTypes.INSTANCE)
        .add(PublicAddress.class, Optional.ofNullable(serverConfig.getPublicAddress())
          .map(PublicAddress::of)
          .orElseGet(() -> PublicAddress.bindAddress(ratpackServer))
        )
        .add(Redirector.TYPE, Redirector.standard())
        .add(ClientErrorHandler.class, errorHandler)
        .add(ServerErrorHandler.class, errorHandler)
        .add(FileRenderer.TYPE, serverConfig.isDevelopment() ? FileRenderer.NON_CACHING : FileRenderer.CACHING)
        .add(PromiseRenderer.TYPE, PromiseRenderer.INSTANCE)
        .add(PublisherRenderer.TYPE, PublisherRenderer.INSTANCE)
        .add(OptionalRenderer.TYPE, OptionalRenderer.INSTANCE)
        .add(RenderableRenderer.TYPE, RenderableRenderer.INSTANCE)
        .add(CharSequenceRenderer.TYPE, CharSequenceRenderer.INSTANCE)
        .add(JsonRenderer.TYPE, JsonRenderer.INSTANCE)
        .add(FormParser.TYPE, FormParser.INSTANCE)
        .add(JsonParser.TYPE, JsonParser.INSTANCE)
        .add(Clock.class, Clock.systemDefaultZone())
        .add(RatpackServer.class, ratpackServer)
        .add(ObjectMapper.class, new ObjectMapper())
        // TODO remove Stopper, and just use RatpackServer instead (will need to update perf and gradle tests)
        .add(Stopper.class, () -> uncheck(() -> {
          ratpackServer.stop();
          return null;
        }))
        .add(HttpClient.class, httpClient)
        .add(ServerSentEventClient.class, ServerSentEventClient.of(httpClient))
        .add(HealthCheckResultsRenderer.TYPE, new HealthCheckResultsRenderer(ByteBufAllocator.DEFAULT))
        .add(RequestId.Generator.class, UuidBasedRequestIdGenerator.INSTANCE);

      addConfigObjects(serverConfig, baseRegistryBuilder);
    } catch (Exception e) {
      // Uncheck because it really shouldn't happen
      throw uncheck(e);
    }

    if (serverConfig.isHasBaseDir()) {
      baseRegistryBuilder.add(FileSystemBinding.class, serverConfig.getBaseDir());
    }

    return baseRegistryBuilder.build();
  }

  private static void addConfigObjects(ServerConfig serverConfig, RegistryBuilder baseRegistryBuilder) {
    for (ConfigObject<?> configObject : serverConfig.getRequiredConfig()) {
      addConfigObject(baseRegistryBuilder, configObject);
    }
  }

  private static <T> void addConfigObject(RegistryBuilder baseRegistryBuilder, ConfigObject<T> configObject) {
    baseRegistryBuilder.add(configObject.getTypeToken(), configObject.getObject());
  }
}

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

package ratpack.core.http.client.internal;

import io.netty.handler.ssl.SslContext;
import ratpack.core.http.HttpMethod;
import ratpack.core.http.MutableHeaders;
import ratpack.core.http.client.ReceivedResponse;
import ratpack.core.http.client.RequestSpec;
import ratpack.func.Action;
import ratpack.func.Function;

import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.time.Duration;

public class DelegatingRequestSpec implements RequestSpec {

  private final RequestSpec delegate;

  public DelegatingRequestSpec(RequestSpec delegate) {
    this.delegate = delegate;
  }

  protected RequestSpec getDelegate() {
    return delegate;
  }

  @Override
  public RequestSpec redirects(int maxRedirects) {
    delegate.redirects(maxRedirects);
    return this;
  }

  @Override
  public int getRedirects() {
    return delegate.getRedirects();
  }

  @Override
  public RequestSpec onRedirect(Function<? super ReceivedResponse, Action<? super RequestSpec>> function) {
    delegate.onRedirect(function);
    return this;
  }

  @Override
  public SslContext getSslContext() {
    return delegate.getSslContext();
  }

  @Override
  public RequestSpec sslContext(SslContext sslContext) {
    delegate.sslContext(sslContext);
    return this;
  }

  @Override
  public Action<? super SSLParameters> getSslParams() {
    return delegate.getSslParams();
  }

  @Override
  public RequestSpec sslParams(Action<? super SSLParameters> sslParams) {
    return delegate.sslParams(sslParams);
  }

  @Override
  public MutableHeaders getHeaders() {
    return delegate.getHeaders();
  }

  @Override
  public RequestSpec headers(Action<? super MutableHeaders> action) throws Exception {
    delegate.headers(action);
    return this;
  }

  @Override
  public RequestSpec method(HttpMethod method) {
    delegate.method(method);
    return this;
  }

  @Override
  public HttpMethod getMethod() {
    return delegate.getMethod();
  }

  @Override
  public RequestSpec decompressResponse(boolean shouldDecompress) {
    delegate.decompressResponse(shouldDecompress);
    return this;
  }

  @Override
  public boolean getDecompressResponse() {
    return delegate.getDecompressResponse();
  }

  @Override
  public URI getUri() {
    return delegate.getUri();
  }

  @Override
  public RequestSpec connectTimeout(Duration duration) {
    delegate.connectTimeout(duration);
    return this;
  }

  @Override
  public Duration getConnectTimeout() {
    return delegate.getConnectTimeout();
  }

  @Override
  public RequestSpec readTimeout(Duration duration) {
    delegate.readTimeout(duration);
    return this;
  }

  @Override
  public Duration getReadTimeout() {
    return delegate.getReadTimeout();
  }

  @Override
  public Body getBody() {
    return delegate.getBody();
  }

  @Override
  public RequestSpec body(Action<? super Body> action) throws Exception {
    delegate.body(action);
    return this;
  }

  @Override
  public RequestSpec basicAuth(String username, String password) {
    delegate.basicAuth(username, password);
    return this;
  }

  @Override
  public RequestSpec maxContentLength(int numBytes) {
    return delegate.maxContentLength(numBytes);
  }

  @Override
  public RequestSpec responseMaxChunkSize(int numBytes) {
    delegate.responseMaxChunkSize(numBytes);
    return this;
  }

  @Override
  public int getMaxContentLength() {
    return delegate.getMaxContentLength();
  }

}

/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import retrofit.converter.Converter;

public final class Response<T> {
  private final com.squareup.okhttp.Response rawResponse;
  private final T body;
  private final ResponseBody errorBody;
  private final Converter converter;

  Response(com.squareup.okhttp.Response rawResponse, T body, ResponseBody errorBody,
      Converter converter) {
    this.rawResponse = rawResponse;
    this.body = body;
    this.errorBody = errorBody;
    this.converter = converter;
  }

  /** The raw response from the HTTP client. */
  public com.squareup.okhttp.Response raw() {
    return rawResponse;
  }

  /** HTTP status code. */
  public int code() {
    return rawResponse.code();
  }

  public Headers headers() {
    return rawResponse.headers();
  }

  /** {@code true} if {@link #code()} is in the range [200..300). */
  public boolean isSuccess() {
    return rawResponse.isSuccessful();
  }

  /** The deserialized response body of a {@linkplain #isSuccess() successful} response. */
  public T body() {
    return body;
  }

  /** The raw response body of an {@linkplain #isSuccess() unsuccessful} response. */
  public ResponseBody errorBody() {
    return errorBody;
  }

  /**
   * The deserialize the response body of an {@linkplain #isSuccess() unsuccessful} response to
   * {@code E}.
   */
  @SuppressWarnings("unchecked")
  public <E> E errorBodyAs(Class<E> errorClass) {
    try {
      return (E) converter.fromBody(errorBody, errorClass);
    } catch (IOException e) {
      throw new AssertionError(e); // Body is buffered.
    }
  }
}

/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.tsunami.common.net.http;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.function.Function;

/**
 * HTTP Status Codes defined in RFC 2616, RFC 6585, RFC 4918 and RFC 7538.
 *
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html"
 *     target="_top">http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html</a>
 * @see <a href="http://tools.ietf.org/html/rfc6585"
 *     target="_top">http://tools.ietf.org/html/rfc6585</a>
 * @see <a href="https://tools.ietf.org/html/rfc4918"
 *     target="_top">https://tools.ietf.org/html/rfc4918</a>
 * @see <a href="https://tools.ietf.org/html/rfc7538"
 *     target="_top">https://tools.ietf.org/html/rfc7538</a>
 */
public enum HttpStatus {
  // Default
  HTTP_STATUS_UNSPECIFIED(0, "Status Unspecified"),

  // Informational 1xx
  CONTINUE(100, "Continue"),
  SWITCHING_PROTOCOLS(101, "Switching Protocols"),

  // Successful 2xx
  OK(200, "Ok"),
  CREATED(201, "Created"),
  ACCEPTED(202, "Accepted"),
  NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
  NO_CONTENT(204, "No Content"),
  RESET_CONTENT(205, "Reset Content"),
  PARTIAL_CONTENT(206, "Partial Content"),
  MULTI_STATUS(207, "Multi-Status"),

  // Redirection 3xx
  MULTIPLE_CHOICES(300, "Multiple Choices"),
  MOVED_PERMANENTLY(301, "Moved Permanently"),
  FOUND(302, "Found"),
  SEE_OTHER(303, "See Other"),
  NOT_MODIFIED(304, "Not Modified"),
  USE_PROXY(305, "Use Proxy"),
  TEMPORARY_REDIRECT(307, "Temporary Redirect"),
  PERMANENT_REDIRECT(308, "Permanent Redirect"),

  // Client Error 4xx
  BAD_REQUEST(400, "Bad Request"),
  UNAUTHORIZED(401, "Unauthorized"),
  PAYMENT_REQUIRED(402, "Payment Required"),
  FORBIDDEN(403, "Forbidden"),
  NOT_FOUND(404, "Not Found"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
  NOT_ACCEPTABLE(406, "Not Acceptable"),
  PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
  REQUEST_TIMEOUT(408, "Request Timeout"),
  CONFLICT(409, "Conflict"),
  GONE(410, "Gone"),
  LENGTH_REQUIRED(411, "Length Required"),
  PRECONDITION_FAILED(412, "Precondition Failed"),
  REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
  REQUEST_URI_TOO_LONG(414, "Request Uri Too Long"),
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
  REQUEST_RANGE_NOT_SATISFIABLE(416, "Request Range Not Satisfiable"),
  EXPECTATION_FAILED(417, "Expectation Failed"),
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
  LOCKED(423, "Locked"),
  FAILED_DEPENDENCY(424, "Failed Dependency"),
  PRECONDITION_REQUIRED(428, "Precondition Required"),
  TOO_MANY_REQUESTS(429, "Too Many Requests"),
  REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),

  // Server Error 5xx
  INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
  NOT_IMPLEMENTED(501, "Not Implemented"),
  BAD_GATEWAY(502, "Bad Gateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable"),
  GATEWAY_TIMEOUT(504, "Gateway Timeout"),
  HTTP_VERSION_NOT_SUPPORTED(505, "Http Version Not Supported"),
  INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
  NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required"),

  /*
   * IE returns this code for 204 due to its use of URLMon, which returns this
   * code for 'Operation Aborted'. The status text is 'Unknown', the response
   * headers are ''. Known to occur on IE 6 on XP through IE9 on Win7.
   */
  QUIRK_IE_NO_CONTENT(1223, "Quirk IE No Content");

  /** Status indexed by code. */
  private static final ImmutableMap<Integer, HttpStatus> BY_CODE =
      Arrays.stream(HttpStatus.values())
          .collect(toImmutableMap(HttpStatus::code, Function.identity()));

  /**
   * Creates the {@link HttpStatus} from the given status code, or null if there is no known status
   * with that code.
   *
   * @param code the HTTP status code.
   * @return the matching {@link HttpStatus} from the given status code.
   */
  public static HttpStatus fromCode(int code) {
    HttpStatus status = BY_CODE.get(code);
    return status == null ? HTTP_STATUS_UNSPECIFIED : status;
  }

  private final int code;
  private final String name;

  HttpStatus(int code, String name) {
    this.code = code;
    this.name = name;
  }

  public int code() {
    return code;
  }

  public boolean isRedirect() {
    switch (this) {
      case MULTIPLE_CHOICES:
      case MOVED_PERMANENTLY:
      case FOUND:
      case SEE_OTHER:
      case TEMPORARY_REDIRECT:
      case PERMANENT_REDIRECT:
        return true;
      default:
        return false;
    }
  }

  public boolean isSuccess() {
    return code >= 200 && code < 300;
  }

  @Override
  public String toString() {
    return name;
  }
}

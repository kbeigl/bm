package bm.traccar.api;

//	unchecked Ex > usage error
//  ApiException is a RuntimeException: The caller is not forced by the compiler to catch
// ApiException.
//	However, good practice dictates they should if they want to handle it.
public class ApiException extends RuntimeException {

  // checked Ex > situational error
  // > we can reasonably expect the caller of our method to be able to recover.
  // public class ApiException extends Exception {

  private static final long serialVersionUID = -191972923538140025L;

  public static final String
      APIEX_NO_CONNECTION = "Cannot connect to server!", // ResourceAccessException
      APIEX_CLIENT_RESPONSE = "Exception with REST client!"; // RestClientResponseException

  public ApiException(String message) {
    super(message);
  }

  public ApiException(String message, Throwable cause) {
    super(message, cause);
  }
}

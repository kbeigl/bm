package bm.traccar.api;

public class ApiHelper {

  /**
   * Due to inconsistent use of Integer and Long in the generated DTOs we need this helper to narrow
   * Long to Integer with range check.
   *
   * <p>Maybe create Api Aspect to automatically check/cast all Long params to Integer as needed?
   */
  public static Integer toInt(Long longObj) {
    if (longObj == null) return null;

    Integer integer;
    try {
      // if (longObj < Integer.MIN_VALUE || longObj > Integer.MAX_VALUE)
      integer = Math.toIntExact(longObj); // assert range implicitely
    } catch (ArithmeticException e) {
      throw new ApiException(
          "IllegalArgument: Value out of range for Integer: "
              + longObj
              + "\nDue to inconsistencies between model and method generation Long objcts "
              + "with Integer value ranges have to be used for API invocations.",
          e);
    }
    return integer;
  }
}

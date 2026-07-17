package io.github.trethore.graphene.internal.cef;

final class CefTestProxies {
  private CefTestProxies() {}

  static Object objectMethod(Object proxy, String methodName, Object[] arguments) {
    return switch (methodName) {
      case "equals" -> proxy == arguments[0];
      case "hashCode" -> System.identityHashCode(proxy);
      case "toString" -> proxy.getClass().getSimpleName();
      default -> throw new IllegalArgumentException(methodName);
    };
  }

  static Object defaultValue(Class<?> type) {
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    if (type == double.class) {
      return 0.0;
    }
    return null;
  }
}

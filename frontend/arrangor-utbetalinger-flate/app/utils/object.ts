export const flipObject = <T extends Record<string, PropertyKey>>(obj: T) => {
  return Object.fromEntries(Object.entries(obj).map(([key, value]) => [value, key])) as Record<
    T[keyof T] & PropertyKey,
    keyof T
  >;
};

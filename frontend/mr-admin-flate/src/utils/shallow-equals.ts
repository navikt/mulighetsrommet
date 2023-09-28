export function shallowEquals(a: any, b: any): boolean {
  // Check if both values are strictly equal (e.g., primitives)
  if (a === b) {
    return true;
  }

  if (typeof a === "object" && typeof b === "object") {
    const keysA = Object.keys(a);
    const keysB = Object.keys(b);

    if (keysA.length !== keysB.length) {
      return false;
    }

    // Check if all keys in object 'a' have the same value in object 'b'
    return keysA.every((key) => Object.prototype.hasOwnProperty.call(b, key) && a[key] === b[key]);
  }

  // If neither of the above conditions is met, the values are not shallow equal
  return false;
}

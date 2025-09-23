import { shallowEquals } from "./shallow-equals";

export function addOrRemoveBy<T>(array: T[], item: T, compare: (a: T, b: T) => boolean): T[] {
  const exists = array.some((a) => compare(a, item));

  if (exists) {
    return array.filter((c) => !compare(c, item));
  } else {
    return [...array, item];
  }
}

export function addOrRemove<T>(array: T[], item: T): T[] {
  const exists = array.some((a) => shallowEquals(a, item));

  if (exists) {
    return array.filter((c) => {
      return !shallowEquals(c, item);
    });
  } else {
    return [...array, item];
  }
}

export function formaterNOK(tall: number) {
  return `${formaterTall(tall)}\u{00A0}NOK`;
}

export function formaterTall(tall: number) {
  return Intl.NumberFormat("no-nb", { maximumFractionDigits: 5 }).format(tall);
}

export function formaterKontoNummer(kontoNummer?: string): string {
  return !kontoNummer
    ? ""
    : `${kontoNummer.substring(0, 4)} ${kontoNummer.substring(4, 6)} ${kontoNummer.substring(6, 11)}`;
}

export function jsonPointerToFieldPath(pointer: string): string {
  return pointer
    .replace(/^\//, "") // Remove leading slash
    .replace(/\/$/, "") // Remove trailing slash
    .split("/") // Split by slash
    .filter(Boolean) // Remove empty parts (handles double slashes)
    .join(".") // Join with dots
    .replace(/~1/g, "/") // Decode escaped '/'
    .replace(/~0/g, "~"); // Decode escaped '~'
}

export function compare<T>(aValue: T, bValue: T): number {
  /* eslint-disable eqeqeq */
  if (aValue == null && bValue == null) {
    return 0;
  } else if (aValue == null) {
    return 1;
  } else if (bValue == null) {
    return -1;
  }
  /* eslint-enable eqeqeq */

  if (typeof aValue === "number" && typeof bValue === "number") {
    return bValue - aValue;
  }

  if (bValue < aValue) {
    return -1;
  } else if (bValue > aValue) {
    return 1;
  } else {
    return 0;
  }
}

import { shallowEquals } from "./shallow-equals";

export function addOrRemove<T>(array: T[], item: T): T[] {
  const exists = array.some((a) => shallowEquals(a, item));

  if (exists) {
    return array.filter((c) => {
      return !shallowEquals(c, item);
    });
  } else {
    const result = array;
    result.push(item);
    return result;
  }
}

export function formaterNOK(tall: number) {
  return `${formaterTall(tall)} (NOK)`;
}

export function formaterTall(tall: number) {
  return Intl.NumberFormat("no-nb").format(tall);
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
    .split("/")          // Split by slash
    .filter(Boolean)     // Remove empty parts (handles double slashes)
    .join(".")           // Join with dots
    .replace(/~1/g, "/") // Decode escaped '/'
    .replace(/~0/g, "~"); // Decode escaped '~'
}

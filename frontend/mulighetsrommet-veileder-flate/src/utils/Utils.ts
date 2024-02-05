export const inneholderUrl = (string: string) => {
  return window.location.href.indexOf(string) > -1;
};

export function erPreview() {
  return inneholderUrl("/preview");
}

function specialChar(string: string | { label: string }) {
  return string
    .toString()
    .toLowerCase()
    .split("æ")
    .join("ae")
    .split("ø")
    .join("o")
    .split("å")
    .join("a");
}

export function kebabCase(string: string | { label: string }) {
  return specialChar(string).trim().replace(/\s+/g, "-").replace(/_/g, "-");
}

export function formaterDato(dato: string | Date, fallback = ""): string {
  const result = new Date(dato).toLocaleString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  if (result === "Invalid Date") {
    return fallback;
  }

  return result;
}

export function utledLopenummerFraTiltaksnummer(tiltaksnummer: string): string {
  const parts = tiltaksnummer.split("#");
  if (parts.length >= 2) {
    return parts[1];
  }

  return tiltaksnummer;
}

export function addOrRemove<T>(array: T[], item: T): T[] {
  const exists = array.includes(item);

  if (exists) {
    return array.filter((c) => {
      return c !== item;
    });
  } else {
    const result = array;
    result.push(item);
    return result;
  }
}

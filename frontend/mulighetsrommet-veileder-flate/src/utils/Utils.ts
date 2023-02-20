export const erTomtObjekt = (objekt: Object): boolean => {
  return Object.keys(objekt).length === 0;
};

export const inneholderUrl = (string: string) => {
  return window.location.href.indexOf(string) > -1;
};

export const erPreview = inneholderUrl('/preview/');

function specialChar(string: string | { label: string }) {
  return string.toString().toLowerCase().split('æ').join('ae').split('ø').join('o').split('å').join('a');
}

export function kebabCase(string: string | { label: string }) {
  return specialChar(string).trim().replace(/\s+/g, '-').replace(/_/g, '-');
}

export function formatMetrikker(string: string) {
  return kebabCase(string).substring(0, 40);
}

export function capitalize(text?: string): string {
  return text ? text.slice(0, 1).toUpperCase() + text.slice(1, text.length).toLowerCase() : '';
}

export function formaterDato(dato: string | Date, fallback = ''): string {
  const result = new Date(dato).toLocaleString('no-NO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });

  if (result === 'Invalid Date') {
    return fallback;
  }

  return result;
}

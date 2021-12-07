export const erTomtObjekt = (objekt: Object): boolean => {
  return Object.keys(objekt).length === 0;
};

export const inneholderUrl = (string: string) => {
  return window.location.href.indexOf(string) > -1;
};

export const redigerTiltaksvariant = inneholderUrl('rediger-tiltaksvariant');
export const opprettTiltaksvariant = inneholderUrl('opprett-tiltaksvariant');

export function specialChar(string: string | { label: string }) {
  return string.toString().toLowerCase().split('æ').join('ae').split('ø').join('o').split('å').join('a');
}
export function kebabCase(string: string | { label: string }) {
  return specialChar(string).replace(/\s+/g, '-');
}

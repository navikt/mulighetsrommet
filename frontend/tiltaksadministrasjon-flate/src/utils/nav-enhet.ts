export function formaterNavEnhet(enhet: { navn: string; enhetsnummer: string }) {
  return `${enhet.navn} (${enhet.enhetsnummer})`;
}

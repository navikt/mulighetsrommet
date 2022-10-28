export default function resolveProductionUrl(document) {
  if (document._type !== "tiltaksgjennomforing") {
    return null;
  }
  return `https://mulighetsrommet-veileder-flate.intern.nav.no/preview/${document._id}?preview=true`;
}

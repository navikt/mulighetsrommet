import { Deltakerliste } from "../../domene/domene";

interface Props {
  deltakerliste: Deltakerliste;
}

export function DeltakerlisteDetaljer({ deltakerliste }: Props) {
  const { tiltaksnavn, tiltaksnummer, avtalenavn, tiltakstype } = deltakerliste.detaljer;
  return (
    <dl className="prose mt-5">
      <dt>Tiltaksnavn:</dt>
      <dd>{tiltaksnavn}</dd>
      <dt>Tiltaksnummer:</dt>
      <dd>{tiltaksnummer}</dd>
      <dt>Avtalenavn:</dt>
      <dd>{avtalenavn}</dd>
      <dt>Tiltakstype:</dt>
      <dd>{tiltakstype}</dd>
    </dl>
  );
}

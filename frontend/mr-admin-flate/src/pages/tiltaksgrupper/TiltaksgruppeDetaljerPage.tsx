import { useTiltaksgruppe } from "../../api/tiltaksgrupper/useTiltaksgrupper";
import { Tilbakelenke } from "../../components/navigering/Tilbakelenke";

export function TiltaksgruppeDetaljerPage() {
  const tiltaksgruppe = useTiltaksgruppe();
  return (
    <>
      <Tilbakelenke>Tilbake</Tilbakelenke>
      <h1>
        {tiltaksgruppe?.navn} - {tiltaksgruppe?.arenaKode}
      </h1>
    </>
  );
}

import { Alert, HGrid } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client";
import { TiltakDetaljerFaneContainer } from "../TiltakDetaljerFaneContainer";
import ArrangorInfo from "./ArrangorInfo";
import styles from "./KontaktinfoFane.module.scss";
import NavKontaktpersonInfo from "./NavKontaktpersonInfo";
import { isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function KontaktinfoFane({ tiltak }: Props) {
  return (
    <TiltakDetaljerFaneContainer harInnhold={true} className={styles.kontaktinfo_container}>
      {tiltak.faneinnhold?.kontaktinfoInfoboks && (
        <Alert variant="info" style={{ whiteSpace: "pre-wrap" }}>
          {tiltak.faneinnhold.kontaktinfoInfoboks}
        </Alert>
      )}
      <HGrid columns="1fr 1fr" align="start" gap="5">
        {isTiltakGruppe(tiltak) ? (
          <ArrangorInfo arrangor={tiltak.arrangor} faneinnhold={tiltak.faneinnhold?.kontaktinfo} />
        ) : (
          <Alert variant="info">Kontaktinfo til arrangør er ikke lagt inn</Alert>
        )}
        <NavKontaktpersonInfo kontaktinfo={tiltak.kontaktinfo} />
      </HGrid>
    </TiltakDetaljerFaneContainer>
  );
}

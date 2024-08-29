import { Alert } from "@navikt/ds-react";
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
      <div className={styles.grid_container}>
        {isTiltakGruppe(tiltak) && (
          <ArrangorInfo arrangor={tiltak.arrangor} faneinnhold={tiltak.faneinnhold?.kontaktinfo} />
        )}
        <NavKontaktpersonInfo kontaktinfo={tiltak.kontaktinfo} />
      </div>
    </TiltakDetaljerFaneContainer>
  );
}

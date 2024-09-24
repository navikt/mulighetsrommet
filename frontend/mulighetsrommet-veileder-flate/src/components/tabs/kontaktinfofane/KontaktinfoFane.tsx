import { Alert, HGrid, VStack } from "@navikt/ds-react";
import { VeilederflateTiltak } from "@mr/api-client";
import { TiltakDetaljerFaneContainer } from "../TiltakDetaljerFaneContainer";
import ArrangorInfo from "./ArrangorInfo";
import styles from "./KontaktinfoFane.module.scss";
import NavKontaktpersonInfo from "./NavKontaktpersonInfo";
import { isTiltakMedArrangor } from "@/api/queries/useArbeidsmarkedstiltakById";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function KontaktinfoFane({ tiltak }: Props) {
  return (
    <TiltakDetaljerFaneContainer harInnhold={true} className={styles.kontaktinfo_container}>
      <VStack gap="5">
        {tiltak.faneinnhold?.kontaktinfoInfoboks && (
          <Alert variant="info" style={{ whiteSpace: "pre-wrap" }}>
            {tiltak.faneinnhold.kontaktinfoInfoboks}
          </Alert>
        )}

        <HGrid columns="1fr 1fr" align="start" gap="5">
          {isTiltakMedArrangor(tiltak) ? (
            <ArrangorInfo
              arrangor={tiltak.arrangor}
              faneinnhold={tiltak.faneinnhold?.kontaktinfo}
            />
          ) : (
            <Alert variant="info">Kontaktinfo til ekstern part er ikke tilgjengelig</Alert>
          )}
          <NavKontaktpersonInfo kontaktinfo={tiltak.kontaktinfo} />
        </HGrid>
      </VStack>
    </TiltakDetaljerFaneContainer>
  );
}

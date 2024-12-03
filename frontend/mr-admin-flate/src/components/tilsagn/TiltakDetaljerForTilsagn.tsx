import { TiltaksgjennomforingDto } from "@mr/api-client";
import { GjennomforingStatusTag } from "@mr/frontend-common";
import { Heading, HGrid, VStack } from "@navikt/ds-react";
import { formaterDato } from "../../utils/Utils";
import { Metadata } from "../detaljside/Metadata";

interface Props {
  tiltaksgjennomforing: TiltaksgjennomforingDto;
}

export function TiltakDetaljerForTilsagn({ tiltaksgjennomforing }: Props) {
  return (
    <VStack gap="4">
      <Heading size="medium">Tiltaksgjennomføring</Heading>
      <HGrid columns="2fr 2fr 2fr 1fr 1fr 1fr 1fr">
        <Metadata header="Tiltaksnavn" verdi={tiltaksgjennomforing.navn} />
        <Metadata
          header="Arrangør"
          verdi={`${tiltaksgjennomforing.arrangor.navn} - ${tiltaksgjennomforing.arrangor.organisasjonsnummer}`}
        />
        <Metadata header="Tiltaksnummer" verdi={tiltaksgjennomforing.tiltaksnummer} />
        <Metadata header="Startdato" verdi={formaterDato(tiltaksgjennomforing.startDato)} />
        <Metadata header="Sluttdato" verdi={formaterDato(tiltaksgjennomforing.startDato)} />
        <Metadata header="Antall plasser" verdi={tiltaksgjennomforing.antallPlasser} />
        <Metadata
          header="Status"
          verdi={<GjennomforingStatusTag status={tiltaksgjennomforing.status.status} />}
        />
      </HGrid>
    </VStack>
  );
}

import { GjennomforingDto } from "@mr/api-client-v2";
import { Heading, HGrid, VStack } from "@navikt/ds-react";
import { formaterDato } from "../../utils/Utils";
import { Metadata } from "../detaljside/Metadata";
import { GjennomforingStatusTag } from "@mr/frontend-common";

interface Props {
  gjennomforing: GjennomforingDto;
}

export function GjennomforingDetaljerMini({ gjennomforing }: Props) {
  return (
    <VStack gap="4">
      <Heading size="medium" level="2">
        Gjennomføring
      </Heading>
      <HGrid columns="2fr 2fr 1fr 1fr 1fr 1fr 1fr">
        <Metadata header="Tiltaksnavn" verdi={gjennomforing.navn} />
        <Metadata
          header="Arrangør"
          verdi={`${gjennomforing.arrangor.navn} - ${gjennomforing.arrangor.organisasjonsnummer}`}
        />
        <Metadata header="Tiltaksnummer" verdi={gjennomforing.tiltaksnummer} />
        <Metadata header="Startdato" verdi={formaterDato(gjennomforing.startDato)} />
        <Metadata header="Sluttdato" verdi={formaterDato(gjennomforing.sluttDato) || "-"} />
        <Metadata header="Antall plasser" verdi={gjennomforing.antallPlasser} />
        <Metadata
          header="Status"
          verdi={<GjennomforingStatusTag status={gjennomforing.status.status} />}
        />
      </HGrid>
    </VStack>
  );
}

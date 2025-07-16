import { GjennomforingDto } from "@mr/api-client-v2";
import { Heading, HGrid, HStack, VStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { ReactNode } from "react";
import { GjennomforingStatusTag } from "@/components/statuselementer/GjennomforingStatusTag";
import { formaterDato } from "@mr/frontend-common/utils/date";

interface Props {
  gjennomforing: GjennomforingDto;
  meny?: ReactNode;
}

export function GjennomforingDetaljerMini({ gjennomforing, meny }: Props) {
  return (
    <VStack gap="4">
      <HStack justify={"space-between"}>
        <Heading size="medium" level="2">
          Gjennomføring
        </Heading>
        {meny}
      </HStack>
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
          verdi={<GjennomforingStatusTag status={gjennomforing.status} />}
        />
      </HGrid>
    </VStack>
  );
}

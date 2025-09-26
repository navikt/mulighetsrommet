import { GjennomforingDto } from "@tiltaksadministrasjon/api-client";
import { Heading, HGrid, HStack, VStack } from "@navikt/ds-react";
import { Metadata } from "@/components/detaljside/Metadata";
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
        <Metadata header="Tiltaksnavn" value={gjennomforing.navn} />
        <Metadata
          header="Arrangør"
          value={`${gjennomforing.arrangor.navn} - ${gjennomforing.arrangor.organisasjonsnummer}`}
        />
        <Metadata header="Tiltaksnummer" value={gjennomforing.tiltaksnummer} />
        <Metadata header="Startdato" value={formaterDato(gjennomforing.startDato)} />
        <Metadata header="Sluttdato" value={formaterDato(gjennomforing.sluttDato) || "-"} />
        <Metadata header="Antall plasser" value={gjennomforing.antallPlasser} />
        <Metadata
          header="Status"
          value={<GjennomforingStatusTag status={gjennomforing.status} />}
        />
      </HGrid>
    </VStack>
  );
}

import { GjennomforingDto } from "@tiltaksadministrasjon/api-client";
import { Heading, HGrid, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { GjennomforingStatusTag } from "@/components/statuselementer/GjennomforingStatusTag";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { Metadata } from "@mr/frontend-common/components/datadriven/Metadata";

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
        <Metadata label="Tiltaksnavn" value={gjennomforing.navn} />
        <Metadata
          label="Arrangør"
          value={`${gjennomforing.arrangor.navn} - ${gjennomforing.arrangor.organisasjonsnummer}`}
        />
        <Metadata label="Tiltaksnummer" value={gjennomforing.tiltaksnummer} />
        <Metadata label="Startdato" value={formaterDato(gjennomforing.startDato)} />
        <Metadata label="Sluttdato" value={formaterDato(gjennomforing.sluttDato) || "-"} />
        <Metadata label="Antall plasser" value={gjennomforing.antallPlasser} />
        <Metadata label="Status" value={<GjennomforingStatusTag status={gjennomforing.status} />} />
      </HGrid>
    </VStack>
  );
}

import { GjennomforingDto } from "@tiltaksadministrasjon/api-client";
import { Heading, HGrid, HStack, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { GjennomforingStatusTag } from "@/components/statuselementer/GjennomforingStatusTag";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";

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
        <MetadataVStack label={gjennomforingTekster.tiltaksnavnLabel} value={gjennomforing.navn} />
        <MetadataVStack
          label="Arrangør"
          value={`${gjennomforing.arrangor.navn} (${gjennomforing.arrangor.organisasjonsnummer})`}
        />
        <MetadataVStack
          label={gjennomforingTekster.lopenummerLabel}
          value={gjennomforing.lopenummer}
        />
        <MetadataVStack
          label={gjennomforingTekster.startdatoLabel}
          value={formaterDato(gjennomforing.startDato)}
        />
        <MetadataVStack
          label={gjennomforingTekster.sluttdatoLabel}
          value={formaterDato(gjennomforing.sluttDato) || "-"}
        />
        <MetadataVStack
          label={gjennomforingTekster.antallPlasserLabel}
          value={gjennomforing.antallPlasser}
        />
        <MetadataVStack
          label="Status"
          value={<GjennomforingStatusTag status={gjennomforing.status} />}
        />
      </HGrid>
    </VStack>
  );
}

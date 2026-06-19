import { GjennomforingAvtaleDto } from "@tiltaksadministrasjon/api-client";
import { HGrid, VStack } from "@navikt/ds-react";
import { GjennomforingStatusTag } from "@/components/statuselementer/GjennomforingStatusTag";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";

interface Props {
  gjennomforing: GjennomforingAvtaleDto;
}

export function GjennomforingAvtaleHeader({ gjennomforing }: Props) {
  return (
    <VStack className="px-4 bg-ax-bg-default">
      <Separator />
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
      <Separator />
    </VStack>
  );
}

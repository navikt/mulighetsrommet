import { GjennomforingDto } from "@tiltaksadministrasjon/api-client";
import { Box, Heading, HGrid, VStack } from "@navikt/ds-react";
import { GjennomforingStatusTag } from "@/components/statuselementer/GjennomforingStatusTag";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { isGruppetiltak } from "@/api/gjennomforing/utils";

export function GjennomforingDetaljerMini({ gjennomforing }: { gjennomforing: GjennomforingDto }) {
  return (
    <Box
      borderColor="neutral-subtle"
      borderWidth="0 0 1 0"
      paddingBlock="space-0 space-16"
      marginBlock="space-0 space-16"
    >
      <VStack gap="space-16">
        <Heading size="medium" level="2">
          Gjennomføring
        </Heading>
        <HGrid columns="2fr 2fr 1fr 1fr 1fr 1fr 1fr">
          <MetadataVStack
            label={gjennomforingTekster.tiltaksnavnLabel}
            value={gjennomforing.navn}
          />
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
          {isGruppetiltak(gjennomforing) && (
            <MetadataVStack
              label={gjennomforingTekster.antallPlasserLabel}
              value={gjennomforing.antallPlasser}
            />
          )}
          <MetadataVStack
            label="Status"
            value={<GjennomforingStatusTag status={gjennomforing.status} />}
          />
        </HGrid>
      </VStack>
    </Box>
  );
}

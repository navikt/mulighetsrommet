import { useTilskudd } from "@/api/tilskudd/useTilskuddOrError";
import { TilskuddLayout } from "@/components/tilskudd/TilskuddLayout";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Heading, VStack } from "@navikt/ds-react";
import { TilskuddVedtak } from "@tiltaksadministrasjon/api-client";

export function TilskuddDetaljerPage() {
  const { gjennomforingId, tilskuddId } = useRequiredParams(["gjennomforingId", "tilskuddId"]);
  const { data: tilskudd } = useTilskudd(tilskuddId);
  return (
    <TilskuddLayout gjennomforingId={gjennomforingId}>
      <Heading level="1" size="medium">{tilskudd.type.navn}</Heading>
      <MetadataVStack label="Tilskuddsnummer" value={tilskudd.tilskuddsnummer} />
    <VStack gap="space-16">
      {tilskudd.vedtak.map((vedtak: TilskuddVedtak) =>
        (<VStack gap="space-8">
          <MetadataVStack label="Soknad journalpost" value={vedtak.soknadJournalpostId} />
          <MetadataVStack label="Soknad dato" value={formaterDato(vedtak.soknadDato)} />
          <MetadataVStack label="Soknad belop" value={formaterValutaBelop(vedtak.soknadBelop)} />
          {vedtak.utbetalingBelop && <MetadataVStack label="Belop" value={formaterValutaBelop(vedtak.utbetalingBelop)} />}
        </VStack>)
      )}
    </VStack>
  </TilskuddLayout>
  );
}

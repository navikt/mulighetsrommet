import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { TilskuddBehandlingDto, Valuta } from "@tiltaksadministrasjon/api-client";
import { Heading, VStack } from "@navikt/ds-react";
import { FormGroup } from "@/layouts/FormGroup";
import { opplaeringTilskuddToString } from "@/utils/Utils";

interface Props {
  behandling: TilskuddBehandlingDto;
}

export function SaksopplysningerDetaljer({ behandling }: Props) {
  const { soknadJournalpostId, soknadDato, periode, kostnadssted, tilskudd } = behandling;

  return (
    <>
      <Heading size="small" level="3" spacing>
        Informasjon fra søknad
      </Heading>
      <VStack gap="space-20" align="start">
        <MetadataVStack label="JournalpostID" value={soknadJournalpostId} />
        <MetadataVStack label="Søknadsdato" value={formaterDato(soknadDato)} />
        <MetadataVStack label="Periode" value={formaterPeriode(periode)} />
        <MetadataVStack label="Kostnadssted" value={kostnadssted} />
        {tilskudd.map((t) => (
          <FormGroup key={t.id}>
            <VStack gap="space-8">
              <MetadataVStack
                label="Tilskuddstype"
                value={opplaeringTilskuddToString(t.tilskuddOpplaeringType)}
              />
              <MetadataVStack
                label="Beløp fra søknad"
                value={formaterValuta(t.soknadBelop, Valuta.NOK)}
              />
              <MetadataVStack label="Utbetalingsmottaker" value={t.utbetalingMottaker} />
            </VStack>
          </FormGroup>
        ))}
      </VStack>
    </>
  );
}

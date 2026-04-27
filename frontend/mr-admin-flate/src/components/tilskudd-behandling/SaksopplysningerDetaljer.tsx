import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { TilskuddBehandlingDto } from "@tiltaksadministrasjon/api-client";
import { Heading, VStack } from "@navikt/ds-react";
import { FormGroup } from "@/layouts/FormGroup";

interface Props {
  behandling: TilskuddBehandlingDto;
}

export function SaksopplysningerDetaljer({ behandling }: Props) {
  const { soknadJournalpostId, soknadDato, periode, kostnadssted, vedtak } = behandling;

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
        {vedtak.map((v) => (
          <FormGroup key={v.id}>
            <VStack gap="space-8">
              <MetadataVStack label="Tilskuddstype" value={v.tilskuddOpplaeringType} />
              <MetadataVStack
                label="Beløp fra søknad"
                value={formaterValuta(v.soknadBelop, v.soknadValuta)}
              />
              <MetadataVStack label="Utbetalingsmottaker" value={v.utbetalingMottaker} />
            </VStack>
          </FormGroup>
        ))}
      </VStack>
    </>
  );
}

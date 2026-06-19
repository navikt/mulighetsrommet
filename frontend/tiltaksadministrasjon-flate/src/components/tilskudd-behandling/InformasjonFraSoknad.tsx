import { KostnadsstedDto, Periode } from "@tiltaksadministrasjon/api-client";
import { Heading, VStack } from "@navikt/ds-react";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { DataElementStatusTag } from "@mr/frontend-common";
import { DataElementStatus } from "@mr/frontend-common/components/datadriven/types";

interface InformasjonFraSoknadProps {
  status: DataElementStatus | null;
  journalpostId: string | null;
  soknadsdato: string | null;
  periode: Periode | null;
  kostnadssted: KostnadsstedDto | null;
}

export function InformasjonFraSoknad(props: InformasjonFraSoknadProps) {
  return (
    <>
      <Heading size="small" level="3" spacing>
        Informasjon fra søknad
      </Heading>
      <VStack gap="space-16">
        <Definisjonsliste
          definitions={[
            {
              key: "Status",
              value: props.status ? <DataElementStatusTag {...props.status} /> : null,
            },
            { key: "Journalpost-ID i Gosys", value: props.journalpostId },
            {
              key: "Søknadsdato",
              value: props.soknadsdato ? formaterDato(props.soknadsdato) : null,
            },
            {
              key: "Tilskuddsperiode",
              value: props.periode ? formaterPeriode(props.periode) : null,
            },
            {
              key: "Kostnadssted",
              value: props.kostnadssted
                ? `${props.kostnadssted.navn} (${props.kostnadssted.enhetsnummer})`
                : null,
            },
          ]}
        />
      </VStack>
    </>
  );
}

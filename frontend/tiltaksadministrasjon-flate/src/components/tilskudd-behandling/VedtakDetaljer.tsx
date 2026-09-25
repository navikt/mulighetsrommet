import { DataElementStatus, ValutaBelop } from "@tiltaksadministrasjon/api-client";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { DataElementStatusTag } from "@mr/frontend-common";
import { VStack } from "@navikt/ds-react";
import { MetadataFritekstfelt } from "@mr/frontend-common/components/datadriven/Metadata";

interface VedtakDetaljerProps {
  vedtakResultat: {
    status: DataElementStatus;
  };
  utbetalingBelop: ValutaBelop | null;
  kommentarVedtaksbrev: string | null;
  internKommentar: string | null;
}

export function VedtakDetaljer({
  vedtakResultat,
  utbetalingBelop,
  kommentarVedtaksbrev,
  internKommentar,
}: VedtakDetaljerProps) {
  return (
    <VStack gap="space-20">
      <Definisjonsliste
        title="Vedtak"
        definitions={[
          { key: "Vedtaksresultat", value: <DataElementStatusTag {...vedtakResultat.status} /> },
          {
            key: "Beløp til utbetaling",
            value: utbetalingBelop ? formaterValutaBelop(utbetalingBelop) : "-",
          },
        ]}
      />
      <MetadataFritekstfelt label="Kommentar til brukeren" value={kommentarVedtaksbrev ?? "-"} />
      <MetadataFritekstfelt label="Kommentar (internt i Nav)" value={internKommentar ?? "-"} />
    </VStack>
  );
}

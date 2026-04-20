import {
  MetadataFritekstfelt,
  MetadataVStack,
  Separator,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { TilskuddBehandlingDto, VedtakResultat } from "@tiltaksadministrasjon/api-client";
import { Heading, VStack } from "@navikt/ds-react";
import { FormGroup } from "@/layouts/FormGroup";

interface Props {
  behandling: TilskuddBehandlingDto;
}

export function VedtakDetaljer({ behandling }: Props) {
  return (
    <>
      <Heading size="medium" level="3" spacing>
        Vedtak
      </Heading>
      <VStack gap="space-20" align="start">
        {behandling.vedtak.map((v) => (
          <FormGroup key={v.id}>
            <VStack gap="space-4">
              <MetadataVStack label="Tilskuddstype" value={v.tilskuddOpplaeringType} />
              <MetadataVStack label="Hvem skal motta utbetalingen?" value={v.utbetalingMottaker} />
              <MetadataVStack label="Beløp fra søknad" value={v.soknadBelop} />
            </VStack>
            <Separator />
            <MetadataVStack
              label="Vedtaksresultat"
              value={v.vedtakResultat === VedtakResultat.INNVILGELSE ? "Innvilgelse" : "Avslag"}
            />
            <MetadataFritekstfelt
              label="Kommentarer til deltaker (vil vises i vedtaksbrev)"
              value={v.kommentarVedtaksbrev}
            />
          </FormGroup>
        ))}
      </VStack>
    </>
  );
}

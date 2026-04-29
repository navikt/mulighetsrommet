import {
  MetadataFritekstfelt,
  MetadataVStack,
  Separator,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { TilskuddBehandlingDto, VedtakResultat } from "@tiltaksadministrasjon/api-client";
import { Heading, VStack } from "@navikt/ds-react";
import { FormGroup } from "@/layouts/FormGroup";
import { opplaeringTilskuddToString } from "@/utils/Utils";

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
        {behandling.tilskudd.map((t) => (
          <FormGroup key={t.id}>
            <VStack gap="space-4">
              <MetadataVStack
                label="Tilskuddstype"
                value={opplaeringTilskuddToString(t.tilskuddOpplaeringType)}
              />
              <MetadataVStack label="Hvem skal motta utbetalingen?" value={t.utbetalingMottaker} />
              <MetadataVStack label="Beløp fra søknad" value={t.soknadBelop} />
            </VStack>
            <Separator />
            <MetadataVStack
              label="Vedtaksresultat"
              value={t.vedtakResultat === VedtakResultat.INNVILGELSE ? "Innvilgelse" : "Avslag"}
            />
            <MetadataFritekstfelt
              label="Kommentarer til deltaker (vil vises i vedtaksbrev)"
              value={t.kommentarVedtaksbrev}
            />
          </FormGroup>
        ))}
      </VStack>
    </>
  );
}

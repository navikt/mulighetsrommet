import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakDokument } from "@/api/tiltak-dokument/useTiltakDokument";
import { Bolk } from "@/components/detaljside/Bolk";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { BodyShort, Heading, VStack } from "@navikt/ds-react";

export function TiltakDokumentDetaljer() {
  const { tiltakDokumentId } = useRequiredParams(["tiltakDokumentId"]);
  const { data: tiltakDokument } = useTiltakDokument(tiltakDokumentId);

  return (
    <Bolk aria-label="Grunninfo">
      <VStack gap="space-8">
        <div>
          <Heading size="small" level="2">
            Tiltakstype
          </Heading>
          <BodyShort>{tiltakDokument.tiltakstype.navn}</BodyShort>
        </div>
        {tiltakDokument.stedForGjennomforing && (
          <div>
            <Heading size="small" level="2">
              Sted for gjennomføring
            </Heading>
            <BodyShort>{tiltakDokument.stedForGjennomforing}</BodyShort>
          </div>
        )}
        {tiltakDokument.arrangor && (
          <div>
            <Heading size="small" level="2">
              Arrangør
            </Heading>
            <BodyShort>
              {tiltakDokument.arrangor.navn} — {tiltakDokument.arrangor.organisasjonsnummer}
            </BodyShort>
            {tiltakDokument.arrangorKontaktpersoner.length > 0 && (
              <VStack gap="space-4" className="mt-2">
                <Heading size="xsmall" level="3">
                  {gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
                </Heading>
                {tiltakDokument.arrangorKontaktpersoner.map((kp) => (
                  <ArrangorKontaktpersonDetaljer key={kp.id} kontaktperson={kp} />
                ))}
              </VStack>
            )}
          </div>
        )}
        {tiltakDokument.administratorer.length > 0 && (
          <div>
            <Heading size="small" level="2">
              Administratorer
            </Heading>
            <VStack gap="space-4">
              {tiltakDokument.administratorer.map((admin) => (
                <BodyShort key={admin.navIdent}>
                  {admin.navn} ({admin.navIdent})
                </BodyShort>
              ))}
            </VStack>
          </div>
        )}
      </VStack>
    </Bolk>
  );
}

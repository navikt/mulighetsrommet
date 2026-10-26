import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakBeskrivelse } from "@/api/tiltak-beskrivelse/useTiltakBeskrivelse";
import { Bolk } from "@/components/detaljside/Bolk";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { BodyShort, Heading, VStack } from "@navikt/ds-react";

export function TiltakBeskrivelseDetaljer() {
  const { tiltakBeskrivelseId } = useRequiredParams(["tiltakBeskrivelseId"]);
  const { data: tiltakBeskrivelse } = useTiltakBeskrivelse(tiltakBeskrivelseId);

  return (
    <Bolk aria-label="Grunninfo">
      <VStack gap="space-8">
        <div>
          <Heading size="small" level="2">
            Tiltakstype
          </Heading>
          <BodyShort>{tiltakBeskrivelse.tiltakstype.navn}</BodyShort>
        </div>
        {tiltakBeskrivelse.tiltaksnummer && (
          <div>
            <Heading size="small" level="2">
              Tiltaksnummer
            </Heading>
            <BodyShort>{tiltakBeskrivelse.tiltaksnummer}</BodyShort>
          </div>
        )}
        {tiltakBeskrivelse.stedForGjennomforing && (
          <div>
            <Heading size="small" level="2">
              Sted for gjennomføring
            </Heading>
            <BodyShort>{tiltakBeskrivelse.stedForGjennomforing}</BodyShort>
          </div>
        )}
        {tiltakBeskrivelse.arrangor && (
          <div>
            <Heading size="small" level="2">
              Arrangør
            </Heading>
            <BodyShort>
              {tiltakBeskrivelse.arrangor.navn} — {tiltakBeskrivelse.arrangor.organisasjonsnummer}
            </BodyShort>
            {tiltakBeskrivelse.arrangorKontaktpersoner.length > 0 && (
              <VStack gap="space-4" className="mt-2">
                <Heading size="xsmall" level="3">
                  {gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel}
                </Heading>
                {tiltakBeskrivelse.arrangorKontaktpersoner.map((kp) => (
                  <ArrangorKontaktpersonDetaljer key={kp.id} kontaktperson={kp} />
                ))}
              </VStack>
            )}
          </div>
        )}
        {tiltakBeskrivelse.administratorer.length > 0 && (
          <div>
            <Heading size="small" level="2">
              Administratorer
            </Heading>
            <VStack gap="space-4">
              {tiltakBeskrivelse.administratorer.map((admin) => (
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

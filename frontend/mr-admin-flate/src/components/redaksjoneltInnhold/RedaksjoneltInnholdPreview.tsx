import { useTiltakstypeFaneinnhold } from "@/api/gjennomforing/useTiltakstypeFaneinnhold";
import { Alert, BodyLong, Heading, VStack } from "@navikt/ds-react";
import { PortableText } from "@portabletext/react";
import {
  EmbeddedTiltakstype,
  Faneinnhold,
  GjennomforingKontaktperson,
  Kontorstruktur,
} from "@mr/api-client-v2";
import { LokalInformasjonContainer } from "@mr/frontend-common";
import { Suspense, useState } from "react";
import { Laster } from "../laster/Laster";
import { LenkerList } from "../lenker/LenkerList";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { Metadata } from "../detaljside/Metadata";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { Bolk } from "../detaljside/Bolk";
import { gjennomforingTekster } from "../ledetekster/gjennomforingLedetekster";
import { Kontaktperson } from "@/pages/gjennomforing/Kontaktperson";
import { CaretDownFillIcon, CaretUpFillIcon } from "@navikt/aksel-icons";

interface RedaksjoneltInnholdPreviewProps {
  tiltakstype: EmbeddedTiltakstype;
  beskrivelse?: string | null;
  faneinnhold?: Faneinnhold | null;
  kontorstruktur: Kontorstruktur;
  kontaktpersoner: GjennomforingKontaktperson[];
}

export function RedaksjoneltInnholdPreview() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);
  const { tiltakstype, redaksjoneltInnhold, kontorstruktur } = avtale;

  return (
    <Suspense fallback={<Laster tekst="Laster innhold" />}>
      <RedaksjoneltInnhold
        tiltakstype={tiltakstype}
        kontorstruktur={kontorstruktur}
        beskrivelse={redaksjoneltInnhold.beskrivelse}
        faneinnhold={redaksjoneltInnhold.faneinnhold}
        kontaktpersoner={[]}
      />
    </Suspense>
  );
}

export function RedaksjoneltInnhold(props: RedaksjoneltInnholdPreviewProps) {
  const { tiltakstype, kontaktpersoner, beskrivelse, faneinnhold, kontorstruktur } = props;

  const { data: tiltakstypeSanityData } = useTiltakstypeFaneinnhold(tiltakstype.id);
  return (
    <TwoColumnGrid separator>
      <RedaksjoneltInnholdContainer>
        {tiltakstypeSanityData.beskrivelse && (
          <>
            <Heading size="medium">Generell informasjon</Heading>
            <BodyLong size="large" style={{ whiteSpace: "pre-wrap" }}>
              {tiltakstypeSanityData.beskrivelse}
            </BodyLong>
          </>
        )}
        {beskrivelse && (
          <LokalInformasjonContainer>
            <BodyLong style={{ whiteSpace: "pre-wrap" }} textColor="subtle" size="medium">
              {beskrivelse}
            </BodyLong>
          </LokalInformasjonContainer>
        )}
        {someValuesExists([
          faneinnhold?.forHvem,
          faneinnhold?.forHvemInfoboks,
          tiltakstypeSanityData.faneinnhold?.forHvem,
          tiltakstypeSanityData.faneinnhold?.forHvemInfoboks,
        ]) ? (
          <>
            <Heading size="medium">For hvem</Heading>
            <DetaljerFane
              gjennomforing={faneinnhold?.forHvem}
              gjennomforingAlert={faneinnhold?.forHvemInfoboks}
              tiltakstype={tiltakstypeSanityData.faneinnhold?.forHvem}
              tiltakstypeAlert={tiltakstypeSanityData.faneinnhold?.forHvemInfoboks}
            />
          </>
        ) : null}

        {someValuesExists([
          faneinnhold?.detaljerOgInnhold,
          faneinnhold?.detaljerOgInnholdInfoboks,
          tiltakstypeSanityData.faneinnhold?.detaljerOgInnhold,
          tiltakstypeSanityData.faneinnhold?.detaljerOgInnholdInfoboks,
        ]) ? (
          <>
            <Heading size="medium">Detaljer og innhold</Heading>
            <DetaljerFane
              gjennomforing={faneinnhold?.detaljerOgInnhold}
              gjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
              tiltakstype={tiltakstypeSanityData.faneinnhold?.detaljerOgInnhold}
              tiltakstypeAlert={tiltakstypeSanityData.faneinnhold?.detaljerOgInnholdInfoboks}
            />
          </>
        ) : null}

        {someValuesExists([
          faneinnhold?.pameldingOgVarighet,
          faneinnhold?.pameldingOgVarighetInfoboks,
          tiltakstypeSanityData.faneinnhold?.pameldingOgVarighet,
          tiltakstypeSanityData.faneinnhold?.pameldingOgVarighetInfoboks,
        ]) ? (
          <>
            <Heading size="medium">Påmelding og varighet</Heading>
            <DetaljerFane
              gjennomforing={faneinnhold?.pameldingOgVarighet}
              gjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
              tiltakstype={tiltakstypeSanityData.faneinnhold?.pameldingOgVarighet}
              tiltakstypeAlert={tiltakstypeSanityData.faneinnhold?.pameldingOgVarighetInfoboks}
            />
          </>
        ) : null}

        {someValuesExists([faneinnhold?.kontaktinfo, faneinnhold?.kontaktinfoInfoboks]) ? (
          <>
            <Heading size="medium">Kontaktinfo</Heading>
            <DetaljerFane
              gjennomforing={faneinnhold?.kontaktinfo}
              gjennomforingAlert={faneinnhold?.kontaktinfoInfoboks}
            />
          </>
        ) : null}

        {someValuesExists([faneinnhold?.lenker]) ? (
          <div className="prose">
            <Heading size="medium">Lenker</Heading>
            <LenkerList lenker={faneinnhold?.lenker || []} />
          </div>
        ) : null}

        {someValuesExists([faneinnhold?.delMedBruker, tiltakstypeSanityData.delingMedBruker]) ? (
          <>
            <Heading size="medium">Del med bruker</Heading>
            <BodyLong as="div" size="small" className="prose">
              {faneinnhold?.delMedBruker ?? tiltakstypeSanityData.delingMedBruker}
            </BodyLong>
          </>
        ) : null}
      </RedaksjoneltInnholdContainer>
      <RedaksjoneltInnholdContainer>
        <Bolk aria-label={gjennomforingTekster.tilgjengeligIModiaLabel}>
          <Metadata
            header={gjennomforingTekster.tilgjengeligIModiaLabel}
            value={<RegionOgUnderenheter kontorstruktur={kontorstruktur} />}
          />
        </Bolk>
        {kontaktpersoner.length > 0 && (
          <Bolk>
            <Metadata
              header={gjennomforingTekster.kontaktpersonNav.mainLabel}
              value={
                <VStack gap="2">
                  {kontaktpersoner.map((kp, index) => (
                    <Kontaktperson key={index} kontaktperson={kp} />
                  ))}
                </VStack>
              }
            />
          </Bolk>
        )}
      </RedaksjoneltInnholdContainer>
    </TwoColumnGrid>
  );
}

function someValuesExists(params: any[]): boolean {
  return params.some((p) => !!p);
}

interface DetaljerFaneProps {
  gjennomforingAlert?: string | null;
  tiltakstypeAlert?: string | null;
  gjennomforing?: any;
  tiltakstype?: any;
}

const DetaljerFane = ({
  gjennomforingAlert,
  tiltakstypeAlert,
  gjennomforing,
  tiltakstype,
}: DetaljerFaneProps) => {
  if (!gjennomforingAlert && !tiltakstypeAlert && !gjennomforing && !tiltakstype) {
    return <></>;
  }

  return (
    <div>
      {tiltakstype && (
        <>
          {tiltakstypeAlert && (
            <Alert style={{ whiteSpace: "pre-wrap" }} variant="info">
              {tiltakstypeAlert}
            </Alert>
          )}
          <BodyLong as="div" size="small">
            <PortableText value={tiltakstype} />
          </BodyLong>
        </>
      )}
      {(gjennomforing || gjennomforingAlert) && (
        <LokalInformasjonContainer>
          <Heading level="2" size="small" spacing className="mt-0">
            Lokal Informasjon
          </Heading>
          {gjennomforingAlert && (
            <Alert style={{ whiteSpace: "pre-wrap" }} variant="info">
              {gjennomforingAlert}
            </Alert>
          )}
          <BodyLong as="div" size="small">
            <PortableText value={gjennomforing} />
          </BodyLong>
        </LokalInformasjonContainer>
      )}
    </div>
  );
};

function RegionOgUnderenheter({ kontorstruktur }: { kontorstruktur: Kontorstruktur }) {
  const [openRegions, setOpenRegions] = useState<string[]>([]);

  const toggleRegion = (enhetsnummer: string) => {
    setOpenRegions((prev) =>
      prev.includes(enhetsnummer)
        ? prev.filter((num) => num !== enhetsnummer)
        : [...prev, enhetsnummer],
    );
  };

  function isRegionOpen(enhetsnummer: string) {
    return openRegions.includes(enhetsnummer);
  }

  return (
    <ul>
      {kontorstruktur.map((kontor) => {
        return (
          <li className="font-bold my-2 ml-3" key={kontor.region.enhetsnummer}>
            <button
              className="hover:cursor-pointer flex"
              onClick={() => toggleRegion(kontor.region.enhetsnummer)}
              title={`${kontor.region.navn} (${kontor.kontorer.length} kontorer)`}
            >
              {kontor.region.navn} ({kontor.kontorer.length || 0})
              {isRegionOpen(kontor.region.enhetsnummer) ? (
                <CaretUpFillIcon className="text-xl" />
              ) : (
                <CaretDownFillIcon className="text-xl" />
              )}
            </button>
            {isRegionOpen(kontor.region.enhetsnummer) && (
              <ul className="list-disc ml-5">
                {kontor.kontorer.map((kontor) => (
                  <li className="ml-5 font-thin" key={kontor.enhetsnummer}>
                    {kontor.navn}
                  </li>
                ))}
              </ul>
            )}
          </li>
        );
      })}
    </ul>
  );
}

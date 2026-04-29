import { useAvtale } from "@/api/avtaler/useAvtale";
import { usePollTiltaksnummer } from "@/api/gjennomforing/usePollTiltaksnummer";
import { AmoKategoriseringDetaljer } from "@/components/amoKategorisering/AmoKategoriseringDetaljer";
import { NokkeltallDeltakere } from "@/components/gjennomforing/NokkeltallDeltakere";
import { StengtHosArrangorTable } from "@/components/gjennomforing/stengt/StengtHosArrangorTable";
import { TiltakTilgjengeligForArrangor } from "@/components/gjennomforing/TilgjengeligTiltakForArrangor";
import { Laster } from "@/components/laster/Laster";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { UtdanningslopDetaljer } from "@/components/utdanning/UtdanningslopDetaljer";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { BodyShort, Heading, HelpText, HStack, Link, Tag, VStack } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { GjennomforingDetaljerAvtale } from "@/pages/gjennomforing/GjennomforingDetaljerAvtale";
import { GjennomforingDetaljerVarighet } from "@/pages/gjennomforing/GjennomforingDetaljerVarighet";
import { GjennomforingDetaljerAdministratorer } from "@/pages/gjennomforing/GjennomforingDetaljerAdministratorer";
import { DetaljerLayout } from "@/components/detaljside/DetaljerLayout";
import {
  AmoKategoriseringDto,
  GjennomforingAvtaleDto,
  GjennomforingVeilederinfoDto,
  PrismodellDto,
  TiltakstypeDto,
  UtdanningslopDto,
} from "@tiltaksadministrasjon/api-client";
import { PrismodellDetaljer } from "@/components/avtaler/PrismodellDetaljer";
import { kursOgTiltakErStudiespesialisering } from "@/utils/Utils";
import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";

interface Props {
  tiltakstype: TiltakstypeDto;
  gjennomforing: GjennomforingAvtaleDto;
  veilederinfo: null | GjennomforingVeilederinfoDto;
  prismodell: PrismodellDto;
  amoKategorisering: null | AmoKategoriseringDto;
  utdanningslop: null | UtdanningslopDto;
}

export function GjennomforingAvtaleDetaljer(props: Props) {
  const { tiltakstype, gjennomforing, veilederinfo, utdanningslop, amoKategorisering, prismodell } =
    props;
  const { data: avtale } = useAvtale(gjennomforing.avtaleId);

  const gjennomforingMeta: Definition[] = [
    { key: gjennomforingTekster.tiltaksnavnLabel, value: gjennomforing.navn },
    {
      key: gjennomforingTekster.tiltakstypeLabel,
      value: tiltakstype.navn,
    },
    {
      key: gjennomforingTekster.tiltaksnummerLabel,
      value: gjennomforing.tiltaksnummer ?? <HentTiltaksnummer id={gjennomforing.id} />,
    },
    {
      key: gjennomforingTekster.lopenummerLabel,
      value: (
        <HStack gap="space-8">
          {gjennomforing.lopenummer}
          <HelpText title="Hva betyr feltet 'Løpenummer'?">
            <VStack gap="space-8">
              <Heading level="3" size="xsmall">
                Hva betyr feltet 'Løpenummer'?
              </Heading>
              <BodyShort>
                Hver tiltaksgjennomføring har et unikt løpenummer. Alle tilsagn og utbetalinger kan
                spores tilbake til gjennomføringen basert på løpenummeret. I tillegg vises det i
                utbetalingsløsningen for tiltaksarrangører.
              </BodyShort>
              <BodyShort>Løpenummeret vil på sikt erstatte "tiltaksnummeret" fra Arena.</BodyShort>
            </VStack>
          </HelpText>
        </HStack>
      ),
    },
  ];

  const { arrangor } = gjennomforing;
  const arrangorMeta: Definition[] = [
    ...(avtale.arrangor
      ? [
          {
            key: gjennomforingTekster.tiltaksarrangorHovedenhetLabel,
            value: (
              <Link as={ReactRouterLink} to={`/arrangorer/${avtale.arrangor.id}`}>
                {avtale.arrangor.navn} - {avtale.arrangor.organisasjonsnummer}
              </Link>
            ),
          },
        ]
      : []),
    {
      key: gjennomforingTekster.tiltaksarrangorUnderenhetLabel,
      value: `${arrangor.navn} - ${arrangor.organisasjonsnummer}`,
    },
    {
      key: gjennomforingTekster.kontaktpersonerHosTiltaksarrangorLabel,
      value:
        arrangor.kontaktpersoner.length > 0
          ? arrangor.kontaktpersoner.map((kontaktperson) => (
              <ArrangorKontaktpersonDetaljer key={kontaktperson.id} kontaktperson={kontaktperson} />
            ))
          : "-",
    },
  ];

  return (
    <GjennomforingPageLayout>
      <TwoColumnGrid separator>
        <DetaljerLayout>
          <Definisjonsliste title="Gjennomføring" definitions={gjennomforingMeta} />
          <GjennomforingDetaljerAvtale avtale={avtale} />
          {utdanningslop && <UtdanningslopDetaljer utdanningslop={utdanningslop} />}
          {amoKategorisering &&
            !kursOgTiltakErStudiespesialisering(
              amoKategorisering.kurstype,
              tiltakstype.tiltakskode,
            ) && <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} />}
          <GjennomforingDetaljerVarighet
            tiltakstype={tiltakstype}
            gjennomforing={gjennomforing}
            veilederinfo={veilederinfo}
          />
        </DetaljerLayout>
        <DetaljerLayout>
          <GjennomforingDetaljerAdministratorer gjennomforing={gjennomforing} />
          <Definisjonsliste title="Arrangør" definitions={arrangorMeta} columns={1} />
          {gjennomforing.stengt.length !== 0 && (
            <StengtHosArrangorTable readOnly gjennomforing={gjennomforing} />
          )}
          {!harStartet(gjennomforing) && (
            <TiltakTilgjengeligForArrangor gjennomforing={gjennomforing} />
          )}
          <PrismodellDetaljer prismodeller={[prismodell]} />
        </DetaljerLayout>
      </TwoColumnGrid>
      <Separator />
      <NokkeltallDeltakere gjennomforingId={gjennomforing.id} />
    </GjennomforingPageLayout>
  );
}

function HentTiltaksnummer({ id }: { id: string }) {
  const { isError, isLoading, data } = usePollTiltaksnummer(id);
  return isError ? (
    <Tag data-color="danger" variant="outline">
      Klarte ikke hente tiltaksnummer
    </Tag>
  ) : isLoading ? (
    <HStack align={"center"} gap="space-4">
      <Laster />
      <span>Henter tiltaksnummer i Arena</span>
    </HStack>
  ) : (
    data?.tiltaksnummer
  );
}

function harStartet(gjennomforing: GjennomforingAvtaleDto) {
  return new Date() > new Date(gjennomforing.startDato);
}

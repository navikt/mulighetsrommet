import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { usePollTiltaksnummer } from "@/api/gjennomforing/usePollTiltaksnummer";
import { AmoKategoriseringDetaljer } from "@/components/amoKategorisering/AmoKategoriseringDetaljer";
import { NokkeltallDeltakere } from "@/components/gjennomforing/NokkeltallDeltakere";
import { StengtHosArrangorTable } from "@/components/gjennomforing/stengt/StengtHosArrangorTable";
import { TiltakTilgjengeligForArrangor } from "@/components/gjennomforing/TilgjengeligTiltakForArrangor";
import { Laster } from "@/components/laster/Laster";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { UtdanningslopDetaljer } from "@/components/utdanning/UtdanningslopDetaljer";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { formatertVentetid } from "@/utils/Utils";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { formaterDato, formaterPeriodeUdefinertSlutt } from "@mr/frontend-common/utils/date";
import { BodyShort, Heading, HelpText, HStack, Tag, VStack } from "@navikt/ds-react";
import { Link } from "react-router";
import { GjennomforingPageLayout } from "./GjennomforingPageLayout";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { PrismodellDetaljer } from "@/components/avtaler/PrismodellDetaljer";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { kreverDeltidsprosent } from "@/utils/tiltakstype";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";

export function GjennomforingDetaljer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const detaljer = useGjennomforing(gjennomforingId);
  const { gjennomforing, veilederinfo, utdanningslop, amoKategorisering, prismodell } = detaljer;
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);

  const avtaleMeta: Definition[] = [
    {
      key: gjennomforingTekster.avtaleLabel,
      value: avtale ? (
        <Link to={`/avtaler/${avtale.id}`}>
          {avtale.navn} {avtale.avtalenummer ?? null}
        </Link>
      ) : (
        gjennomforingTekster.ingenAvtaleForGjennomforingenLabel
      ),
    },
    {
      key: "Avtaleperiode",
      value: avtale
        ? `${formaterPeriodeUdefinertSlutt({ start: avtale.startDato, slutt: avtale.sluttDato })}`
        : "",
    },
  ];

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
        <HStack gap="2">
          {gjennomforing.lopenummer}
          <HelpText title="Hva betyr feltet 'Løpenummer'?">
            <VStack gap="2">
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

  const varighetMeta: Definition[] = [
    {
      key: gjennomforingTekster.startdatoLabel,
      value: formaterDato(gjennomforing.startDato),
    } as Definition,
    {
      key: gjennomforingTekster.sluttdatoLabel,
      value: formaterDato(gjennomforing.sluttDato) ?? "-",
    },
    {
      key: gjennomforingTekster.oppstart.label,
      value: gjennomforingTekster.oppstart.beskrivelse(gjennomforing.oppstart),
    },
    veilederinfo && {
      key: gjennomforingTekster.apentForPameldingLabel,
      value: veilederinfo.apentForPamelding ? "Ja" : "Nei",
    },
    { key: gjennomforingTekster.antallPlasserLabel, value: gjennomforing.antallPlasser },
    kreverDeltidsprosent(tiltakstype) && {
      key: gjennomforingTekster.deltidsprosentLabel,
      value: gjennomforing.deltidsprosent,
    },
    veilederinfo?.estimertVentetid && {
      key: gjennomforingTekster.estimertVentetidLabel,
      value: formatertVentetid(
        veilederinfo.estimertVentetid.verdi,
        veilederinfo.estimertVentetid.enhet,
      ),
    },
    {
      key: gjennomforingTekster.pamelding.label,
      value: gjennomforingTekster.pamelding.beskrivelse(gjennomforing.pameldingType),
    },
  ].filter((definition): definition is Definition => !!definition);

  const administratorMeta: Definition[] = [
    {
      key: gjennomforingTekster.administratorerForGjennomforingenLabel,
      value: gjennomforing.administratorer.length ? (
        <ul>
          {gjennomforing.administratorer.map((admin) => {
            return (
              <li key={admin.navIdent}>
                <Lenke to={`${NOM_ANSATT_SIDE}${admin.navIdent}`} isExternal>
                  {`${admin.navn} - ${admin.navIdent}`}{" "}
                </Lenke>
              </li>
            );
          })}
        </ul>
      ) : (
        gjennomforingTekster.ingenAdministratorerSattForGjennomforingenLabel
      ),
    },
  ];

  const { arrangor } = gjennomforing;
  const arrangorMeta: Definition[] = [
    ...(avtale?.arrangor
      ? [
          {
            key: gjennomforingTekster.tiltaksarrangorHovedenhetLabel,
            value: (
              <Link to={`/arrangorer/${avtale.arrangor.id}`}>
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
        <VStack justify={"space-between"}>
          <Definisjonsliste title="Gjennomføring" definitions={gjennomforingMeta} />
          <Separator />
          <Definisjonsliste title="Avtaledetaljer" definitions={avtaleMeta} />
          <Separator />
          <Definisjonsliste title="Varighet og påmelding" definitions={varighetMeta} />
          {utdanningslop && <UtdanningslopDetaljer utdanningslop={utdanningslop} />}
          {amoKategorisering && (
            <>
              <Separator />
              <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} />
            </>
          )}
          {prismodell && (
            <>
              <Separator />
              <Heading level="3" size="small" spacing>
                {avtaletekster.prismodell.heading}
              </Heading>
              <PrismodellDetaljer prismodell={[prismodell]} />
            </>
          )}
        </VStack>
        <VStack justify="space-between">
          <Definisjonsliste title="Administratorer" definitions={administratorMeta} />
          <Separator />
          <Definisjonsliste title="Arrangør" definitions={arrangorMeta} columns={1} />
          {veilederinfo?.oppmoteSted && (
            <>
              <Separator />
              <Definisjonsliste
                title="Sted"
                definitions={[
                  { key: gjennomforingTekster.oppmoteStedLabel, value: veilederinfo.oppmoteSted },
                ]}
                columns={1}
              />
            </>
          )}
          {gjennomforing.stengt.length !== 0 && (
            <StengtHosArrangorTable
              readOnly
              gjennomforingId={gjennomforing.id}
              stengt={gjennomforing.stengt}
            />
          )}
          {new Date() < new Date(gjennomforing.startDato) && (
            <TiltakTilgjengeligForArrangor gjennomforing={gjennomforing} />
          )}
        </VStack>
      </TwoColumnGrid>
      <Separator />
      <NokkeltallDeltakere gjennomforingId={gjennomforing.id} />
    </GjennomforingPageLayout>
  );
}

function HentTiltaksnummer({ id }: { id: string }) {
  const { isError, isLoading, data } = usePollTiltaksnummer(id);
  return isError ? (
    <Tag variant="error">Klarte ikke hente tiltaksnummer</Tag>
  ) : isLoading ? (
    <HStack align={"center"} gap="1">
      <Laster />
      <span>Henter tiltaksnummer i Arena</span>
    </HStack>
  ) : (
    data?.tiltaksnummer
  );
}

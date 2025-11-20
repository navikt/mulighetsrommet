import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { getDisplayName } from "@/api/enhet/helpers";
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
import { formatertVentetid, isKursTiltak } from "@/utils/Utils";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { HelpText, HStack, Tag, VStack } from "@navikt/ds-react";
import { Link } from "react-router";
import { GjennomforingPageLayout } from "./GjennomforingPageLayout";
import { GjennomforingOppstartstype } from "@tiltaksadministrasjon/api-client";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

export function GjennomforingDetaljer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);

  const {
    navn,
    tiltakstype,
    tiltaksnummer,
    startDato,
    sluttDato,
    oppstart,
    antallPlasser,
    deltidsprosent,
    apentForPamelding,
    administratorer,
    arenaAnsvarligEnhet,
    arrangor,
    stedForGjennomforing,
    oppmoteSted,
    amoKategorisering,
    utdanningslop,
  } = gjennomforing;

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
      value:
        avtale &&
        `${formaterDato(avtale.startDato)} -
                        ${formaterDato(avtale.sluttDato) ?? ""}`,
    },
  ];

  const gjennomforingMeta: Definition[] = [
    { key: gjennomforingTekster.tiltaksnavnLabel, value: navn },
    {
      key: gjennomforingTekster.tiltakstypeLabel,
      value: tiltakstype.navn,
    },
    { key: gjennomforingTekster.apentForPameldingLabel, value: apentForPamelding ? "Ja" : "Nei" },
    {
      key: gjennomforingTekster.tiltaksnummerLabel,
      value: tiltaksnummer ?? <HentTiltaksnummer id={gjennomforing.id} />,
    },
  ];

  const varighetMeta: Definition[] = [
    { key: gjennomforingTekster.startdatoLabel, value: formaterDato(startDato) },
    { key: gjennomforingTekster.sluttdatoLabel, value: formaterDato(sluttDato) ?? "" },
    {
      key: gjennomforingTekster.oppstartstypeLabel,
      value: oppstart === GjennomforingOppstartstype.FELLES ? "Felles" : "Løpende oppstart",
    },
    { key: gjennomforingTekster.antallPlasserLabel, value: antallPlasser },

    ...(isKursTiltak(tiltakstype.tiltakskode)
      ? [{ key: gjennomforingTekster.deltidsprosentLabel, value: deltidsprosent }]
      : []),
    ...(gjennomforing.estimertVentetid
      ? [
          {
            key: gjennomforingTekster.estimertVentetidLabel,
            value: formatertVentetid(
              gjennomforing.estimertVentetid.verdi,
              gjennomforing.estimertVentetid.enhet,
            ),
          },
        ]
      : []),
  ];
  const administratorMeta: Definition[] = [
    {
      key: gjennomforingTekster.administratorerForGjennomforingenLabel,
      value: administratorer.length ? (
        <ul>
          {administratorer.map((admin) => {
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
    {
      key: gjennomforingTekster.ansvarligEnhetFraArenaLabel,
      value: arenaAnsvarligEnhet ? (
        <HStack gap="2">
          {getDisplayName(arenaAnsvarligEnhet)}
          <HelpText title="Hva betyr feltet 'Ansvarlig enhet fra Arena'?">
            Ansvarlig enhet fra Arena blir satt i Arena basert på tiltaksansvarlig sin enhet når man
            oppretter tiltak i Arena.
          </HelpText>
        </HStack>
      ) : (
        "-"
      ),
    },
  ];
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

  const stedMeta: Definition[] = [
    { key: gjennomforingTekster.stedForGjennomforingLabel, value: stedForGjennomforing ?? "-" },
    { key: gjennomforingTekster.oppmoteStedLabel, value: oppmoteSted ?? "-" },
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
          {amoKategorisering && <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} />}
        </VStack>
        <VStack>
          <Definisjonsliste title="Administratorer" definitions={administratorMeta} />
          <Separator />
          <Definisjonsliste title="Arrangør" definitions={arrangorMeta} columns={1} />
          {(stedForGjennomforing || oppmoteSted) && (
            <>
              <Separator />
              <Definisjonsliste title="Sted" definitions={stedMeta} columns={1} />
            </>
          )}
          {gjennomforing.stengt.length !== 0 && (
            <StengtHosArrangorTable gjennomforing={gjennomforing} readOnly />
          )}
        </VStack>
        {new Date() < new Date(gjennomforing.startDato) && (
          <TiltakTilgjengeligForArrangor gjennomforing={gjennomforing} />
        )}
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

import { getDisplayName } from "@/api/enhet/helpers";
import { AmoKategoriseringDetaljer } from "@/components/amoKategorisering/AmoKategoriseringDetaljer";
import { RegistrerteOpsjoner } from "@/components/avtaler/opsjoner/RegistrerteOpsjoner";
import { hentOpsjonsmodell } from "@/components/avtaler/opsjoner/opsjonsmodeller";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { UtdanningslopDetaljer } from "@/components/utdanning/UtdanningslopDetaljer";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { avtaletypeTilTekst, formaterDato } from "@/utils/Utils";
import { AvtaleDto, Avtaletype, AvtaltSatsDto, Prismodell, Toggles } from "@mr/api-client-v2";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import {
  Definition,
  Definisjonsliste,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { Alert, Box, Heading, HelpText, HStack, VStack } from "@navikt/ds-react";
import { Link } from "react-router";
import { usePrismodeller } from "@/api/tilsagn/usePrismodeller";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";
import { formaterTall } from "@mr/frontend-common/utils/utils";

interface Props {
  avtale: AvtaleDto;
}

export function AvtaleDetaljer({ avtale }: Props) {
  const {
    navn,
    avtalenummer,
    tiltakstype,
    avtaletype,
    startDato,
    sluttDato,
    administratorer,
    sakarkivNummer,
    arenaAnsvarligEnhet,
    arrangor,
    amoKategorisering,
    utdanningslop,
    opsjonsmodell,
  } = avtale;
  const { data: prismodeller } = usePrismodeller(avtale.tiltakstype.tiltakskode);

  const prismodell = prismodeller.find(({ type }) => type === avtale.prismodell) ?? {
    type: avtale.prismodell,
    beskrivelse: avtale.prismodell,
  };

  const { data: enableTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN,
    avtale?.tiltakstype.tiltakskode ? [avtale.tiltakstype.tiltakskode] : [],
  );

  const avtaleMeta: Definition[] = [
    { key: avtaletekster.avtalenavnLabel, value: navn },
    { key: avtaletekster.avtaletypeLabel, value: avtaletypeTilTekst(avtaletype) },
    { key: avtaletekster.avtalenummerLabel, value: avtalenummer },
    { key: avtaletekster.sakarkivNummerLabel, value: sakarkivNummer ?? "-" },
  ];

  const tiltakMeta: Definition[] = [
    {
      key: avtaletekster.tiltakstypeLabel,
      value: <Link to={`/tiltakstyper/${tiltakstype.id}`}>{tiltakstype.navn}</Link>,
    },
    { key: avtaletekster.prismodell.label, value: prismodell.beskrivelse },
  ];

  const varighet: Definition[] = [
    { key: avtaletekster.startdatoLabel, value: formaterDato(startDato) },
    {
      key: avtaletekster.sluttdatoLabel(avtale.opsjonerRegistrert.length > 0),
      value: sluttDato ? formaterDato(sluttDato) : "-",
    },
    ...(avtale.avtaletype !== Avtaletype.FORHANDSGODKJENT
      ? [
          {
            key: avtaletekster.avtaltForlengelseLabel,
            value:
              opsjonsmodell.customOpsjonsmodellNavn ?? hentOpsjonsmodell(opsjonsmodell.type)?.label,
          },
        ]
      : []),
    ...(opsjonsmodell.opsjonMaksVarighet
      ? [
          {
            key: avtaletekster.maksVarighetLabel,
            value: formaterDato(opsjonsmodell.opsjonMaksVarighet),
          },
        ]
      : []),
  ];

  const administratorMeta: Definition[] = [
    {
      key: avtaletekster.administratorerForAvtalenLabel,
      value: administratorer?.length ? (
        <ul>
          <VStack gap="2">
            {administratorer.map((admin) => {
              return (
                <li key={admin.navIdent}>
                  <Lenke to={`${NOM_ANSATT_SIDE}${admin.navIdent}`} isExternal>
                    {`${admin.navn} - ${admin.navIdent}`}{" "}
                  </Lenke>
                </li>
              );
            })}
          </VStack>
        </ul>
      ) : (
        avtaletekster.ingenAdministratorerSattLabel
      ),
    },
    {
      key: avtaletekster.ansvarligEnhetFraArenaLabel,
      value: arenaAnsvarligEnhet ? (
        <HStack gap="2">
          {getDisplayName(arenaAnsvarligEnhet)}
          <HelpText title="Hva betyr feltet 'Ansvarlig enhet fra Arena'?">
            Ansvarlig enhet fra Arena blir satt i Arena basert på tiltaksansvarlig sin enhet når det
            opprettes avtale i Arena.
          </HelpText>
        </HStack>
      ) : (
        "-"
      ),
    },
  ];

  const arrangorMeta: Definition[] = [
    {
      key: avtaletekster.tiltaksarrangorHovedenhetLabel,
      value: (
        <Link to={`/arrangorer/${arrangor?.id}`}>
          {arrangor?.navn} - {arrangor?.organisasjonsnummer}
        </Link>
      ),
    },
    {
      key: avtaletekster.tiltaksarrangorUnderenheterLabel,
      value: (
        <ul>
          <VStack gap="2">
            {arrangor?.underenheter.map((enhet) => (
              <li key={enhet.organisasjonsnummer}>
                {`${enhet.navn} - ${enhet.organisasjonsnummer}`}
              </li>
            ))}
          </VStack>
        </ul>
      ),
    },
    {
      key: avtaletekster.kontaktpersonerHosTiltaksarrangorLabel,
      value:
        arrangor && arrangor.kontaktpersoner.length > 0
          ? arrangor.kontaktpersoner.map((kontaktperson) => (
              <ArrangorKontaktpersonDetaljer key={kontaktperson.id} kontaktperson={kontaktperson} />
            ))
          : "-",
    },
  ];

  return (
    <TwoColumnGrid separator>
      <VStack>
        <Definisjonsliste title="Avtaleinformasjon" definitions={avtaleMeta} />
        <Separator />
        <Definisjonsliste title="Tiltak" definitions={tiltakMeta} />
        <Separator />
        {enableTilsagn ? <PrismodellDetaljer avtale={avtale} /> : avtale.prisbetingelser}
        <Separator />
        {amoKategorisering && (
          <>
            <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} />
            <Separator />
          </>
        )}
        {utdanningslop ? <UtdanningslopDetaljer utdanningslop={utdanningslop} /> : null}
        <Definisjonsliste title="Avtalens varighet" definitions={varighet} />
        {avtale.opsjonerRegistrert.length > 0 ? (
          <RegistrerteOpsjoner readOnly avtale={avtale} />
        ) : null}
      </VStack>
      <VStack>
        {administratorer && (
          <Definisjonsliste title="Administratorer" definitions={administratorMeta} />
        )}
        <Separator />
        {arrangor ? (
          <Definisjonsliste title="Arrangør" definitions={arrangorMeta} columns={1} />
        ) : (
          <Alert variant="warning">{avtaletekster.arrangorManglerVarsel}</Alert>
        )}
        <Separator />
      </VStack>
    </TwoColumnGrid>
  );
}

export function AvtalteSatser({ satser }: { satser: AvtaltSatsDto[] }) {
  return (
    <Box>
      <Heading level="3" size="small" spacing>
        {avtaletekster.avtaltPrisLabel}
      </Heading>
      {satser.map((sats) => (
        <HStack
          gap="4"
          padding="4"
          key={sats.periodeStart}
          className="border-bg-subtle border-1 rounded-md"
        >
          <Metadata header={avtaletekster.prismodell.valuta.label} verdi={sats.valuta} />
          <Metadata header={avtaletekster.prismodell.pris.label} verdi={formaterTall(sats.pris)} />
          <Metadata
            header={avtaletekster.prismodell.periodeStart.label}
            verdi={formaterDato(sats.periodeStart)}
          />
          <Metadata
            header={avtaletekster.prismodell.periodeSlutt.label}
            verdi={formaterDato(sats.periodeSlutt)}
          />
        </HStack>
      ))}
    </Box>
  );
}

export function PrismodellDetaljer({ avtale }: { avtale: AvtaleDto }) {
  const { data: satser } = useForhandsgodkjenteSatser(avtale.tiltakstype.tiltakskode);

  switch (avtale.prismodell) {
    case Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
      return <AvtalteSatser satser={satser} />;

    case Prismodell.AVTALT_PRIS_PER_MANEDSVERK:
    case Prismodell.AVTALT_PRIS_PER_UKESVERK:
      return <AvtalteSatser satser={avtale.satser} />;

    case Prismodell.ANNEN_AVTALT_PRIS:
      return (
        <Definisjonsliste
          title={avtaletekster.avtaltPrisLabel}
          definitions={[
            {
              key: avtaletekster.prisOgBetalingLabel,
              value: avtale.prisbetingelser ?? "-",
            },
          ]}
        />
      );
  }
}

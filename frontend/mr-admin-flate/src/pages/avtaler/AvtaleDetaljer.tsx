import { getDisplayName } from "@/api/enhet/helpers";
import { AmoKategoriseringDetaljer } from "@/components/amoKategorisering/AmoKategoriseringDetaljer";
import { RegistrerteOpsjoner } from "@/components/avtaler/opsjoner/RegistrerteOpsjoner";
import { hentOpsjonsmodell } from "@/components/avtaler/opsjoner/opsjonsmodeller";
import { Separator } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { UtdanningslopDetaljer } from "@/components/utdanning/UtdanningslopDetaljer";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { avtaletypeTilTekst } from "@/utils/Utils";
import { Avtaletype } from "@tiltaksadministrasjon/api-client";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { Alert, HelpText, HStack, VStack } from "@navikt/ds-react";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { Link } from "react-router";
import { AvtalePrismodell } from "@/components/avtaler/AvtalePrismodell";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";

export function AvtaleDetaljer() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);
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

  const avtaleMeta: Definition[] = [
    { key: avtaletekster.avtalenavnLabel, value: navn },
    { key: avtaletekster.avtalenummerLabel, value: avtalenummer },
    { key: avtaletekster.sakarkivNummerLabel, value: sakarkivNummer ?? "-" },
  ];

  const tiltakMeta: Definition[] = [
    {
      key: avtaletekster.tiltakstypeLabel,
      value: <Link to={`/tiltakstyper/${tiltakstype.id}`}>{tiltakstype.navn}</Link>,
    },
    { key: avtaletekster.avtaletypeLabel, value: avtaletypeTilTekst(avtaletype) },
  ];

  const varighet: Definition[] = [
    { key: avtaletekster.startdatoLabel, value: formaterDato(startDato) },
    {
      key: avtaletekster.sluttdatoLabel(avtaletype, avtale.opsjonerRegistrert.length > 0),
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
          {arrangor?.underenheter.map((enhet) => (
            <li key={enhet.organisasjonsnummer}>
              {`${enhet.navn} - ${enhet.organisasjonsnummer}`}
            </li>
          ))}
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
        {amoKategorisering && (
          <>
            <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} />
            <Separator />
          </>
        )}
        {utdanningslop ? <UtdanningslopDetaljer utdanningslop={utdanningslop} /> : null}
        <Definisjonsliste title="Avtalens varighet" definitions={varighet} />
        {avtale.opsjonerRegistrert.length > 0 ? <RegistrerteOpsjoner readOnly /> : null}
        <Separator />
        <AvtalePrismodell avtale={avtale} />
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
      </VStack>
    </TwoColumnGrid>
  );
}

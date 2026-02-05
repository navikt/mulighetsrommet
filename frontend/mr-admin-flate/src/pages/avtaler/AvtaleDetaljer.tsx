import { AmoKategoriseringDetaljer } from "@/components/amoKategorisering/AmoKategoriseringDetaljer";
import { RegistrerteOpsjoner } from "@/components/avtaler/opsjoner/RegistrerteOpsjoner";
import { hentOpsjonsmodell } from "@/components/avtaler/opsjoner/opsjonsmodeller";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { UtdanningslopDetaljer } from "@/components/utdanning/UtdanningslopDetaljer";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { avtaletypeTilTekst } from "@/utils/Utils";
import { Avtaletype, RammedetaljerDto } from "@tiltaksadministrasjon/api-client";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { NOM_ANSATT_SIDE } from "@mr/frontend-common/constants";
import { Alert, Heading, VStack } from "@navikt/ds-react";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { Link } from "react-router";
import { PrismodellDetaljer } from "@/components/avtaler/PrismodellDetaljer";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { MetadataHStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { useAvtaleRammedetaljer } from "@/api/avtaler/useAvtaleRammedetaljer";
import { formaterValuta, formaterValutaBelop } from "@mr/frontend-common/utils/utils";

export function AvtaleDetaljer() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);
  const { data: rammedetaljer } = useAvtaleRammedetaljer(avtaleId);
  const {
    navn,
    avtalenummer,
    tiltakstype,
    avtaletype,
    startDato,
    sluttDato,
    administratorer,
    sakarkivNummer,
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
        avtaletekster.ingenAdministratorerSattLabel
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
        <Definisjonsliste title="Avtalens varighet" definitions={varighet} />
        {avtale.opsjonerRegistrert.length > 0 ? <RegistrerteOpsjoner readOnly /> : null}
        {amoKategorisering && (
          <>
            <Separator />
            <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} />
          </>
        )}
        {utdanningslop ? <UtdanningslopDetaljer utdanningslop={utdanningslop} /> : null}
        <Separator />
        <Heading level="3" size="small" spacing>
          {avtaletekster.prismodell.heading}
        </Heading>
        <PrismodellDetaljer prismodell={avtale.prismodeller} />
        <Separator />
        <Heading level="3" size="small" spacing>
          {avtaletekster.rammedetaljer.heading}
        </Heading>
        <Rammedetaljer rammedetaljer={rammedetaljer} />
      </VStack>
      <VStack>
        <Definisjonsliste title="Administratorer" definitions={administratorMeta} />
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

interface RammedetaljerProps {
  rammedetaljer: RammedetaljerDto | null;
}
function Rammedetaljer({ rammedetaljer }: RammedetaljerProps) {
  if (!rammedetaljer?.totalRamme) {
    return null;
  }
  return (
    <VStack gap="2" width="30rem">
      <MetadataHStack label="Total ramme" value={formaterValutaBelop(rammedetaljer.totalRamme)} />
      {rammedetaljer.utbetaltArena && (
        <MetadataHStack
          label="Utbetalt fra Arena"
          value={formaterValutaBelop(rammedetaljer.utbetaltArena)}
        />
      )}
      {rammedetaljer.utbetaltTiltaksadmin.map((utbetalt) => (
        <MetadataHStack
          key={utbetalt.valuta}
          label="Utbetalt fra Tiltaksadministrasjon"
          value={formaterValutaBelop(utbetalt)}
        />
      ))}
      <MetadataHStack
        label="Gjenstående"
        value={formaterValutaBelop(rammedetaljer.gjenstaendeRamme)}
      />
    </VStack>
  );
}

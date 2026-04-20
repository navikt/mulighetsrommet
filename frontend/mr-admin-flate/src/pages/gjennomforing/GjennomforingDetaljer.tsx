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
import {
  BodyShort,
  Heading,
  HelpText,
  HStack,
  InfoCard,
  Link,
  Tag,
  VStack,
} from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";
import { GjennomforingPageLayout } from "./GjennomforingPageLayout";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import {
  MetadataFritekstfelt,
  Separator,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { isEnkeltplass, isGruppetiltak } from "@/api/gjennomforing/utils";
import { GjennomforingDetaljerAvtale } from "@/pages/gjennomforing/GjennomforingDetaljerAvtale";
import { GjennomforingDetaljerVarighet } from "@/pages/gjennomforing/GjennomforingDetaljerVarighet";
import { GjennomforingDetaljerAdministratorer } from "@/pages/gjennomforing/GjennomforingDetaljerAdministratorer";
import { DetaljerLayout } from "@/components/detaljside/DetaljerLayout";
import {
  Besluttelse,
  GjennomforingAvtaleDto,
  TotrinnskontrollDto,
} from "@tiltaksadministrasjon/api-client";
import { PrismodellDetaljer } from "@/components/avtaler/PrismodellDetaljer";
import { kursOgTiltakErStudiespesialisering } from "@/utils/Utils";
import { isBesluttet, isTilBeslutning } from "@/utils/totrinnskontroll";
import { formaterDato } from "@mr/frontend-common/utils/date";

export function GjennomforingDetaljer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const detaljer = useGjennomforing(gjennomforingId);
  const { gjennomforing, veilederinfo, utdanningslop, amoKategorisering, prismodell, okonomi } =
    detaljer;
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);
  const { data: avtale } = usePotentialAvtale(
    isGruppetiltak(gjennomforing) ? gjennomforing.avtaleId : null,
  );

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
    ...(isEnkeltplass(gjennomforing)
      ? [
          {
            key: gjennomforingTekster.ansvarligEnhet.label,
            value: `${gjennomforing.ansvarligEnhet.navn} (${gjennomforing.ansvarligEnhet.enhetsnummer})`,
          },
        ]
      : []),
  ];

  const { arrangor } = gjennomforing;
  const arrangorMeta: Definition[] = [
    ...(avtale?.arrangor
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
          {avtale && <GjennomforingDetaljerAvtale avtale={avtale} />}
          <GjennomforingDetaljerVarighet
            tiltakstype={tiltakstype}
            gjennomforing={gjennomforing}
            veilederinfo={veilederinfo}
          />
          {utdanningslop && <UtdanningslopDetaljer utdanningslop={utdanningslop} />}
          {amoKategorisering &&
            !kursOgTiltakErStudiespesialisering(
              amoKategorisering.kurstype,
              tiltakstype.tiltakskode,
            ) && <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} />}
          <PrismodellDetaljer prismodeller={[prismodell]} />
        </DetaljerLayout>
        <DetaljerLayout>
          {isGruppetiltak(gjennomforing) && (
            <GjennomforingDetaljerAdministratorer gjennomforing={gjennomforing} />
          )}
          <Definisjonsliste title="Arrangør" definitions={arrangorMeta} columns={1} />
          {veilederinfo?.oppmoteSted && (
            <Definisjonsliste
              title="Sted"
              definitions={[
                { key: gjennomforingTekster.oppmoteStedLabel, value: veilederinfo.oppmoteSted },
              ]}
              columns={1}
            />
          )}
          {isGruppetiltak(gjennomforing) && gjennomforing.stengt.length !== 0 && (
            <StengtHosArrangorTable readOnly gjennomforing={gjennomforing} />
          )}
          {isGruppetiltak(gjennomforing) && !harStartet(gjennomforing) && (
            <TiltakTilgjengeligForArrangor gjennomforing={gjennomforing} />
          )}
          {isEnkeltplass(gjennomforing) && okonomi && <OkonomiStatus okonomi={okonomi} />}
        </DetaljerLayout>
      </TwoColumnGrid>
      <Separator />
      {isGruppetiltak(gjennomforing) && <NokkeltallDeltakere gjennomforingId={gjennomforing.id} />}
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

function OkonomiStatus({ okonomi }: { okonomi: TotrinnskontrollDto }) {
  if (isBesluttet(okonomi) && okonomi.besluttelse === Besluttelse.GODKJENT) {
    return (
      <InfoCard data-color="success">
        <InfoCard.Header>
          <InfoCard.Title>Økonomi godkjent</InfoCard.Title>
        </InfoCard.Header>
        <InfoCard.Content>
          <BodyShort>
            {okonomi.besluttetAv.navn} godkjente økonomi den{" "}
            {formaterDato(okonomi.besluttetTidspunkt)}.
          </BodyShort>
        </InfoCard.Content>
      </InfoCard>
    );
  }

  if (isBesluttet(okonomi) && okonomi.besluttelse === Besluttelse.AVVIST) {
    return (
      <InfoCard data-color="danger">
        <InfoCard.Header>
          <InfoCard.Title>Økonomi avslått</InfoCard.Title>
        </InfoCard.Header>
        <InfoCard.Content>
          <BodyShort spacing>
            {okonomi.besluttetAv.navn} avslo økonomi den {formaterDato(okonomi.besluttetTidspunkt)}.
          </BodyShort>
          {okonomi.forklaring && (
            <MetadataFritekstfelt label="Forklaring" value={okonomi.forklaring} />
          )}
        </InfoCard.Content>
      </InfoCard>
    );
  }

  if (isTilBeslutning(okonomi)) {
    return (
      <InfoCard data-color="info">
        <InfoCard.Header>
          <InfoCard.Title>Økonomi venter på godkjenning</InfoCard.Title>
        </InfoCard.Header>
        <InfoCard.Content>
          <BodyShort>
            {okonomi.behandletAv.navn} sendte gjennomføringen til godkjenning den{" "}
            {formaterDato(okonomi.behandletTidspunkt)}.
          </BodyShort>
        </InfoCard.Content>
      </InfoCard>
    );
  }

  throw Error("Unhåndtert status");
}

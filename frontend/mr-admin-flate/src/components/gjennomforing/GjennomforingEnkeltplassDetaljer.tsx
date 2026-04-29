import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { usePollTiltaksnummer } from "@/api/gjennomforing/usePollTiltaksnummer";
import { GodkjennOkonomiModal } from "@/components/gjennomforing/GodkjennOkonomiModal";
import { SettPaVentOkonomiModal } from "@/components/gjennomforing/SettPaVentOkonomiModal";
import { Laster } from "@/components/laster/Laster";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import {
  BodyShort,
  Button,
  Heading,
  HelpText,
  HStack,
  InfoCard,
  Tag,
  VStack,
} from "@navikt/ds-react";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import {
  MetadataFritekstfelt,
  Separator,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { GjennomforingDetaljerVarighet } from "@/pages/gjennomforing/GjennomforingDetaljerVarighet";
import { DetaljerLayout } from "@/components/detaljside/DetaljerLayout";
import {
  Besluttelse,
  DeltakerDto,
  GjennomforingEnkeltplassDto,
  GjennomforingHandling,
  GjennomforingVeilederinfoDto,
  PrismodellDto,
  TiltakstypeDto,
  TotrinnskontrollDto,
} from "@tiltaksadministrasjon/api-client";
import { isBesluttet, isTilBeslutning } from "@/utils/totrinnskontroll";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { DeltakerinformasjonOgBetalingsbetingelser } from "@/components/tilskudd-behandling/DeltakerinformasjonOgBetalingsbetingelser";
import { ReactNode, useState } from "react";
import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";

interface Props {
  tiltakstype: TiltakstypeDto;
  gjennomforing: GjennomforingEnkeltplassDto;
  veilederinfo: null | GjennomforingVeilederinfoDto;
  prismodell: PrismodellDto;
  okonomi: null | TotrinnskontrollDto;
  enkeltplassDeltaker: null | DeltakerDto;
}

export function GjennomforingEnkeltplassDetaljer(props: Props) {
  const { tiltakstype, gjennomforing, veilederinfo, prismodell, enkeltplassDeltaker, okonomi } =
    props;
  const handlinger = useGjennomforingHandlinger(gjennomforing.id);

  const gjennomforingMeta: Definition[] = [
    {
      key: gjennomforingTekster.tiltakstypeLabel,
      value: tiltakstype.navn,
    },
    {
      key: gjennomforingTekster.innholdAnnet.label,
      value: enkeltplassDeltaker?.innholdAnnet,
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
    {
      key: gjennomforingTekster.ansvarligEnhet.label,
      value: `${gjennomforing.ansvarligEnhet.navn} (${gjennomforing.ansvarligEnhet.enhetsnummer})`,
    },
  ];

  const { arrangor } = gjennomforing;
  const arrangorMeta: Definition[] = [
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
          <GjennomforingDetaljerVarighet
            tiltakstype={tiltakstype}
            gjennomforing={gjennomforing}
            veilederinfo={veilederinfo}
          />
        </DetaljerLayout>
        <DetaljerLayout>
          <Definisjonsliste title="Arrangør" definitions={arrangorMeta} columns={1} />
          {okonomi && (
            <OkonomiStatus
              okonomi={okonomi}
              gjennomforingId={gjennomforing.id}
              handlinger={handlinger}
            />
          )}
          {enkeltplassDeltaker && (
            <DeltakerinformasjonOgBetalingsbetingelser
              deltaker={enkeltplassDeltaker}
              prisbetingelser={prismodell.prisbetingelser}
            />
          )}
        </DetaljerLayout>
      </TwoColumnGrid>
      <Separator />
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

function OkonomiStatus({
  okonomi,
  gjennomforingId,
  handlinger,
}: {
  okonomi: TotrinnskontrollDto;
  gjennomforingId: string;
  handlinger: GjennomforingHandling[];
}) {
  const [godkjennOpen, setGodkjennOpen] = useState(false);
  const [settPaVentOpen, setSettPaVentOpen] = useState(false);

  const kanGodkjenne = handlinger.includes(GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI);
  const kanSettePaVent = handlinger.includes(
    GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_OKONOMI,
  );

  const card = resolveCard(okonomi);

  return (
    <>
      <InfoCard data-color={card.color}>
        <InfoCard.Header>
          <InfoCard.Title>{card.title}</InfoCard.Title>
        </InfoCard.Header>
        <InfoCard.Content>
          {card.body}
          {(kanSettePaVent || kanGodkjenne) && (
            <HStack gap="space-4">
              {kanSettePaVent && (
                <Button size="small" variant="secondary" onClick={() => setSettPaVentOpen(true)}>
                  Sett på vent
                </Button>
              )}
              {kanGodkjenne && (
                <Button size="small" onClick={() => setGodkjennOpen(true)}>
                  Godkjenn enkeltplass
                </Button>
              )}
            </HStack>
          )}
        </InfoCard.Content>
      </InfoCard>
      <GodkjennOkonomiModal
        open={godkjennOpen}
        setOpen={setGodkjennOpen}
        gjennomforingId={gjennomforingId}
      />
      <SettPaVentOkonomiModal
        open={settPaVentOpen}
        setOpen={setSettPaVentOpen}
        gjennomforingId={gjennomforingId}
      />
    </>
  );
}

interface CardData {
  color: "success" | "warning" | "info";
  title: string;
  body: ReactNode;
}

function resolveCard(okonomi: TotrinnskontrollDto): CardData {
  if (isBesluttet(okonomi) && okonomi.besluttelse === Besluttelse.GODKJENT) {
    return {
      color: "success",
      title: "Økonomi godkjent",
      body: (
        <BodyShort>
          {okonomi.besluttetAv.navn} godkjente økonomi den{" "}
          {formaterDato(okonomi.besluttetTidspunkt)}.
        </BodyShort>
      ),
    };
  }

  if (isBesluttet(okonomi) && okonomi.besluttelse === Besluttelse.AVVIST) {
    return {
      color: "warning",
      title: "Enkeltplass satt på vent",
      body: (
        <>
          <BodyShort spacing>
            {okonomi.besluttetAv.navn} satte godkjenning av enkeltplass på vent den{" "}
            {formaterDato(okonomi.besluttetTidspunkt)}.
          </BodyShort>
          {okonomi.forklaring && (
            <MetadataFritekstfelt label="Forklaring" value={okonomi.forklaring} />
          )}
        </>
      ),
    };
  }

  if (isTilBeslutning(okonomi)) {
    return {
      color: "info",
      title: "Enkeltplass venter på godkjenning",
      body: (
        <BodyShort spacing>
          {okonomi.behandletAv.navn} sendte gjennomføringen til godkjenning den{" "}
          {formaterDato(okonomi.behandletTidspunkt)}.
        </BodyShort>
      ),
    };
  }

  throw Error("Uhåndtert status");
}

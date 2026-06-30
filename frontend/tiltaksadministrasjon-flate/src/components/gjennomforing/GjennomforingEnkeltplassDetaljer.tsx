import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { GodkjennOkonomiModal } from "@/components/gjennomforing/GodkjennOkonomiModal";
import { SettPaVentOkonomiModal } from "@/components/gjennomforing/SettPaVentOkonomiModal";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { ArrangorKontaktpersonDetaljer } from "@/pages/arrangor/ArrangorKontaktpersonDetaljer";
import { BodyShort, Button, Heading, HelpText, HStack, InfoCard, VStack } from "@navikt/ds-react";
import {
  Definisjonsliste,
  Definition,
} from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import {
  MetadataFritekstfelt,
  Separator,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { DetaljerLayout } from "@/components/detaljside/DetaljerLayout";
import {
  AmoKategoriseringDto,
  DeltakerDto,
  GjennomforingEnkeltplassDto,
  GjennomforingHandling,
  GjennomforingVeilederinfoDto,
  PrismodellDto,
  TiltakstypeDto,
  TotrinnskontrollDto,
  TotrinnskontrollDtoBesluttet,
  UtdanningslopDto,
} from "@tiltaksadministrasjon/api-client";
import { erSattPaVent } from "@/utils/totrinnskontroll";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { useState } from "react";
import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";
import { UtdanningslopDetaljer } from "../utdanning/UtdanningslopDetaljer";
import { kursOgTiltakErStudiespesialisering } from "@/utils/Utils";
import { AmoKategoriseringDetaljer } from "../amoKategorisering/AmoKategoriseringDetaljer";
import { BetalingsbetingelserEnkeltplass } from "./BetalingsbetingelserEnkeltplass";
import { GjennomforingEnkeltplassVarighet } from "@/pages/gjennomforing/GjennomforingEnkeltplassVarighet";
import { formaterNavEnhet } from "@/utils/nav-enhet";

interface Props {
  tiltakstype: TiltakstypeDto;
  gjennomforing: GjennomforingEnkeltplassDto;
  veilederinfo: null | GjennomforingVeilederinfoDto;
  prismodell: PrismodellDto;
  okonomi: null | TotrinnskontrollDto;
  enkeltplassDeltaker: null | DeltakerDto;
  amoKategorisering: null | AmoKategoriseringDto;
  utdanningslop: null | UtdanningslopDto;
}

export function GjennomforingEnkeltplassDetaljer(props: Props) {
  const {
    tiltakstype,
    gjennomforing,
    prismodell,
    enkeltplassDeltaker,
    okonomi,
    amoKategorisering,
    utdanningslop,
  } = props;
  const handlinger = useGjennomforingHandlinger(gjennomforing.id);
  const [godkjennOpen, setGodkjennOpen] = useState(false);
  const [settPaVentOpen, setSettPaVentOpen] = useState(false);

  const kanGodkjenne = handlinger.includes(GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI);
  const kanSettePaVent = handlinger.includes(
    GjennomforingHandling.SETT_PA_VENT_ENKELTPLASS_OKONOMI,
  );

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
      value: formaterNavEnhet(gjennomforing.ansvarligEnhet),
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
    <VStack>
      <GjennomforingPageLayout>
        <TwoColumnGrid separator>
          <DetaljerLayout>
            <Definisjonsliste title="Gjennomføring" definitions={gjennomforingMeta} />
            {utdanningslop && <UtdanningslopDetaljer utdanningslop={utdanningslop} />}
            {amoKategorisering &&
              !kursOgTiltakErStudiespesialisering(
                amoKategorisering.kurstype,
                tiltakstype.tiltakskode,
              ) && (
                <AmoKategoriseringDetaljer amoKategorisering={amoKategorisering} erEnkeltplass />
              )}
            <GjennomforingEnkeltplassVarighet gjennomforing={gjennomforing} />
          </DetaljerLayout>
          <DetaljerLayout>
            <Definisjonsliste title="Arrangør" definitions={arrangorMeta} columns={1} />
            {enkeltplassDeltaker && <BetalingsbetingelserEnkeltplass prismodell={prismodell} />}
            {erSattPaVent(okonomi) && <OkonomiStatusSattPaVent okonomi={okonomi} />}
          </DetaljerLayout>
        </TwoColumnGrid>
        <Separator />
      </GjennomforingPageLayout>
      <HStack align="center" justify="end" gap="space-8">
        {kanSettePaVent && (
          <>
            <BodyShort>
              Hvis økonomien ikke skal godkjennes, må du gi Nav-veileder beskjed i andre kanaler.
            </BodyShort>

            <Button size="small" variant="secondary" onClick={() => setSettPaVentOpen(true)}>
              Sett på vent
            </Button>
          </>
        )}
        {kanGodkjenne && (
          <Button size="small" onClick={() => setGodkjennOpen(true)}>
            Godkjenn enkeltplass
          </Button>
        )}
      </HStack>
      <GodkjennOkonomiModal
        open={godkjennOpen}
        setOpen={setGodkjennOpen}
        gjennomforingId={gjennomforing.id}
        prismodell={prismodell}
      />
      <SettPaVentOkonomiModal
        open={settPaVentOpen}
        setOpen={setSettPaVentOpen}
        gjennomforingId={gjennomforing.id}
      />
    </VStack>
  );
}

function OkonomiStatusSattPaVent({ okonomi }: { okonomi: TotrinnskontrollDtoBesluttet }) {
  return (
    <InfoCard data-color="warning">
      <InfoCard.Header>
        <InfoCard.Title>Enkeltplass satt på vent</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content>
        <BodyShort spacing>
          {okonomi.besluttetAv.navn} satte godkjenning av enkeltplass på vent den{" "}
          {formaterDato(okonomi.besluttetTidspunkt)}.
        </BodyShort>
        {okonomi.forklaring && (
          <MetadataFritekstfelt label="Forklaring" value={okonomi.forklaring} />
        )}
      </InfoCard.Content>
    </InfoCard>
  );
}

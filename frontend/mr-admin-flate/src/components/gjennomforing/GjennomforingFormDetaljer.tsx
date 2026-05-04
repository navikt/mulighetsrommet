import { GjennomforingAmoKategoriseringForm } from "@/components/amoKategorisering/GjennomforingAmoKategoriseringForm";
import { FormGroup } from "@/layouts/FormGroup";
import { SkjemaKolonne } from "@/layouts/SkjemaKolonne";
import {
  AvtaleDto,
  GjennomforingAvtaleDto,
  GjennomforingDeltakerSummary,
  GjennomforingOppstartstype,
  GjennomforingPameldingType,
  Rolle,
  Tiltakskode,
  TiltakstypeDto,
} from "@tiltaksadministrasjon/api-client";
import { Alert, DatePicker, HGrid, TextField, VStack } from "@navikt/ds-react";
import { ChangeEvent, useEffect, useRef } from "react";
import { useFormContext } from "react-hook-form";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { EndreDatoAdvarselModal } from "@/components/modal/EndreDatoAdvarselModal";
import { administratorOptions } from "@/components/skjema/administratorOptions";
import { GjennomforingUtdanningslopForm } from "@/components/utdanning/GjennomforingUtdanningslopForm";
import { GjennomforingArrangorForm } from "./GjennomforingArrangorForm";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { addDuration, formaterDato } from "@mr/frontend-common/utils/date";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { OPPMOTE_STED_MAX_LENGTH } from "@/constants";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { PrismodellDetaljer } from "../avtaler/PrismodellDetaljer";
import { kreverDeltidsprosent, kreverDirekteVedtak } from "@/utils/tiltakstype";
import { useNavAnsatte } from "@/api/ansatt/useNavAnsatte";
import { GjennomforingFormValues } from "@/pages/gjennomforing/form/validation";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormCombobox } from "@/components/skjema/FormCombobox";
import { NumberInput } from "@/components/skjema/NumberInput";

interface Props {
  tiltakstype: TiltakstypeDto;
  avtale: AvtaleDto;
  gjennomforing: GjennomforingAvtaleDto | null;
  deltakere: GjennomforingDeltakerSummary | null;
}

export function GjennomforingFormDetaljer(props: Props) {
  const { tiltakstype, avtale, gjennomforing, deltakere } = props;

  const { data: navAnsatte } = useNavAnsatte([Rolle.TILTAKSGJENNOMFORINGER_SKRIV]);

  const endreSluttDatoModalRef = useRef<HTMLDialogElement>(null);

  const { setValue, watch } = useFormContext<GjennomforingFormValues>();

  const watchStartDato = watch("startDato");

  useEffect(() => {
    if (watchStartDato && new Date(watchStartDato) < new Date()) {
      setValue("tilgjengeligForArrangorDato", null);
    }
  }, [setValue, watchStartDato]);

  const watchSluttDato = watch("sluttDato");
  const valgtPrismodell = avtale.prismodeller.find((p) => p.id === watch("prismodellId"));
  const antallDeltakere = deltakere?.antallDeltakere ?? 0;

  function visAdvarselForSluttDato(sluttDato: string | null) {
    if (!gjennomforing || antallDeltakere === 0 || !sluttDato) {
      return;
    }

    const shouldDisplayWarning = !gjennomforing.sluttDato || sluttDato < gjennomforing.sluttDato;
    if (shouldDisplayWarning) {
      endreSluttDatoModalRef.current?.showModal();
    }
  }

  const minStartdato = new Date(avtale.startDato);
  const maxStartdato = addDuration(new Date(), { years: 2 });
  const maxSluttdato =
    addDuration(gjennomforing?.sluttDato, { years: 6 }) ?? addDuration(maxStartdato, { years: 6 });

  const visOppmotested = tiltakstype.tiltakskode !== Tiltakskode.TILPASSET_JOBBSTOTTE;

  return (
    <>
      <TwoColumnGrid separator>
        <SkjemaKolonne>
          <FormGroup>
            <FormTextField<GjennomforingFormValues>
              name="navn"
              label={gjennomforingTekster.tiltaksnavnLabel}
              autoFocus
            />
            {gjennomforing?.tiltaksnummer ? (
              <TextField
                size="small"
                readOnly
                label={gjennomforingTekster.tiltaksnummerLabel}
                autoFocus
                value={gjennomforing.tiltaksnummer}
              />
            ) : null}
          </FormGroup>

          <FormGroup>
            <TextField
              size="small"
              readOnly
              label={gjennomforingTekster.avtaleMedTiltakstype(avtale.tiltakstype.navn)}
              value={avtale.navn || ""}
            />
            <GjennomforingAmoKategoriseringForm avtale={avtale} />
            <GjennomforingUtdanningslopForm avtale={avtale} />
          </FormGroup>

          <FormGroup>
            <ControlledSokeSelect
              size="small"
              label="Oppstartstype"
              placeholder="Velg oppstart"
              name="oppstart"
              readOnly={kreverDirekteVedtak(tiltakstype)}
              onChange={(e) => {
                if (e.target.value === GjennomforingOppstartstype.FELLES) {
                  setValue("pameldingType", GjennomforingPameldingType.TRENGER_GODKJENNING);
                } else {
                  setValue("pameldingType", GjennomforingPameldingType.DIREKTE_VEDTAK);
                }
              }}
              options={[
                {
                  label: "Felles oppstartsdato",
                  value: GjennomforingOppstartstype.FELLES,
                },
                {
                  label: "Løpende oppstart",
                  value: GjennomforingOppstartstype.LOPENDE,
                },
              ]}
            />
            <ControlledSokeSelect
              size="small"
              label={gjennomforingTekster.pamelding.label}
              placeholder="Velg påmeldingstype"
              name="pameldingType"
              readOnly={
                kreverDirekteVedtak(tiltakstype) ||
                watch("oppstart") === GjennomforingOppstartstype.FELLES
              }
              options={[
                {
                  label: gjennomforingTekster.pamelding.beskrivelse(
                    GjennomforingPameldingType.TRENGER_GODKJENNING,
                  ),
                  value: GjennomforingPameldingType.TRENGER_GODKJENNING,
                },
                {
                  label: gjennomforingTekster.pamelding.beskrivelse(
                    GjennomforingPameldingType.DIREKTE_VEDTAK,
                  ),
                  value: GjennomforingPameldingType.DIREKTE_VEDTAK,
                },
              ]}
            />
            <HGrid align="start" gap="space-16" columns={2}>
              <DatePicker>
                <DatePicker.Input
                  value={formaterDato(avtale.startDato)}
                  label={gjennomforingTekster.avtaleStartdatoLabel}
                  readOnly
                  size="small"
                />
              </DatePicker>
              {avtale.sluttDato ? (
                <DatePicker>
                  <DatePicker.Input
                    value={formaterDato(avtale.sluttDato)}
                    label={gjennomforingTekster.avtaleSluttdatoLabel}
                    readOnly
                    size="small"
                  />
                </DatePicker>
              ) : (
                " - "
              )}
            </HGrid>
            <HGrid align="start" gap="space-16" columns={2}>
              <FormDateInput
                label={gjennomforingTekster.startdatoLabel}
                fromDate={minStartdato}
                toDate={maxStartdato}
                name={"startDato"}
              />
              <FormDateInput
                key={watchSluttDato}
                label={gjennomforingTekster.sluttdatoLabel}
                fromDate={minStartdato}
                toDate={maxSluttdato}
                name={"sluttDato"}
                rules={{
                  onChange: (event: ChangeEvent<HTMLInputElement>) => {
                    visAdvarselForSluttDato(event.target.value);
                  },
                }}
              />
            </HGrid>
            <HGrid align="start" gap="space-16" columns={2}>
              <NumberInput<GjennomforingFormValues>
                name="antallPlasser"
                label={gjennomforingTekster.antallPlasserLabel}
              />
              {kreverDeltidsprosent(tiltakstype) && (
                <NumberInput<GjennomforingFormValues>
                  name="deltidsprosent"
                  step="0.01"
                  min={0}
                  max={100}
                  label={gjennomforingTekster.deltidsprosentLabel}
                />
              )}
            </HGrid>
            {visOppmotested && (
              <VStack gap="space-8">
                <FormTextarea<GjennomforingFormValues>
                  name="oppmoteSted"
                  resize
                  maxLength={OPPMOTE_STED_MAX_LENGTH}
                  label="Oppmøtested"
                  description="Skriv inn adressen der bruker skal møte opp til tiltaket og eventuelt klokkeslett. For tiltak uten spesifikk adresse (for eksempel digitalt jobbsøkerkurs), kan du la feltet stå tomt."
                />
              </VStack>
            )}
          </FormGroup>
        </SkjemaKolonne>
        <SkjemaKolonne>
          <FormGroup>
            <FormCombobox<GjennomforingFormValues>
              name="administratorer"
              id="administratorer"
              label={
                <LabelWithHelpText
                  label={gjennomforingTekster.administratorerForGjennomforingenLabel}
                  helpTextTitle="Mer informasjon"
                >
                  Bestemmer hvem som eier gjennomføringen. Notifikasjoner sendes til
                  administratorene.
                </LabelWithHelpText>
              }
              placeholder="Velg en"
              isMultiSelect
              options={administratorOptions(navAnsatte)}
            />
          </FormGroup>
          {avtale.arrangor ? (
            <FormGroup>
              <GjennomforingArrangorForm readOnly={false} arrangor={avtale.arrangor} />
            </FormGroup>
          ) : (
            <Alert variant="warning">{avtaletekster.arrangorManglerVarsel}</Alert>
          )}
          <FormGroup>
            <FormSelect<GjennomforingFormValues> name="prismodellId" label="Prismodell">
              <option value={""}>-- Velg prismodell --</option>
              {avtale.prismodeller.map((prismodell) => (
                <option key={prismodell.id} value={prismodell.id}>
                  {prismodell.navn}
                </option>
              ))}
            </FormSelect>
            {valgtPrismodell && <PrismodellDetaljer prismodeller={[valgtPrismodell]} />}
          </FormGroup>
        </SkjemaKolonne>
      </TwoColumnGrid>
      {gjennomforing && (
        <EndreDatoAdvarselModal
          modalRef={endreSluttDatoModalRef}
          onCancel={() => setValue("sluttDato", gjennomforing.sluttDato)}
          antallDeltakere={antallDeltakere}
        />
      )}
    </>
  );
}

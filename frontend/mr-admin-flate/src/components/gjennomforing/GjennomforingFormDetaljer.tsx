import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useGjennomforingAdministratorer } from "@/api/ansatt/useGjennomforingAdministratorer";
import { GjennomforingAmoKategoriseringForm } from "@/components/amoKategorisering/GjennomforingAmoKategoriseringForm";
import { FormGroup } from "@/components/skjema/FormGroup";
import { SkjemaKolonne } from "@/components/skjema/SkjemaKolonne";
import {
  AvtaleDto,
  FeatureToggle,
  GjennomforingDeltakerSummary,
  GjennomforingAvtaleDto,
  GjennomforingOppstartstype,
  GjennomforingPameldingType,
  GjennomforingRequest,
  GjennomforingVeilederinfoDto,
  TiltakstypeDto,
} from "@tiltaksadministrasjon/api-client";
import {
  Alert,
  DatePicker,
  HGrid,
  HStack,
  Select,
  Switch,
  Textarea,
  TextField,
  UNSAFE_Combobox,
  VStack,
} from "@navikt/ds-react";
import { useEffect, useRef, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { EndreDatoAdvarselModal } from "@/components/modal/EndreDatoAdvarselModal";
import { AdministratorOptions } from "@/components/skjema/AdministratorOptions";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { GjennomforingUtdanningslopForm } from "@/components/utdanning/GjennomforingUtdanningslopForm";
import { GjennomforingArrangorForm } from "./GjennomforingArrangorForm";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { addDuration, formaterDato } from "@mr/frontend-common/utils/date";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { OPPMOTE_STED_MAX_LENGTH } from "@/constants";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { PrismodellDetaljer } from "../avtaler/PrismodellDetaljer";
import { kreverDirekteVedtak, kreverDeltidsprosent } from "@/utils/tiltakstype";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";

interface Props {
  tiltakstype: TiltakstypeDto;
  avtale: AvtaleDto;
  gjennomforing: GjennomforingAvtaleDto | null;
  veilederinfo: GjennomforingVeilederinfoDto | null;
  deltakere: GjennomforingDeltakerSummary | null;
}

export function GjennomforingFormDetaljer(props: Props) {
  const { tiltakstype, avtale, gjennomforing, veilederinfo, deltakere } = props;

  const { data: administratorer } = useGjennomforingAdministratorer();
  const { data: ansatt } = useHentAnsatt();

  const endreSluttDatoModalRef = useRef<HTMLDialogElement>(null);
  const { data: enablePameldingType } = useFeatureToggle(
    FeatureToggle.TILTAKSADMINISTRASJON_PAMELDING_TYPE,
  );

  const {
    register,
    control,
    formState: { errors },
    getValues,
    setValue,
    watch,
  } = useFormContext<GjennomforingRequest>();

  const watchStartDato = watch("startDato");

  useEffect(() => {
    if (watchStartDato && new Date(watchStartDato) < new Date()) {
      setValue("tilgjengeligForArrangorDato", null);
    }
  }, [setValue, watchStartDato]);

  const watchSluttDato = watch("sluttDato");
  const valgtPrismodell = avtale.prismodeller.find((p) => p.id === watch("prismodellId"));
  const antallDeltakere = deltakere?.antallDeltakere ?? 0;

  function visAdvarselForSluttDato() {
    if (
      gjennomforing &&
      antallDeltakere > 0 &&
      watchSluttDato &&
      gjennomforing.sluttDato !== watchSluttDato
    ) {
      endreSluttDatoModalRef.current?.showModal();
    }
  }

  const minStartdato = new Date(avtale.startDato);
  const maxStartdato = addDuration(new Date(), { years: 2 });
  const maxSluttdato =
    addDuration(gjennomforing?.sluttDato, { years: 6 }) ?? addDuration(maxStartdato, { years: 6 });

  return (
    <>
      <TwoColumnGrid separator>
        <SkjemaKolonne>
          <FormGroup>
            <TextField
              size="small"
              error={errors.navn?.message as string}
              label={gjennomforingTekster.tiltaksnavnLabel}
              autoFocus
              {...register("navn")}
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
            {errors.avtaleId?.message ? (
              <Alert variant="warning">{errors.avtaleId.message as string}</Alert>
            ) : null}
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
            {enablePameldingType && (
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
            )}
            <HGrid columns={2}>
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
            <HGrid columns={2}>
              <ControlledDateInput
                label={gjennomforingTekster.startdatoLabel}
                fromDate={minStartdato}
                toDate={maxStartdato}
                defaultSelected={getValues("startDato")}
                onChange={(val) => setValue("startDato", val)}
                error={errors.startDato?.message}
              />
              <ControlledDateInput
                key={watchSluttDato}
                label={gjennomforingTekster.sluttdatoLabel}
                fromDate={minStartdato}
                toDate={maxSluttdato}
                defaultSelected={getValues("sluttDato")}
                onChange={(val) => {
                  setValue("sluttDato", val);
                  visAdvarselForSluttDato();
                }}
                error={errors.sluttDato?.message}
              />
            </HGrid>
            <HGrid align="start" columns={2}>
              <TextField
                size="small"
                error={errors.antallPlasser?.message as string}
                type="number"
                style={{ width: "180px" }}
                label={gjennomforingTekster.antallPlasserLabel}
                {...register("antallPlasser", {
                  valueAsNumber: true,
                })}
              />
              {kreverDeltidsprosent(tiltakstype) && (
                <TextField
                  size="small"
                  error={errors.deltidsprosent?.message as string}
                  type="number"
                  step="0.01"
                  min={0}
                  max={100}
                  style={{ width: "180px" }}
                  label={gjennomforingTekster.deltidsprosentLabel}
                  {...register("deltidsprosent", {
                    valueAsNumber: true,
                  })}
                />
              )}
            </HGrid>
            <EstimertVentetidForm veilederinfo={veilederinfo} />
            <VStack gap="space-8">
              <Textarea
                size="small"
                resize
                value={watch("oppmoteSted") || ""}
                maxLength={OPPMOTE_STED_MAX_LENGTH}
                label="Oppmøtested"
                description="Skriv inn adressen der bruker skal møte opp til tiltaket og eventuelt klokkeslett. For tiltak uten spesifikk adresse (for eksempel digitalt jobbsøkerkurs), kan du la feltet stå tomt."
                {...register("oppmoteSted")}
                error={errors.oppmoteSted ? (errors.oppmoteSted.message as string) : null}
              />
            </VStack>
          </FormGroup>
        </SkjemaKolonne>
        <SkjemaKolonne>
          <FormGroup>
            <Controller
              control={control}
              name="administratorer"
              render={({ field }) => (
                <UNSAFE_Combobox
                  size="small"
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
                  selectedOptions={AdministratorOptions(
                    ansatt,
                    gjennomforing?.administratorer.map((g) => g.navIdent) || [],
                    administratorer,
                  ).filter((option) => field.value.includes(option.value))}
                  name={field.name}
                  error={errors.administratorer?.message}
                  options={AdministratorOptions(
                    ansatt,
                    gjennomforing?.administratorer.map((g) => g.navIdent) || [],
                    administratorer,
                  )}
                  onToggleSelected={(option, isSelected) => {
                    if (isSelected) {
                      field.onChange([...field.value, option]);
                    } else {
                      field.onChange(field.value.filter((v) => v !== option));
                    }
                  }}
                />
              )}
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
            <Select
              size="small"
              label="Prismodell"
              error={errors.prismodellId?.message}
              {...register("prismodellId")}
            >
              <option value={""}>-- Velg prismodell --</option>
              {avtale.prismodeller.map((prismodell) => (
                <option key={prismodell.id} value={prismodell.id}>
                  {prismodell.navn}
                </option>
              ))}
            </Select>
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

interface EstimertVentetidFormProps {
  veilederinfo: GjennomforingVeilederinfoDto | null;
}

export function EstimertVentetidForm(props: EstimertVentetidFormProps) {
  const [visEstimertVentetid, setVisEstimertVentetid] = useState<boolean>(
    !!props.veilederinfo?.estimertVentetid?.enhet,
  );

  const {
    register,
    formState: { errors },
    setValue,
    watch,
  } = useFormContext<GjennomforingRequest>();

  useEffect(() => {
    const resetEstimertVentetid = () => {
      if (!visEstimertVentetid) {
        setValue("estimertVentetid", null);
      }
    };

    resetEstimertVentetid();
  }, [setValue, visEstimertVentetid]);

  if (watch("oppstart") === GjennomforingOppstartstype.FELLES) {
    return null;
  }

  return (
    <fieldset className="border-none p-0 [&>legend]:font-ax-bold [&>legend]:mb-2">
      <HStack gap="space-4">
        <LabelWithHelpText label="Estimert ventetid" helpTextTitle="Hva er estimert ventetid?">
          Estimert ventetid er et felt som kan brukes hvis dere sitter på informasjon om estimert
          ventetid for tiltaket. Hvis dere legger inn en verdi i feltene her blir det synlig for
          alle ansatte i Nav.
        </LabelWithHelpText>
      </HStack>
      <Switch
        checked={visEstimertVentetid}
        onClick={() => setVisEstimertVentetid(!visEstimertVentetid)}
      >
        Registrer estimert ventetid
      </Switch>
      {visEstimertVentetid && (
        <HStack align="start" justify="start" gap="space-40">
          <TextField
            size="small"
            type="number"
            min={0}
            label="Antall"
            error={errors.estimertVentetid?.verdi?.message as string}
            {...register("estimertVentetid.verdi", {
              valueAsNumber: true,
            })}
          />
          <Select
            size="small"
            label="Måleenhet"
            error={errors.estimertVentetid?.enhet?.message as string}
            {...register("estimertVentetid.enhet")}
          >
            <option value="uke">Uker</option>
            <option value="maned">Måneder</option>
          </Select>
        </HStack>
      )}
    </fieldset>
  );
}

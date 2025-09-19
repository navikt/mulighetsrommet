import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useGjennomforingAdministratorer } from "@/api/ansatt/useGjennomforingAdministratorer";
import { useGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";
import { GjennomforingAmoKategoriseringForm } from "@/components/amoKategorisering/GjennomforingAmoKategoriseringForm";
import { InferredGjennomforingSchema } from "@/components/redaksjoneltInnhold/GjennomforingSchema";
import { FormGroup } from "@/components/skjema/FormGroup";
import { SkjemaKolonne } from "@/components/skjema/SkjemaKolonne";
import { isKursTiltak } from "@/utils/Utils";
import {
  AvtaleDto,
  GjennomforingDto,
  GjennomforingOppstartstype,
  Tiltakskode,
} from "@tiltaksadministrasjon/api-client";
import {
  Alert,
  DatePicker,
  HGrid,
  HStack,
  Select,
  Switch,
  TextField,
  UNSAFE_Combobox,
} from "@navikt/ds-react";
import { useEffect, useRef } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { gjennomforingTekster } from "@/components/ledetekster/gjennomforingLedetekster";
import { EndreDatoAdvarselModal } from "@/components/modal/EndreDatoAdvarselModal";
import { AdministratorOptions } from "@/components/skjema/AdministratorOptions";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { GjennomforingUtdanningslopForm } from "@/components/utdanning/GjennomforingUtdanningslopForm";
import { SelectOppstartstype } from "./SelectOppstartstype";
import { GjennomforingArrangorForm } from "./GjennomforingArrangorForm";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { addDuration, formaterDato } from "@mr/frontend-common/utils/date";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";

interface Props {
  gjennomforing?: GjennomforingDto;
  avtale: AvtaleDto;
}

export function GjennomforingFormDetaljer({ gjennomforing, avtale }: Props) {
  const { data: administratorer } = useGjennomforingAdministratorer();
  const { data: ansatt } = useHentAnsatt();

  const { data: deltakerSummary } = useGjennomforingDeltakerSummary(gjennomforing?.id);

  const endreSluttDatoModalRef = useRef<HTMLDialogElement>(null);

  const {
    register,
    control,
    formState: { errors },
    getValues,
    setValue,
    watch,
  } = useFormContext<InferredGjennomforingSchema>();

  const watchVisEstimertVentetid = watch("visEstimertVentetid");

  useEffect(() => {
    const resetEstimertVentetid = () => {
      if (!watchVisEstimertVentetid) {
        setValue("estimertVentetid", null);
      }
    };

    resetEstimertVentetid();
  }, [setValue, watchVisEstimertVentetid]);

  const watchStartDato = watch("startOgSluttDato.startDato");
  const antallDeltakere = deltakerSummary?.antallDeltakere;

  useEffect(() => {
    if (watchStartDato && new Date(watchStartDato) < new Date()) {
      setValue("tilgjengeligForArrangorDato", null);
    }
  }, [setValue, watchStartDato]);

  const watchSluttDato = watch("startOgSluttDato.sluttDato");

  function visAdvarselForSluttDato() {
    if (
      gjennomforing &&
      antallDeltakere &&
      antallDeltakere > 0 &&
      watchSluttDato &&
      gjennomforing.sluttDato !== watchSluttDato
    ) {
      endreSluttDatoModalRef.current?.showModal();
    }
  }

  const minStartdato = new Date(avtale.startDato);
  const maxSluttdato =
    addDuration(gjennomforing?.sluttDato, { years: 6 }) ?? addDuration(minStartdato, { years: 6 });

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
            {avtale.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ? (
              <GjennomforingAmoKategoriseringForm avtale={avtale} />
            ) : null}
            {avtale.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING ? (
              <GjennomforingUtdanningslopForm avtale={avtale} />
            ) : null}
          </FormGroup>

          <FormGroup>
            <SelectOppstartstype
              name="oppstart"
              readonly={!isKursTiltak(avtale.tiltakstype.tiltakskode)}
            />
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
                toDate={maxSluttdato}
                defaultSelected={getValues("startOgSluttDato.startDato")}
                onChange={(val) =>
                  setValue("startOgSluttDato.startDato", (val ?? undefined) as unknown as never, {
                    shouldValidate: true,
                  })
                }
                error={errors.startOgSluttDato?.startDato?.message}
              />
              <ControlledDateInput
                key={watchSluttDato}
                label={gjennomforingTekster.sluttdatoLabel}
                fromDate={minStartdato}
                toDate={maxSluttdato}
                defaultSelected={getValues("startOgSluttDato.sluttDato")}
                onChange={(val) => {
                  setValue("startOgSluttDato.sluttDato", val);
                  visAdvarselForSluttDato();
                }}
                error={errors.startOgSluttDato?.sluttDato?.message}
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
              {isKursTiltak(avtale.tiltakstype.tiltakskode) && (
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
            {watch("oppstart") === GjennomforingOppstartstype.LOPENDE ? (
              <fieldset className="border-none p-0 [&>legend]:font-bold [&>legend]:mb-2">
                <HStack gap="1">
                  <LabelWithHelpText
                    label="Estimert ventetid"
                    helpTextTitle="Hva er estimert ventetid?"
                  >
                    Estimert ventetid er et felt som kan brukes hvis dere sitter på informasjon om
                    estimert ventetid for tiltaket. Hvis dere legger inn en verdi i feltene her blir
                    det synlig for alle ansatte i Nav.
                  </LabelWithHelpText>
                </HStack>
                <Switch checked={watch("visEstimertVentetid")} {...register("visEstimertVentetid")}>
                  Registrer estimert ventetid
                </Switch>
                {watch("visEstimertVentetid") ? (
                  <HStack align="start" justify="start" gap="10">
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
                ) : null}
              </fieldset>
            ) : null}
          </FormGroup>
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
        </SkjemaKolonne>
        <SkjemaKolonne>
          {avtale.arrangor ? (
            <FormGroup>
              <GjennomforingArrangorForm readOnly={false} arrangor={avtale.arrangor} />
            </FormGroup>
          ) : (
            <Alert variant="warning">{avtaletekster.arrangorManglerVarsel}</Alert>
          )}
        </SkjemaKolonne>
      </TwoColumnGrid>
      {gjennomforing && (
        <EndreDatoAdvarselModal
          modalRef={endreSluttDatoModalRef}
          onCancel={() => setValue("startOgSluttDato.sluttDato", gjennomforing.sluttDato)}
          antallDeltakere={deltakerSummary?.antallDeltakere ?? 0}
        />
      )}
    </>
  );
}

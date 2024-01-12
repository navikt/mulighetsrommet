import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { Button, Checkbox, HStack, Label, TextField, Textarea } from "@navikt/ds-react";
import {
  Avtale,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
  Toggles,
} from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";
import { useFieldArray, useFormContext } from "react-hook-form";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useHentKontaktpersoner } from "../../api/ansatt/useHentKontaktpersoner";
import { useTiltaksgjennomforingAdministratorer } from "../../api/ansatt/useTiltaksgjennomforingAdministratorer";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { addYear } from "../../utils/Utils";
import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import { Separator } from "../detaljside/Metadata";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import skjemastyles from "../skjema/Skjema.module.scss";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";
import { arrangorUnderenheterOptions, erArenaOpphav } from "./TiltaksgjennomforingSkjemaConst";

interface Props {
  tiltaksgjennomforing?: Tiltaksgjennomforing;
  avtale: Avtale;
}

export const TiltaksgjennomforingSkjemaDetaljer = ({ tiltaksgjennomforing, avtale }: Props) => {
  const { data: virksomhet } = useVirksomhet(avtale.leverandor.organisasjonsnummer || "");
  const { data: administratorer } = useTiltaksgjennomforingAdministratorer();

  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();

  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner } = useHentKontaktpersoner();

  const kontaktpersonerOption = (selectedIndex: number) => {
    const excludedKontaktpersoner = watch("kontaktpersoner")
      .filter((_: any, i: number) => i !== selectedIndex)
      .map((k: any) => k["navIdent"]);

    const options = kontaktpersoner
      ?.filter((kontaktperson) => !excludedKontaktpersoner.includes(kontaktperson.navIdent))
      ?.map((kontaktperson) => ({
        label: `${kontaktperson.fornavn} ${kontaktperson.etternavn} - ${kontaktperson.navIdent}`,
        value: kontaktperson.navIdent,
      }));

    return options || [];
  };

  const {
    register,
    control,
    formState: { errors },
    setValue,
    watch,
  } = useFormContext();
  const {
    fields: kontaktpersonFields,
    append: appendKontaktperson,
    remove: removeKontaktperson,
  } = useFieldArray({
    name: "kontaktpersoner",
    control,
  });

  const watchErMidlertidigStengt = watch("midlertidigStengt.erMidlertidigStengt");

  const regionerOptions = avtale.kontorstruktur
    .map((struk) => struk.region)
    .map((kontor) => ({ value: kontor.enhetsnummer, label: kontor.navn }));

  const navEnheterOptions = avtale.kontorstruktur
    .flatMap((struk) => struk.kontorer)
    .filter((kontor) => kontor.overordnetEnhet === watch("navRegion"))
    .map((kontor) => ({ label: kontor.navn, value: kontor.enhetsnummer }));

  const minStartdato = new Date();
  const maxSluttdato = addYear(minStartdato, 5);
  const { data: midlertidigStengt } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_MIDLERTIDIG_STENGT,
  );

  const valgteNavEnheter = watch("navEnheter");

  return (
    <div className={skjemastyles.container}>
      <div className={skjemastyles.input_container}>
        <div className={skjemastyles.column}>
          <FormGroup>
            <TextField
              size="small"
              readOnly={erArenaOpphav(tiltaksgjennomforing)}
              error={errors.navn?.message as string}
              label="Tiltaksnavn"
              autoFocus
              {...register("navn")}
            />
            {tiltaksgjennomforing?.tiltaksnummer ? (
              <TextField
                size="small"
                readOnly
                label="Tiltaksnummer"
                autoFocus
                value={tiltaksgjennomforing?.tiltaksnummer}
              />
            ) : null}
          </FormGroup>
          <Separator />
          <FormGroup>
            <TextField
              size="small"
              readOnly
              label={`Avtale (tiltakstype: ${avtale.tiltakstype.navn})`}
              value={avtale.navn || ""}
            />
          </FormGroup>
          <Separator />
          <FormGroup>
            <ControlledSokeSelect
              size="small"
              label="Oppstartstype"
              placeholder="Velg oppstart"
              {...register("oppstart")}
              options={[
                {
                  label: "Felles oppstartsdato",
                  value: TiltaksgjennomforingOppstartstype.FELLES,
                },
                {
                  label: "Løpende oppstart",
                  value: TiltaksgjennomforingOppstartstype.LOPENDE,
                },
              ]}
            />
            <FraTilDatoVelger
              size="small"
              fra={{
                label: "Startdato",
                readOnly: erArenaOpphav(tiltaksgjennomforing),
                fromDate: minStartdato,
                toDate: maxSluttdato,
                ...register("startOgSluttDato.startDato"),
                format: "iso-string",
              }}
              til={{
                label: "Sluttdato",
                readOnly: erArenaOpphav(tiltaksgjennomforing),
                fromDate: minStartdato,
                toDate: maxSluttdato,
                ...register("startOgSluttDato.sluttDato"),
                format: "iso-string",
              }}
            />
            <Checkbox
              size="small"
              readOnly={erArenaOpphav(tiltaksgjennomforing)}
              {...register("apentForInnsok")}
            >
              Åpen for innsøk
            </Checkbox>
            {midlertidigStengt ? (
              <Checkbox size="small" {...register("midlertidigStengt.erMidlertidigStengt")}>
                Midlertidig stengt
              </Checkbox>
            ) : null}
            {watchErMidlertidigStengt && (
              <FraTilDatoVelger
                size="small"
                fra={{
                  label: "Stengt fra",
                  fromDate: minStartdato,
                  toDate: maxSluttdato,
                  ...register("midlertidigStengt.stengtFra"),
                  format: "date",
                }}
                til={{
                  label: "Stengt til",
                  fromDate: watch("midlertidigStengt.stengtFra") ?? new Date(),
                  toDate: maxSluttdato,
                  ...register("midlertidigStengt.stengtTil"),
                  format: "date",
                }}
              />
            )}
            <HStack justify="space-between" columns={2}>
              <TextField
                size="small"
                readOnly={erArenaOpphav(tiltaksgjennomforing)}
                error={errors.antallPlasser?.message as string}
                type="number"
                style={{ width: "180px" }}
                label="Antall plasser"
                {...register("antallPlasser", {
                  valueAsNumber: true,
                })}
              />
              {isTiltakMedFellesOppstart(avtale.tiltakstype.arenaKode) && (
                <TextField
                  size="small"
                  readOnly={erArenaOpphav(tiltaksgjennomforing)}
                  error={errors.deltidsprosent?.message as string}
                  type="number"
                  step="0.01"
                  style={{ width: "180px" }}
                  label="Deltidsprosent"
                  {...register("deltidsprosent", {
                    valueAsNumber: true,
                  })}
                />
              )}
            </HStack>
          </FormGroup>
          <Separator />
          <FormGroup>
            <ControlledMultiSelect
              size="small"
              placeholder={isLoadingAnsatt ? "Laster..." : "Velg en"}
              label={"Administratorer for gjennomføringen"}
              helpText="Bestemmer hvem som eier gjennomføringen. Notifikasjoner sendes til administratorene."
              {...register("administratorer")}
              options={AdministratorOptions(
                ansatt,
                tiltaksgjennomforing?.administratorer,
                administratorer,
              )}
            />
          </FormGroup>
        </div>
        <div className={skjemastyles.vertical_separator} />
        <div className={skjemastyles.column}>
          <div className={skjemastyles.gray_container}>
            <FormGroup>
              <ControlledSokeSelect
                size="small"
                label="NAV-region"
                placeholder="Velg en"
                {...register("navRegion")}
                onChange={() => {
                  setValue("navEnheter", []);
                }}
                options={regionerOptions}
              />
              <ControlledMultiSelect
                size="small"
                placeholder={"Velg en"}
                label={"NAV-enheter (kontorer)"}
                helpText="Bestemmer hvem gjennomføringen skal vises til i Modia, basert på hvilket kontor brukeren har tilhørighet til."
                {...register("navEnheter")}
                options={navEnheterOptions}
              />
            </FormGroup>
            <Separator />
            <FormGroup>
              <div>
                {kontaktpersonFields?.map((field, index) => {
                  return (
                    <div className={skjemastyles.kontaktperson_container} key={field.id}>
                      <button
                        className={skjemastyles.kontaktperson_button}
                        type="button"
                        onClick={() => {
                          if (watch("kontaktpersoner")!.length > 1) {
                            removeKontaktperson(index);
                          } else {
                            setValue("kontaktpersoner", [
                              {
                                navIdent: "",
                                navEnheter: [],
                              },
                            ]);
                          }
                        }}
                      >
                        <XMarkIcon fontSize="1.5rem" />
                      </button>
                      <div className={skjemastyles.kontaktperson_inputs}>
                        <ControlledSokeSelect
                          helpText="Bestemmer kontaktperson som veilederene kan hendvende seg til for informasjon om gjennomføringen. Kan gjelde for én eller flere enheter."
                          size="small"
                          placeholder={
                            isLoadingKontaktpersoner ? "Laster kontaktpersoner..." : "Velg en"
                          }
                          label={"Kontaktperson i NAV"}
                          {...register(`kontaktpersoner.${index}.navIdent`, {
                            shouldUnregister: true,
                          })}
                          options={kontaktpersonerOption(index)}
                        />
                        <ControlledMultiSelect
                          size="small"
                          placeholder={isLoadingKontaktpersoner ? "Laster enheter..." : "Velg en"}
                          label={"Område"}
                          {...register(`kontaktpersoner.${index}.navEnheter`, {
                            shouldUnregister: true,
                          })}
                          options={navEnheterOptions.filter((enhet) =>
                            valgteNavEnheter.includes(enhet.value),
                          )}
                        />
                      </div>
                    </div>
                  );
                })}
                <Button
                  className={skjemastyles.kontaktperson_button}
                  type="button"
                  size="small"
                  onClick={() =>
                    appendKontaktperson({
                      navIdent: "",
                      navEnheter: [],
                    })
                  }
                >
                  <PlusIcon aria-label="Legg til ny kontaktperson" /> Legg til ny kontaktperson
                </Button>
              </div>
            </FormGroup>
          </div>
          <div className={skjemastyles.gray_container}>
            <FormGroup>
              <TextField
                size="small"
                label="Tiltaksarrangør hovedenhet"
                placeholder=""
                defaultValue={`${avtale.leverandor.navn} - ${avtale.leverandor.organisasjonsnummer}`}
                readOnly
              />
              <ControlledSokeSelect
                size="small"
                label="Tiltaksarrangør underenhet"
                placeholder="Velg underenhet for tiltaksarrangør"
                {...register("tiltaksArrangorUnderenhetOrganisasjonsnummer")}
                onChange={() => {
                  setValue("arrangorKontaktpersonId", null);
                }}
                onClearValue={() => {
                  setValue("tiltaksArrangorUnderenhetOrganisasjonsnummer", "");
                }}
                readOnly={
                  !avtale.leverandor.organisasjonsnummer || erArenaOpphav(tiltaksgjennomforing)
                }
                options={arrangorUnderenheterOptions(avtale, virksomhet)}
              />
              {watch("tiltaksArrangorUnderenhetOrganisasjonsnummer") &&
                !tiltaksgjennomforing?.arrangor?.slettet && (
                  <div className={skjemastyles.virksomhet_kontaktperson_container}>
                    <VirksomhetKontaktpersoner
                      title={"Kontaktperson hos arrangøren"}
                      orgnr={watch("tiltaksArrangorUnderenhetOrganisasjonsnummer")}
                      formValueName={"arrangorKontaktpersonId"}
                    />
                  </div>
                )}
              {isTiltakMedFellesOppstart(avtale.tiltakstype.arenaKode) ? (
                <>
                  <Separator />
                  <Label>Fremmøte</Label>
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "row",
                      gap: "1rem",
                    }}
                  >
                    <ControlledDateInput
                      label="Dato"
                      readOnly={erArenaOpphav(tiltaksgjennomforing)}
                      fromDate={minStartdato}
                      toDate={maxSluttdato}
                      size="small"
                      {...register("fremmoteDato")}
                      format="iso-string"
                    />
                    <TextField
                      size="small"
                      type="time"
                      style={{
                        width: "100px",
                      }}
                      readOnly={erArenaOpphav(tiltaksgjennomforing)}
                      error={errors.fremmoteTid?.message as string}
                      {...register("fremmoteTid")}
                      label="Klokkeslett"
                    />
                  </div>
                  <Textarea
                    size="small"
                    resize="vertical"
                    minRows={3}
                    style={{ maxWidth: "450px" }}
                    description="Informasjon om hvor brukeren skal møte opp."
                    readOnly={erArenaOpphav(tiltaksgjennomforing)}
                    error={errors.fremmoteSted?.message as string}
                    {...register("fremmoteSted")}
                    label="Sted"
                  />
                </>
              ) : (
                <TextField
                  size="small"
                  label="Sted for gjennomføring"
                  description="Skriv inn stedet tiltaket skal gjennomføres, for eksempel Fredrikstad eller Tromsø. For tiltak uten eksplisitt lokasjon (for eksempel digital jobbklubb), kan du la feltet stå tomt."
                  {...register("stedForGjennomforing")}
                  error={
                    errors.stedForGjennomforing
                      ? (errors.stedForGjennomforing.message as string)
                      : null
                  }
                />
              )}
            </FormGroup>
          </div>
        </div>
      </div>
    </div>
  );
};

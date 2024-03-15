import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { Alert, Button, HelpText, HStack, Select, Switch, TextField } from "@navikt/ds-react";
import {
  Avtale,
  Tiltaksgjennomforing,
  TiltaksgjennomforingKontaktperson,
  Tiltakskode,
} from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";
import { useEffect } from "react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useHentKontaktpersoner } from "../../api/ansatt/useHentKontaktpersoner";
import { useTiltaksgjennomforingAdministratorer } from "../../api/ansatt/useTiltaksgjennomforingAdministratorer";
import { useMigrerteTiltakstyper } from "../../api/tiltakstyper/useMigrerteTiltakstyper";
import { addYear } from "../../utils/Utils";
import { isTiltakMedFellesOppstart } from "../../utils/tiltakskoder";
import { Separator } from "../detaljside/Metadata";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import skjemastyles from "../skjema/Skjema.module.scss";
import { SelectOppstartstype } from "./SelectOppstartstype";
import { InferredTiltaksgjennomforingSchema } from "../redaksjonelt-innhold/TiltaksgjennomforingSchema";
import { erArenaOpphavOgIngenEierskap } from "./TiltaksgjennomforingSkjemaConst";
import { TiltaksgjennomforingArrangorSkjema } from "./TiltaksgjennomforingArrangorSkjema";

interface Props {
  tiltaksgjennomforing?: Tiltaksgjennomforing;
  avtale: Avtale;
}

function visApentForInnsok(arenaKode: Tiltakskode) {
  return [
    Tiltakskode.JOBBK,
    Tiltakskode.DIGIOPPARB,
    Tiltakskode.GRUPPEAMO,
    Tiltakskode.GRUFAGYRKE,
  ].includes(arenaKode);
}

export const TiltaksgjennomforingSkjemaDetaljer = ({ tiltaksgjennomforing, avtale }: Props) => {
  const { data: administratorer } = useTiltaksgjennomforingAdministratorer();
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner } = useHentKontaktpersoner();
  const { data: migrerteTiltakstyper = [] } = useMigrerteTiltakstyper();

  const kontaktpersonerOption = (selectedIndex: number) => {
    const excludedKontaktpersoner = watch("kontaktpersoner")
      ?.filter((_: any, i: number) => i !== selectedIndex)
      .map((k: any) => k["navIdent"]);

    const options = kontaktpersoner
      ?.filter((kontaktperson) => !excludedKontaktpersoner?.includes(kontaktperson.navIdent))
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
  } = useFormContext<InferredTiltaksgjennomforingSchema>();
  const {
    fields: kontaktpersonFields,
    append: appendKontaktperson,
    remove: removeKontaktperson,
  } = useFieldArray({
    name: "kontaktpersoner",
    control,
  });

  const watchVisEstimertVentetid = watch("visEstimertVentetid");

  useEffect(() => {
    const resetEstimertVentetid = () => {
      if (!watchVisEstimertVentetid) {
        setValue("estimertVentetid", null);
      }
    };

    resetEstimertVentetid();
  }, [watchVisEstimertVentetid]);

  const regionerOptions = avtale.kontorstruktur
    .map((struk) => struk.region)
    .map((kontor) => ({ value: kontor.enhetsnummer, label: kontor.navn }));

  const navEnheterOptions = avtale.kontorstruktur
    .flatMap((struk) => struk.kontorer)
    .filter((kontor) => kontor.overordnetEnhet === watch("navRegion"))
    .map((kontor) => ({ label: kontor.navn, value: kontor.enhetsnummer }));

  const minStartdato = new Date(avtale.startDato);
  const maxSluttdato = addYear(minStartdato, 5);

  const valgteNavEnheter = watch("navEnheter");

  const eierIkkeGjennomforing = erArenaOpphavOgIngenEierskap(
    tiltaksgjennomforing,
    migrerteTiltakstyper,
  );

  return (
    <div className={skjemastyles.container}>
      <div className={skjemastyles.input_container}>
        <div className={skjemastyles.column}>
          <FormGroup>
            <TextField
              size="small"
              readOnly={eierIkkeGjennomforing}
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
            {errors.avtaleId?.message ? (
              <Alert variant="warning">{errors.avtaleId.message as string}</Alert>
            ) : null}
          </FormGroup>
          <Separator />
          <FormGroup>
            <SelectOppstartstype
              name="oppstart"
              readonly={!isTiltakMedFellesOppstart(avtale.tiltakstype.arenaKode)}
            />
            <FraTilDatoVelger
              size="small"
              fra={{
                label: "Startdato",
                readOnly: eierIkkeGjennomforing,
                fromDate: minStartdato,
                toDate: maxSluttdato,
                ...register("startOgSluttDato.startDato"),
                format: "iso-string",
              }}
              til={{
                label: "Sluttdato",
                readOnly: eierIkkeGjennomforing,
                fromDate: minStartdato,
                toDate: maxSluttdato,
                ...register("startOgSluttDato.sluttDato"),
                format: "iso-string",
              }}
            />
            {visApentForInnsok(avtale.tiltakstype.arenaKode) ? (
              <Switch size="small" readOnly={eierIkkeGjennomforing} {...register("apentForInnsok")}>
                Åpen for innsøk
              </Switch>
            ) : null}

            <HStack justify="space-between">
              <TextField
                size="small"
                readOnly={eierIkkeGjennomforing}
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
                  readOnly={eierIkkeGjennomforing}
                  error={errors.deltidsprosent?.message as string}
                  type="number"
                  step="0.01"
                  min={0}
                  max={100}
                  style={{ width: "180px" }}
                  label="Deltidsprosent"
                  {...register("deltidsprosent", {
                    valueAsNumber: true,
                  })}
                />
              )}
            </HStack>
            <Separator />
            <fieldset className={skjemastyles.fieldset_no_styling}>
              <HStack gap="1">
                <legend>Estimert ventetid</legend>
                <HelpText title="Hva er estimert ventetid?">
                  Estimert ventetid er et felt som kan brukes hvis dere sitter på informasjon om
                  estimert ventetid for tiltaket. Hvis dere legger inn en verdi i feltene her blir
                  det synlig for alle ansatte i NAV.
                </HelpText>
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
                  setValue("navEnheter", [] as any);
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
                      <Button
                        className={skjemastyles.kontaktperson_fjern_button}
                        variant="tertiary"
                        size="small"
                        type="button"
                        onClick={() => removeKontaktperson(index)}
                      >
                        <XMarkIcon fontSize="1.5rem" />
                      </Button>
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
                          placeholder={
                            isLoadingKontaktpersoner
                              ? "Laster enheter..."
                              : "Velg et eller flere områder"
                          }
                          label={"Område"}
                          {...register(`kontaktpersoner.${index}.navEnheter`, {
                            shouldUnregister: true,
                          })}
                          options={navEnheterOptions.filter((enhet) =>
                            valgteNavEnheter.includes(enhet.value),
                          )}
                        />
                        <TextField
                          size="small"
                          label="Beskrivelse"
                          placeholder="Unngå personopplysninger"
                          maxLength={67}
                          {...register(`kontaktpersoner.${index}.beskrivelse`, {
                            shouldUnregister: true,
                          })}
                        />
                      </div>
                    </div>
                  );
                })}
                <Button
                  className={skjemastyles.kontaktperson_button}
                  type="button"
                  size="small"
                  variant="tertiary"
                  onClick={() => appendKontaktperson({} as TiltaksgjennomforingKontaktperson)}
                >
                  <PlusIcon aria-label="Legg til ny kontaktperson" /> Legg til ny kontaktperson
                </Button>
              </div>
            </FormGroup>
          </div>
          <div className={skjemastyles.gray_container}>
            <TiltaksgjennomforingArrangorSkjema readOnly={eierIkkeGjennomforing} avtale={avtale} />
          </div>
        </div>
      </div>
    </div>
  );
};

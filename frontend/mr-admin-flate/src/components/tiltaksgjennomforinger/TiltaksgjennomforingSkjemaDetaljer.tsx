import { Button, Checkbox, TextField } from "@navikt/ds-react";
import {
  Avtale,
  Tiltaksgjennomforing,
  TiltaksgjennomforingOppstartstype,
} from "mulighetsrommet-api-client";
import skjemastyles from "../skjema/Skjema.module.scss";
import { useFieldArray, useFormContext } from "react-hook-form";
import { tilgjengelighetsstatusTilTekst } from "../../utils/Utils";
import { arenaOpphav, arrangorUnderenheterOptions } from "./TiltaksgjennomforingSkjemaConst";
import { useHentKontaktpersoner } from "../../api/ansatt/useHentKontaktpersoner";
import { useHentBetabrukere } from "../../api/ansatt/useHentBetabrukere";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { SokeSelect } from "../skjema/SokeSelect";
import { FormGroup } from "../skjema/FormGroup";
import { AvbrytTiltaksgjennomforing } from "./AvbrytTiltaksgjennomforing";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";
import { Separator } from "../detaljside/Metadata";

interface Props {
  tiltaksgjennomforing?: Tiltaksgjennomforing;
  avtale: Avtale;
  onClose: () => void;
}

export const TiltaksgjennomforingSkjemaDetaljer = ({
  tiltaksgjennomforing,
  avtale,
  onClose,
}: Props) => {
  const redigeringsModus = !!tiltaksgjennomforing;
  const { data: virksomhet } = useVirksomhet(avtale.leverandor.organisasjonsnummer || "");
  const { data: betabrukere } = useHentBetabrukere();

  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();

  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner } = useHentKontaktpersoner();

  const kontaktpersonerOption = () => {
    const options = kontaktpersoner?.map((kontaktperson) => ({
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

  const navEnheterOptions = avtale.navEnheter.map((enhet) => ({
    value: enhet.enhetsnummer,
    label: enhet.navn,
  }));

  return (
    <div className={skjemastyles.container}>
      <div className={skjemastyles.input_container}>
        <div className={skjemastyles.column}>
          <FormGroup>
            <TextField
              size="small"
              readOnly={arenaOpphav(tiltaksgjennomforing)}
              error={errors.navn?.message as string}
              label="Tiltaksnavn"
              autoFocus
              data-testid="tiltaksgjennomforingnavn-input"
              {...register("navn")}
            />
          </FormGroup>
          <Separator />
          <FormGroup>
            <TextField size="small" readOnly label={"Avtale"} value={avtale.navn || ""} />
          </FormGroup>
          <Separator />
          <FormGroup>
            <SokeSelect
              size="small"
              label="Oppstartstype"
              readOnly={arenaOpphav(tiltaksgjennomforing)}
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
                readOnly: arenaOpphav(tiltaksgjennomforing),
                ...register("startOgSluttDato.startDato"),
              }}
              til={{
                label: "Sluttdato",
                readOnly: arenaOpphav(tiltaksgjennomforing),
                ...register("startOgSluttDato.sluttDato"),
              }}
            />
            <Checkbox
              size="small"
              readOnly={arenaOpphav(tiltaksgjennomforing)}
              {...register("apenForInnsok")}
            >
              Åpen for innsøk
            </Checkbox>
            <Checkbox size="small" {...register("midlertidigStengt.erMidlertidigStengt")}>
              Midlertidig stengt
            </Checkbox>
            {watchErMidlertidigStengt && (
              <FraTilDatoVelger
                size="small"
                fra={{
                  label: "Stengt fra",
                  ...register("midlertidigStengt.stengtFra"),
                }}
                til={{
                  label: "Stengt til",
                  ...register("midlertidigStengt.stengtTil"),
                }}
              />
            )}
            <TextField
              size="small"
              readOnly={arenaOpphav(tiltaksgjennomforing)}
              error={errors.antallPlasser?.message as string}
              type="number"
              style={{
                width: "180px",
              }}
              label="Antall plasser"
              {...register("antallPlasser", {
                valueAsNumber: true,
              })}
            />
            {!arenaOpphav(tiltaksgjennomforing) && redigeringsModus ? (
              <AvbrytTiltaksgjennomforing onAvbryt={onClose} />
            ) : null}
          </FormGroup>
          <Separator />
          <FormGroup>
            <TextField
              readOnly
              size="small"
              label="Tilgjengelighetsstatus"
              description="Statusen vises til veileder i Modia"
              value={tilgjengelighetsstatusTilTekst(tiltaksgjennomforing?.tilgjengelighet)}
            />
            <TextField
              size="small"
              label="Estimert ventetid"
              description="Kommuniser estimert ventetid til veileder i Modia"
              maxLength={60}
              {...register("estimertVentetid")}
            />
          </FormGroup>
          <Separator />
          <FormGroup>
            <SokeSelect
              size="small"
              placeholder={isLoadingAnsatt ? "Laster..." : "Velg en"}
              label={"Administrator for gjennomføringen"}
              {...register("administrator")}
              options={AdministratorOptions(
                ansatt,
                tiltaksgjennomforing?.administrator,
                betabrukere,
              )}
              onClearValue={() => setValue("administrator", "")}
            />
          </FormGroup>
        </div>
        <div className={skjemastyles.vertical_separator} />
        <div className={skjemastyles.column}>
          <div className={skjemastyles.gray_container}>
            <FormGroup>
              <TextField
                size="small"
                readOnly
                label={"NAV-region"}
                value={avtale.navRegion?.navn || ""}
              />
              <ControlledMultiSelect
                size="small"
                placeholder={"Velg en"}
                label={"NAV-enheter (kontorer)"}
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
                        <SokeSelect
                          size="small"
                          placeholder={
                            isLoadingKontaktpersoner ? "Laster kontaktpersoner..." : "Velg en"
                          }
                          label={"Kontaktperson i NAV"}
                          {...register(`kontaktpersoner.${index}.navIdent`, {
                            shouldUnregister: true,
                          })}
                          options={kontaktpersonerOption()}
                        />
                        <ControlledMultiSelect
                          size="small"
                          placeholder={isLoadingKontaktpersoner ? "Laster enheter..." : "Velg en"}
                          label={"Område"}
                          {...register(`kontaktpersoner.${index}.navEnheter`, {
                            shouldUnregister: true,
                          })}
                          options={navEnheterOptions}
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
                  <PlusIcon /> Legg til ny kontaktperson
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
              <SokeSelect
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
                readOnly={!avtale.leverandor.organisasjonsnummer}
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
            </FormGroup>
          </div>
        </div>
      </div>
    </div>
  );
};

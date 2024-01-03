import { Textarea, TextField } from "@navikt/ds-react";
import {
  Avtale,
  Avtaletype,
  EmbeddedTiltakstype,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
  Opphav,
  Tiltakstype,
} from "mulighetsrommet-api-client";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import { useHentBetabrukere } from "../../api/ansatt/useHentBetabrukere";
import { useSokVirksomheter } from "../../api/virksomhet/useSokVirksomhet";
import { useVirksomhet } from "../../api/virksomhet/useVirksomhet";
import { addYear } from "../../utils/Utils";
import { Separator } from "../detaljside/Metadata";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import skjemastyles from "../skjema/Skjema.module.scss";
import { VirksomhetKontaktpersoner } from "../virksomhet/VirksomhetKontaktpersoner";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common/components/ControlledSokeSelect";
import { SelectOption } from "mulighetsrommet-frontend-common/components/SokeSelect";
import { MultiValue } from "react-select";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { FormGroup } from "../skjema/FormGroup";
import { getLokaleUnderenheterAsSelectOptions, underenheterOptions } from "./AvtaleSkjemaConst";
import { InferredAvtaleSchema } from "./AvtaleSchema";

const minStartdato = new Date(2000, 0, 1);

interface Props {
  tiltakstyper: Tiltakstype[];
  ansatt: NavAnsatt;
  avtale?: Avtale;
  enheter: NavEnhet[];
}

export function AvtaleSkjemaDetaljer({ tiltakstyper, ansatt, enheter, avtale }: Props) {
  const [sokLeverandor, setSokLeverandor] = useState(avtale?.leverandor?.organisasjonsnummer || "");
  const [valgtLeverandor, setValgtLeverandor] = useState<{ name?: string; value?: any }>({});
  const { data: leverandorVirksomheter = [] } = useSokVirksomheter(sokLeverandor);

  const { data: betabrukere } = useHentBetabrukere();

  const {
    register,
    formState: { errors },
    watch,
    setValue,
  } = useFormContext<InferredAvtaleSchema>();

  const watchedTiltakstype: EmbeddedTiltakstype | undefined = watch("tiltakstype");
  const arenaKode = watchedTiltakstype?.arenaKode;

  const watchedLeverandor = watch("leverandor");
  const { data: leverandorData } = useVirksomhet(watchedLeverandor);

  const underenheterForLeverandor = leverandorData?.underenheter ?? [];

  const arenaOpphav = avtale?.opphav === Opphav.ARENA;

  const navRegionerOptions = enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .map((enhet) => ({
      value: enhet.enhetsnummer,
      label: enhet.navn,
    }));

  const { startDato } = watch("startOgSluttDato");
  const sluttDatoFraDato = startDato ? new Date(startDato) : minStartdato;
  const sluttDatoTilDato = addYear(startDato ? new Date(startDato) : new Date(), 5);

  const leverandorOptions = () => {
    const options = leverandorVirksomheter.map((enhet) => ({
      value: enhet.organisasjonsnummer,
      label: `${enhet.navn} - ${enhet.organisasjonsnummer}`,
    }));

    // Fordi leverandør søk nulles ut når man velger leverandør legger vi til
    // den valgte leverandøren manuelt i lista her.
    if (valgtLeverandor?.name && valgtLeverandor?.value) {
      options.push({
        label: valgtLeverandor?.name,
        value: valgtLeverandor?.value,
      });
    }

    return options;
  };

  return (
    <div className={skjemastyles.container}>
      <div className={skjemastyles.input_container}>
        <div className={skjemastyles.column}>
          <FormGroup cols={avtale?.avtalenummer ? 2 : 1}>
            <TextField
              size="small"
              readOnly={arenaOpphav}
              error={errors.navn?.message}
              label="Avtalenavn"
              autoFocus
              {...register("navn")}
            />
            {avtale?.avtalenummer ? (
              <TextField size="small" readOnly label="Avtalenummer" value={avtale.avtalenummer} />
            ) : null}
          </FormGroup>
          <Separator />
          <FormGroup cols={2}>
            <ControlledSokeSelect
              size="small"
              readOnly={arenaOpphav}
              placeholder="Velg en"
              label={"Tiltakstype"}
              {...register("tiltakstype")}
              options={tiltakstyper.map((tiltakstype) => ({
                value: {
                  arenaKode: tiltakstype.arenaKode,
                  navn: tiltakstype.navn,
                  id: tiltakstype.id,
                },
                label: tiltakstype.navn,
              }))}
            />
            <ControlledSokeSelect
              size="small"
              readOnly={arenaOpphav}
              placeholder="Velg en"
              label={"Avtaletype"}
              {...register("avtaletype")}
              options={[
                {
                  value: Avtaletype.FORHAANDSGODKJENT,
                  label: "Forhåndsgodkjent avtale",
                },
                {
                  value: Avtaletype.RAMMEAVTALE,
                  label: "Rammeavtale",
                },
                {
                  value: Avtaletype.AVTALE,
                  label: "Avtale",
                },
              ]}
            />
          </FormGroup>
          <Separator />
          <FormGroup>
            <FraTilDatoVelger
              size="small"
              fra={{
                label: "Startdato",
                readOnly: arenaOpphav,
                fromDate: minStartdato,
                toDate: sluttDatoTilDato,
                ...register("startOgSluttDato.startDato"),
                format: "iso-string",
              }}
              til={{
                label: "Sluttdato",
                readOnly: arenaOpphav,
                fromDate: sluttDatoFraDato,
                toDate: sluttDatoTilDato,
                ...register("startOgSluttDato.sluttDato"),
                format: "iso-string",
              }}
            />
          </FormGroup>
          <Separator />
          <FormGroup>
            <TextField
              size="small"
              error={errors.url?.message}
              label="URL til avtale i Websak"
              {...register("url")}
            />
          </FormGroup>
          <Separator />
          {arenaKode && erAnskaffetTiltak(arenaKode) && (
            <>
              <FormGroup>
                <Textarea
                  size="small"
                  readOnly={arenaOpphav}
                  error={errors.prisbetingelser?.message}
                  label="Pris og betalingsinformasjon"
                  {...register("prisbetingelser")}
                />
              </FormGroup>
              <Separator />
            </>
          )}
          <FormGroup>
            <ControlledMultiSelect
              size="small"
              helpText="Bestemmer hvem som eier avtalen. Notifikasjoner sendes til administratorene."
              placeholder="Administratorer"
              label="Administratorer for avtalen"
              {...register("administratorer")}
              options={AdministratorOptions(ansatt, avtale?.administratorer, betabrukere)}
            />
          </FormGroup>
        </div>
        <div className={skjemastyles.vertical_separator} />
        <div className={skjemastyles.column}>
          <div className={skjemastyles.gray_container}>
            <FormGroup>
              <ControlledMultiSelect
                size="small"
                placeholder="Velg en"
                label={"NAV-regioner"}
                {...register("navRegioner")}
                additionalOnChange={(selectedOptions) => {
                  if (watch("navRegioner").length > 1) {
                    const alleLokaleUnderenheter = velgAlleLokaleUnderenheter(
                      selectedOptions,
                      enheter,
                    );
                    setValue("navEnheter", alleLokaleUnderenheter as [string, ...string[]]);
                  } else {
                    const alleLokaleUnderenheter = velgAlleLokaleUnderenheter(
                      selectedOptions,
                      enheter,
                    );
                    const navEnheter = watch("navEnheter").filter((enhet) =>
                      alleLokaleUnderenheter.includes(enhet),
                    );
                    setValue("navEnheter", navEnheter as [string, ...string[]]);
                  }
                }}
                options={navRegionerOptions}
              />
              <ControlledMultiSelect
                size="small"
                placeholder="Velg en"
                label={"NAV-enheter (kontorer)"}
                helpText="Bestemmer hvilke NAV-enheter som kan velges i gjennomføringene til avtalen."
                {...register("navEnheter")}
                options={getLokaleUnderenheterAsSelectOptions(watch("navRegioner"), enheter)}
              />
            </FormGroup>
          </div>
          <div className={skjemastyles.gray_container}>
            <FormGroup>
              <ControlledSokeSelect
                size="small"
                readOnly={arenaOpphav}
                placeholder="Skriv for å søke etter tiltaksarrangør"
                label={"Tiltaksarrangør hovedenhet"}
                {...register("leverandor")}
                onChange={(v) => {
                  setValgtLeverandor(v.target);
                }}
                onInputChange={(value) => {
                  setSokLeverandor(value);
                }}
                onClearValue={() => setValue("leverandor", "")}
                options={leverandorOptions()}
              />
              <ControlledMultiSelect
                size="small"
                placeholder="Velg underenhet for tiltaksarrangør"
                label={"Tiltaksarrangør underenhet"}
                helpText="Bestemmer hvilke arrangører som kan velges i gjennomføringene til avtalen."
                readOnly={!watchedLeverandor}
                {...register("leverandorUnderenheter")}
                options={underenheterOptions(underenheterForLeverandor)}
              />
            </FormGroup>
            {watchedLeverandor && !avtale?.leverandor?.slettet && (
              <FormGroup>
                <div className={skjemastyles.kontaktperson_container}>
                  <VirksomhetKontaktpersoner
                    title="Kontaktperson hos leverandøren"
                    orgnr={watchedLeverandor}
                    formValueName="leverandorKontaktpersonId"
                  />
                </div>
              </FormGroup>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function velgAlleLokaleUnderenheter(
  selectedOptions: MultiValue<SelectOption<string>>,
  enheter: NavEnhet[],
): string[] {
  const regioner = selectedOptions?.map((option) => option.value);
  const navEnheter = getLokaleUnderenheterAsSelectOptions(regioner, enheter).map(
    (option) => option.value,
  );
  return navEnheter;
}

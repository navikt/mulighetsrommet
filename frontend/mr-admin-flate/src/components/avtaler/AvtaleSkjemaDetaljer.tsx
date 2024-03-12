import { HGrid, Textarea, TextField } from "@navikt/ds-react";
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
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common/components/ControlledSokeSelect";
import { SelectOption } from "mulighetsrommet-frontend-common/components/SokeSelect";
import { useFormContext } from "react-hook-form";
import { MultiValue } from "react-select";
import { useAvtaleAdministratorer } from "../../api/ansatt/useAvtaleAdministratorer";
import { useMigrerteTiltakstyperForAvtaler } from "../../api/tiltakstyper/useMigrerteTiltakstyper";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import { addYear } from "../../utils/Utils";
import { Separator } from "../detaljside/Metadata";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import { FraTilDatoVelger } from "../skjema/FraTilDatoVelger";
import skjemastyles from "../skjema/Skjema.module.scss";
import { InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import { getLokaleUnderenheterAsSelectOptions } from "./AvtaleSkjemaConst";
import { AvtaleArrangorSkjema } from "./AvtaleArrangorSkjema";

const minStartdato = new Date(2000, 0, 1);

interface Props {
  tiltakstyper: Tiltakstype[];
  ansatt: NavAnsatt;
  avtale?: Avtale;
  enheter: NavEnhet[];
}

export function AvtaleSkjemaDetaljer({ tiltakstyper, ansatt, enheter, avtale }: Props) {
  const { data: migrerteTiltakstyper } = useMigrerteTiltakstyperForAvtaler();

  const { data: administratorer } = useAvtaleAdministratorer();

  const {
    register,
    formState: { errors },
    watch,
    setValue,
  } = useFormContext<InferredAvtaleSchema>();

  const watchedTiltakstype: EmbeddedTiltakstype | undefined = watch("tiltakstype");
  const arenaKode = watchedTiltakstype?.arenaKode;

  const valgtTiltakstypeFraArena = !migrerteTiltakstyper?.includes(watchedTiltakstype?.arenaKode);

  const arenaOpphavOgIngenEierskap = avtale?.opphav === Opphav.ARENA && valgtTiltakstypeFraArena;

  const navRegionerOptions = enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .map((enhet) => ({
      value: enhet.enhetsnummer,
      label: enhet.navn,
    }));

  const { startDato } = watch("startOgSluttDato");
  const sluttDatoFraDato = startDato ? new Date(startDato) : minStartdato;
  const sluttDatoTilDato = addYear(startDato ? new Date(startDato) : new Date(), 5);

  return (
    <div className={skjemastyles.container}>
      <div className={skjemastyles.input_container}>
        <div className={skjemastyles.column}>
          <FormGroup>
            <HGrid gap="4" columns={avtale?.avtalenummer ? 2 : 1}>
              <TextField
                size="small"
                readOnly={arenaOpphavOgIngenEierskap}
                error={errors.navn?.message}
                label="Avtalenavn"
                autoFocus
                {...register("navn")}
              />
              {avtale?.avtalenummer ? (
                <TextField size="small" readOnly label="Avtalenummer" value={avtale.avtalenummer} />
              ) : null}
            </HGrid>
          </FormGroup>
          <Separator />
          <FormGroup>
            <HGrid gap="4" columns={2}>
              <ControlledSokeSelect
                size="small"
                readOnly={arenaOpphavOgIngenEierskap}
                placeholder="Velg en"
                label={"Tiltakstype"}
                {...register("tiltakstype")}
                options={tiltakstyper.map((tiltakstype) => ({
                  value: {
                    arenaKode: tiltakstype.arenaKode,
                    navn: tiltakstype.navn,
                    id: tiltakstype.id,
                  },
                  label: !migrerteTiltakstyper?.includes(tiltakstype.arenaKode)
                    ? `${tiltakstype.navn} må opprettes i Arena`
                    : tiltakstype.navn,
                  isDisabled: !migrerteTiltakstyper?.includes(tiltakstype.arenaKode),
                }))}
              />
              <ControlledSokeSelect
                size="small"
                readOnly={arenaOpphavOgIngenEierskap}
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
            </HGrid>
          </FormGroup>
          <Separator />
          <FormGroup>
            <FraTilDatoVelger
              size="small"
              fra={{
                label: "Startdato",
                readOnly: arenaOpphavOgIngenEierskap,
                fromDate: minStartdato,
                toDate: sluttDatoTilDato,
                ...register("startOgSluttDato.startDato"),
                format: "iso-string",
              }}
              til={{
                label: "Sluttdato",
                readOnly: arenaOpphavOgIngenEierskap,
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
                  readOnly={arenaOpphavOgIngenEierskap}
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
              options={AdministratorOptions(ansatt, avtale?.administratorer, administratorer)}
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
            <AvtaleArrangorSkjema readOnly={arenaOpphavOgIngenEierskap} />
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

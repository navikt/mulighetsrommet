import { useAvtaleAdministratorer } from "@/api/ansatt/useAvtaleAdministratorer";
import { useMigrerteTiltakstyperForAvtaler } from "@/api/tiltakstyper/useMigrerteTiltakstyper";
import {
  Alert,
  Heading,
  HGrid,
  Loader,
  Select,
  Textarea,
  TextField,
  UNSAFE_Combobox,
} from "@navikt/ds-react";
import {
  Avtale,
  Avtaletype,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
  Opphav,
  Tiltakskode,
  TiltakskodeArena,
  Tiltakstype,
  Toggles,
} from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common/components/ControlledSokeSelect";
import { LabelWithHelpText } from "mulighetsrommet-frontend-common/components/label/LabelWithHelpText";
import { SelectOption } from "mulighetsrommet-frontend-common/components/SokeSelect";
import { DeepPartial, useFormContext } from "react-hook-form";
import { MultiValue } from "react-select";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import { useNusData } from "../../api/nusdata/useNusData";
import { erAnskaffetTiltak } from "../../utils/tiltakskoder";
import { addYear, avtaletypeTilTekst } from "../../utils/Utils";
import { Separator } from "../detaljside/Metadata";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "../redaksjonelt-innhold/AvtaleSchema";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "../skjema/FormGroup";
import skjemastyles from "../skjema/Skjema.module.scss";
import { AvtaleArrangorSkjema } from "./AvtaleArrangorSkjema";
import { getLokaleUnderenheterAsSelectOptions } from "./AvtaleSkjemaConst";

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
  } = useFormContext<DeepPartial<InferredAvtaleSchema>>();

  const watchedTiltakstype = watch("tiltakstype");
  const arenaKode = watchedTiltakstype?.arenaKode;

  const valgtTiltakstypeFraArena = !migrerteTiltakstyper?.includes(
    watchedTiltakstype?.arenaKode ?? "",
  );

  const arenaOpphavOgIngenEierskap = avtale?.opphav === Opphav.ARENA && valgtTiltakstypeFraArena;

  const navRegionerOptions = enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .map((enhet) => ({
      value: enhet.enhetsnummer,
      label: enhet.navn,
    }));

  const { startDato } = watch("startOgSluttDato") ?? {};
  const sluttDatoFraDato = startDato ? new Date(startDato) : minStartdato;
  const sluttDatoTilDato = addYear(startDato ? new Date(startDato) : new Date(), 5);

  return (
    <div className={skjemastyles.container}>
      <div className={skjemastyles.input_container}>
        <div className={skjemastyles.column}>
          <FormGroup>
            <TextField
              size="small"
              readOnly={arenaOpphavOgIngenEierskap}
              error={errors.navn?.message}
              label={avtaletekster.avtalenavnLabel}
              autoFocus
              {...register("navn")}
            />
          </FormGroup>

          <Separator />

          <FormGroup>
            <HGrid align="start" gap="4" columns={2}>
              {avtale?.avtalenummer ? (
                <TextField
                  size="small"
                  readOnly
                  label={avtaletekster.arenaAvtalenummerLabel}
                  value={avtale.avtalenummer}
                />
              ) : (
                <TextField
                  size="small"
                  readOnly
                  label={avtaletekster.lopenummerLabel}
                  value={avtale?.lopenummer}
                />
              )}
              <TextField
                size="small"
                placeholder="åå/12345"
                error={errors.websaknummer?.message}
                label={
                  <LabelWithHelpText
                    label={avtaletekster.websaknummerLabel}
                    helpTextTitle={avtaletekster.websaknummerHelpTextTitle}
                  >
                    I Websak skal det opprettes tre typer arkivsaker med egne saksnummer:
                    <ol>
                      <li>En sak for hver anskaffelse.</li>
                      <li>En sak for kontrakt/avtale med hver leverandør (Avtalesaken).</li>
                      <li>
                        En sak for oppfølging og forvaltning av avtale (Avtaleforvaltningssaken).
                      </li>
                    </ol>
                    Det er <b>2) Saksnummeret til Avtalesaken</b> som skal refereres til herfra.
                  </LabelWithHelpText>
                }
                {...register("websaknummer")}
              />
            </HGrid>
          </FormGroup>

          <Separator />

          <FormGroup>
            <HGrid gap="4" columns={2}>
              <ControlledSokeSelect
                size="small"
                readOnly={arenaOpphavOgIngenEierskap}
                placeholder="Velg en"
                label={avtaletekster.tiltakstypeLabel}
                {...register("tiltakstype")}
                onChange={(event) => {
                  const options = event.target.value?.arenaKode
                    ? avtaletypeOptions(event.target.value.arenaKode)
                    : [];
                  const avtaletype = options.length === 1 ? options[0].value : undefined;
                  setValue("avtaletype", avtaletype);
                }}
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
                label={avtaletekster.avtaletypeLabel}
                {...register("avtaletype")}
                options={arenaKode ? avtaletypeOptions(arenaKode) : []}
              />
            </HGrid>
            {watch("tiltakstype")?.arenaKode === "GRUFAGYRKE" ? <AvtaleKategoriVelger /> : null}
          </FormGroup>

          <Separator />

          <FormGroup>
            <Heading size="small" as="h3">
              Avtalens varighet
            </Heading>
            <HGrid columns={2}>
              <ControlledDateInput
                size="small"
                label={avtaletekster.startdatoLabel}
                readOnly={arenaOpphavOgIngenEierskap}
                fromDate={minStartdato}
                toDate={sluttDatoTilDato}
                {...register("startOgSluttDato.startDato")}
                format={"iso-string"}
              />
              <ControlledDateInput
                size="small"
                label={avtaletekster.sluttdatoLabel}
                readOnly={arenaOpphavOgIngenEierskap}
                fromDate={sluttDatoFraDato}
                toDate={sluttDatoTilDato}
                {...register("startOgSluttDato.sluttDato")}
                format={"iso-string"}
                invalidDatoEtterPeriode={
                  "Avtaleperioden kan ikke vare lenger enn 5 år for anskaffede tiltak"
                }
              />
            </HGrid>
          </FormGroup>

          <Separator />

          {arenaKode && erAnskaffetTiltak(arenaKode) && (
            <>
              <FormGroup>
                <Textarea
                  size="small"
                  readOnly={arenaOpphavOgIngenEierskap}
                  error={errors.prisbetingelser?.message}
                  label={avtaletekster.prisOgBetalingLabel}
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
              label={avtaletekster.administratorerForAvtalenLabel}
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
                label={avtaletekster.navRegionerLabel}
                {...register("navRegioner")}
                additionalOnChange={(selectedOptions) => {
                  if ((watch("navRegioner")?.length ?? 0) > 1) {
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
                    const navEnheter = watch("navEnheter")?.filter((enhet) =>
                      alleLokaleUnderenheter.includes(enhet ?? ""),
                    );
                    setValue("navEnheter", navEnheter as [string, ...string[]]);
                  }
                }}
                options={navRegionerOptions}
              />
              <ControlledMultiSelect
                size="small"
                placeholder="Velg en"
                label={avtaletekster.navEnheterLabel}
                helpText="Bestemmer hvilke NAV-enheter som kan velges i gjennomføringene til avtalen."
                {...register("navEnheter")}
                options={getLokaleUnderenheterAsSelectOptions(watch("navRegioner") ?? [], enheter)}
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
  return getLokaleUnderenheterAsSelectOptions(regioner, enheter).map((option) => option.value);
}

function avtaletypeOptions(arenaKode: TiltakskodeArena): { value: Avtaletype; label: string }[] {
  const forhaandsgodkjent = {
    value: Avtaletype.FORHAANDSGODKJENT,
    label: avtaletypeTilTekst(Avtaletype.FORHAANDSGODKJENT),
  };
  const rammeavtale = {
    value: Avtaletype.RAMMEAVTALE,
    label: avtaletypeTilTekst(Avtaletype.RAMMEAVTALE),
  };
  const avtale = {
    value: Avtaletype.AVTALE,
    label: avtaletypeTilTekst(Avtaletype.AVTALE),
  };
  const offentligOffentlig = {
    value: Avtaletype.OFFENTLIG_OFFENTLIG,
    label: avtaletypeTilTekst(Avtaletype.OFFENTLIG_OFFENTLIG),
  };
  switch (arenaKode) {
    case "ARBFORB":
    case "VASV":
      return [forhaandsgodkjent];
    case "AVKLARAG":
    case "INDOPPFAG":
    case "ARBRRHDAG":
    case "DIGIOPPARB":
    case "JOBBK":
      return [avtale, rammeavtale];
    case "GRUFAGYRKE":
    case "GRUPPEAMO":
      return [avtale, offentligOffentlig, rammeavtale];
    default:
      return [];
  }
}

function AvtaleKategoriVelger() {
  const { data: enableNusKategorier } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_NUSKATEGORIER,
  );

  const {
    setValue,
    watch,
    formState: { errors },
  } = useFormContext<InferredAvtaleSchema>();

  const { data, isLoading, isError } = useNusData(
    Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
    "2437",
  ); // TODO Ikke hardkode tiltakstype eller versjon

  if (!data?.data || isLoading) {
    return <Loader />;
  }

  if (isError) {
    return <Alert variant="error">Kunne ikke hente data fra SSB</Alert>;
  }

  if (!enableNusKategorier) {
    return null;
  }

  const utdanningsnivaa = watch("nusData.utdanningsnivaa");
  const utdanningskategorier = watch("nusData.utdanningskategorier", []) || [];

  const options =
    data.data
      .find(({ nivaa }) => nivaa === utdanningsnivaa)
      ?.kategorier.map(({ name, code }) => ({ label: name, value: code })) || [];

  return (
    <HGrid gap="4" columns={1}>
      <Select
        size="small"
        label="Utdanningsnivå"
        value={utdanningsnivaa}
        error={errors?.nusData?.utdanningsnivaa?.message}
        onChange={(utdanningsnivaa) => {
          setValue("nusData.utdanningsnivaa", utdanningsnivaa.target.value);
          setValue("nusData.utdanningskategorier", []);
        }}
      >
        <option value={""}>Velg utdanningsnivå...</option>
        {data.data.map(({ nivaa }) => (
          <option key={nivaa} value={nivaa}>
            {nivaa}
          </option>
        ))}
      </Select>
      <UNSAFE_Combobox
        clearButton
        size="small"
        label="Utdanningskategori"
        disabled={!utdanningsnivaa}
        isMultiSelect
        error={errors.nusData?.utdanningskategorier?.message}
        options={options}
        selectedOptions={utdanningskategorier.map((kategori) => ({
          value: kategori.code,
          label: kategori.name,
        }))}
        onToggleSelected={(option, isSelected) =>
          isSelected
            ? setValue("nusData.utdanningskategorier", [
                ...utdanningskategorier,
                {
                  code: option,
                  name: options.find((o) => o.value === option)?.label || "",
                },
              ])
            : setValue(
                "nusData.utdanningskategorier",
                utdanningskategorier.filter((o) => o.code !== option),
              )
        }
      ></UNSAFE_Combobox>
    </HGrid>
  );
}

import { useAvtaleAdministratorer } from "@/api/ansatt/useAvtaleAdministratorer";
import { useMigrerteTiltakstyperForAvtaler } from "@/api/tiltakstyper/useMigrerteTiltakstyper";
import { AvtaleAmoKategoriseringSkjema } from "@/components/amoKategorisering/AvtaleAmoKategoriseringSkjema";
import { HGrid, Textarea, TextField } from "@navikt/ds-react";
import {
  Avtale,
  Avtaletype,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
  Opphav,
  Tiltakskode,
  Tiltakstype,
  Toggles,
} from "mulighetsrommet-api-client";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common/components/ControlledSokeSelect";
import { LabelWithHelpText } from "mulighetsrommet-frontend-common/components/label/LabelWithHelpText";
import { SelectOption } from "mulighetsrommet-frontend-common/components/SokeSelect";
import { DeepPartial, useFormContext } from "react-hook-form";
import { MultiValue } from "react-select";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { erAnskaffetTiltak } from "@/utils/tiltakskoder";
import { avtaletypeTilTekst } from "@/utils/Utils";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { FormGroup } from "@/components/skjema/FormGroup";
import { AvtaleArrangorSkjema } from "./AvtaleArrangorSkjema";
import { AvtaleDatoContainer } from "./avtaledatoer/AvtaleDatoContainer";
import { getLokaleUnderenheterAsSelectOptions } from "./AvtaleSkjemaConst";
import { SkjemaDetaljerContainer } from "@/components/skjema/SkjemaDetaljerContainer";
import { SkjemaInputContainer } from "@/components/skjema/SkjemaInputContainer";
import { SkjemaKolonne } from "@/components/skjema/SkjemaKolonne";
import { VertikalSeparator } from "@/components/skjema/VertikalSeparator";

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

  const { data: enableGruppeAmoKategorier } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_GRUPPE_AMO_KATEGORIER,
  );

  const watchedTiltakstype = watch("tiltakstype");
  const tiltakskode = watchedTiltakstype?.tiltakskode;

  const arenaOpphavOgIngenEierskap = avtale?.opphav === Opphav.ARENA && !erMigrert(tiltakskode);

  const navRegionerOptions = enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .map((enhet) => ({
      value: enhet.enhetsnummer,
      label: enhet.navn,
    }));

  function erMigrert(tiltakskode?: Tiltakskode | null): boolean {
    if (!tiltakskode) return false;
    return migrerteTiltakstyper.includes(tiltakskode);
  }

  return (
    <SkjemaDetaljerContainer>
      <SkjemaInputContainer>
        <SkjemaKolonne>
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

          <FormGroup>
            <HGrid align="start" gap="4" columns={2}>
              <TextField
                size="small"
                readOnly
                label={avtaletekster.avtalenummerLabel}
                value={avtale?.avtalenummer}
                placeholder="Genereres automatisk ved opprettelse"
              />
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
          <FormGroup>
            <HGrid gap="4" columns={2}>
              <ControlledSokeSelect
                size="small"
                readOnly={arenaOpphavOgIngenEierskap}
                placeholder="Velg en"
                label={avtaletekster.tiltakstypeLabel}
                {...register("tiltakstype")}
                onChange={(event) => {
                  setValue("amoKategorisering", undefined);
                  const options = event.target.value?.tiltakskode
                    ? avtaletypeOptions(event.target.value.tiltakskode)
                    : [];
                  const avtaletype = options[0]?.value;
                  setValue("avtaletype", avtaletype);
                }}
                options={tiltakstyper.map((tiltakstype) => ({
                  value: {
                    navn: tiltakstype.navn,
                    id: tiltakstype.id,
                    tiltakskode: tiltakstype.tiltakskode,
                  },
                  label: !erMigrert(tiltakstype.tiltakskode)
                    ? `${tiltakstype.navn} må opprettes i Arena`
                    : tiltakstype.navn,
                  isDisabled: !erMigrert(tiltakstype.tiltakskode),
                }))}
              />
              <ControlledSokeSelect
                size="small"
                readOnly={arenaOpphavOgIngenEierskap}
                placeholder="Velg en"
                label={avtaletekster.avtaletypeLabel}
                {...register("avtaletype")}
                options={tiltakskode ? avtaletypeOptions(tiltakskode) : []}
              />
            </HGrid>
            {enableGruppeAmoKategorier &&
            tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ? (
              <AvtaleAmoKategoriseringSkjema />
            ) : null}
          </FormGroup>

          <AvtaleDatoContainer
            avtale={avtale}
            arenaOpphavOgIngenEierskap={arenaOpphavOgIngenEierskap}
          />

          {tiltakskode && erAnskaffetTiltak(tiltakskode) && (
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
        </SkjemaKolonne>
        <VertikalSeparator />
        <SkjemaKolonne>
          <div>
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
                velgAlle
                placeholder="Velg en"
                label={avtaletekster.navEnheterLabel}
                helpText="Bestemmer hvilke NAV-enheter som kan velges i gjennomføringene til avtalen."
                {...register("navEnheter")}
                options={getLokaleUnderenheterAsSelectOptions(watch("navRegioner") ?? [], enheter)}
              />
            </FormGroup>
          </div>
          <FormGroup>
            <AvtaleArrangorSkjema readOnly={arenaOpphavOgIngenEierskap} />
          </FormGroup>
        </SkjemaKolonne>
      </SkjemaInputContainer>
    </SkjemaDetaljerContainer>
  );
}

function velgAlleLokaleUnderenheter(
  selectedOptions: MultiValue<SelectOption<string>>,
  enheter: NavEnhet[],
): string[] {
  const regioner = selectedOptions?.map((option) => option.value);
  return getLokaleUnderenheterAsSelectOptions(regioner, enheter).map((option) => option.value);
}

function avtaletypeOptions(tiltakskode: Tiltakskode): { value: Avtaletype; label: string }[] {
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
  switch (tiltakskode) {
    case Tiltakskode.ARBEIDSFORBEREDENDE_TRENING:
    case Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET:
      return [forhaandsgodkjent];
    case Tiltakskode.OPPFOLGING:
    case Tiltakskode.JOBBKLUBB:
    case Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK:
    case Tiltakskode.AVKLARING:
    case Tiltakskode.ARBEIDSRETTET_REHABILITERING:
      return [avtale, rammeavtale];
    case Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING:
    case Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING:
      return [avtale, offentligOffentlig, rammeavtale];
    default:
      return [];
  }
}

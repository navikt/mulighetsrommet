import { useAvtaleAdministratorer } from "@/api/ansatt/useAvtaleAdministratorer";
import { AvtaleAmoKategoriseringSkjema } from "@/components/amoKategorisering/AvtaleAmoKategoriseringSkjema";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { FormGroup } from "@/components/skjema/FormGroup";
import { SkjemaDetaljerContainer } from "@/components/skjema/SkjemaDetaljerContainer";
import { SkjemaInputContainer } from "@/components/skjema/SkjemaInputContainer";
import { SkjemaKolonne } from "@/components/skjema/SkjemaKolonne";
import { VertikalSeparator } from "@/components/skjema/VertikalSeparator";
import { erAnskaffetTiltak } from "@/utils/tiltakskoder";
import { avtaletypeTilTekst } from "@/utils/Utils";
import {
  AvtaleDto,
  Avtaletype,
  NavAnsatt,
  NavEnhet,
  NavEnhetType,
  OpsjonsmodellKey,
  Tiltakskode,
  TiltakstypeDto,
} from "@mr/api-client";
import { ControlledSokeSelect } from "@mr/frontend-common/components/ControlledSokeSelect";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";
import { HGrid, Textarea, TextField } from "@navikt/ds-react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { MultiValue } from "react-select";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { AvtaleArrangorSkjema } from "./AvtaleArrangorSkjema";
import { AvtaleDatoContainer } from "./avtaledatoer/AvtaleDatoContainer";
import { getLokaleUnderenheterAsSelectOptions } from "./AvtaleSkjemaConst";
import { opsjonsmodeller } from "./opsjoner/opsjonsmodeller";
import { useCallback, useEffect } from "react";
import { AvtaleUtdanningslopSkjema } from "../utdanning/AvtaleUtdanningslopSkjema";

interface Props {
  tiltakstyper: TiltakstypeDto[];
  ansatt: NavAnsatt;
  avtale?: AvtaleDto;
  enheter: NavEnhet[];
}

export function AvtaleSkjemaDetaljer({ tiltakstyper, ansatt, enheter, avtale }: Props) {
  const { data: administratorer } = useAvtaleAdministratorer();

  const {
    register,
    formState: { errors },
    watch,
    setValue,
  } = useFormContext<DeepPartial<InferredAvtaleSchema>>();

  const watchedTiltakstype = watch("tiltakstype");
  const tiltakskode = watchedTiltakstype?.tiltakskode;

  function updateOpsjonsmodell(avtaletype: Avtaletype) {
    if (avtaletype === Avtaletype.FORHAANDSGODKJENT) {
      setValue("opsjonsmodellData", {
        opsjonsmodell: OpsjonsmodellKey.AVTALE_VALGFRI_SLUTTDATO,
        customOpsjonsmodellNavn: null,
        opsjonMaksVarighet: null,
      });
    } else {
      setValue("opsjonsmodellData", {
        opsjonsmodell: undefined,
        customOpsjonsmodellNavn: null,
        opsjonMaksVarighet: null,
      });
    }
  }

  const updateOpsjonsmodellClb = useCallback(updateOpsjonsmodell, [setValue]);
  const watchedOpsjonsmodell = watch("opsjonsmodellData.opsjonsmodell");
  useEffect(() => {
    if (avtale?.avtaletype && avtale.opsjonsmodellData && !avtale.opsjonsmodellData.opsjonsmodell) {
      if (!watchedOpsjonsmodell) {
        updateOpsjonsmodellClb(avtale?.avtaletype);
      }
    }
  }, [avtale, updateOpsjonsmodellClb, watchedOpsjonsmodell]);

  const navRegionerOptions = enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .map((enhet) => ({
      value: enhet.enhetsnummer,
      label: enhet.navn,
    }));

  const opsjonsmodell = opsjonsmodeller.find(
    (m) => m.value === watch("opsjonsmodellData.opsjonsmodell"),
  );

  return (
    <SkjemaDetaljerContainer>
      <SkjemaInputContainer>
        <SkjemaKolonne>
          <FormGroup>
            <TextField
              size="small"
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
                placeholder="Velg en"
                label={avtaletekster.tiltakstypeLabel}
                {...register("tiltakstype")}
                onChange={(event) => {
                  setValue("amoKategorisering", null);
                  const options = (event.target.value as { tiltakskode: string })?.tiltakskode
                    ? avtaletypeOptions(
                        (event.target.value as { tiltakskode: Tiltakskode }).tiltakskode,
                      )
                    : [];
                  const avtaletype = options[0]?.value;
                  if (options.length === 1) {
                    setValue("avtaletype", avtaletype);
                  } else {
                    setValue("avtaletype", undefined);
                  }
                  updateOpsjonsmodell(avtaletype);
                }}
                options={tiltakstyper.map((tiltakstype) => ({
                  value: {
                    navn: tiltakstype.navn,
                    id: tiltakstype.id,
                    tiltakskode: tiltakstype.tiltakskode,
                  },
                  label: tiltakstype.navn,
                }))}
              />
              <ControlledSokeSelect
                size="small"
                placeholder="Velg en"
                label={avtaletekster.avtaletypeLabel}
                {...register("avtaletype")}
                options={tiltakskode ? avtaletypeOptions(tiltakskode) : []}
              />
            </HGrid>
            {tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ? (
              <AvtaleAmoKategoriseringSkjema />
            ) : null}
            {tiltakskode === Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING ? (
              <AvtaleUtdanningslopSkjema />
            ) : null}
          </FormGroup>

          <AvtaleDatoContainer avtale={avtale} opsjonsmodell={opsjonsmodell} />

          {tiltakskode && erAnskaffetTiltak(tiltakskode) && (
            <>
              <FormGroup>
                <Textarea
                  size="small"
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
                helpText="Bestemmer hvilke Nav-enheter som kan velges i gjennomføringene til avtalen."
                {...register("navEnheter")}
                options={getLokaleUnderenheterAsSelectOptions(watch("navRegioner") ?? [], enheter)}
              />
            </FormGroup>
          </div>
          <FormGroup>
            <AvtaleArrangorSkjema readOnly={false} />
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

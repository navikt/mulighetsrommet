import { useAvtaleAdministratorer } from "@/api/ansatt/useAvtaleAdministratorer";
import { AvtaleAmoKategoriseringForm } from "@/components/amoKategorisering/AvtaleAmoKategoriseringForm";
import { AvtaleFormValues } from "@/schemas/avtale";
import { FormGroup } from "@/components/skjema/FormGroup";
import { avtaletypeTilTekst } from "@/utils/Utils";
import {
  Avtaletype,
  OpsjonLoggRegistrert,
  OpsjonsmodellType,
  OpsjonStatus,
  Prismodell,
  Tiltakskode,
  Toggles,
} from "@mr/api-client-v2";
import { ControlledSokeSelect } from "@mr/frontend-common/components/ControlledSokeSelect";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { HGrid, List, Textarea, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { AdministratorOptions } from "../skjema/AdministratorOptions";
import { ControlledMultiSelect } from "../skjema/ControlledMultiSelect";
import { AvtaleUtdanningslopForm } from "../utdanning/AvtaleUtdanningslopForm";
import { AvtaleArrangorForm } from "./AvtaleArrangorForm";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import AvtalePrisOgFaktureringForm from "./AvtalePrisOgFaktureringForm";
import { AvtaleVarighet } from "./AvtaleVarighet";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { useEffect } from "react";

export function AvtaleDetaljerForm({
  opsjonerRegistrert,
}: {
  opsjonerRegistrert?: OpsjonLoggRegistrert[];
  avtalenummer?: string;
}) {
  const { data: administratorer } = useAvtaleAdministratorer();
  const { data: ansatt } = useHentAnsatt();
  const { data: tiltakstyper } = useTiltakstyper();

  const {
    register,
    formState: { errors },
    getValues,
    setValue,
    watch,
  } = useFormContext<AvtaleFormValues>();

  const avtaletype = watch("avtaletype");
  const tiltakskode = watch("tiltakskode");
  const watchedAdministratorer = watch("administratorer");

  const { data: enableTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN,
    tiltakskode ? [tiltakskode] : [],
  );

  useEffect(() => {
    if (!tiltakskode) return;
    setValue("amoKategorisering", null);
    setValue("utdanningslop", null);

    const avtaletype = getAvtaletypeOptions(tiltakskode)[0]?.value;

    if (avtaletype) {
      setValue("avtaletype", avtaletype);
    }
  }, [setValue, tiltakskode]);

  useEffect(() => {
    if (!avtaletype) return;

    if (avtaletype === Avtaletype.FORHANDSGODKJENT) {
      setValue("opsjonsmodell", {
        type: OpsjonsmodellType.VALGFRI_SLUTTDATO,
        customOpsjonsmodellNavn: null,
        opsjonMaksVarighet: null,
      });

      setValue("prismodell", Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK);
    } else {
      setValue("opsjonsmodell", {
        type: getValues("opsjonsmodell.type"),
        customOpsjonsmodellNavn: null,
        opsjonMaksVarighet: null,
      });

      setValue("prismodell", null);
    }
  }, [avtaletype, getValues, setValue]);

  const antallOpsjonerUtlost = (
    opsjonerRegistrert?.filter((log) => log.status === OpsjonStatus.OPSJON_UTLOST) || []
  ).length;

  return (
    <TwoColumnGrid separator>
      <VStack>
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
              placeholder="åå/12345"
              error={errors.sakarkivNummer?.message}
              label={
                <LabelWithHelpText
                  label={avtaletekster.sakarkivNummerLabel}
                  helpTextTitle={avtaletekster.sakarkivNummerHelpTextTitle}
                >
                  I Public 360 skal det opprettes tre typer arkivsaker med egne saksnummer:
                  <List>
                    <List.Item>En sak for hver anskaffelse.</List.Item>
                    <List.Item>
                      En sak for kontrakt/avtale med hver leverandør (Avtalesaken).
                    </List.Item>
                    <List.Item>
                      En sak for oppfølging og forvaltning av avtale (Avtaleforvaltningssaken).
                    </List.Item>
                  </List>
                  Det er <b>2. Saksnummeret til Avtalesaken</b> som skal refereres til herfra.
                </LabelWithHelpText>
              }
              {...register("sakarkivNummer")}
            />
          </HGrid>
        </FormGroup>
        <FormGroup>
          <HGrid gap="4" columns={2}>
            <ControlledSokeSelect
              size="small"
              placeholder="Velg en"
              label={avtaletekster.tiltakstypeLabel}
              {...register("tiltakskode")}
              options={tiltakstyper.map((tiltakstype) => ({
                value: tiltakstype.tiltakskode as string,
                label: tiltakstype.navn,
              }))}
            />
            <ControlledSokeSelect
              size="small"
              placeholder="Velg en"
              readOnly={antallOpsjonerUtlost > 0}
              label={avtaletekster.avtaletypeLabel}
              {...register("avtaletype")}
              options={tiltakskode ? getAvtaletypeOptions(tiltakskode) : []}
            />
          </HGrid>
          {tiltakskode === Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING ? (
            <AvtaleAmoKategoriseringForm />
          ) : null}
          {tiltakskode === Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING ? (
            <AvtaleUtdanningslopForm />
          ) : null}
        </FormGroup>
        {avtaletype && (
          <FormGroup>
            <AvtaleVarighet antallOpsjonerUtlost={antallOpsjonerUtlost} />
          </FormGroup>
        )}
        {tiltakskode && enableTilsagn ? (
          <FormGroup>
            <AvtalePrisOgFaktureringForm tiltakskode={tiltakskode} />
          </FormGroup>
        ) : (
          <Textarea
            size="small"
            error={errors.prisbetingelser?.message}
            label={avtaletekster.prisOgBetalingLabel}
            {...register("prisbetingelser")}
          />
        )}
      </VStack>
      <VStack>
        <FormGroup>
          <ControlledMultiSelect
            size="small"
            helpText="Bestemmer hvem som eier avtalen. Notifikasjoner sendes til administratorene."
            placeholder="Administratorer"
            label={avtaletekster.administratorerForAvtalenLabel}
            {...register("administratorer")}
            options={AdministratorOptions(ansatt, watchedAdministratorer, administratorer)}
          />
        </FormGroup>
        <AvtaleArrangorForm readOnly={false} />
      </VStack>
    </TwoColumnGrid>
  );
}

function getAvtaletypeOptions(tiltakskode: Tiltakskode): { value: Avtaletype; label: string }[] {
  const forhandsgodkjent = {
    value: Avtaletype.FORHANDSGODKJENT,
    label: avtaletypeTilTekst(Avtaletype.FORHANDSGODKJENT),
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
      return [forhandsgodkjent];
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

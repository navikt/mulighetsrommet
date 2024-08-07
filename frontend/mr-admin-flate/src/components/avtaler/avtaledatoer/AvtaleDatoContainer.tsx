import { Heading, HGrid } from "@navikt/ds-react";
import { Avtale, Avtaletype, Toggles } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { addYear } from "@/utils/Utils";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "../../redaksjoneltInnhold/AvtaleSchema";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { FormGroup } from "../../skjema/FormGroup";
import { AvtaleVarighet } from "./AvtaleVarighet";
import { MIN_START_DATO_FOR_AVTALER, MAKS_AAR_FOR_AVTALER } from "../../../constants";

interface Props {
  avtale?: Avtale;
  arenaOpphavOgIngenEierskap: boolean;
}

export function AvtaleDatoContainer({ avtale, arenaOpphavOgIngenEierskap }: Props) {
  const { register, watch, setValue } = useFormContext<DeepPartial<InferredAvtaleSchema>>();
  const avtaletype = watch("avtaletype");
  const { startDato } = watch("startOgSluttDato") ?? {};
  const sluttDatoFraDato = startDato ? new Date(startDato) : MIN_START_DATO_FOR_AVTALER;
  const sluttDatoTilDato = addYear(
    startDato ? new Date(startDato) : new Date(),
    MAKS_AAR_FOR_AVTALER,
  );
  const { data: registrereOpsjonsmodellIsEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_REGISTRERE_OPSJONSMODELL,
  );

  function erForhandsgodkjent(avtaletype: Avtaletype): boolean {
    return [Avtaletype.FORHAANDSGODKJENT].includes(avtaletype);
  }

  useEffect(() => {
    if (!avtaletype) {
      setValue("opsjonsmodellData.opsjonsmodell", undefined);
      setValue("opsjonsmodellData.opsjonMaksVarighet", undefined);
      setValue("opsjonsmodellData.customOpsjonsmodellNavn", undefined);
    }
  }, [avtaletype]);

  if (!avtaletype) return null;

  if (avtaletype && (erForhandsgodkjent(avtaletype) || !registrereOpsjonsmodellIsEnabled)) {
    return (
      <FormGroup>
        <Heading size="small" as="h3">
          Avtalens varighet
        </Heading>
        <HGrid columns={2}>
          <ControlledDateInput
            size="small"
            label={avtaletekster.startdatoLabel}
            readOnly={arenaOpphavOgIngenEierskap}
            fromDate={MIN_START_DATO_FOR_AVTALER}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.startDato")}
            format={"iso-string"}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.valgfriSluttdatoLabel(avtaletype)}
            readOnly={arenaOpphavOgIngenEierskap}
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.sluttDato")}
            format={"iso-string"}
            invalidDatoEtterPeriode={`Avtaleperioden kan ikke vare lenger enn ${MAKS_AAR_FOR_AVTALER} år`}
          />
        </HGrid>
      </FormGroup>
    );
  } else if (registrereOpsjonsmodellIsEnabled) {
    return (
      <FormGroup>
        <AvtaleVarighet
          avtale={avtale}
          arenaOpphavOgIngenEierskap={arenaOpphavOgIngenEierskap}
          minStartDato={MIN_START_DATO_FOR_AVTALER}
          sluttDatoFraDato={sluttDatoFraDato}
          sluttDatoTilDato={sluttDatoTilDato}
          maksAar={MAKS_AAR_FOR_AVTALER}
        />
      </FormGroup>
    );
  } else {
    return null;
  }
}
